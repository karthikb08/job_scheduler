package com.sb.service;

import com.sb.model.Job;
import org.springframework.stereotype.Component;

@Component
public class ExportDataHandler
        implements JobHandler {
    @Override
    public com.sb.model.JobType supportedType() {
        return com.sb.model.JobType.EXPORT_DATA;
    }

    @Override
    public void handle(Job job) {

        System.out.println(
                "EXPORT_DATA handler executing | jobId="
                        + job.getId()
        );

        System.out.println(
                "EXPORT_DATA handler completed | jobId="
                        + job.getId()
        );
    }
}
