package com.taskflow.dto;

import com.taskflow.entity.JobAttempt;
import com.taskflow.enums.ExecutionOutcome;
import java.time.Instant;
import java.util.UUID;

public class JobAttemptResponse {
    private UUID id;
    private UUID jobId;
    private int attemptNumber;
    private String workerId;
    private Instant startedAt;
    private Instant completedAt;
    private ExecutionOutcome outcome;
    private String errorDetails;
    private Long executionDurationMs;

    public JobAttemptResponse() {
    }

    public static JobAttemptResponse fromEntity(JobAttempt attempt) {
        JobAttemptResponse response = new JobAttemptResponse();
        response.setId(attempt.getId());
        response.setJobId(attempt.getJobId());
        response.setAttemptNumber(attempt.getAttemptNumber());
        response.setWorkerId(attempt.getWorkerId());
        response.setStartedAt(attempt.getStartedAt());
        response.setCompletedAt(attempt.getCompletedAt());
        response.setOutcome(attempt.getOutcome());
        response.setErrorDetails(attempt.getErrorDetails());
        response.setExecutionDurationMs(attempt.getExecutionDurationMs());
        return response;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getJobId() {
        return jobId;
    }

    public void setJobId(UUID jobId) {
        this.jobId = jobId;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(int attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public ExecutionOutcome getOutcome() {
        return outcome;
    }

    public void setOutcome(ExecutionOutcome outcome) {
        this.outcome = outcome;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
    }

    public Long getExecutionDurationMs() {
        return executionDurationMs;
    }

    public void setExecutionDurationMs(Long executionDurationMs) {
        this.executionDurationMs = executionDurationMs;
    }
}
