package com.taskflow.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.exception.InvalidJobPayloadException;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class DataProcessingHandler implements JobHandler {

    private static final String JOB_TYPE = "DATA_PROCESSING";
    private final ObjectMapper objectMapper;

    public DataProcessingHandler(ObjectMapper objectMapper) {
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
            if (root.has("batchSize")) {
                int size = root.get("batchSize").asInt();
                if (size < 1 || size > 50000) {
                    throw new InvalidJobPayloadException("Field 'batchSize' must be between 1 and 50,000");
                }
            } else if (!root.has("records") || !root.get("records").isArray()) {
                throw new InvalidJobPayloadException("Payload must contain either an integer 'batchSize' or an array 'records'");
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

        int count;
        long sum = 0;
        int processed = 0;

        if (root.has("records") && root.get("records").isArray()) {
            JsonNode records = root.get("records");
            count = records.size();
            for (JsonNode rec : records) {
                if (rec.isNumber()) {
                    sum += rec.asLong();
                } else if (rec.isObject() && rec.has("value") && rec.get("value").isNumber()) {
                    sum += rec.get("value").asLong();
                }
                processed++;
            }
        } else {
            count = root.path("batchSize").asInt(100);
            processed = count;
            sum = (long) count * (count + 1) / 2;
        }

        // Simulate proportional compute time (capped at 100ms)
        Thread.sleep(Math.min(count, 100));

        Map<String, Object> result = new HashMap<>();
        result.put("status", "COMPLETED");
        result.put("recordsProcessed", processed);
        result.put("calculatedAggregate", sum);
        result.put("processedByWorker", context.workerId());

        return JobExecutionResult.success(objectMapper.writeValueAsString(result));
    }
}
