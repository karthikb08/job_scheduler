package com.sb.service;

import com.sb.model.Job;
import com.sb.model.JobType;
import org.springframework.stereotype.Component;

@Component
public class SendEmailHandler
        implements JobHandler {

    @Override
    public JobType supportedType() {
        return JobType.SEND_EMAIL;
    }

    @Override
    public void handle(Job job) {
        System.out.println("SEND_EMAIL handler executing | jobId=" + job.getId());
        System.out.println("SEND_EMAIL handler completed | jobId=" + job.getId());
    }
}