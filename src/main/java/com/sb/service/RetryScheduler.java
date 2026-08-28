package com.sb.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.sb.repository.JobRepository;

import com.sb.model.JobStatus;

import java.time.Instant;

@Component
public class RetryScheduler {

    private final com.sb.repository.JobRepository repository;

    private final JobService jobService;

    public RetryScheduler(JobRepository repository, JobService jobService) {
        this.repository = repository;
        this.jobService = jobService;
    }

    @Scheduled(fixedDelayString = "${job.retry.scan-delay-ms:5000}")
    public void retryJobs() {

        Instant now = Instant.now();

        repository
                .findTop100ByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
                        JobStatus.RETRYING,
                        now
                )
                .forEach(job -> {
                    boolean published = jobService.publishDueJob(job.getId(), JobStatus.RETRYING);

                    if (published) {
                        System.out.println("Retry job published | jobId=" + job.getId());
                    }
                });

        repository
                .findTop100ByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
                        JobStatus.CREATED,
                        now
                )
                .forEach(job -> {
                    boolean published = jobService.publishDueJob(job.getId(), JobStatus.CREATED);
                    if (published) {
                        System.out.println("Scheduled job published | jobId=" + job.getId());
                    }
                });
    }
}