package com.taskflow.service;

import com.taskflow.config.TaskFlowProperties;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class RetryService {

    private final TaskFlowProperties properties;

    public RetryService(TaskFlowProperties properties) {
        this.properties = properties;
    }

    /**
     * Calculates the next retry timestamp using exponential backoff with jitter:
     * delay = min(initialDelay * 2^(attemptNumber - 1), maxDelay) +/- jitter
     */
    public Instant calculateNextRetry(int attemptNumber) {
        long initialDelay = properties.getRetry().getInitialDelaySeconds();
        long maxDelay = properties.getRetry().getMaxDelaySeconds();
        double jitterFactor = properties.getRetry().getJitterFactor();

        long baseDelay;
        if (attemptNumber <= 1) {
            baseDelay = initialDelay;
        } else if (attemptNumber > 30) {
            baseDelay = maxDelay;
        } else {
            // Safe shift to prevent overflow
            long multiplier = 1L << (attemptNumber - 1);
            try {
                baseDelay = Math.min(Math.multiplyExact(initialDelay, multiplier), maxDelay);
            } catch (ArithmeticException e) {
                baseDelay = maxDelay;
            }
        }

        // Apply jitter
        double jitterRange = baseDelay * jitterFactor;
        double jitter = (ThreadLocalRandom.current().nextDouble() * 2 - 1) * jitterRange;
        long finalDelaySeconds = Math.max(1, Math.round(baseDelay + jitter));

        return Instant.now().plusSeconds(finalDelaySeconds);
    }

    /**
     * Checks if a job has reached or exceeded its maximum allowed execution attempts.
     */
    public boolean isExhausted(int attemptCount, int maxAttempts) {
        int limit = maxAttempts > 0 ? maxAttempts : properties.getRetry().getDefaultMaxAttempts();
        return attemptCount >= limit;
    }
}
