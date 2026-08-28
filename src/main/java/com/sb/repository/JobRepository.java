package com.sb.repository;

import com.sb.model.Job;
import com.sb.model.JobStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface JobRepository extends MongoRepository<Job, String> {

    Optional<Job> findByIdempotencyKey(String idempotencyKey);
    List<Job> findTop100ByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(JobStatus status, Instant scheduledAt);
}
