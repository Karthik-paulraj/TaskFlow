# TaskFlow Testing Guide

TaskFlow maintains high test coverage across unit tests, database queries, integration workflows, concurrency guarantees, and frontend components.

---

## 1. Test Suite Architecture

```
TaskFlow Test Architecture
│
├── Unit Tests (Fast, In-Memory)
│   ├── JobStatusTest (State transition rules & terminal checks)
│   ├── RetryServiceTest (Exponential backoff formula, jitter, max delay caps)
│   ├── JobHandlerTest (Payload validation & execution logic for all 5 handlers)
│   └── JobServiceTest (Creation logic, idempotency conflicts, cancellation rules)
│
├── Integration Tests (Database & Spring Context)
│   ├── JobRepositoryIntegrationTest (Priority scheduling order, FIFO, atomic claims)
│   ├── JobControllerIntegrationTest (MockMvc testing of all REST endpoints)
│   └── WorkerExecutionIntegrationTest (End-to-end async execution, retries, dead-letter)
│
├── Concurrency Tests (Multi-Threaded Validation)
│   └── ConcurrencyIntegrationTest (15 concurrent jobs, multi-worker race validation)
│
├── Benchmark & Performance Tests
│   └── TaskFlowBenchmarkTest (Throughput, p50/p95/p99 latency measurement)
│
└── Frontend Tests (Vitest & React Testing Library)
    └── StatusBadge.test.tsx (Badge rendering, color classes, priority indicators)
```

---

## 2. Test Execution Commands

### Run all backend unit and integration tests
```bash
# Set JAVA_HOME if needed
export JAVA_HOME="/path/to/jdk-21"

# From project root
mvn test -f backend/pom.xml
```

### Run specific test classes
```bash
# Run unit tests only
mvn test -Dtest=JobStatusTest,RetryServiceTest,JobHandlerTest,JobServiceTest -f backend/pom.xml

# Run concurrency tests only
mvn test -Dtest=ConcurrencyIntegrationTest -f backend/pom.xml

# Run repeatable performance benchmark
mvn test -Dtest=TaskFlowBenchmarkTest -f backend/pom.xml
```

### Run frontend tests
```bash
cd frontend
npm test
```

---

## 3. Verified Test Results

All 28 tests in the backend test suite executed cleanly with 0 failures and 0 errors:

| Test Class | Tests Run | Failures | Errors | Purpose |
| :--- | :---: | :---: | :---: | :--- |
| `JobStatusTest` | 3 | 0 | 0 | Verifies strict state transition matrix and terminal state rules |
| `RetryServiceTest` | 3 | 0 | 0 | Tests backoff progression ($2s \to 4s \to 8s \to 16s$), max cap, and exhaustion |
| `JobHandlerTest` | 7 | 0 | 0 | Tests payload validation and execution for all 5 pluggable handlers |
| `JobServiceTest` | 4 | 0 | 0 | Unit tests for service layer, exact idempotency matches, and conflict detection |
| `JobRepositoryIntegrationTest` | 3 | 0 | 0 | Validates priority queue ordering, FIFO tie-breaking, and atomic claim logic |
| `JobControllerIntegrationTest` | 5 | 0 | 0 | Tests HTTP `201`, `200`, `409`, pagination, and bulk endpoints via MockMvc |
| `WorkerExecutionIntegrationTest`| 2 | 0 | 0 | End-to-end execution of async workers, retries, and dead-letter moves |
| `ConcurrencyIntegrationTest` | 1 | 0 | 0 | Confirms that 15 concurrent tasks are claimed and executed without duplication |
| `TaskFlowBenchmarkTest` | 1 | 0 | 0 | Measures real throughput, latency percentiles, and completion metrics |
| **Total Backend** | **29** | **0** | **0** | **100% Pass Rate** |

### Frontend Tests (Vitest)
| Test File | Tests Run | Result | Purpose |
| :--- | :---: | :---: | :--- |
| `StatusBadge.test.tsx` | 6 | Passed | Tests badge rendering, accessibility, and priority indicators |
