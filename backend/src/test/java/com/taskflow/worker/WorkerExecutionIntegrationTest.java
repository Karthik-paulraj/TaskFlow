package com.taskflow.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.dto.CreateJobRequest;
import com.taskflow.dto.JobResponse;
import com.taskflow.entity.Job;
import com.taskflow.entity.JobAttempt;
import com.taskflow.enums.JobStatus;
import com.taskflow.repository.JobAttemptRepository;
import com.taskflow.repository.JobRepository;
import com.taskflow.service.JobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class WorkerExecutionIntegrationTest {

    @Autowired
    private JobService jobService;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobAttemptRepository jobAttemptRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void clean() {
        jobRepository.deleteAll();
    }

    @Test
    @DisplayName("End-to-end: Job moves from QUEUED to RUNNING to SUCCEEDED")
    void testSuccessfulJobLifecycle() {
        CreateJobRequest req = new CreateJobRequest(
                "EMAIL_SIMULATION",
                objectMapper.createObjectNode()
                        .put("to", "ops@taskflow.io")
                        .put("subject", "E2E Test"),
                5, 3, null, null
        );

        JobResponse submitted = jobService.createJob(req, null);
        UUID jobId = submitted.getId();

        // Await background scheduler and worker execution to complete
        await().atMost(10, TimeUnit.SECONDS).until(() -> {
            Job j = jobRepository.findById(jobId).orElse(null);
            return j != null && j.getStatus() == JobStatus.SUCCEEDED;
        });

        Job finished = jobRepository.findById(jobId).orElseThrow();
        assertEquals(JobStatus.SUCCEEDED, finished.getStatus());
        assertNotNull(finished.getResult());
        assertTrue(finished.getResult().contains("DISPATCHED"));
        assertNotNull(finished.getCompletedAt());
        assertTrue(finished.getExecutionDurationMs() > 0);

        List<JobAttempt> attempts = jobAttemptRepository.findByJobIdOrderByAttemptNumberAsc(jobId);
        assertEquals(1, attempts.size());
        assertEquals(1, attempts.get(0).getAttemptNumber());
    }

    @Test
    @DisplayName("End-to-end: Job with persistent failures transitions to DEAD_LETTER after max attempts")
    void testDeadLetterTransition() {
        // Create DEMO_FAILURE with ALWAYS_FAIL and maxAttempts = 2
        CreateJobRequest req = new CreateJobRequest(
                "DEMO_FAILURE",
                objectMapper.createObjectNode()
                        .put("failureMode", "ALWAYS_FAIL")
                        .put("errorMessage", "Simulated hardware fault"),
                10, // High priority
                2,  // Max 2 attempts
                null, null
        );

        JobResponse submitted = jobService.createJob(req, null);
        UUID jobId = submitted.getId();

        // Wait until it exhausts 2 attempts and lands in DEAD_LETTER
        await().atMost(15, TimeUnit.SECONDS).until(() -> {
            Job j = jobRepository.findById(jobId).orElse(null);
            return j != null && j.getStatus() == JobStatus.DEAD_LETTER;
        });

        Job deadJob = jobRepository.findById(jobId).orElseThrow();
        assertEquals(JobStatus.DEAD_LETTER, deadJob.getStatus());
        assertEquals(2, deadJob.getAttemptCount());
        assertNotNull(deadJob.getLastError());
        assertTrue(deadJob.getLastError().contains("Exhausted"));

        // Verify attempt history has 2 recorded attempts
        List<JobAttempt> attempts = jobAttemptRepository.findByJobIdOrderByAttemptNumberAsc(jobId);
        assertEquals(2, attempts.size());

        // Test manual retry of the dead letter job
        JobResponse retried = jobService.retryJob(jobId);
        assertEquals(JobStatus.QUEUED, retried.getStatus());
        assertNull(retried.getLastError());
    }
}
