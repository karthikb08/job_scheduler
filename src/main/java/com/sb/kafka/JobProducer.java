package com.sb.kafka;

import com.sb.config.KafkaTopicConfig;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import com.sb.model.Job;
import com.sb.kafka.JobMessage;

@Component
public class JobProducer {

    private final KafkaTemplate<String, JobMessage> kafkaTemplate;

    public JobProducer(
            KafkaTemplate<String,JobMessage> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(Job job) {

        JobMessage message = new JobMessage(
                job.getId(),
                job.getType(),
                job.getPriority()
        );

        kafkaTemplate.send(
                KafkaTopicConfig.JOBS_TOPIC,
                message.jobId(),
                message
        );
    }
}
