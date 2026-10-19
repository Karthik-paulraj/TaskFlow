package com.taskflow.entity;

import com.taskflow.enums.ExecutionOutcome;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Historical record of an execution attempt for a Job.
 */
@Entity
@Table(name = "job_attempts")
public class JobAttempt {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Column(name = "worker_id", nullable = false, length = 128)
    private String workerId;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 32)
    private ExecutionOutcome outcome;

    @Column(name = "error_details", columnDefinition = "TEXT")
    private String errorDetails;

    @Column(name = "execution_duration_ms", nullable = false)
    private Long executionDurationMs;

    public JobAttempt() {
    }

    public JobAttempt(UUID jobId, int attemptNumber, String workerId, Instant startedAt, Instant completedAt, ExecutionOutcome outcome, String errorDetails, Long executionDurationMs) {
        this.id = UUID.randomUUID();
        this.jobId = jobId;
        this.attemptNumber = attemptNumber;
        this.workerId = workerId;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.outcome = outcome;
        this.errorDetails = errorDetails;
        this.executionDurationMs = executionDurationMs;
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
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
