package com.sb.service;

import com.sb.model.JobType;
import com.sb.model.Job;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProcessInvoiceHandlerTest {

    private final ProcessInvoiceHandler processInvoiceHandler = new ProcessInvoiceHandler();

    @Test
    void shouldHandleProcessInvoiceJob() {

        Job job = new Job();

        job.setId("test-process-job-job-001");
        job.setType(JobType.PROCESS_INVOICE);
        job.setPayload("{\"invoiceId\":\"INV-1001\"}");

        assertDoesNotThrow(() -> processInvoiceHandler.handle(job));
    }

    @Test
    void shouldReturnProcessInvoiceJobType() {

        assertEquals(JobType.PROCESS_INVOICE, processInvoiceHandler.supportedType());
    }
}