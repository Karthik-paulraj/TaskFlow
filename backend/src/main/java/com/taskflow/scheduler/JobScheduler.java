package com.taskflow.scheduler;

import com.taskflow.config.TaskFlowProperties;
import com.taskflow.repository.JobRepository;
import com.taskflow.worker.WorkerEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Priority-based scheduler that identifies eligible jobs and coordinates safe claiming.
 */
@Component
public class JobScheduler {

    private static final Logger log = LoggerFactory.getLogger(JobScheduler.class);

    private final JobRepository jobRepository;
    private final WorkerEngine workerEngine;
    private final TaskFlowProperties properties;
    private final TransactionTemplate transactionTemplate;

    public JobScheduler(JobRepository jobRepository,
                        WorkerEngine workerEngine,
                        TaskFlowProperties properties,
                        PlatformTransactionManager transactionManager) {
        this.jobRepository = jobRepository;
        this.workerEngine = workerEngine;
        this.properties = properties;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Scheduled(fixedDelayString = "${taskflow.scheduler.poll-interval-ms:500}")
    public void pollAndSchedule() {
        int availableSlots = workerEngine.getAvailableCapacity();
        if (availableSlots <= 0) {
            return; // Workers fully occupied, avoid unnecessary database polling
        }

        int batchSize = Math.min(availableSlots, properties.getScheduler().getBatchSize());
        Instant now = Instant.now();

        List<UUID> eligibleJobIds = jobRepository.findEligibleJobIds(now, org.springframework.data.domain.PageRequest.of(0, batchSize));
        if (eligibleJobIds.isEmpty()) {
            return;
        }

        log.debug("Found {} eligible jobs ready for dispatch", eligibleJobIds.size());

        for (UUID jobId : eligibleJobIds) {
            // Check available capacity before each claim
            if (workerEngine.getAvailableCapacity() <= 0) {
                break;
            }

            // Attempt atomic claim in a dedicated transaction
            Boolean claimed = transactionTemplate.execute(status -> {
                int updated = jobRepository.claimJobAtomically(jobId, "scheduler-dispatcher", Instant.now());
                return updated > 0;
            });

            if (Boolean.TRUE.equals(claimed)) {
                workerEngine.submitJob(jobId);
                log.debug("Dispatched claimed job {} to worker engine", jobId);
            }
        }
    }
}
