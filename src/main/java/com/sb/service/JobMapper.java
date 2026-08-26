package com.sb.service;

import com.sb.model.Job;
import com.sb.dto.JobResponse;
import org.springframework.stereotype.Component;

@Component
public class JobMapper {

    public JobResponse toResponse(Job job) {

        return new JobResponse(
                job.getId(),
                job.getType(),
                job.getPriority(),
                job.getStatus(),
                job.getCreatedAt(),
                job.getRetryCount(),
                job.getMaxRetries(),
                job.getScheduledAt(),
                job.getLastError()
        );
    }
}