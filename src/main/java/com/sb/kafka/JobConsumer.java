package com.sb.kafka;


import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import com.sb.worker.PriorityJobDispatcher;
import org.springframework.kafka.support.Acknowledgment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class JobConsumer {

    private static final Logger log = LoggerFactory.getLogger(JobConsumer.class);
    private final PriorityJobDispatcher dispatcher;

    public JobConsumer(PriorityJobDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @KafkaListener(
            topics = "${job.kafka.topic}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(JobMessage message, Acknowledgment acknowledgment) throws Exception {

        log.info("1. Kafka RECEIVED | jobId={} | type={} | priority={}", message.jobId(), message.jobType(), message.priority());

        try {
            log.info("2. Job SUBMITTING to dispatcher | jobId={}", message.jobId());
            dispatcher.submit(message).get(15, java.util.concurrent.TimeUnit.MINUTES);
            log.info("6. Worker processing SUCCESS | jobId={}", message.jobId());
            acknowledgment.acknowledge();
            log.info("7. Kafka ACK sent | jobId={}", message.jobId());
        } catch (Exception e) {
            log.error("Kafka processing FAILED - ACK will NOT be sent | jobId={}", message.jobId(), e);
            throw e;
        }
    }
}