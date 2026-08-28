package com.sb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class JobProcessingApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobProcessingApplication.class, args);
    }
}