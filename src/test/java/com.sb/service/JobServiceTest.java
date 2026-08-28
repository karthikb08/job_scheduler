package com.sb.service;

import com.sb.model.Job;
import com.sb.model.JobPriority;
import com.sb.model.JobStatus;
import com.sb.model.JobType;
import com.sb.dto.CreateJobRequest;
import com.sb.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepository repository;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private JobPublisher publisher;

    @Mock
    private JobHandlerRegistry handlerRegistry;

    @InjectMocks
    private JobService jobService;

    private Job job;

    @BeforeEach
    void setUp() {
        job = createJob();
    }

    @Test
    void shouldReturnExistingJobForSameIdempotencyKey() {

        when(
                repository.findByIdempotencyKey(
                        "same-key"
                )
        ).thenReturn(
                Optional.of(job)
        );

        CreateJobRequest request =
                new CreateJobRequest(
                        JobType.GENERATE_REPORT,
                        JobPriority.HIGH,
                        3,
                        null,
                        "test-key"
                );

        Job result = jobService.create(request, "same-key");

        assertSame(job, result);
        verify(repository, never()).save(any(Job.class));
        verify(publisher, never()).publish(any());
    }

    @Test
    void shouldCreateJobWithDefaultRetries() {

        CreateJobRequest request =
                new CreateJobRequest(
                        JobType.GENERATE_REPORT,
                        JobPriority.HIGH,
                        null,
                        null,
                        "{\"report\":\"monthly-sales\"}"
                );

        when(
                repository.findByIdempotencyKey(
                        "new-key"
                )
        ).thenReturn(
                Optional.empty()
        );

        when(
                repository.save(any(Job.class))
        ).thenAnswer(invocation -> {

            Job saved =
                    invocation.getArgument(0);

            if (saved.getId() == null) {
                saved.setId(
                        UUID.randomUUID().toString()
                );
            }

            return saved;
        });

        when(repository.findById(anyString())).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);

            Job found = new Job();

            found.setId(id);

            found.setType(JobType.GENERATE_REPORT);
            found.setPriority(JobPriority.HIGH);
            found.setStatus(JobStatus.QUEUED);
            found.setCreatedAt(Instant.now());
            found.setRetryCount(0);
            found.setMaxRetries(3);
            return Optional.of(found);
        });

        Job result =
                jobService.create(
                        request,
                        "new-key"
                );

        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(JobType.GENERATE_REPORT, result.getType());
        assertEquals(JobPriority.HIGH, result.getPriority());
        assertEquals(3, result.getMaxRetries());
        assertEquals(0, result.getRetryCount());

        verify(repository)
                .save(any(Job.class));
    }

    @Test
    void shouldCreateScheduledJobWithoutImmediatelyPublishing() {

        Instant future =
                Instant.now().plusSeconds(60);

        CreateJobRequest request =
                new CreateJobRequest(
                        JobType.EXPORT_DATA,
                        JobPriority.LOW,
                        5,
                        future,
                        "{\"fileType\":\"CSV\"}"
                );

        when(
                repository.findByIdempotencyKey(
                        "scheduled-key"
                )
        ).thenReturn(Optional.empty());

        when(
                repository.save(any(Job.class))
        ).thenAnswer(invocation -> {

            Job saved =
                    invocation.getArgument(0);

            if (saved.getId() == null) {
                saved.setId(
                        UUID.randomUUID().toString()
                );
            }

            return saved;
        });

        Job result =
                jobService.create(
                        request,
                        "scheduled-key"
                );

        assertNotNull(result);
        assertEquals(JobType.EXPORT_DATA, result.getType());
        assertEquals(JobPriority.LOW, result.getPriority());
        assertEquals(JobStatus.CREATED, result.getStatus());
        assertEquals(5, result.getMaxRetries());
        assertEquals(future, result.getScheduledAt());

        verify(repository).save(any(Job.class));
        verify(publisher, never()).publish(any(Job.class));}

    @Test
    void shouldClaimQueuedJob() {

        Job runningJob = createJob();

        runningJob.setStatus(
                JobStatus.RUNNING
        );

        when(
                mongoTemplate.findAndModify(
                        any(Query.class),
                        any(Update.class),
                        any(FindAndModifyOptions.class),
                        eq(Job.class)
                )
        ).thenReturn(runningJob);

        Job result = jobService.claimForExecution(runningJob.getId());
        assertNotNull(result);
        assertEquals(JobStatus.RUNNING, result.getStatus());
        verify(mongoTemplate)
                .findAndModify(
                        any(Query.class),
                        any(Update.class),
                        any(FindAndModifyOptions.class),
                        eq(Job.class)
                );
    }

    @Test
    void shouldCompleteSuccessfulJob() {

        job.setStatus(JobStatus.RUNNING);

        doNothing().when(handlerRegistry).handle(job);

        jobService.execute(job);

        verify(handlerRegistry).handle(job);

        verify(mongoTemplate)
                .updateFirst(
                        any(Query.class),
                        any(Update.class),
                        eq(Job.class)
                );
    }

    @Test
    void shouldScheduleRetryWhenJobFails() {

        job.setStatus(
                JobStatus.RUNNING
        );

        job.setRetryCount(0);
        job.setMaxRetries(3);

        doThrow(
                new RuntimeException(
                        "Temporary failure"
                )
        )
                .when(handlerRegistry)
                .handle(job);

        jobService.execute(job);

        verify(mongoTemplate)
                .updateFirst(
                        any(Query.class),
                        any(Update.class),
                        eq(Job.class)
                );
    }

    @Test
    void shouldMarkJobFailedWhenMaximumRetriesReached() {

        job.setStatus(
                JobStatus.RUNNING
        );

        job.setRetryCount(3);
        job.setMaxRetries(3);

        doThrow(
                new RuntimeException(
                        "Permanent failure"
                )
        )
                .when(handlerRegistry)
                .handle(job);

        jobService.execute(job);

        ArgumentCaptor<Update> captor =
                ArgumentCaptor.forClass(
                        Update.class
                );

        verify(mongoTemplate)
                .updateFirst(
                        any(Query.class),
                        captor.capture(),
                        eq(Job.class)
                );
        String update = captor.getValue().toString();
        assertTrue(update.contains("FAILED"));
    }

    @Test
    void shouldCancelQueuedJob() {

        Job cancelledJob =
                createJob();

        cancelledJob.setStatus(
                JobStatus.CANCELLED
        );

        when(
                mongoTemplate.findAndModify(
                        any(Query.class),
                        any(Update.class),
                        any(FindAndModifyOptions.class),
                        eq(Job.class)
                )
        ).thenReturn(cancelledJob);

        Job result =
                jobService.cancel(
                        cancelledJob.getId()
                );

        assertEquals(JobStatus.CANCELLED, result.getStatus());
    }

    @Test
    void shouldPublishDueRetryJob() {

        Job queued =
                createJob();

        queued.setStatus(
                JobStatus.QUEUED
        );

        when(
                mongoTemplate.findAndModify(
                        any(Query.class),
                        any(Update.class),
                        any(FindAndModifyOptions.class),
                        eq(Job.class)
                )
        ).thenReturn(queued);

        boolean result = jobService.publishDueJob(queued.getId(), JobStatus.RETRYING);

        assertTrue(result);
        verify(publisher).publish(queued);
    }

    private Job createJob() {

        Job job = new Job();

        job.setId(UUID.randomUUID().toString());
        job.setType(JobType.GENERATE_REPORT);
        job.setPriority(JobPriority.HIGH);

        job.setStatus(JobStatus.QUEUED);
        job.setCreatedAt(Instant.now());

        job.setRetryCount(0);

        job.setMaxRetries(3);

        return job;
    }
}