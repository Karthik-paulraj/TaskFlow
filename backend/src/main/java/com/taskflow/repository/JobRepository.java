package com.taskflow.repository;

import com.taskflow.entity.Job;
import com.taskflow.enums.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID>, JpaSpecificationExecutor<Job> {

    Optional<Job> findByIdempotencyKey(String idempotencyKey);

    long countByStatus(JobStatus status);

    List<Job> findByStatus(JobStatus status);

    @Query("SELECT j FROM Job j WHERE j.status = :status ORDER BY j.startedAt DESC")
    List<Job> findRunningJobsOrderByStartedAtDesc(@Param("status") JobStatus status, Pageable pageable);

    /**
     * Find eligible job IDs ordered by priority DESC, created_at ASC.
     * Evaluates QUEUED jobs whose scheduledAt <= now, and RETRY_WAIT jobs whose nextRetryAt <= now.
     * Strongly typed JPQL ensures seamless UUID mapping across PostgreSQL and H2.
     */
    @Query("""
            SELECT j.id FROM Job j
            WHERE (j.status = com.taskflow.enums.JobStatus.QUEUED AND j.scheduledAt <= :now)
               OR (j.status = com.taskflow.enums.JobStatus.RETRY_WAIT AND j.nextRetryAt <= :now)
            ORDER BY j.priority DESC, j.createdAt ASC
            """)
    List<UUID> findEligibleJobIds(@Param("now") Instant now, Pageable pageable);

    /**
     * Atomic claim: updates a job to RUNNING only if its current status is QUEUED or RETRY_WAIT.
     * Returns 1 if claimed successfully, 0 if another worker claimed it first.
     */
    @Modifying
    @Query("""
            UPDATE Job j
            SET j.status = com.taskflow.enums.JobStatus.RUNNING,
                j.workerId = :workerId,
                j.startedAt = :now,
                j.attemptCount = j.attemptCount + 1,
                j.updatedAt = :now
            WHERE j.id = :id
              AND (j.status = com.taskflow.enums.JobStatus.QUEUED OR j.status = com.taskflow.enums.JobStatus.RETRY_WAIT)
            """)
    int claimJobAtomically(@Param("id") UUID id, @Param("workerId") String workerId, @Param("now") Instant now);

    /**
     * Finds stale RUNNING jobs whose updatedAt is older than the lease threshold.
     */
    @Query("SELECT j FROM Job j WHERE j.status = com.taskflow.enums.JobStatus.RUNNING AND j.updatedAt < :threshold")
    List<Job> findStaleRunningJobs(@Param("threshold") Instant threshold);

    /**
     * Count jobs created within a time range.
     */
    @Query("SELECT COUNT(j) FROM Job j WHERE j.createdAt >= :start AND j.createdAt < :end")
    long countCreatedBetween(@Param("start") Instant start, @Param("end") Instant end);

    /**
     * Count jobs completed with given status within a time range.
     */
    @Query("SELECT COUNT(j) FROM Job j WHERE j.status = :status AND j.completedAt >= :start AND j.completedAt < :end")
    long countCompletedWithStatusBetween(@Param("status") JobStatus status, @Param("start") Instant start, @Param("end") Instant end);

    /**
     * Average execution duration of completed jobs within a time range.
     */
    @Query("SELECT AVG(j.executionDurationMs) FROM Job j WHERE j.completedAt >= :start AND j.completedAt < :end AND j.executionDurationMs IS NOT NULL")
    Double averageExecutionDurationBetween(@Param("start") Instant start, @Param("end") Instant end);
}
