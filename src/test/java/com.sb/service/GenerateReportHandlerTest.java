package com.sb.service;

import com.sb.model.JobType;
import com.sb.model.Job;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GenerateReportHandlerTest {

    private final GenerateReportHandler generateReportHandler = new GenerateReportHandler();

    @Test
    void shouldHandleGenerateReportJob() {

        Job job = new Job();

        job.setId("test-001");
        job.setType(JobType.GENERATE_REPORT);
        job.setPayload("{\"report\":\"monthly-sales\",\"format\":\"PDF\"}");

        assertDoesNotThrow(() -> generateReportHandler.handle(job));
    }

    @Test
    void shouldReturnGenerateReportJobType() {

        assertEquals(JobType.GENERATE_REPORT, generateReportHandler.supportedType());
    }
}