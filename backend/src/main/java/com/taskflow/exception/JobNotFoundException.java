package com.taskflow.exception;

import java.util.UUID;

public class JobNotFoundException extends TaskFlowException {
    public JobNotFoundException(UUID id) {
        super("Job not found with ID: " + id);
    }
}
