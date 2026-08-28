package com.sb.config;

import com.sb.kafka.JobMessage;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    @Bean
    public ProducerFactory<String, JobMessage>
    producerFactory(
            @Value("${spring.kafka.bootstrap-servers}")
            String bootstrapServers) {

        Map<String, Object> properties = new HashMap<>();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,JsonSerializer.class);
        //Wait for all in-sync replicas.
        properties.put(ProducerConfig.ACKS_CONFIG,"all");
        //Producer idempotence
        properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,true);
        return new DefaultKafkaProducerFactory<>(properties);
    }

    @Bean
    public KafkaTemplate<String, JobMessage> kafkaTemplate(ProducerFactory<String, JobMessage> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}
