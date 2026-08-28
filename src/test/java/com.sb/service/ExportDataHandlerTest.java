package com.sb.service;

import com.sb.model.JobType;
import com.sb.model.Job;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExportDataHandlerTest {

    private final ExportDataHandler exportDataHandler = new ExportDataHandler();

    @Test
    void shouldHandleExportDataJob() {

        Job job = new Job();

        job.setId("export-test-job-001");
        job.setType(JobType.EXPORT_DATA);
        job.setPayload(
                "{\"format\":\"CSV\",\"table\":\"customers\"}"
        );

        assertDoesNotThrow(() -> exportDataHandler.handle(job));
    }

    @Test
    void shouldReturnExportDataJobType() {

        assertEquals(JobType.EXPORT_DATA, exportDataHandler.supportedType());
    }
}