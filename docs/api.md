# TaskFlow REST API Documentation

TaskFlow exposes a comprehensive RESTful API for background job creation, lifecycle management, execution history inspection, and real-time observability.

- **Base URL**: `http://localhost:8085`
- **Interactive Swagger UI**: `http://localhost:8085/swagger-ui.html`
- **OpenAPI 3 Specification**: `http://localhost:8085/api-docs`

---

## 1. Job Management Endpoints

### 1.1 Create Job
Submits a single background job to the queue.

- **URL**: `POST /api/jobs`
- **Headers**:
  - `Content-Type: application/json`
  - `Idempotency-Key: <string>` *(Optional, max 128 chars)*
- **Request Body**:
```json
{
  "jobType": "EMAIL_SIMULATION",
  "payload": {
    "to": "dev@taskflow.io",
    "subject": "Deployment Notice",
    "body": "System upgraded successfully."
  },
  "priority": 8,
  "maxAttempts": 3,
  "scheduledAt": "2026-10-20T10:00:00Z",
  "idempotencyKey": "order-req-8492"
}
```

- **Response (`201 Created`)**:
```json
{
  "id": "e0b82143-698f-490b-b184-c689e477e0ab",
  "jobType": "EMAIL_SIMULATION",
  "payload": {
    "to": "dev@taskflow.io",
    "subject": "Deployment Notice",
    "body": "System upgraded successfully."
  },
  "status": "QUEUED",
  "priority": 8,
  "createdAt": "2026-10-19T16:45:10.123Z",
  "scheduledAt": "2026-10-20T10:00:00Z",
  "startedAt": null,
  "completedAt": null,
  "updatedAt": "2026-10-19T16:45:10.123Z",
  "attemptCount": 0,
  "maxAttempts": 3,
  "nextRetryAt": null,
  "result": null,
  "lastError": null,
  "idempotencyKey": "order-req-8492",
  "workerId": null,
  "executionDurationMs": null
}
```

- **Conflict (`409 Conflict`)**:
When an idempotency key is re-submitted with different parameters:
```json
{
  "timestamp": "2026-10-19T16:45:12.345Z",
  "status": 409,
  "error": "Conflict",
  "message": "Conflict: An existing job with idempotency key 'order-req-8492' has different parameters",
  "path": "/api/jobs",
  "validationErrors": null
}
```

---

### 1.2 Bulk Job Submission
Submits a batch of jobs (up to 50) with partial failure handling.

- **URL**: `POST /api/jobs/bulk`
- **Request Body**:
```json
{
  "jobs": [
    {
      "jobType": "DATA_PROCESSING",
      "payload": { "batchSize": 250 },
      "priority": 5
    },
    {
      "jobType": "EMAIL_SIMULATION",
      "payload": { "to": "invalid-address", "subject": "Test" },
      "priority": 5
    }
  ]
}
```

- **Response (`200 OK`)**:
```json
{
  "submittedJobs": [
    {
      "id": "78a9c2f1-6789-4091-a123-bc98ef441234",
      "jobType": "DATA_PROCESSING",
      "status": "QUEUED",
      "priority": 5,
      ...
    }
  ],
  "failures": [
    {
      "index": 1,
      "jobType": "EMAIL_SIMULATION",
      "error": "Field 'to' must be a valid email address: invalid-address"
    }
  ],
  "totalSubmitted": 1,
  "totalFailed": 1
}
```

---

### 1.3 List Jobs (Pagination, Filters & Sorting)
Retrieves a paginated list of jobs.

- **URL**: `GET /api/jobs`
- **Query Parameters**:
  - `status`: Filter by status (`QUEUED`, `RUNNING`, `SUCCEEDED`, `RETRY_WAIT`, `FAILED`, `DEAD_LETTER`, `CANCELLED`)
  - `jobType`: Filter by job type (e.g. `DATA_PROCESSING`)
  - `priority`: Filter by priority integer (`1` to `10`)
  - `search`: Case-insensitive substring search in ID, error, or idempotency key
  - `page`: Page index (0-based, default `0`)
  - `size`: Page size (1 to 100, default `20`)
  - `sortBy`: Safe allowlisted field (`createdAt`, `priority`, `status`, `updatedAt`, `executionDurationMs`, default `createdAt`)
  - `sortDirection`: `asc` or `desc` (default `desc`)

- **Response (`200 OK`)**:
```json
{
  "content": [
    {
      "id": "e0b82143-698f-490b-b184-c689e477e0ab",
      "jobType": "EMAIL_SIMULATION",
      "status": "SUCCEEDED",
      "priority": 8,
      "attemptCount": 1,
      "maxAttempts": 3,
      "executionDurationMs": 94,
      "createdAt": "2026-10-19T16:45:10Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

---

### 1.4 Get Job Details
- **URL**: `GET /api/jobs/{id}`
- **Response (`200 OK`)**: Full Job DTO.
- **Error (`404 Not Found`)**: When Job ID does not exist.

---

### 1.5 Get Job Execution Attempts
Retrieves historical logs of each execution attempt.

- **URL**: `GET /api/jobs/{id}/attempts`
- **Response (`200 OK`)**:
```json
[
  {
    "id": "18f9d023-4567-4890-a1b2-c3d4e5f67890",
    "jobId": "e0b82143-698f-490b-b184-c689e477e0ab",
    "attemptNumber": 1,
    "workerId": "taskflow-worker-2",
    "startedAt": "2026-10-19T16:45:11.100Z",
    "completedAt": "2026-10-19T16:45:11.194Z",
    "outcome": "SUCCESS",
    "errorDetails": null,
    "executionDurationMs": 94
  }
]
```

---

### 1.6 Cancel Job
Cancels an unexecuted job in `QUEUED` or `RETRY_WAIT` status.

- **URL**: `POST /api/jobs/{id}/cancel`
- **Response (`200 OK`)**: Job updated with `status = "CANCELLED"`.
- **Error (`409 Conflict`)**: If job is already `RUNNING` or `SUCCEEDED`.

---

### 1.7 Retry Job
Manually re-queues an eligible job in `FAILED` or `DEAD_LETTER` status.

- **URL**: `POST /api/jobs/{id}/retry`
- **Response (`200 OK`)**: Job reset with `status = "QUEUED"` and new maxAttempts allowance.
- **Error (`409 Conflict`)**: If job is not in a retryable failed state.

---

## 2. Real-Time Observability Endpoints

### 2.1 Overview Statistics
- **URL**: `GET /api/stats/overview`
- **Response (`200 OK`)**:
```json
{
  "totalJobs": 42,
  "queuedJobs": 3,
  "runningJobs": 2,
  "succeededJobs": 35,
  "failedJobs": 1,
  "deadLetterJobs": 1,
  "cancelledJobs": 0,
  "retryWaitJobs": 0
}
```

### 2.2 Time-Series Aggregation
- **URL**: `GET /api/stats/timeseries?minutes=30&bucketSeconds=60`
- **Response (`200 OK`)**:
```json
[
  {
    "timestamp": "2026-10-19T16:15:00Z",
    "submitted": 12,
    "succeeded": 11,
    "failed": 0,
    "avgDurationMs": 28.5
  }
]
```

### 2.3 Worker Pool Status
- **URL**: `GET /api/stats/workers`
- **Response (`200 OK`)**:
```json
{
  "configuredWorkers": 6,
  "activeWorkers": 2,
  "idleWorkers": 4,
  "queueDepth": 3,
  "currentlyRunningJobs": [
    {
      "id": "e0b82143-698f-490b-b184-c689e477e0ab",
      "jobType": "DATA_PROCESSING",
      "workerId": "taskflow-worker-1",
      "priority": 8,
      "startedAt": "2026-10-19T16:47:00Z"
    }
  ]
}
```

---

## 3. Operational & Health Endpoints

- **Liveness & Readiness Health**: `GET /actuator/health`
- **Prometheus Metrics**: `GET /actuator/prometheus`
