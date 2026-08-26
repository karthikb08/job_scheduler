package com.sb.dto;

import com.sb.model.JobPriority;
import com.sb.model.JobStatus;
import com.sb.model.JobType;

import java.time.Instant;

public record JobResponse(

        String id,

        JobType type,

        JobPriority priority,

        JobStatus status,

        Instant createdAt,

        Instant scheduledAt,

        int retryCount,

        int maxRetries
) {
}