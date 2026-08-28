package com.sb.service;

import com.sb.model.Job;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import  com.sb.model.JobType;

@Component
public class JobHandlerRegistry {
    private final Map<JobType, JobHandler> handlers;

    public JobHandlerRegistry(List<JobHandler> handlerList) {

        this.handlers =
                handlerList.stream()
                        .collect(
                                Collectors.toUnmodifiableMap(
                                        JobHandler::supportedType,
                                        Function.identity()
                                )
                        );

        System.out.println("========== JOB HANDLERS ==========");

        handlers.forEach((type, handler) ->
                System.out.println(
                        "Registered handler: "
                                + handler.getClass().getName()
                                + " | type="
                                + type
                )
        );

        System.out.println("Registered types = " + handlers.keySet());

        System.out.println("==================================");
    }

    public void handle(Job job) {

        com.sb.model.JobType jobType = job.getType();

        JobHandler handler = handlers.get(jobType);

        if (handler == null) {

            throw new IllegalArgumentException("No handler found for job type: "
                    + jobType + " | registered types=" + handlers.keySet());
        }

        handler.handle(job);
    }
}