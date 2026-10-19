package com.taskflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.config.TaskFlowProperties;
import com.taskflow.dto.CreateJobRequest;
import com.taskflow.dto.JobResponse;
import com.taskflow.entity.Job;
import com.taskflow.enums.JobStatus;
import com.taskflow.exception.DuplicateIdempotencyKeyException;
import com.taskflow.exception.InvalidStateTransitionException;
import com.taskflow.handler.JobHandlerRegistry;
import com.taskflow.repository.JobAttemptRepository;
import com.taskflow.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class JobServiceTest {

    private JobRepository jobRepository;
    private JobAttemptRepository jobAttemptRepository;
    private JobHandlerRegistry handlerRegistry;
    private TaskFlowProperties properties;
    private ObjectMapper objectMapper;
    private JobService jobService;

    @BeforeEach
    void setUp() {
        jobRepository = Mockito.mock(JobRepository.class);
        jobAttemptRepository = Mockito.mock(JobAttemptRepository.class);
        handlerRegistry = Mockito.mock(JobHandlerRegistry.class);
        properties = new TaskFlowProperties();
        objectMapper = new ObjectMapper();

        when(handlerRegistry.getHandler(any())).thenReturn(new com.taskflow.handler.EmailSimulationHandler(objectMapper));

        jobService = new JobService(jobRepository, jobAttemptRepository, handlerRegistry, properties, objectMapper);
    }

    @Test
    @DisplayName("Creates new job successfully when idempotency key is absent or unique")
    void testCreateJobSuccess() {
        CreateJobRequest req = new CreateJobRequest(
                "EMAIL_SIMULATION",
                objectMapper.createObjectNode().put("to", "user@test.com").put("subject", "Hello"),
                7,
                3,
                null,
                "key-123"
        );

        when(jobRepository.findByIdempotencyKey("key-123")).thenReturn(Optional.empty());
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JobResponse response = jobService.createJob(req, null);

        assertNotNull(response);
        assertEquals("EMAIL_SIMULATION", response.getJobType());
        assertEquals(7, response.getPriority());
        assertEquals(JobStatus.QUEUED, response.getStatus());
    }

    @Test
    @DisplayName("Idempotency: returns existing job when payload and parameters match")
    void testIdempotencyExactMatch() {
        String payload = "{\"to\":\"user@test.com\",\"subject\":\"Hello\"}";
        Job existing = new Job(
                UUID.randomUUID(), "EMAIL_SIMULATION", payload, 7, 3, null, "key-123"
        );

        CreateJobRequest req = new CreateJobRequest(
                "EMAIL_SIMULATION",
                objectMapper.createObjectNode().put("to", "user@test.com").put("subject", "Hello"),
                7,
                3,
                null,
                "key-123"
        );

        when(jobRepository.findByIdempotencyKey("key-123")).thenReturn(Optional.of(existing));

        JobResponse response = jobService.createJob(req, null);
        assertEquals(existing.getId(), response.getId());
    }

    @Test
    @DisplayName("Idempotency: throws Conflict when payload or parameters differ for the same key")
    void testIdempotencyConflict() {
        String existingPayload = "{\"to\":\"first@test.com\",\"subject\":\"Hello\"}";
        Job existing = new Job(
                UUID.randomUUID(), "EMAIL_SIMULATION", existingPayload, 7, 3, null, "key-123"
        );

        CreateJobRequest req = new CreateJobRequest(
                "EMAIL_SIMULATION",
                objectMapper.createObjectNode().put("to", "DIFFERENT@test.com").put("subject", "Hello"),
                7,
                3,
                null,
                "key-123"
        );

        when(jobRepository.findByIdempotencyKey("key-123")).thenReturn(Optional.of(existing));

        assertThrows(DuplicateIdempotencyKeyException.class, () -> jobService.createJob(req, null));
    }

    @Test
    @DisplayName("Cancel job: allows cancelling QUEUED jobs but forbids SUCCEEDED jobs")
    void testCancelJobRules() {
        UUID jobId = UUID.randomUUID();
        Job queuedJob = new Job(jobId, "EMAIL_SIMULATION", "{}", 5, 3, null, null);
        queuedJob.setStatus(JobStatus.QUEUED);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(queuedJob));
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        JobResponse cancelled = jobService.cancelJob(jobId);
        assertEquals(JobStatus.CANCELLED, cancelled.getStatus());

        // Now test cancelling a SUCCEEDED job
        Job succeededJob = new Job(jobId, "EMAIL_SIMULATION", "{}", 5, 3, null, null);
        succeededJob.setStatus(JobStatus.SUCCEEDED);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(succeededJob));

        assertThrows(InvalidStateTransitionException.class, () -> jobService.cancelJob(jobId));
    }
}
