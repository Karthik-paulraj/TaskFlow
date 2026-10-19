package com.taskflow.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public class CreateJobRequest {

    @NotBlank(message = "Job type is required")
    @Size(max = 64, message = "Job type must not exceed 64 characters")
    private String jobType;

    private JsonNode payload;

    @Min(value = 1, message = "Priority must be at least 1")
    @Max(value = 10, message = "Priority must not exceed 10")
    private Integer priority = 5;

    @Min(value = 1, message = "Max attempts must be at least 1")
    @Max(value = 20, message = "Max attempts must not exceed 20")
    private Integer maxAttempts = 3;

    private Instant scheduledAt;

    @Size(max = 128, message = "Idempotency key must not exceed 128 characters")
    private String idempotencyKey;

    public CreateJobRequest() {
    }

    public CreateJobRequest(String jobType, JsonNode payload, Integer priority, Integer maxAttempts, Instant scheduledAt, String idempotencyKey) {
        this.jobType = jobType;
        this.payload = payload;
        this.priority = priority != null ? priority : 5;
        this.maxAttempts = maxAttempts != null ? maxAttempts : 3;
        this.scheduledAt = scheduledAt;
        this.idempotencyKey = idempotencyKey;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public JsonNode getPayload() {
        return payload;
    }

    public void setPayload(JsonNode payload) {
        this.payload = payload;
    }

    public Integer getPriority() {
        return priority != null ? priority : 5;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Integer getMaxAttempts() {
        return maxAttempts != null ? maxAttempts : 3;
    }

    public void setMaxAttempts(Integer maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
