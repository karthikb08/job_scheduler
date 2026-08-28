package com.sb.service;

import com.sb.model.Job;
import com.sb.model.JobType;
import org.springframework.stereotype.Component;

@Component
public class GenerateReportHandler implements JobHandler {
    @Override
    public JobType supportedType() {
        return com.sb.model.JobType.GENERATE_REPORT;
    }

    @Override
    public void handle(Job job) {

        System.out.println("GENERATE_REPORT handler executing | jobId=" + job.getId());

        if ("FAIL".equalsIgnoreCase(job.getPayload())) {
            System.out.println("GENERATE_REPORT handler FAILED | jobId=" + job.getId());
            throw new RuntimeException("Simulated report generation failure");
        }

        // Simulate report generation
        try {
            Thread.sleep(9000);//added for delay to check the cancel
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Report generation interrupted", e);
        }

        System.out.println("GENERATE_REPORT handler completed | jobId=" + job.getId());
    }
}