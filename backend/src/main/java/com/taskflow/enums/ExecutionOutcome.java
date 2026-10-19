package com.taskflow.enums;

/**
 * Outcome of an individual job execution attempt.
 */
public enum ExecutionOutcome {
    SUCCESS,
    FAILURE,
    RETRYABLE_FAILURE,
    TIMEOUT
}
