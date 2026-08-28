package com.sb.kafka;

import com.sb.config.KafkaTopicConfig;
import com.sb.model.JobPriority;
import com.sb.model.JobType;
import com.sb.model.Job;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class JobProducerTest {

    @Test
    void shouldPublishJobToKafka() {

        KafkaTemplate<String, JobMessage> kafkaTemplate = mock(KafkaTemplate.class);

        JobProducer jobProducer = new JobProducer(kafkaTemplate);

        Job job = new Job();

        job.setId("test-job-001");
        job.setType(JobType.GENERATE_REPORT);
        job.setPriority(JobPriority.HIGH);
        jobProducer.publish(job);
        verify(kafkaTemplate)
                .send(
                        eq(KafkaTopicConfig.JOBS_TOPIC),
                        eq("test-job-001"),
                        any(JobMessage.class)
                );
    }

    @Test
    void shouldPublishCorrectJobMessage() {

        KafkaTemplate<String, JobMessage> kafkaTemplate = mock(KafkaTemplate.class);
        JobProducer jobProducer = new JobProducer(kafkaTemplate);

        Job job = new Job();
        job.setId("test-job-002");
        job.setType(JobType.PROCESS_INVOICE);
        job.setPriority(JobPriority.MEDIUM);

        jobProducer.publish(job);

        verify(kafkaTemplate)
                .send(
                        eq(KafkaTopicConfig.JOBS_TOPIC),
                        eq("test-job-002"),
                        eq(
                                new JobMessage(
                                        "test-job-002",
                                        JobType.PROCESS_INVOICE,
                                        JobPriority.MEDIUM
                                )
                        )
                );
    }

    @Test
    void shouldUseJobIdAsKafkaKey() {

        KafkaTemplate<String, JobMessage> kafkaTemplate = mock(KafkaTemplate.class);
        JobProducer jobProducer = new JobProducer(kafkaTemplate);

        Job job = new Job();
        job.setId("Test-123");
        job.setType(JobType.EXPORT_DATA);
        job.setPriority(JobPriority.LOW);

        jobProducer.publish(job);
        verify(kafkaTemplate)
                .send(
                        eq(KafkaTopicConfig.JOBS_TOPIC),
                        eq("Test-123"),
                        any(JobMessage.class)
                );
    }

    @Test
    void shouldConvertJobToJobMessage() {

        KafkaTemplate<String, JobMessage> kafkaTemplate = mock(KafkaTemplate.class);
        JobProducer jobProducer = new JobProducer(kafkaTemplate);

        Job job = new Job();
        job.setId("test-job-100");
        job.setType(JobType.SEND_EMAIL);
        job.setPriority(JobPriority.LOW);
        jobProducer.publish(job);

        var captor = org.mockito.ArgumentCaptor.forClass(JobMessage.class);
        verify(kafkaTemplate)
                .send(
                        eq(KafkaTopicConfig.JOBS_TOPIC),
                        eq("test-job-100"),
                        captor.capture()
                );
        JobMessage jobMessage = captor.getValue();
        assertEquals("test-job-100", jobMessage.jobId());
        assertEquals(JobType.SEND_EMAIL, jobMessage.jobType());assertEquals(JobPriority.LOW, jobMessage.priority());
    }
}