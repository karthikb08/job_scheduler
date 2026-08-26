package com.sb.service;

import com.sb.model.Job;
import com.sb.kafka.JobMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class JobPublisher {

    private final KafkaTemplate<String, JobMessage>
            kafkaTemplate;

    private final String topic;

    public JobPublisher(
            KafkaTemplate<String, JobMessage>
                    kafkaTemplate,

            @Value("${job.kafka.topic}")
            String topic) {

        this.kafkaTemplate = kafkaTemplate;

        this.topic = topic;
    }

    public void publish(Job job) {

        JobMessage message = new JobMessage(
                job.getId(),
                job.getType(),
                job.getPriority()
        );

        kafkaTemplate.send(
                topic,
                job.getId(),
                message
        );
    }
}