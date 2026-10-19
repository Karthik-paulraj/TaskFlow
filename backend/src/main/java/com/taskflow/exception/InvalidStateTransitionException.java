package com.taskflow.exception;

public class InvalidStateTransitionException extends TaskFlowException {
    public InvalidStateTransitionException(String message) {
        super(message);
    }
}
