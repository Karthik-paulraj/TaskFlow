package com.taskflow.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.exception.InvalidJobPayloadException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class ReportGenerationHandler implements JobHandler {

    private static final String JOB_TYPE = "REPORT_GENERATION";
    private final ObjectMapper objectMapper;

    public ReportGenerationHandler(ObjectMapper objectMapper) {
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
            if (!root.hasNonNull("reportType") || root.get("reportType").asText().isBlank()) {
                throw new InvalidJobPayloadException("Field 'reportType' is required and must not be blank");
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
        String reportType = root.get("reportType").asText();

        // Simulate report generation computation (120ms)
        Thread.sleep(120);

        String reportId = "rep_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Map<String, Object> summary = Map.of(
                "reportId", reportId,
                "reportType", reportType,
                "generatedAt", Instant.now().toString(),
                "totalRecordsEvaluated", 1450,
                "metrics", Map.of(
                        "successRate", 99.4,
                        "p95LatencyMs", 42.5,
                        "resourceUsagePercent", 38.2
                ),
                "generatedBy", context.workerId()
        );

        return JobExecutionResult.success(objectMapper.writeValueAsString(summary));
    }
}
