package com.taskflow.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.exception.InvalidJobPayloadException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class EmailSimulationHandler implements JobHandler {

    private static final String JOB_TYPE = "EMAIL_SIMULATION";
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private final ObjectMapper objectMapper;

    public EmailSimulationHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String getJobType() {
        return JOB_TYPE;
    }

    @Override
    public void validatePayload(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            throw new InvalidJobPayloadException("Payload cannot be empty for " + JOB_TYPE);
        }
        try {
            JsonNode root = objectMapper.readTree(payloadJson);
            if (!root.hasNonNull("to") || root.get("to").asText().isBlank()) {
                throw new InvalidJobPayloadException("Field 'to' is required and must not be blank");
            }
            String to = root.get("to").asText();
            if (!EMAIL_PATTERN.matcher(to).matches()) {
                throw new InvalidJobPayloadException("Field 'to' must be a valid email address: " + to);
            }
            if (!root.hasNonNull("subject") || root.get("subject").asText().isBlank()) {
                throw new InvalidJobPayloadException("Field 'subject' is required and must not be blank");
            }
        } catch (InvalidJobPayloadException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidJobPayloadException("Invalid JSON payload for " + JOB_TYPE + ": " + e.getMessage());
        }
    }

    @Override
    public JobExecutionResult execute(JobContext context) throws Exception {
        JsonNode root = objectMapper.readTree(context.payloadJson());
        String to = root.get("to").asText();
        String subject = root.get("subject").asText();
        String body = root.has("body") ? root.get("body").asText() : "";

        // Simulate network dispatch delay safely (50-150ms)
        Thread.sleep(80);

        String messageId = "msg_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        Map<String, Object> result = Map.of(
                "status", "DISPATCHED",
                "messageId", messageId,
                "recipient", to,
                "subject", subject,
                "bytesSent", (subject.length() + body.length()),
                "dispatchedByWorker", context.workerId()
        );

        return JobExecutionResult.success(objectMapper.writeValueAsString(result));
    }
}
