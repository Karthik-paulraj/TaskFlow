package com.taskflow.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JobStatusTest {

    @Test
    @DisplayName("Verify valid state transitions")
    void testValidTransitions() {
        assertTrue(JobStatus.QUEUED.canTransitionTo(JobStatus.RUNNING));
        assertTrue(JobStatus.QUEUED.canTransitionTo(JobStatus.CANCELLED));

        assertTrue(JobStatus.RUNNING.canTransitionTo(JobStatus.SUCCEEDED));
        assertTrue(JobStatus.RUNNING.canTransitionTo(JobStatus.RETRY_WAIT));
        assertTrue(JobStatus.RUNNING.canTransitionTo(JobStatus.FAILED));
        assertTrue(JobStatus.RUNNING.canTransitionTo(JobStatus.DEAD_LETTER));

        assertTrue(JobStatus.RETRY_WAIT.canTransitionTo(JobStatus.QUEUED));
        assertTrue(JobStatus.RETRY_WAIT.canTransitionTo(JobStatus.RUNNING));
        assertTrue(JobStatus.RETRY_WAIT.canTransitionTo(JobStatus.CANCELLED));

        assertTrue(JobStatus.FAILED.canTransitionTo(JobStatus.QUEUED)); // Manual retry
        assertTrue(JobStatus.DEAD_LETTER.canTransitionTo(JobStatus.QUEUED)); // Manual retry
    }

    @Test
    @DisplayName("Verify invalid state transitions are rejected")
    void testInvalidTransitions() {
        assertFalse(JobStatus.SUCCEEDED.canTransitionTo(JobStatus.RUNNING));
        assertFalse(JobStatus.SUCCEEDED.canTransitionTo(JobStatus.QUEUED));
        assertFalse(JobStatus.CANCELLED.canTransitionTo(JobStatus.RUNNING));
        assertFalse(JobStatus.CANCELLED.canTransitionTo(JobStatus.QUEUED));

        assertFalse(JobStatus.QUEUED.canTransitionTo(JobStatus.SUCCEEDED));
        assertFalse(JobStatus.QUEUED.canTransitionTo(JobStatus.DEAD_LETTER));
    }

    @Test
    @DisplayName("Verify terminal status classifications")
    void testTerminalStates() {
        assertTrue(JobStatus.SUCCEEDED.isTerminal());
        assertTrue(JobStatus.FAILED.isTerminal());
        assertTrue(JobStatus.DEAD_LETTER.isTerminal());
        assertTrue(JobStatus.CANCELLED.isTerminal());

        assertFalse(JobStatus.QUEUED.isTerminal());
        assertFalse(JobStatus.RUNNING.isTerminal());
        assertFalse(JobStatus.RETRY_WAIT.isTerminal());
    }
}
