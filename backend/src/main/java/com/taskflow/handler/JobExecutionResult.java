package com.taskflow.handler;

public record JobExecutionResult(
        boolean success,
        String resultJson,
        String errorMessage,
        boolean retryable
) {
    public static JobExecutionResult success(String resultJson) {
        return new JobExecutionResult(true, resultJson, null, false);
    }

    public static JobExecutionResult retryableFailure(String errorMessage) {
        return new JobExecutionResult(false, null, errorMessage, true);
    }

    public static JobExecutionResult terminalFailure(String errorMessage) {
        return new JobExecutionResult(false, null, errorMessage, false);
    }
}
