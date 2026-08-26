package com.sb.dto;


import com.sb.model.JobPriority;
import com.sb.model.JobType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CreateJobRequest ( @NotNull

    JobType type,
    @NotNull
    JobPriority priority,
    @Min(
    value = 0,
    message = "maxRetries cannot be negative"
    )
     @Max(
     value = 20,
     message = "maxRetries cannot exceed 20"
    )
    Integer maxRetries,
    Instant scheduledAt,
    String payload
) {}