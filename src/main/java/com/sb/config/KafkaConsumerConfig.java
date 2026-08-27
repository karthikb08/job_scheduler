package com.sb.config;

import com.sb.kafka.JobMessage;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConsumerFactory<String, JobMessage>
    consumerFactory(
            @Value("${spring.kafka.bootstrap-servers}")
            String bootstrapServers,

            @Value("${spring.kafka.consumer.group-id}")
            String groupId) {

        Map<String, Object> properties =
                new HashMap<>();

        properties.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers
        );

        properties.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                groupId
        );

        properties.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );

        properties.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                JsonDeserializer.class
        );

        properties.put(
                JsonDeserializer.TRUSTED_PACKAGES,
                "com.example.jobsystem.kafka"
        );

        properties.put(
                JsonDeserializer.VALUE_DEFAULT_TYPE,
                JobMessage.class.getName()
        );

        /*
         * We manually acknowledge Kafka messages.
         */
        properties.put(
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                false
        );

        properties.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest"
        );

        return new DefaultKafkaConsumerFactory<>(
                properties
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory
            <String, JobMessage>
    kafkaListenerContainerFactory(
            ConsumerFactory<String, JobMessage>
                    consumerFactory) {

        var factory =
                new ConcurrentKafkaListenerContainerFactory
                        <String, JobMessage>();

        factory.setConsumerFactory(
                consumerFactory
        );

        /*
         * Kafka consumer concurrency.
         */
        factory.setConcurrency(3);

        factory.getContainerProperties()
                .setAckMode(
                        ContainerProperties.AckMode.MANUAL
                );

        return factory;
    }
}
