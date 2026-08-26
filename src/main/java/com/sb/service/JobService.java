package com.sb.service;

import com.sb.model.Job;
import com.sb.model.JobStatus;
import com.sb.dto.CreateJobRequest;
import com.sb.exception.InvalidJobStateException;
import com.sb.exception.JobNotFoundException;
import com.sb.repository.JobRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class JobService {

    private static final int DEFAULT_MAX_RETRIES = 3;

    private final JobRepository repository;

    private final MongoTemplate mongoTemplate;

    private final JobPublisher publisher;

    private final JobHandlerRegistry handlerRegistry;

    public JobService(
            JobRepository repository,
            MongoTemplate mongoTemplate,
            JobPublisher publisher,
            JobHandlerRegistry handlerRegistry) {

        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
        this.publisher = publisher;
        this.handlerRegistry = handlerRegistry;
    }

    public Job create(
            CreateJobRequest request,
            String idempotencyKey) {

        /*
         * First check.
         *
         * Fast path for duplicate requests.
         */
        var existing =
                repository.findByIdempotencyKey(idempotencyKey);

        if (existing.isPresent()) {
            return existing.get();
        }

        Job job = new Job();

        job.setId(UUID.randomUUID().toString());

        job.setType(request.type());

        job.setPriority(request.priority());

        job.setStatus(JobStatus.CREATED);

        job.setCreatedAt(Instant.now());

        job.setRetryCount(0);

        job.setMaxRetries(
                request.maxRetries() == null
                        ? DEFAULT_MAX_RETRIES
                        : request.maxRetries()
        );

        job.setScheduledAt(request.scheduledAt());

        job.setIdempotencyKey(idempotencyKey);

        try {

            /*
             * Unique MongoDB index protects
             * concurrent duplicate requests.
             */
            Job saved = repository.save(job);

            /*
             * Immediate jobs are queued now.
             */
            if (saved.getScheduledAt() == null
                    || !saved.getScheduledAt()
                    .isAfter(Instant.now())) {

                queue(saved.getId());

                return get(saved.getId());
            }

            /*
             * Future scheduled job remains CREATED.
             * RetryScheduler will queue it later.
             */
            return saved;

        } catch (DuplicateKeyException e) {

            /*
             * Race condition:
             *
             * Request A and B both checked the key.
             * Both didn't find it.
             * MongoDB unique index allows only one.
             */
            return repository
                    .findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> e);
        }
    }

    public Job get(String id) {

        return repository
                .findById(id)
                .orElseThrow(
                        () -> new JobNotFoundException(id)
                );
    }

    public Job cancel(String id) {

        Query query =
                Query.query(
                        Criteria.where("_id")
                                .is(id)
                                .and("status")
                                .is(JobStatus.QUEUED)
                );

        Update update =
                new Update()
                        .set(
                                "status",
                                JobStatus.CANCELLED
                        )
                        .set(
                                "cancelledAt",
                                Instant.now()
                        );

        Job cancelled =
                mongoTemplate.findAndModify(
                        query,
                        update,
                        FindAndModifyOptions.options()
                                .returnNew(true),
                        Job.class
                );

        if (cancelled != null) {
            return cancelled;
        }

        Job current = get(id);

        if (current.getStatus()
                == JobStatus.CANCELLED) {

            return current;
        }

        throw new InvalidJobStateException(
                "Job " + id
                        + " cannot be cancelled from status "
                        + current.getStatus()
        );
    }

    public boolean queue(String id) {

        Query query =
                Query.query(
                        Criteria.where("_id")
                                .is(id)
                                .and("status")
                                .is(JobStatus.CREATED)
                );

        Update update =
                new Update()
                        .set(
                                "status",
                                JobStatus.QUEUED
                        )
                        .set(
                                "scheduledAt",
                                null
                        );

        Job queued =
                mongoTemplate.findAndModify(
                        query,
                        update,
                        FindAndModifyOptions.options()
                                .returnNew(true),
                        Job.class
                );

        if (queued == null) {
            return false;
        }

        publisher.publish(queued);

        return true;
    }

    /*
     * Very important:
     *
     * QUEUED -> RUNNING is atomic.
     *
     * This protects against duplicate Kafka delivery.
     */
    public Job claimForExecution(String id) {

        Query query =
                Query.query(
                        Criteria.where("_id")
                                .is(id)
                                .and("status")
                                .is(JobStatus.QUEUED)
                );

        Update update =
                new Update()
                        .set(
                                "status",
                                JobStatus.RUNNING
                        )
                        .set(
                                "startedAt",
                                Instant.now()
                        );

        return mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options()
                        .returnNew(true),
                Job.class
        );
    }

    public void execute(Job job) {

        try {

            System.out.println(
                    "Executing job "
                            + job.getId()
                            + " type="
                            + job.getType()
                            + " priority="
                            + job.getPriority()
            );

            handlerRegistry.handle(job);

            Query query =
                    Query.query(
                            Criteria.where("_id")
                                    .is(job.getId())
                                    .and("status")
                                    .is(JobStatus.RUNNING)
                    );

            Update update =
                    new Update()
                            .set(
                                    "status",
                                    JobStatus.COMPLETED
                            )
                            .set(
                                    "completedAt",
                                    Instant.now()
                            )
                            .set(
                                    "lastError",
                                    null
                            );

            mongoTemplate.updateFirst(
                    query,
                    update,
                    Job.class
            );

            System.out.println(
                    "Job completed: "
                            + job.getId()
            );

        } catch (Exception e) {

            System.out.println(
                    "Job failed: "
                            + job.getId()
                            + " error="
                            + e.getMessage()
            );

            handleFailure(job, e);
        }
    }

    private void handleFailure(
            Job job,
            Exception exception) {

        Query query =
                Query.query(
                        Criteria.where("_id")
                                .is(job.getId())
                                .and("status")
                                .is(JobStatus.RUNNING)
                );

        int nextRetry =
                job.getRetryCount() + 1;

        if (nextRetry <= job.getMaxRetries()) {

            long delay =
                    retryDelayMillis(nextRetry);

            Update update =
                    new Update()
                            .set(
                                    "status",
                                    JobStatus.RETRYING
                            )
                            .set(
                                    "retryCount",
                                    nextRetry
                            )
                            .set(
                                    "scheduledAt",
                                    Instant.now()
                                            .plusMillis(delay)
                            )
                            .set(
                                    "lastError",
                                    safeError(exception)
                            );

            mongoTemplate.updateFirst(
                    query,
                    update,
                    Job.class
            );

            System.out.println(
                    "Job scheduled for retry: "
                            + job.getId()
                            + " retry="
                            + nextRetry
            );

        } else {

            Update update =
                    new Update()
                            .set(
                                    "status",
                                    JobStatus.FAILED
                            )
                            .set(
                                    "retryCount",
                                    nextRetry
                            )
                            .set(
                                    "lastError",
                                    safeError(exception)
                            );

            mongoTemplate.updateFirst(
                    query,
                    update,
                    Job.class
            );

            System.out.println(
                    "Job permanently failed: "
                            + job.getId()
            );
        }
    }

    public boolean publishDueJob(
            String id,
            JobStatus expectedStatus) {

        Query query =
                Query.query(
                        Criteria.where("_id")
                                .is(id)
                                .and("status")
                                .is(expectedStatus)
                                .and("scheduledAt")
                                .lte(Instant.now())
                );

        Update update =
                new Update()
                        .set(
                                "status",
                                JobStatus.QUEUED
                        )
                        .set(
                                "scheduledAt",
                                null
                        );

        Job queued =
                mongoTemplate.findAndModify(
                        query,
                        update,
                        FindAndModifyOptions.options()
                                .returnNew(true),
                        Job.class
                );

        if (queued == null) {
            return false;
        }

        publisher.publish(queued);

        return true;
    }

    private long retryDelayMillis(
            int retryNumber) {

        /*
         * Retry 1 = 2 seconds
         * Retry 2 = 4 seconds
         * Retry 3 = 8 seconds
         */
        return Duration
                .ofSeconds(
                        2L *
                                (1L << Math.min(
                                        retryNumber - 1,
                                        6
                                ))
                )
                .toMillis();
    }

    private String safeError(Exception exception) {

        String message =
                exception.getMessage() == null
                        ? exception.getClass()
                        .getSimpleName()
                        : exception.getMessage();

        return message.length() > 1000
                ? message.substring(0, 1000)
                : message;
    }
}