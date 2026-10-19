package com.taskflow.controller;

import com.taskflow.dto.*;
import com.taskflow.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
@Tag(name = "Jobs", description = "Background job creation, execution history, and lifecycle management operations")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    @Operation(summary = "Submit a background job", description = "Validates and enqueues a background job with optional priority, schedule, and idempotency key.")
    public ResponseEntity<JobResponse> createJob(
            @Valid @RequestBody CreateJobRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKeyHeader) {
        JobResponse created = jobService.createJob(request, idempotencyKeyHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/bulk")
    @Operation(summary = "Submit multiple jobs in bulk", description = "Submits up to 50 jobs in a single request with partial failure handling.")
    public ResponseEntity<BulkCreateJobResponse> createBulkJobs(@Valid @RequestBody BulkCreateJobRequest request) {
        BulkCreateJobResponse response = jobService.createBulkJobs(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "List and search jobs", description = "Returns a paginated list of background jobs matching optional status, type, priority, or search term filters.")
    public ResponseEntity<PagedResponse<JobResponse>> listJobs(@ModelAttribute JobFilterRequest filter) {
        PagedResponse<JobResponse> response = jobService.listJobs(filter);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get job details", description = "Retrieves complete metadata, status, payload, and result for a specific job ID.")
    public ResponseEntity<JobResponse> getJobById(@PathVariable("id") UUID id) {
        JobResponse response = jobService.getJobById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/attempts")
    @Operation(summary = "Get job attempt history", description = "Retrieves all execution attempts, outcomes, durations, and errors recorded for a job.")
    public ResponseEntity<List<JobAttemptResponse>> getJobAttempts(@PathVariable("id") UUID id) {
        List<JobAttemptResponse> attempts = jobService.getJobAttempts(id);
        return ResponseEntity.ok(attempts);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a queued job", description = "Cancels an eligible QUEUED or RETRY_WAIT job so it is never claimed by a worker.")
    public ResponseEntity<JobResponse> cancelJob(@PathVariable("id") UUID id) {
        JobResponse response = jobService.cancelJob(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/retry")
    @Operation(summary = "Manually retry a failed or dead-letter job", description = "Re-queues a FAILED or DEAD_LETTER job for fresh execution.")
    public ResponseEntity<JobResponse> retryJob(@PathVariable("id") UUID id) {
        JobResponse response = jobService.retryJob(id);
        return ResponseEntity.ok(response);
    }
}
