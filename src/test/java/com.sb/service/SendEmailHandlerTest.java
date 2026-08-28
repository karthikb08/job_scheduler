package com.sb.service;

import com.sb.model.JobType;
import com.sb.model.Job;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SendEmailHandlerTest {

    private final SendEmailHandler sendEmailHandler = new SendEmailHandler();

    @Test
    void shouldHandleSendEmailJob() {

        Job job = new Job();

        job.setId("test-email-job-001");
        job.setType(JobType.SEND_EMAIL);
        job.setPayload(
                "{\"to\":\"customer@example.com\","
                        + "\"subject\":\"Invoice\"}"
        );

        assertDoesNotThrow(() -> sendEmailHandler.handle(job));
    }

    @Test
    void shouldReturnSendEmailJobType() {

        assertEquals(JobType.SEND_EMAIL, sendEmailHandler.supportedType());
    }
}