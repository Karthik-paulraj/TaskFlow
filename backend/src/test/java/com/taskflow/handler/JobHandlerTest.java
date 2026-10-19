package com.taskflow.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.exception.InvalidJobPayloadException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JobHandlerTest {

    private ObjectMapper objectMapper;
    private EmailSimulationHandler emailHandler;
    private ReportGenerationHandler reportHandler;
    private DataProcessingHandler dataHandler;
    private DemoFailureHandler failureHandler;
    private DelayedTaskHandler delayedHandler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        emailHandler = new EmailSimulationHandler(objectMapper);
        reportHandler = new ReportGenerationHandler(objectMapper);
        dataHandler = new DataProcessingHandler(objectMapper);
        failureHandler = new DemoFailureHandler(objectMapper);
        delayedHandler = new DelayedTaskHandler(objectMapper);
    }

    @Test
    @DisplayName("EmailSimulationHandler validates required fields")
    void testEmailValidation() {
        assertThrows(InvalidJobPayloadException.class, () -> emailHandler.validatePayload("{}"));
        assertThrows(InvalidJobPayloadException.class, () -> emailHandler.validatePayload("{\"to\":\"not-an-email\"}"));
        assertDoesNotThrow(() -> emailHandler.validatePayload("{\"to\":\"user@example.com\",\"subject\":\"Test\"}"));
    }

    @Test
    @DisplayName("EmailSimulationHandler executes and returns dispatched result")
    void testEmailExecution() throws Exception {
        JobContext ctx = new JobContext(
                UUID.randomUUID(), 1, "{\"to\":\"user@example.com\",\"subject\":\"Welcome\",\"body\":\"Hello!\"}", "worker-1"
        );
        JobExecutionResult result = emailHandler.execute(ctx);
        assertTrue(result.success());
        assertTrue(result.resultJson().contains("DISPATCHED"));
        assertTrue(result.resultJson().contains("user@example.com"));
    }

    @Test
    @DisplayName("ReportGenerationHandler validates reportType")
    void testReportValidation() {
        assertThrows(InvalidJobPayloadException.class, () -> reportHandler.validatePayload("{}"));
        assertDoesNotThrow(() -> reportHandler.validatePayload("{\"reportType\":\"DAILY_SUMMARY\"}"));
    }

    @Test
    @DisplayName("ReportGenerationHandler executes and returns report statistics")
    void testReportExecution() throws Exception {
        JobContext ctx = new JobContext(
                UUID.randomUUID(), 1, "{\"reportType\":\"DAILY_SUMMARY\"}", "worker-1"
        );
        JobExecutionResult result = reportHandler.execute(ctx);
        assertTrue(result.success());
        assertTrue(result.resultJson().contains("DAILY_SUMMARY"));
        assertTrue(result.resultJson().contains("metrics"));
    }

    @Test
    @DisplayName("DataProcessingHandler calculates correct aggregate")
    void testDataProcessingExecution() throws Exception {
        JobContext ctx = new JobContext(
                UUID.randomUUID(), 1, "{\"records\":[10, 20, 30, 40]}", "worker-1"
        );
        JobExecutionResult result = dataHandler.execute(ctx);
        assertTrue(result.success());
        assertTrue(result.resultJson().contains("\"recordsProcessed\":4"));
        assertTrue(result.resultJson().contains("\"calculatedAggregate\":100"));
    }

    @Test
    @DisplayName("DemoFailureHandler handles FAIL_N_TIMES mode")
    void testDemoFailureRecovery() throws Exception {
        String payload = "{\"failureMode\":\"FAIL_N_TIMES\",\"failUntilAttempt\":2}";

        // Attempt 1 -> fails
        JobContext ctx1 = new JobContext(UUID.randomUUID(), 1, payload, "worker-1");
        JobExecutionResult res1 = failureHandler.execute(ctx1);
        assertFalse(res1.success());
        assertTrue(res1.retryable());

        // Attempt 2 -> fails
        JobContext ctx2 = new JobContext(UUID.randomUUID(), 2, payload, "worker-1");
        JobExecutionResult res2 = failureHandler.execute(ctx2);
        assertFalse(res2.success());
        assertTrue(res2.retryable());

        // Attempt 3 -> succeeds!
        JobContext ctx3 = new JobContext(UUID.randomUUID(), 3, payload, "worker-1");
        JobExecutionResult res3 = failureHandler.execute(ctx3);
        assertTrue(res3.success());
        assertTrue(res3.resultJson().contains("RECOVERED_SUCCESSFULLY"));
    }

    @Test
    @DisplayName("DelayedTaskHandler validates maximum duration bound")
    void testDelayedTaskValidation() {
        assertThrows(InvalidJobPayloadException.class, () -> delayedHandler.validatePayload("{\"durationMs\":999999}"));
        assertDoesNotThrow(() -> delayedHandler.validatePayload("{\"durationMs\":100}"));
    }
}
