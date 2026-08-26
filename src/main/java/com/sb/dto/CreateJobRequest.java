package com.sb.dto;


import com.sb.model.JobPriority;
import com.sb.model.JobType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CreateJobRequest ( @NotNull

    JobType type,
    @NotNull
    JobPriority priority,
    @Min(0)
    int maxRetries,
    Instant scheduledAt,
    String payload
) {}