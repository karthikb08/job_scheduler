package com.sb.model;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "jobs")
public class Job {
    @org.springframework.data.annotation.Id
    private String id;

    @Indexed(unique = true)
    private String idempotencyKey;

    private JobType type;

    private JobPriority priority;

    private JobStatus status;

    private java.time.Instant createdAt;

    private java.time.Instant updatedAt;

    private int retryCount;

    private int maxRetries;

    private java.time.Instant scheduledAt;

    private String payload;

    private String errorMessage;

    private java.time.Instant startedAt;

    private java.time.Instant completedAt;
}
