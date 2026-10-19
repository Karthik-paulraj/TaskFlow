package com.taskflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.config.TaskFlowProperties;
import com.taskflow.dto.*;
import com.taskflow.entity.Job;
import com.taskflow.entity.JobAttempt;
import com.taskflow.enums.JobStatus;
import com.taskflow.exception.DuplicateIdempotencyKeyException;
import com.taskflow.exception.InvalidJobPayloadException;
import com.taskflow.exception.InvalidStateTransitionException;
import com.taskflow.exception.JobNotFoundException;
import com.taskflow.handler.JobHandler;
import com.taskflow.handler.JobHandlerRegistry;
import com.taskflow.repository.JobAttemptRepository;
import com.taskflow.repository.JobRepository;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class JobService {

    private static final Logger log = LoggerFactory.getLogger(JobService.class);
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "priority", "status", "updatedAt", "executionDurationMs", "scheduledAt"
    );

    private final JobRepository jobRepository;
    private final JobAttemptRepository jobAttemptRepository;
    private final JobHandlerRegistry handlerRegistry;
    private final TaskFlowProperties properties;
    private final ObjectMapper objectMapper;

    public JobService(JobRepository jobRepository,
                      JobAttemptRepository jobAttemptRepository,
                      JobHandlerRegistry handlerRegistry,
                      TaskFlowProperties properties,
                      ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.jobAttemptRepository = jobAttemptRepository;
        this.handlerRegistry = handlerRegistry;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public JobResponse createJob(CreateJobRequest request, String headerIdempotencyKey) {
        String idempotencyKey = (headerIdempotencyKey != null && !headerIdempotencyKey.isBlank())
                ? headerIdempotencyKey
                : request.getIdempotencyKey();

        String payloadJson = serializePayload(request.getPayload());

        // Validate handler and payload
        JobHandler handler = handlerRegistry.getHandler(request.getJobType());
        handler.validatePayload(payloadJson);

        int priority = request.getPriority() != null ? Math.max(1, Math.min(10, request.getPriority())) : 5;
        int maxAttempts = request.getMaxAttempts() != null ? Math.max(1, Math.min(20, request.getMaxAttempts())) : properties.getRetry().getDefaultMaxAttempts();
        Instant scheduledAt = request.getScheduledAt() != null ? request.getScheduledAt() : Instant.now();

        // Idempotency check
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<Job> existingOpt = jobRepository.findByIdempotencyKey(idempotencyKey);
            if (existingOpt.isPresent()) {
                Job existing = existingOpt.get();
                if (isMatchingJob(existing, request.getJobType(), priority, payloadJson)) {
                    log.info("Returning existing job {} for idempotency key {}", existing.getId(), idempotencyKey);
                    return JobResponse.fromEntity(existing, objectMapper);
                } else {
                    throw new DuplicateIdempotencyKeyException(
                            "Conflict: An existing job with idempotency key '" + idempotencyKey + "' has different parameters"
                    );
                }
            }
        }

        Job job = new Job(
                UUID.randomUUID(),
                request.getJobType().toUpperCase(),
                payloadJson,
                priority,
                maxAttempts,
                scheduledAt,
                idempotencyKey
        );

        Job saved = jobRepository.save(job);
        log.info("Created job {} with type {} and priority {}", saved.getId(), saved.getJobType(), saved.getPriority());
        return JobResponse.fromEntity(saved, objectMapper);
    }

    @Transactional
    public BulkCreateJobResponse createBulkJobs(BulkCreateJobRequest bulkRequest) {
        List<JobResponse> submittedJobs = new ArrayList<>();
        List<BulkCreateJobResponse.BulkJobFailure> failures = new ArrayList<>();

        List<CreateJobRequest> jobRequests = bulkRequest.getJobs();
        for (int i = 0; i < jobRequests.size(); i++) {
            CreateJobRequest req = jobRequests.get(i);
            try {
                JobResponse created = createJob(req, null);
                submittedJobs.add(created);
            } catch (Exception e) {
                log.warn("Bulk job at index {} failed: {}", i, e.getMessage());
                failures.add(new BulkCreateJobResponse.BulkJobFailure(i, req.getJobType(), e.getMessage()));
            }
        }

        return new BulkCreateJobResponse(submittedJobs, failures);
    }

    @Transactional(readOnly = true)
    public JobResponse getJobById(UUID id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException(id));
        return JobResponse.fromEntity(job, objectMapper);
    }

    @Transactional(readOnly = true)
    public PagedResponse<JobResponse> listJobs(JobFilterRequest filter) {
        String sortField = ALLOWED_SORT_FIELDS.contains(filter.getSortBy()) ? filter.getSortBy() : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(filter.getSortDirection()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), Sort.by(direction, sortField));

        Specification<Job> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }
            if (filter.getJobType() != null && !filter.getJobType().isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("jobType")), filter.getJobType().trim().toUpperCase()));
            }
            if (filter.getPriority() != null) {
                predicates.add(cb.equal(root.get("priority"), filter.getPriority()));
            }
            if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
                String term = "%" + filter.getSearch().trim().toLowerCase() + "%";
                Predicate searchPredicate = cb.or(
                        cb.like(cb.lower(root.get("jobType")), term),
                        cb.like(cb.lower(root.get("lastError")), term),
                        cb.like(cb.lower(root.get("idempotencyKey")), term)
                );
                predicates.add(searchPredicate);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Job> page = jobRepository.findAll(spec, pageable);
        List<JobResponse> content = page.getContent().stream()
                .map(job -> JobResponse.fromEntity(job, objectMapper))
                .toList();

        return new PagedResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    @Transactional(readOnly = true)
    public List<JobAttemptResponse> getJobAttempts(UUID jobId) {
        if (!jobRepository.existsById(jobId)) {
            throw new JobNotFoundException(jobId);
        }
        return jobAttemptRepository.findByJobIdOrderByAttemptNumberAsc(jobId).stream()
                .map(JobAttemptResponse::fromEntity)
                .toList();
    }

    @Transactional
    public JobResponse cancelJob(UUID id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException(id));

        if (job.getStatus() != JobStatus.QUEUED && job.getStatus() != JobStatus.RETRY_WAIT) {
            throw new InvalidStateTransitionException(
                    "Cannot cancel job in status " + job.getStatus() + ". Only QUEUED or RETRY_WAIT jobs can be cancelled."
            );
        }

        job.transitionTo(JobStatus.CANCELLED);
        job.setCompletedAt(Instant.now());
        Job saved = jobRepository.save(job);
        log.info("Job {} cancelled by user", id);
        return JobResponse.fromEntity(saved, objectMapper);
    }

    @Transactional
    public JobResponse retryJob(UUID id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException(id));

        if (job.getStatus() != JobStatus.FAILED && job.getStatus() != JobStatus.DEAD_LETTER) {
            throw new InvalidStateTransitionException(
                    "Cannot retry job in status " + job.getStatus() + ". Only FAILED or DEAD_LETTER jobs can be manually retried."
            );
        }

        job.transitionTo(JobStatus.QUEUED);
        job.setScheduledAt(Instant.now());
        job.setNextRetryAt(null);
        job.setLastError(null);
        job.setWorkerId(null);
        job.setStartedAt(null);
        job.setCompletedAt(null);
        job.setExecutionDurationMs(null);
        // Extend maxAttempts by the default retry quota so manual retry has attempts to run
        job.setMaxAttempts(job.getAttemptCount() + properties.getRetry().getDefaultMaxAttempts());

        Job saved = jobRepository.save(job);
        log.info("Job {} manually re-queued for execution with new maxAttempts {}", id, saved.getMaxAttempts());
        return JobResponse.fromEntity(saved, objectMapper);
    }

    private String serializePayload(JsonNode payload) {
        if (payload == null || payload.isNull()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new InvalidJobPayloadException("Failed to serialize payload: " + e.getMessage());
        }
    }

    private boolean isMatchingJob(Job existing, String jobType, int priority, String newPayloadJson) {
        if (!existing.getJobType().equalsIgnoreCase(jobType) || existing.getPriority() != priority) {
            return false;
        }
        try {
            JsonNode existingNode = objectMapper.readTree(existing.getPayload());
            JsonNode newNode = objectMapper.readTree(newPayloadJson);
            return existingNode.equals(newNode);
        } catch (Exception e) {
            return existing.getPayload().equals(newPayloadJson);
        }
    }
}
