package com.sb.service;

import com.sb.model.Job;
import com.sb.model.JobType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JobHandlerRegistryTest {


    @Test
    void shouldFindGenerateReportHandler() {

        GenerateReportHandler generateReportHandler = new GenerateReportHandler();
        JobHandlerRegistry jobHandlerRegistry = new JobHandlerRegistry(List.of(generateReportHandler));

        Job job = new Job();
        job.setId("test-001");
        job.setType(JobType.GENERATE_REPORT);
        job.setPayload("{}");

        assertDoesNotThrow(() -> jobHandlerRegistry.handle(job));
    }

    @Test
    void shouldFindExportDataHandler() {

        ExportDataHandler exportDataHandler = new ExportDataHandler();
        JobHandlerRegistry jobHandlerRegistry = new JobHandlerRegistry(List.of(exportDataHandler));

        Job job = new Job();
        job.setId("test-002");
        job.setType(JobType.EXPORT_DATA);
        job.setPayload("{}");

        assertDoesNotThrow(() -> jobHandlerRegistry.handle(job));
    }

    @Test
    void shouldFindProcessInvoiceHandler() {

        ProcessInvoiceHandler processInvoiceHandler = new ProcessInvoiceHandler();
        JobHandlerRegistry jobHandlerRegistry = new JobHandlerRegistry(List.of(processInvoiceHandler));

        Job job = new Job();
        job.setId("test-003");
        job.setType(JobType.PROCESS_INVOICE);
        job.setPayload("{}");

        assertDoesNotThrow(() -> jobHandlerRegistry.handle(job));
    }

    @Test
    void shouldFindSendEmailHandler() {

        SendEmailHandler sendEmailHandler = new SendEmailHandler();
        JobHandlerRegistry jobHandlerRegistry = new JobHandlerRegistry(List.of(sendEmailHandler));

        Job job = new Job();
        job.setId("test-004");
        job.setType(JobType.SEND_EMAIL);
        job.setPayload("{}");

        assertDoesNotThrow(() -> jobHandlerRegistry.handle(job));
    }
}