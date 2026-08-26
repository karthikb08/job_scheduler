package com.sb.kafka;

import com.sb.model.JobPriority;
import com.sb.model.JobType;

public record JobMessage(
        String jobId,
        JobType jobType,
        JobPriority priority
) {
}