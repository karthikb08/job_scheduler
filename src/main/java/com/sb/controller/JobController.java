package com.sb.controller;

import com.sb.dto.CreateJobRequest;
import com.sb.dto.JobResponse;
import com.sb.service.JobMapper;
import com.sb.service.JobService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    private final JobService jobService;
    private final JobMapper mapper;

    public JobController(JobService jobService,JobMapper mapper) {
        this.jobService = jobService;
        this.mapper = mapper;
    }

    //Create Job
    @PostMapping
    public ResponseEntity<JobResponse> submit(
            @RequestHeader("Idempotency-Key")
            String idempotencyKey,
            @Valid
            @RequestBody
            CreateJobRequest request) {

        if (idempotencyKey.isBlank()
                || idempotencyKey.length() > 200) {

            throw new IllegalArgumentException(
                    "Idempotency-Key must contain "
                            + "1-200 characters"
            );
        }

        var job = jobService.create(request, idempotencyKey);

        return ResponseEntity.created(URI.create("/api/v1/jobs/" + job.getId()))
                .body(mapper.toResponse(job));
    }

   //Get Job
    @GetMapping("/{id}")
    public JobResponse get(@PathVariable String id) {
        return mapper.toResponse(jobService.get(id));
    }

    //Cancel Job
    @DeleteMapping("/{id}")
    public JobResponse cancel(@PathVariable String id) {
        return mapper.toResponse(jobService.cancel(id));
    }
}