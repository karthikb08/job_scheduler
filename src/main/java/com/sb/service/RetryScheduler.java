package com.sb.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class RetryScheduler {

    private final com.sb.repository.JobRepository repository;

    private final JobService jobService;

    public RetryScheduler(
            com.sb.repository.JobRepository repository,
            JobService jobService) {

        this.repository = repository;

        this.jobService = jobService;
    }

    @Scheduled(
            fixedDelayString =
                    "${job.retry.scan-delay-ms:2000}"
    )
    public void processDueJobs() {

        Instant now =
                Instant.now();

        /*
         * Retry jobs.
         */
        repository
                .findTop100ByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
                        com.sb.model.JobStatus.RETRYING,
                        now
                )
                .forEach(
                        job ->
                                jobService.publishDueJob(
                                        job.getId(),
                                        com.sb.model.JobStatus.RETRYING
                                )
                );

        /*
         * Scheduled jobs.
         */
        repository
                .findTop100ByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
                        com.sb.model.JobStatus.CREATED,
                        now
                )
                .forEach(
                        job ->
                                jobService.publishDueJob(
                                        job.getId(),
                                        com.sb.model.JobStatus.CREATED
                                )
                );
    }
}