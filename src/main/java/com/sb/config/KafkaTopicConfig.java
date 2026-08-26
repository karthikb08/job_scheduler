package com.sb.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {

    public static final String JOBS_TOPIC = "jobs";

    @Bean
    public NewTopic jobsTopic() {
        return new NewTopic(
                JOBS_TOPIC,
                3,
                (short) 1
        );
    }
}
