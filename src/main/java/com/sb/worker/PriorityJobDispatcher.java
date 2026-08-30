package com.sb.worker;

import com.sb.kafka.JobMessage;
import com.sb.service.JobService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class PriorityJobDispatcher {

    private final JobService jobService;

    private final int workerCount;
    private final int queueCapacity;
    private final PriorityBlockingQueue<WorkItem> queue = new PriorityBlockingQueue<>();
    private final AtomicLong sequence = new AtomicLong();
    private volatile boolean running;
    private Thread[] workers;

    public PriorityJobDispatcher(JobService jobService,

            @Value("${job.worker.threads:4}")
            int workerCount,

            @Value("${job.worker.queue-capacity:1000}")
            int queueCapacity) {

        this.jobService = jobService;
        this.workerCount = workerCount;
        this.queueCapacity = queueCapacity;
    }

    //Start here
    @PostConstruct
    public void start() {
        running = true;workers = new Thread[workerCount];
        for (int i = 0; i < workerCount; i++) {
            workers[i] = new Thread(this::workerLoop, "job-worker-" + i);
            workers[i].start();
        }
        System.out.println("Started " + workerCount + " job workers");
    }

   //Submit priority queue
    public CompletableFuture<Void> submit(JobMessage message) {
        if (!running) {
            return CompletableFuture.failedFuture(new IllegalStateException("Dispatcher is shutting down"));
        }

        if (queue.size() >= queueCapacity) {
            return CompletableFuture.failedFuture(new IllegalStateException("Worker queue is full"));
        }

        CompletableFuture<Void> completableFuture = new CompletableFuture<>();
        WorkItem item = new WorkItem(message, sequence.incrementAndGet(), completableFuture);

        queue.offer(item);

        return completableFuture;
    }

   //Work Loop
    private void workerLoop() {
        while (running || !queue.isEmpty()) {
            WorkItem item = null;
            try {
                item = queue.poll(500, TimeUnit.MILLISECONDS);
                if (item == null) {
                    continue;
                }
                var job = jobService.claimForExecution(item.message().jobId());
                if (job == null) {
                    item.future().complete(null);
                    continue;
                }
                jobService.execute(job);
                item.future().complete(null);

            } catch (InterruptedException exception) {
                if (item != null) {
                    item.future().completeExceptionally(exception);
                }
                Thread.currentThread().interrupt();
                return;
            } catch (Exception exception) {

                if (item != null) {
                    item.future().completeExceptionally(exception);
                }
            }
        }
    }

   //Shutdown
   @PreDestroy
   public void stop() throws InterruptedException {

       System.out.println("========== SHUTDOWN STARTED ==========");
       System.out.println("Stopping job workers...");

       running = false;

       if (workers == null) {
           System.out.println("No workers to stop.");
           return;
       }

       long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);

       for (Thread worker : workers) {
           long remaining = deadline - System.nanoTime();
           if (remaining <= 0) {
               System.out.println("Shutdown timeout reached.");
               break;
           }
           System.out.println("Waiting for worker: " + worker.getName());
           worker.join(TimeUnit.NANOSECONDS.toMillis(remaining));
           System.out.println("Worker finished: " + worker.getName() + " alive=" + worker.isAlive());
       }
       System.out.println("========== JOB WORKERS STOPPED ==========");
   }


    private record WorkItem(JobMessage message, long sequence, CompletableFuture<Void> future)
            implements Comparable<WorkItem> {

        @Override
        public int compareTo(WorkItem other) {
            int priority = Integer.compare(this.message().priority().getValue(),
                    other.message().priority().getValue());
            if (priority != 0) {
                return priority;
            }
            return Long.compare(sequence, other.sequence);
        }
    }
}