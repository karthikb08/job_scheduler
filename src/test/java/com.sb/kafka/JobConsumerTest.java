package com.sb.kafka;

import com.sb.model.JobPriority;
import com.sb.model.JobType;
import com.sb.worker.PriorityJobDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class JobConsumerTest {

    @Test
    void shouldSubmitMessageToDispatcher() throws Exception {
        PriorityJobDispatcher dispatcher = mock(PriorityJobDispatcher.class);
        JobConsumer consumer = new JobConsumer(dispatcher);
        JobMessage message = new JobMessage("job-001", JobType.GENERATE_REPORT, JobPriority.HIGH);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
        when(dispatcher.submit(message)).thenReturn(future);
        consumer.consume(message, acknowledgment);
        verify(dispatcher).submit(message);
    }

    @Test
    void shouldAcknowledgeKafkaMessageAfterSuccessfulProcessing() throws Exception {
        PriorityJobDispatcher dispatcher = mock(PriorityJobDispatcher.class);
        JobConsumer consumer = new JobConsumer(dispatcher);
        JobMessage message = new JobMessage("job-002", JobType.PROCESS_INVOICE, JobPriority.MEDIUM);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
        when(dispatcher.submit(message)).thenReturn(future);
        consumer.consume(message, acknowledgment);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void shouldNotAcknowledgeWhenDispatcherFails() throws Exception {
        PriorityJobDispatcher dispatcher = mock(PriorityJobDispatcher.class);
        JobConsumer consumer = new JobConsumer(dispatcher);
        JobMessage message = new JobMessage("job-003", JobType.SEND_EMAIL, JobPriority.LOW);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        CompletableFuture<Void> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Job execution failed"));
        when(dispatcher.submit(message)).thenReturn(failedFuture);
        assertThrows(Exception.class, () -> consumer.consume(message, acknowledgment));
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void shouldPropagateDispatcherException() throws Exception {
        PriorityJobDispatcher dispatcher = mock(PriorityJobDispatcher.class);
        JobConsumer consumer = new JobConsumer(dispatcher);
        JobMessage message = new JobMessage("job-004", JobType.EXPORT_DATA, JobPriority.HIGH);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        RuntimeException exception = new RuntimeException("Dispatcher failed");
        CompletableFuture<Void> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(exception);

        when(
                dispatcher.submit(message)
        ).thenReturn(failedFuture);
        assertThrows(Exception.class, () -> consumer.consume(message, acknowledgment));
        verify(dispatcher).submit(message);
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void shouldUseSameMessageSubmittedToDispatcher() throws Exception {
        PriorityJobDispatcher dispatcher = mock(PriorityJobDispatcher.class);
        JobConsumer consumer = new JobConsumer(dispatcher);
        JobMessage message = new JobMessage("job-005", JobType.EXPORT_DATA, JobPriority.LOW);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        when(dispatcher.submit(message)).thenReturn(CompletableFuture.completedFuture(null));
        consumer.consume(message, acknowledgment);
        verify(dispatcher).submit(same(message));
    }

    @Test
    void shouldAcknowledgeOnlyAfterDispatcherCompletes() throws Exception {
        PriorityJobDispatcher dispatcher = mock(PriorityJobDispatcher.class);
        JobConsumer consumer = new JobConsumer(dispatcher);
        JobMessage message = new JobMessage("job-006", JobType.GENERATE_REPORT, JobPriority.HIGH);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        CompletableFuture<Void> future = new CompletableFuture<>();
        when(dispatcher.submit(message)).thenReturn(future);
        future.complete(null);
        consumer.consume(message, acknowledgment);
        verify(acknowledgment).acknowledge();
    }
}