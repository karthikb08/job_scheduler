package com.sb.service;

import com.sb.model.Job;
import org.springframework.stereotype.Component;

@Component
public class GenerateReportHandler
        implements JobHandler {
    @Override
    public com.sb.model.JobType supportedType() {
        return com.sb.model.JobType.GENERATE_REPORT;
    }

    @Override
    public void handle(Job job) {

        System.out.println(
                "GENERATE_REPORT handler executing | jobId="
                        + job.getId()
        );

        // Simulate report generation
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Report generation interrupted",
                    e
            );
        }

        System.out.println(
                "GENERATE_REPORT handler completed | jobId="
                        + job.getId()
        );
    }
}