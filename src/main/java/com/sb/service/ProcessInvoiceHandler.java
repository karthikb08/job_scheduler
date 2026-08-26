package com.sb.service;

import com.sb.model.Job;
import org.springframework.stereotype.Component;

@Component
public class ProcessInvoiceHandler
        implements JobHandler {
    @Override
    public com.sb.model.JobType supportedType() {
        return com.sb.model.JobType.PROCESS_INVOICE;
    }

    @Override
    public void handle(Job job) {

        System.out.println(
                "PROCESS_INVOICE handler executing | jobId="
                        + job.getId()
        );

        System.out.println(
                "PROCESS_INVOICE handler completed | jobId="
                        + job.getId()
        );
    }
}