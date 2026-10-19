package com.taskflow.worker;

import com.taskflow.config.TaskFlowProperties;
import com.taskflow.entity.Job;
import com.taskflow.entity.JobAttempt;
import com.taskflow.enums.ExecutionOutcome;
import com.taskflow.enums.JobStatus;
import com.taskflow.handler.JobContext;
import com.taskflow.handler.JobExecutionResult;
import com.taskflow.handler.JobHandler;
import com.taskflow.handler.JobHandlerRegistry;
import com.taskflow.repository.JobAttemptRepository;
import com.taskflow.repository.JobRepository;
import com.taskflow.service.RetryService;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class WorkerEngine {

    private static final Logger log = LoggerFactory.getLogger(WorkerEngine.class);

    private final ThreadPoolExecutor executor;
    private final JobRepository jobRepository;
    private final JobAttemptRepository jobAttemptRepository;
    private final JobHandlerRegistry handlerRegistry;
    private final RetryService retryService;
    private final TransactionTemplate transactionTemplate;
    private final int poolSize;

    public WorkerEngine(TaskFlowProperties properties,
                        JobRepository jobRepository,
                        JobAttemptRepository jobAttemptRepository,
                        JobHandlerRegistry handlerRegistry,
                        RetryService retryService,
                        PlatformTransactionManager transactionManager) {
        this.jobRepository = jobRepository;
        this.jobAttemptRepository = jobAttemptRepository;
        this.handlerRegistry = handlerRegistry;
        this.retryService = retryService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);

        this.poolSize = properties.getWorker().getPoolSize();
        int queueCapacity = properties.getWorker().getQueueCapacity();

        AtomicInteger threadCount = new AtomicInteger(1);
        ThreadFactory threadFactory = r -> {
            Thread t = new Thread(r, "taskflow-worker-" + threadCount.getAndIncrement());
            t.setDaemon(true);
            return t;
        };

        this.executor = new ThreadPoolExecutor(
                poolSize,
                poolSize,
                60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                threadFactory,
                new ThreadPoolExecutor.AbortPolicy()
        );

        log.info("WorkerEngine initialized with pool size {} and queue capacity {}", poolSize, queueCapacity);
    }

    public int getAvailableCapacity() {
        int active = executor.getActiveCount();
        return Math.max(0, poolSize - active);
    }

    public int getConfiguredPoolSize() {
        return poolSize;
    }

    public int getActiveWorkerCount() {
        return executor.getActiveCount();
    }

    public int getIdleWorkerCount() {
        return Math.max(0, poolSize - executor.getActiveCount());
    }

    public void submitJob(UUID jobId) {
        try {
            executor.execute(() -> processJob(jobId));
        } catch (RejectedExecutionException e) {
            log.warn("Worker queue saturated, unable to accept job {}", jobId);
        }
    }

    private void processJob(UUID jobId) {
        String workerThreadName = Thread.currentThread().getName();
        long startTime = System.currentTimeMillis();
        Instant startInstant = Instant.now();

        log.info("[{}] Started processing job {}", workerThreadName, jobId);

        // Fetch job safely
        Job job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            log.warn("[{}] Job {} not found when starting execution", workerThreadName, jobId);
            return;
        }

        JobExecutionResult result;
        try {
            JobHandler handler = handlerRegistry.getHandler(job.getJobType());
            JobContext context = new JobContext(
                    job.getId(),
                    job.getAttemptCount(),
                    job.getPayload(),
                    workerThreadName
            );
            result = handler.execute(context);
        } catch (Exception ex) {
            log.error("[{}] Uncaught exception executing job {}", workerThreadName, jobId, ex);
            result = JobExecutionResult.retryableFailure(
                    ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName()
            );
        }

        long durationMs = System.currentTimeMillis() - startTime;
        Instant endInstant = Instant.now();
        final JobExecutionResult finalResult = result;

        // Persist outcome and attempt in a transaction
        transactionTemplate.executeWithoutResult(status -> {
            Job currentJob = jobRepository.findById(jobId).orElse(null);
            if (currentJob == null) {
                return;
            }

            if (finalResult.success()) {
                currentJob.transitionTo(JobStatus.SUCCEEDED);
                currentJob.setResult(finalResult.resultJson());
                currentJob.setLastError(null);
                currentJob.setCompletedAt(endInstant);
                currentJob.setExecutionDurationMs(durationMs);

                JobAttempt attempt = new JobAttempt(
                        jobId,
                        currentJob.getAttemptCount(),
                        workerThreadName,
                        startInstant,
                        endInstant,
                        ExecutionOutcome.SUCCESS,
                        null,
                        durationMs
                );
                jobAttemptRepository.save(attempt);
                jobRepository.save(currentJob);
                log.info("[{}] Job {} SUCCEEDED in {} ms", workerThreadName, jobId, durationMs);

            } else {
                // Failure handling
                boolean retryable = finalResult.retryable();
                boolean exhausted = retryService.isExhausted(currentJob.getAttemptCount(), currentJob.getMaxAttempts());

                ExecutionOutcome outcome;
                if (retryable && !exhausted) {
                    currentJob.transitionTo(JobStatus.RETRY_WAIT);
                    Instant nextRetry = retryService.calculateNextRetry(currentJob.getAttemptCount());
                    currentJob.setNextRetryAt(nextRetry);
                    currentJob.setLastError(finalResult.errorMessage());
                    outcome = ExecutionOutcome.RETRYABLE_FAILURE;
                    log.warn("[{}] Job {} failed (attempt {}/{}); retrying at {}",
                            workerThreadName, jobId, currentJob.getAttemptCount(), currentJob.getMaxAttempts(), nextRetry);
                } else if (exhausted) {
                    currentJob.transitionTo(JobStatus.DEAD_LETTER);
                    currentJob.setLastError("Exhausted all " + currentJob.getMaxAttempts() + " attempts. Last error: " + finalResult.errorMessage());
                    currentJob.setCompletedAt(endInstant);
                    currentJob.setExecutionDurationMs(durationMs);
                    outcome = ExecutionOutcome.FAILURE;
                    log.error("[{}] Job {} moved to DEAD_LETTER after {} attempts", workerThreadName, jobId, currentJob.getAttemptCount());
                } else {
                    // Terminal non-retryable error
                    currentJob.transitionTo(JobStatus.FAILED);
                    currentJob.setLastError(finalResult.errorMessage());
                    currentJob.setCompletedAt(endInstant);
                    currentJob.setExecutionDurationMs(durationMs);
                    outcome = ExecutionOutcome.FAILURE;
                    log.error("[{}] Job {} FAILED permanently: {}", workerThreadName, jobId, finalResult.errorMessage());
                }

                JobAttempt attempt = new JobAttempt(
                        jobId,
                        currentJob.getAttemptCount(),
                        workerThreadName,
                        startInstant,
                        endInstant,
                        outcome,
                        finalResult.errorMessage(),
                        durationMs
                );
                jobAttemptRepository.save(attempt);
                jobRepository.save(currentJob);
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down WorkerEngine...");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("WorkerEngine stopped.");
    }
}
