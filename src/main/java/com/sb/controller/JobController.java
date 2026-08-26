package com.sb.controller;


import com.sb.dto.CreateJobRequest;
import com.sb.dto.JobResponse;
import com.sb.service.JobService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    public ResponseEntity<JobResponse> createJob(
            @RequestHeader("Idempotency-Key")
            String idempotencyKey,
            @Valid
            @RequestBody
            CreateJobRequest request) {

        JobResponse response =
                jobService.createJob(
                        idempotencyKey,
                        request);

        return ResponseEntity.accepted()
                .body(response);
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<JobResponse> getJob(
            @PathVariable String jobId) {

        return ResponseEntity.ok(
                jobService.getJob(jobId));
    }
}
