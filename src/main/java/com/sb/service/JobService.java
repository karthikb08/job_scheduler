package com.sb.service;

import com.sb.dto.CreateJobRequest;
import com.sb.dto.JobResponse;

public interface JobService {

    JobResponse createJob(
            String idempotencyKey,
            CreateJobRequest request
    );

    JobResponse getJob(String jobId);

    JobResponse cancelJob(String jobId);
}
