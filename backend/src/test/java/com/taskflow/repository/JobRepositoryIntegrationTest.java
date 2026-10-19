package com.taskflow.repository;

import com.taskflow.entity.Job;
import com.taskflow.enums.JobStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JobRepositoryIntegrationTest {

    @Autowired
    private JobRepository jobRepository;

    @BeforeEach
    void cleanUp() {
        jobRepository.deleteAll();
    }

    @Test
    @DisplayName("Scheduler prioritizes higher priority jobs and FIFO for equal priorities")
    void testPriorityAndFifoOrdering() {
        Instant now = Instant.now();

        // Job 1: Priority 5, created 10 seconds ago
        Job job1 = new Job(UUID.randomUUID(), "EMAIL_SIMULATION", "{}", 5, 3, now.minusSeconds(10), null);
        job1.setCreatedAt(now.minusSeconds(10));
        jobRepository.save(job1);

        // Job 2: Priority 9, created 5 seconds ago (Higher priority!)
        Job job2 = new Job(UUID.randomUUID(), "EMAIL_SIMULATION", "{}", 9, 3, now.minusSeconds(5), null);
        job2.setCreatedAt(now.minusSeconds(5));
        jobRepository.save(job2);

        // Job 3: Priority 5, created 20 seconds ago (Same priority as job 1, but older - should come first among priority 5)
        Job job3 = new Job(UUID.randomUUID(), "EMAIL_SIMULATION", "{}", 5, 3, now.minusSeconds(20), null);
        job3.setCreatedAt(now.minusSeconds(20));
        jobRepository.save(job3);

        List<UUID> eligible = jobRepository.findEligibleJobIds(now, org.springframework.data.domain.PageRequest.of(0, 10));
        assertEquals(3, eligible.size());

        // Highest priority first
        assertEquals(job2.getId(), eligible.get(0));

        // Between job3 and job1 (both priority 5), older creation time first (FIFO)
        assertEquals(job3.getId(), eligible.get(1));
        assertEquals(job1.getId(), eligible.get(2));
    }

    @Test
    @DisplayName("Future scheduled jobs are not eligible until scheduled time")
    void testFutureScheduledJobs() {
        Instant now = Instant.now();

        // Job scheduled 1 hour in the future
        Job futureJob = new Job(UUID.randomUUID(), "EMAIL_SIMULATION", "{}", 10, 3, now.plusSeconds(3600), null);
        jobRepository.save(futureJob);

        List<UUID> eligible = jobRepository.findEligibleJobIds(now, org.springframework.data.domain.PageRequest.of(0, 10));
        assertFalse(eligible.contains(futureJob.getId()));

        // When evaluating at scheduled time, it becomes eligible
        List<UUID> eligibleLater = jobRepository.findEligibleJobIds(now.plusSeconds(3601), org.springframework.data.domain.PageRequest.of(0, 10));
        assertTrue(eligibleLater.contains(futureJob.getId()));
    }

    @Test
    @DisplayName("Atomic claim prevents duplicate claiming")
    void testAtomicClaim() {
        Instant now = Instant.now();
        Job job = new Job(UUID.randomUUID(), "EMAIL_SIMULATION", "{}", 5, 3, now, null);
        jobRepository.save(job);

        // First worker claims
        int claimed1 = jobRepository.claimJobAtomically(job.getId(), "worker-alpha", now);
        assertEquals(1, claimed1);

        // Second worker tries to claim same job -> 0 rows updated
        int claimed2 = jobRepository.claimJobAtomically(job.getId(), "worker-beta", now);
        assertEquals(0, claimed2);
    }
}
