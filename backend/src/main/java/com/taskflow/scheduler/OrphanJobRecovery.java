package com.taskflow.scheduler;

import com.taskflow.config.TaskFlowProperties;
import com.taskflow.entity.Job;
import com.taskflow.entity.JobAttempt;
import com.taskflow.enums.ExecutionOutcome;
import com.taskflow.enums.JobStatus;
import com.taskflow.repository.JobAttemptRepository;
import com.taskflow.repository.JobRepository;
import com.taskflow.service.RetryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;

/**
 * Recovers orphaned or abandoned jobs left in RUNNING due to worker crashes or node failures.
 */
@Component
public class OrphanJobRecovery {

    private static final Logger log = LoggerFactory.getLogger(OrphanJobRecovery.class);

    private final JobRepository jobRepository;
    private final JobAttemptRepository jobAttemptRepository;
    private final RetryService retryService;
    private final TaskFlowProperties properties;
    private final TransactionTemplate transactionTemplate;

    public OrphanJobRecovery(JobRepository jobRepository,
                             JobAttemptRepository jobAttemptRepository,
                             RetryService retryService,
                             TaskFlowProperties properties,
                             PlatformTransactionManager transactionManager) {
        this.jobRepository = jobRepository;
        this.jobAttemptRepository = jobAttemptRepository;
        this.retryService = retryService;
        this.properties = properties;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Scheduled(fixedDelayString = "${taskflow.scheduler.recovery-check-interval-ms:15000}")
    public void recoverStaleJobs() {
        long timeoutSeconds = properties.getScheduler().getLeaseTimeoutSeconds();
        Instant threshold = Instant.now().minusSeconds(timeoutSeconds);

        List<Job> staleJobs = jobRepository.findStaleRunningJobs(threshold);
        if (staleJobs.isEmpty()) {
            return;
        }

        log.warn("Detected {} stale RUNNING jobs exceeding {} seconds lease timeout", staleJobs.size(), timeoutSeconds);

        for (Job staleJob : staleJobs) {
            transactionTemplate.executeWithoutResult(status -> {
                Job job = jobRepository.findById(staleJob.getId()).orElse(null);
                if (job == null || job.getStatus() != JobStatus.RUNNING) {
                    return;
                }

                Instant now = Instant.now();
                boolean exhausted = retryService.isExhausted(job.getAttemptCount(), job.getMaxAttempts());

                if (exhausted) {
                    job.transitionTo(JobStatus.DEAD_LETTER);
                    job.setLastError("Worker lease expired (" + timeoutSeconds + "s) and retry attempts exhausted");
                    job.setCompletedAt(now);
                    log.error("Stale job {} moved to DEAD_LETTER after lease timeout", job.getId());
                } else {
                    job.transitionTo(JobStatus.RETRY_WAIT);
                    Instant nextRetry = retryService.calculateNextRetry(job.getAttemptCount());
                    job.setNextRetryAt(nextRetry);
                    job.setLastError("Recovered from expired worker lease (" + timeoutSeconds + "s); re-queued");
                    log.info("Stale job {} recovered and scheduled for retry at {}", job.getId(), nextRetry);
                }

                JobAttempt recoveryAttempt = new JobAttempt(
                        job.getId(),
                        job.getAttemptCount(),
                        "recovery-monitor",
                        job.getStartedAt() != null ? job.getStartedAt() : threshold,
                        now,
                        ExecutionOutcome.TIMEOUT,
                        "Worker lease expired; heartbeat timed out",
                        Math.max(0, now.toEpochMilli() - (job.getStartedAt() != null ? job.getStartedAt().toEpochMilli() : threshold.toEpochMilli()))
                );

                jobAttemptRepository.save(recoveryAttempt);
                jobRepository.save(job);
            });
        }
    }
}
