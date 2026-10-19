package com.taskflow.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.entity.Job;
import com.taskflow.enums.JobStatus;
import java.time.Instant;
import java.util.UUID;

public class JobResponse {
    private UUID id;
    private String jobType;
    private Object payload;
    private JobStatus status;
    private int priority;
    private Instant createdAt;
    private Instant scheduledAt;
    private Instant startedAt;
    private Instant completedAt;
    private Instant updatedAt;
    private int attemptCount;
    private int maxAttempts;
    private Instant nextRetryAt;
    private Object result;
    private String lastError;
    private String idempotencyKey;
    private String workerId;
    private Long executionDurationMs;

    public JobResponse() {
    }

    public static JobResponse fromEntity(Job job, ObjectMapper objectMapper) {
        JobResponse response = new JobResponse();
        response.setId(job.getId());
        response.setJobType(job.getJobType());
        response.setStatus(job.getStatus());
        response.setPriority(job.getPriority());
        response.setCreatedAt(job.getCreatedAt());
        response.setScheduledAt(job.getScheduledAt());
        response.setStartedAt(job.getStartedAt());
        response.setCompletedAt(job.getCompletedAt());
        response.setUpdatedAt(job.getUpdatedAt());
        response.setAttemptCount(job.getAttemptCount());
        response.setMaxAttempts(job.getMaxAttempts());
        response.setNextRetryAt(job.getNextRetryAt());
        response.setLastError(job.getLastError());
        response.setIdempotencyKey(job.getIdempotencyKey());
        response.setWorkerId(job.getWorkerId());
        response.setExecutionDurationMs(job.getExecutionDurationMs());

        if (job.getPayload() != null && objectMapper != null) {
            try {
                response.setPayload(objectMapper.readTree(job.getPayload()));
            } catch (Exception e) {
                response.setPayload(job.getPayload());
            }
        } else {
            response.setPayload(job.getPayload());
        }

        if (job.getResult() != null && objectMapper != null) {
            try {
                response.setResult(objectMapper.readTree(job.getResult()));
            } catch (Exception e) {
                response.setResult(job.getResult());
            }
        } else {
            response.setResult(job.getResult());
        }

        return response;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Instant getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(Instant nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public Long getExecutionDurationMs() {
        return executionDurationMs;
    }

    public void setExecutionDurationMs(Long executionDurationMs) {
        this.executionDurationMs = executionDurationMs;
    }
}
