package com.sb.processor;

import com.sb.model.JobType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class JobProcessorRegistry {

    private final Map<JobType, JobProcessor> processors =
            new EnumMap<>(JobType.class);

    public JobProcessorRegistry(List<JobProcessor> processors) {

        // We'll populate this properly once each processor
        // declares the JobType it supports.
    }

    public JobProcessor getProcessor(JobType type) {

        JobProcessor processor = processors.get(type);

        if (processor == null) {
            throw new IllegalArgumentException(
                    "No processor found for job type: " + type
            );
        }

        return processor;
    }
}