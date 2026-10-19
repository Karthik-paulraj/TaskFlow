package com.taskflow.exception;

public class DuplicateIdempotencyKeyException extends TaskFlowException {
    public DuplicateIdempotencyKeyException(String message) {
        super(message);
    }
}
