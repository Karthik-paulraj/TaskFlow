package com.taskflow.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.exception.InvalidJobPayloadException;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DemoFailureHandler implements JobHandler {

    private static final String JOB_TYPE = "DEMO_FAILURE";
    private final ObjectMapper objectMapper;

    public DemoFailureHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String getJobType() {
        return JOB_TYPE;
    }

    @Override
    public void validatePayload(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            return; // Empty payload is permitted, defaults to ALWAYS_FAIL
        }
        try {
            JsonNode root = objectMapper.readTree(payloadJson);
            if (root.has("failUntilAttempt")) {
                int failUntil = root.get("failUntilAttempt").asInt();
                if (failUntil < 1) {
                    throw new InvalidJobPayloadException("Field 'failUntilAttempt' must be >= 1");
                }
            }
        } catch (InvalidJobPayloadException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidJobPayloadException("Invalid JSON payload for " + JOB_TYPE + ": " + e.getMessage());
        }
    }

    @Override
    public JobExecutionResult execute(JobContext context) throws Exception {
        JsonNode root = null;
        if (context.payloadJson() != null && !context.payloadJson().isBlank()) {
            root = objectMapper.readTree(context.payloadJson());
        }

        String mode = (root != null && root.hasNonNull("failureMode"))
                ? root.get("failureMode").asText().toUpperCase()
                : "ALWAYS_FAIL";

        String customError = (root != null && root.hasNonNull("errorMessage"))
                ? root.get("errorMessage").asText()
                : "Simulated intentional failure during attempt #" + context.attemptNumber();

        int failUntil = (root != null && root.hasNonNull("failUntilAttempt"))
                ? root.get("failUntilAttempt").asInt()
                : Integer.MAX_VALUE;

        // Small processing delay
        Thread.sleep(50);

        if ("PERMANENT_ERROR".equals(mode)) {
            return JobExecutionResult.terminalFailure("Non-retryable terminal failure: " + customError);
        }

        if ("FAIL_N_TIMES".equals(mode)) {
            if (context.attemptNumber() <= failUntil) {
                return JobExecutionResult.retryableFailure(
                        "Controlled failure on attempt " + context.attemptNumber() + " (will fail until attempt " + failUntil + ")"
                );
            }
            // Succeeded on subsequent attempt!
            Map<String, Object> recoveryResult = Map.of(
                    "status", "RECOVERED_SUCCESSFULLY",
                    "recoveredOnAttempt", context.attemptNumber(),
                    "message", "Job successfully recovered after " + failUntil + " initial failures",
                    "processedByWorker", context.workerId()
            );
            return JobExecutionResult.success(objectMapper.writeValueAsString(recoveryResult));
        }

        // Default: ALWAYS_FAIL (retryable until max attempts exhausted -> dead letter)
        return JobExecutionResult.retryableFailure(customError);
    }
}
