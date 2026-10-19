package com.taskflow.handler;

import java.util.UUID;

public record JobContext(
        UUID jobId,
        int attemptNumber,
        String payloadJson,
        String workerId
) {}
