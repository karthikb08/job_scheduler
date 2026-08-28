# Job Processing System

Spring Boot job processing system using Kafka, MongoDB and a priority-based worker queue.

## Tech Stack

- Java 17
- Spring Boot
- Kafka
- MongoDB
- Gradle
- Docker

## Run Application

    Start Kafka and MongoDB:

### bash
    docker compose up -d

# Job Processing System

    Spring Boot + Kafka + MongoDB based asynchronous job processing system.

## Application URLs

### Base URL

http://localhost:8080

### Submit Job

POST `/api/v1/jobs`

    http://localhost:8080/api/v1/jobs

## Headers 

    Content-Type: application/json
    Idempotency-Key: test-report-001

## Request 

    {
    "type": "GENERATE_REPORT",
    "priority": "HIGH",
    "maxRetries": 3,
    "scheduledAt": null,
    "payload": "Generate monthly report"
    }

## Response 

    {
    "id": {jobId},
    "type": "GENERATE_REPORT",
    "priority": "HIGH",
    "status": "QUEUED",
    "createdAt": "2026-08-28T16:08:05.574Z",
    "scheduledAt": null,
    "retryCount": 0,
    "maxRetries": 3,
    "lastError": null
    }

## Job Status
GET `/jobs/{jobId}`

    http://localhost:8080/jobs/{jobId}

## Cancel Jobs
Delete `/api/v1/jobs/{jobId}`

    http://localhost:8080/api/v1/jobs/{jobId}


## Extensibility
The system uses a JobHandler strategy abstraction.
Each job type has a dedicated JobHandler implementation.

Adding a new job type requires:
1. Adding the JobType enum.
2. Implementing JobHandler.
3. Registering the new handler as a Spring component.

## Scalability
The system is designed for horizontal scaling:

- Kafka partitions allow work distribution across consumer instances.
- Worker thread count is configurable.
- Priority processing is handled independently from Kafka transport.
- MongoDB stores durable job state.
- Atomic state transitions prevent duplicate execution.
- Scheduler processing is limited using a top-100 query.
- MongoDB indexes should be created for status/scheduledAt queries.

## Reliability
- Idempotency keys prevent duplicate job creation.
- Kafka acknowledgement occurs only after worker processing completes.
- Failed jobs use exponential backoff.
- Maximum retry count prevents infinite retries.
- Atomic MongoDB state transitions protect concurrent processing.
- Graceful shutdown allows active workers to finish.