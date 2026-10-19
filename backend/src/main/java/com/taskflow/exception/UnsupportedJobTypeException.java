package com.taskflow.exception;

public class UnsupportedJobTypeException extends TaskFlowException {
    public UnsupportedJobTypeException(String jobType) {
        super("Unsupported job type: " + jobType);
    }
}
