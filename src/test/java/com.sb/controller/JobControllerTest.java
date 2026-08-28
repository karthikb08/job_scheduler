package com.sb.controller;

import com.sb.dto.CreateJobRequest;
import com.sb.model.Job;
import com.sb.model.JobPriority;
import com.sb.model.JobStatus;
import com.sb.model.JobType;
import com.sb.service.JobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;
import com.sb.service.JobMapper;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class JobControllerTest {

    private MockMvc mockMvc;
    private JobService jobService;
    private JobMapper jobMapper;

    @BeforeEach
    void setUp() {
        jobService = mock(JobService.class);
        jobMapper = mock(JobMapper.class);
        JobController controller = new JobController(jobService, jobMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    //Create Job
    @Test
    void shouldCreateJob() throws Exception {
        String id = UUID.randomUUID().toString();
        Job job = new Job();
        job.setId(id);
        job.setStatus(JobStatus.QUEUED);
        job.setType(JobType.GENERATE_REPORT);
        job.setPriority(JobPriority.HIGH);
        when(jobService.create(
                any(CreateJobRequest.class),
                anyString()
        )).thenReturn(job);
        mockMvc.perform(
                        post("/api/v1/jobs")
                                .header("Idempotency-Key", UUID.randomUUID().toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\n  \"type\": \"GENERATE_REPORT\",\n  \"priority\": \"HIGH\"\n}\n")
                )
                .andDo(result -> {
                    System.out.println("========================================");
                    System.out.println("CREATE JOB TEST");
                    System.out.println("STATUS = " + result.getResponse().getStatus());
                    System.out.println("BODY = " + result.getResponse().getContentAsString());
                    System.out.println("========================================");
                })
                .andExpect(status().isCreated());
        verify(jobService).create(any(CreateJobRequest.class), anyString());
    }

    //Get Job
    @Test
    void shouldGetJob() throws Exception {
        String id = UUID.randomUUID().toString();
        Job job = new Job();
        job.setId(id);
        job.setStatus(JobStatus.COMPLETED);
        job.setType(JobType.GENERATE_REPORT);
        job.setPriority(JobPriority.HIGH);
        when(jobService.get(id)).thenReturn(job);
        mockMvc.perform(
                        get(
                                "/api/v1/jobs/{id}",
                                id
                        )
                )
                .andDo(result -> {
                    System.out.println("GET STATUS : " + result.getResponse().getStatus());
                    System.out.println("GET BODY   : " + result.getResponse().getContentAsString());
                })
                .andExpect(status().isOk());
        verify(jobService).get(id);
    }

    //Cancel Job
    @Test
    void shouldCancelQueuedJob() throws Exception {
        String id = UUID.randomUUID().toString();
        Job job = new Job();
        job.setId(id);
        job.setStatus(JobStatus.CANCELLED);
        job.setType(JobType.GENERATE_REPORT);
        job.setPriority(JobPriority.HIGH);
        when(jobService.cancel(id)).thenReturn(job);
        mockMvc.perform(delete("/api/v1/jobs/{id}", id))
                .andDo(result -> {
                    System.out.println("DELETE STATUS : " + result.getResponse().getStatus());
                    System.out.println("DELETE BODY   : " + result.getResponse().getContentAsString());
                })
                .andExpect(status().isOk());
        verify(jobService).cancel(id);
    }
}