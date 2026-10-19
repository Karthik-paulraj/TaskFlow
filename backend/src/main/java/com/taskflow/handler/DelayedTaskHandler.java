package com.taskflow.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.exception.InvalidJobPayloadException;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DelayedTaskHandler implements JobHandler {

    private static final String JOB_TYPE = "DELAYED_TASK";
    private static final long MAX_ALLOWED_DURATION_MS = 10000; // Prevent unbounded worker blocking
    private final ObjectMapper objectMapper;

    public DelayedTaskHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String getJobType() {
        return JOB_TYPE;
    }

    @Override
    public void validatePayload(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            return; // Empty payload defaults to 1000ms
        }
        try {
            JsonNode root = objectMapper.readTree(payloadJson);
            long duration = 1000;
            if (root.has("durationMs")) {
                duration = root.get("durationMs").asLong();
            } else if (root.has("delaySeconds")) {
                duration = root.get("delaySeconds").asLong() * 1000;
            }
            if (duration < 0 || duration > MAX_ALLOWED_DURATION_MS) {
                throw new InvalidJobPayloadException(
                        "Duration must be between 0 and " + MAX_ALLOWED_DURATION_MS + " ms (requested: " + duration + " ms)"
                );
            }
        } catch (InvalidJobPayloadException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidJobPayloadException("Invalid JSON payload for " + JOB_TYPE + ": " + e.getMessage());
        }
    }

    @Override
    public JobExecutionResult execute(JobContext context) throws Exception {
        long duration = 1000;
        if (context.payloadJson() != null && !context.payloadJson().isBlank()) {
            JsonNode root = objectMapper.readTree(context.payloadJson());
            if (root.has("durationMs")) {
                duration = root.get("durationMs").asLong();
            } else if (root.has("delaySeconds")) {
                duration = root.get("delaySeconds").asLong() * 1000;
            }
        }
        duration = Math.max(0, Math.min(duration, MAX_ALLOWED_DURATION_MS));

        long start = System.currentTimeMillis();
        Thread.sleep(duration);
        long elapsed = System.currentTimeMillis() - start;

        Map<String, Object> result = Map.of(
                "status", "COMPLETED",
                "requestedDurationMs", duration,
                "actualElapsedMs", elapsed,
                "executedByWorker", context.workerId()
        );

        return JobExecutionResult.success(objectMapper.writeValueAsString(result));
    }
}
