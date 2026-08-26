package com.sb.service;

import com.sb.dto.CreateJobRequest;
import com.sb.dto.JobResponse;
import com.sb.exception.JobNotFoundException;
import com.sb.model.Job;
import com.sb.model.JobStatus;
import com.sb.repository.JobRepository;
import com.sb.utils.ClockProvider;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

@Service
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final ClockProvider clockProvider;

    public JobServiceImpl(com.sb.repository.JobRepository jobRepository, com.sb.utils.ClockProvider clockProvider) {
        this.jobRepository = jobRepository;
        this.clockProvider = clockProvider;
    }

    @Override
    public JobResponse createJob(
            String idempotencyKey,
            CreateJobRequest request) {

        Job existingJob =
                jobRepository.findByIdempotencyKey(idempotencyKey)
                        .orElse(null);

        if (existingJob != null) {
            return toResponse(existingJob);
        }

        var now = clockProvider.now();

        JobStatus initialStatus =
                request.scheduledAt() != null
                        && request.scheduledAt().isAfter(now)
                        ? JobStatus.CREATED
                        : JobStatus.QUEUED;

        //job save
        Job job = new Job();

        job.setIdempotencyKey(idempotencyKey);
        job.setType(request.type());
        job.setPriority(request.priority());
        job.setStatus(initialStatus);
        job.setCreatedAt(now);
        job.setUpdatedAt(now);
        job.setScheduledAt(request.scheduledAt());
        job.setRetryCount(0);
        job.setMaxRetries(request.maxRetries());
        job.setPayload(request.payload());

        Job savedJob = jobRepository.save(job);

        return toResponse(savedJob);
    }

    @Override
    public JobResponse getJob(String jobId) {

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() ->
                        new JobNotFoundException(jobId));

        return toResponse(job);
    }

    @Override
    public JobResponse cancelJob(String jobId) {
        return null;
    }

    private JobResponse toResponse(Job job) {

        return new JobResponse(
                job.getId(),
                job.getType(),
                job.getPriority(),
                job.getStatus(),
                job.getCreatedAt(),
                job.getScheduledAt(),
                job.getRetryCount(),
                job.getMaxRetries()
        );
    }
}
