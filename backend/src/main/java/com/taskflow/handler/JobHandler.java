package com.taskflow.handler;

/**
 * Pluggable handler interface for background job execution.
 */
public interface JobHandler {

    /**
     * The unique job type identifier handled by this component.
     */
    String getJobType();

    /**
     * Validates the job payload JSON string.
     * Throws InvalidJobPayloadException if validation fails.
     */
    void validatePayload(String payloadJson);

    /**
     * Executes the job business logic.
     */
    JobExecutionResult execute(JobContext context) throws Exception;
}
