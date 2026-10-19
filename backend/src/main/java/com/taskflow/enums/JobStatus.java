package com.taskflow.enums;

import java.util.Set;

/**
 * Valid states and transition rules for background jobs in TaskFlow.
 */
public enum JobStatus {
    QUEUED,
    RUNNING,
    SUCCEEDED,
    RETRY_WAIT,
    FAILED,
    DEAD_LETTER,
    CANCELLED;

    public boolean isTerminal() {
        return this == SUCCEEDED || this == FAILED || this == DEAD_LETTER || this == CANCELLED;
    }

    public boolean canTransitionTo(JobStatus nextStatus) {
        if (this == nextStatus) {
            return true;
        }
        return switch (this) {
            case QUEUED -> nextStatus == RUNNING || nextStatus == CANCELLED;
            case RUNNING -> nextStatus == SUCCEEDED || nextStatus == RETRY_WAIT || nextStatus == FAILED || nextStatus == DEAD_LETTER;
            case RETRY_WAIT -> nextStatus == QUEUED || nextStatus == RUNNING || nextStatus == CANCELLED;
            case FAILED, DEAD_LETTER -> nextStatus == QUEUED; // Allowed for manual retry
            case SUCCEEDED, CANCELLED -> false;
        };
    }

    public static Set<JobStatus> eligibleForClaiming() {
        return Set.of(QUEUED, RETRY_WAIT);
    }
}
