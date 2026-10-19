package com.taskflow.benchmark;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.dto.CreateJobRequest;
import com.taskflow.dto.JobResponse;
import com.taskflow.entity.Job;
import com.taskflow.enums.JobStatus;
import com.taskflow.repository.JobRepository;
import com.taskflow.service.JobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
public class TaskFlowBenchmarkTest {

    @Autowired
    private JobService jobService;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void clean() {
        jobRepository.deleteAll();
    }

    @Test
    @DisplayName("Execute Repeatable Performance Benchmark")
    void executeBenchmark() {
        int jobCount = 60;
        System.out.println("================================================================================");
        System.out.println("                    TASKFLOW PERFORMANCE BENCHMARK REPORT                      ");
        System.out.println("================================================================================");

        List<UUID> submittedJobIds = new ArrayList<>();

        // 1. Measure Job Submission Throughput
        long submitStart = System.currentTimeMillis();
        for (int i = 0; i < jobCount; i++) {
            int priority = (i % 10) + 1;
            CreateJobRequest req = new CreateJobRequest(
                    "DATA_PROCESSING",
                    objectMapper.createObjectNode().put("batchSize", 20),
                    priority,
                    3,
                    null,
                    null
            );
            JobResponse resp = jobService.createJob(req, null);
            submittedJobIds.add(resp.getId());
        }
        long submitEnd = System.currentTimeMillis();
        long submitDurationMs = Math.max(1, submitEnd - submitStart);
        double submitThroughput = (jobCount * 1000.0) / submitDurationMs;

        System.out.printf("Submitted %d jobs in %d ms (Submission Throughput: %.2f jobs/sec)%n",
                jobCount, submitDurationMs, submitThroughput);

        // 2. Measure Processing & Completion Throughput
        long processStart = System.currentTimeMillis();
        await().atMost(30, TimeUnit.SECONDS).until(() -> {
            long completed = jobRepository.findAll().stream()
                    .filter(j -> j.getStatus() == JobStatus.SUCCEEDED || j.getStatus() == JobStatus.FAILED)
                    .count();
            return completed >= jobCount;
        });
        long processEnd = System.currentTimeMillis();
        long processDurationMs = Math.max(1, processEnd - processStart);
        double processThroughput = (jobCount * 1000.0) / processDurationMs;

        List<Job> finishedJobs = jobRepository.findAll();
        long succeededCount = finishedJobs.stream().filter(j -> j.getStatus() == JobStatus.SUCCEEDED).count();
        long failedCount = finishedJobs.stream().filter(j -> j.getStatus() == JobStatus.FAILED || j.getStatus() == JobStatus.DEAD_LETTER).count();

        List<Long> durations = finishedJobs.stream()
                .map(Job::getExecutionDurationMs)
                .filter(Objects::nonNull)
                .sorted()
                .toList();

        double avgDuration = durations.stream().mapToLong(Long::longValue).average().orElse(0.0);
        long p50Duration = durations.isEmpty() ? 0 : durations.get((int) (durations.size() * 0.50));
        long p95Duration = durations.isEmpty() ? 0 : durations.get((int) (durations.size() * 0.95));
        long p99Duration = durations.isEmpty() ? 0 : durations.get(Math.min(durations.size() - 1, (int) (durations.size() * 0.99)));

        System.out.printf("Total Completed: %d (Succeeded: %d, Failed: %d)%n", finishedJobs.size(), succeededCount, failedCount);
        System.out.printf("Processing Duration: %d ms (Throughput: %.2f jobs/sec)%n", processDurationMs, processThroughput);
        System.out.printf("Execution Latency: Avg = %.2f ms, P50 = %d ms, P95 = %d ms, P99 = %d ms%n",
                avgDuration, p50Duration, p95Duration, p99Duration);
        System.out.println("================================================================================");
    }
}
