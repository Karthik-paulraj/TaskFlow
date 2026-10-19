package com.taskflow.service;

import com.taskflow.config.TaskFlowProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class RetryServiceTest {

    private RetryService retryService;
    private TaskFlowProperties properties;

    @BeforeEach
    void setUp() {
        properties = new TaskFlowProperties();
        properties.getRetry().setInitialDelaySeconds(2);
        properties.getRetry().setMaxDelaySeconds(60);
        properties.getRetry().setJitterFactor(0.0); // Zero jitter for deterministic unit testing
        properties.getRetry().setDefaultMaxAttempts(3);
        retryService = new RetryService(properties);
    }

    @Test
    @DisplayName("Exponential backoff doubles on subsequent attempts")
    void testExponentialBackoff() {
        Instant now = Instant.now();

        // Attempt 1 -> 2s * 2^(0) = 2s
        Instant retry1 = retryService.calculateNextRetry(1);
        long delay1 = Duration.between(now, retry1).toSeconds();
        assertEquals(2, delay1);

        // Attempt 2 -> 2s * 2^(1) = 4s
        Instant retry2 = retryService.calculateNextRetry(2);
        long delay2 = Duration.between(now, retry2).toSeconds();
        assertEquals(4, delay2);

        // Attempt 3 -> 2s * 2^(2) = 8s
        Instant retry3 = retryService.calculateNextRetry(3);
        long delay3 = Duration.between(now, retry3).toSeconds();
        assertEquals(8, delay3);

        // Attempt 4 -> 2s * 2^(3) = 16s
        Instant retry4 = retryService.calculateNextRetry(4);
        long delay4 = Duration.between(now, retry4).toSeconds();
        assertEquals(16, delay4);
    }

    @Test
    @DisplayName("Backoff is capped at maxDelaySeconds")
    void testMaxDelayCapping() {
        Instant now = Instant.now();
        // High attempt count should cap at 60s
        Instant retryHigh = retryService.calculateNextRetry(15);
        long delay = Duration.between(now, retryHigh).toSeconds();
        assertEquals(60, delay);
    }

    @Test
    @DisplayName("Attempt exhaustion checking")
    void testIsExhausted() {
        assertFalse(retryService.isExhausted(1, 3));
        assertFalse(retryService.isExhausted(2, 3));
        assertTrue(retryService.isExhausted(3, 3));
        assertTrue(retryService.isExhausted(4, 3));
    }
}
