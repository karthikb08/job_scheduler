package com.sb.worker;

import com.sb.model.Job;
import com.sb.model.JobType;
import com.sb.model.JobPriority;
import com.sb.kafka.JobMessage;
import com.sb.service.JobService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PriorityJobDispatcherTest {

    private PriorityJobDispatcher dispatcher;

    @AfterEach
    void tearDown() throws InterruptedException {
        if (dispatcher != null) {
            dispatcher.stop();
        }
    }

    @Test
    void shouldSubmitJobSuccessfully() throws Exception {

        JobService jobService =
                mock(JobService.class);

        dispatcher = new PriorityJobDispatcher(jobService, 1, 10);

        dispatcher.start();

        JobMessage message = createMessage("job-001", JobPriority.HIGH);

        Job job =
                new Job();

        when(jobService.claimForExecution("job-001")).thenReturn(job);
        CompletableFuture<Void> future = dispatcher.submit(message);
        assertNotNull(future);
        future.get(5, TimeUnit.SECONDS);
        verify(jobService).claimForExecution("job-001");
        verify(jobService).execute(job);
    }

    @Test
    void shouldRejectSubmissionWhenDispatcherIsStopped() {

        JobService jobService =
                mock(JobService.class);

        dispatcher = new PriorityJobDispatcher(jobService, 1, 10);

        CompletableFuture<Void> future = dispatcher.submit(createMessage("job-001", JobPriority.HIGH));

        assertTrue(future.isCompletedExceptionally());
        assertThrows(Exception.class, future::join);
        verifyNoInteractions(jobService);
    }

    @Test
    void shouldRejectJobWhenQueueIsFull() throws Exception {

        JobService jobService = mock(JobService.class);
        dispatcher = new PriorityJobDispatcher(jobService, 1, 1);

        dispatcher.start();

        CompletableFuture<Void> workerBlocked = new CompletableFuture<>();

        Job job =
                new Job();

        when(
                jobService.claimForExecution("job-001")
        ).thenAnswer(invocation -> {

            workerBlocked.join();

            return job;
        });

        CompletableFuture<Void> first =
                dispatcher.submit(
                        createMessage(
                                "job-001",
                                JobPriority.LOW
                        )
                );


        verify(
                jobService,
                timeout(2000)
        ).claimForExecution("job-001");


        CompletableFuture<Void> second =
                dispatcher.submit(
                        createMessage(
                                "job-002",
                                JobPriority.LOW
                        )
                );

        CompletableFuture<Void> third =
                dispatcher.submit(
                        createMessage(
                                "job-003",
                                JobPriority.LOW
                        )
                );

        assertTrue(
                third.isCompletedExceptionally()
        );

        workerBlocked.complete(null);

        first.get(5, TimeUnit.SECONDS);

        second.get(5, TimeUnit.SECONDS);
    }

    @Test
    void shouldCompleteFutureWhenJobCannotBeClaimed() throws Exception {

        JobService jobService =
                mock(JobService.class);

        dispatcher = new PriorityJobDispatcher(jobService, 1, 10);

        dispatcher.start();

        when(
                jobService.claimForExecution("job-001")
        ).thenReturn(null);

        CompletableFuture<Void> future =
                dispatcher.submit(
                        createMessage(
                                "job-001",
                                JobPriority.HIGH
                        )
                );

        assertDoesNotThrow(
                () -> future.get(
                        5,
                        TimeUnit.SECONDS
                )
        );

        verify(jobService).claimForExecution("job-001");
        verify(jobService, never()).execute(any());
    }

    @Test
    void shouldCompleteFutureAfterSuccessfulExecution()
            throws Exception {

        JobService jobService = mock(JobService.class);
        dispatcher = new PriorityJobDispatcher(jobService, 1, 10);

        dispatcher.start();

        Job job = new Job();

        when(
                jobService.claimForExecution("job-001")
        ).thenReturn(job);

        CompletableFuture<Void> future =
                dispatcher.submit(
                        createMessage(
                                "job-001",
                                JobPriority.HIGH
                        )
                );

        future.get(5, TimeUnit.SECONDS);
        assertTrue(future.isDone());
        assertFalse(future.isCompletedExceptionally());
        verify(jobService).execute(job);
    }

    @Test
    void shouldCompleteFutureExceptionallyWhenExecutionFails()
            throws Exception {

        JobService jobService =
                mock(JobService.class);

        dispatcher = new PriorityJobDispatcher(jobService, 1, 10);

        dispatcher.start();

        Job job = new Job();

        when(
                jobService.claimForExecution("job-001")
        ).thenReturn(job);

        RuntimeException failure = new RuntimeException("Execution failed");

        doThrow(failure).when(jobService).execute(job);

        CompletableFuture<Void> future =
                dispatcher.submit(
                        createMessage(
                                "job-001",
                                JobPriority.HIGH
                        )
                );

        assertThrows(
                Exception.class,
                () -> future.get(
                        5,
                        TimeUnit.SECONDS
                )
        );

        assertTrue(future.isCompletedExceptionally());
        verify(jobService).execute(job);
    }

    @Test
    @Timeout(10)
    void shouldProcessMultipleJobs() throws Exception {

        JobService jobService = mock(JobService.class);

        dispatcher =
                new PriorityJobDispatcher(
                        jobService,
                        2,
                        10
                );

        dispatcher.start();

        Job job1 = new Job(), job2 = new Job();

        when(
                jobService.claimForExecution("job-001")
        ).thenReturn(job1);

        when(
                jobService.claimForExecution("job-002")
        ).thenReturn(job2);

        CompletableFuture<Void> future1 =
                dispatcher.submit(
                        createMessage(
                                "job-001",
                                JobPriority.HIGH
                        )
                );

        CompletableFuture<Void> future2 =
                dispatcher.submit(
                        createMessage(
                                "job-002",
                                JobPriority.MEDIUM
                        )
                );

        CompletableFuture.allOf(
                future1,
                future2
        ).get(
                5,
                TimeUnit.SECONDS
        );

        verify(jobService, timeout(2000)).execute(job1);
        verify(jobService, timeout(2000)).execute(job2);
    }

    @Test
    void shouldShutdownWorkersGracefully() throws Exception {

        JobService jobService = mock(JobService.class);

        dispatcher =
                new PriorityJobDispatcher(
                        jobService,
                        2,
                        10
                );

        dispatcher.start();

        assertDoesNotThrow(() -> dispatcher.stop());

        assertDoesNotThrow(() -> dispatcher.stop()
        );
    }

    private JobMessage createMessage(String jobId, JobPriority priority) {

        return new JobMessage(jobId,
                JobType.EXPORT_DATA,
                priority
        );
    }
}