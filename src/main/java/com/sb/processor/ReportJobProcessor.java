package com.sb.processor;

import com.sb.model.Job;
import org.springframework.stereotype.Component;

@Component
public class ReportJobProcessor implements JobProcessor {

    @Override
    public void process(Job job) {

        System.out.println(
                "Generating report for job: " + job.getId()
        );
    }
}