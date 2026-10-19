package com.taskflow.concurrency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.dto.CreateJobRequest;
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

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ConcurrencyIntegrationTest {

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
    @DisplayName("Concurrent database claiming: no job is claimed by multiple workers simultaneously")
    void testConcurrentAtomicClaiming() throws Exception {
        int jobCount = 15;
        List<UUID> jobIds = new ArrayList<>();

        for (int i = 0; i < jobCount; i++) {
            CreateJobRequest req = new CreateJobRequest(
                    "DATA_PROCESSING",
                    objectMapper.createObjectNode().put("batchSize", 50),
                    (i % 10) + 1, // Varied priorities
                    3,
                    null,
                    null
            );
            jobIds.add(jobService.createJob(req, null).getId());
        }

        // Wait for all 15 jobs to be processed to completion
        await().atMost(25, TimeUnit.SECONDS).until(() -> {
            long completed = jobRepository.findAll().stream()
                    .filter(j -> j.getStatus() == JobStatus.SUCCEEDED)
                    .count();
            return completed == jobCount;
        });

        // Verify each job has exactly 1 attempt and status is SUCCEEDED
        for (UUID jobId : jobIds) {
            Job job = jobRepository.findById(jobId).orElseThrow();
            assertEquals(JobStatus.SUCCEEDED, job.getStatus());
            assertEquals(1, job.getAttemptCount());

            List<JobAttempt> attempts = jobAttemptRepository.findByJobIdOrderByAttemptNumberAsc(jobId);
            assertEquals(1, attempts.size(), "Job " + jobId + " had unexpected attempt count!");
        }
    }
}
