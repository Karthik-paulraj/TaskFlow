package com.taskflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.dto.JobResponse;
import com.taskflow.dto.StatsOverviewResponse;
import com.taskflow.dto.TimeseriesPoint;
import com.taskflow.dto.WorkerStatsResponse;
import com.taskflow.entity.Job;
import com.taskflow.enums.JobStatus;
import com.taskflow.repository.JobRepository;
import com.taskflow.worker.WorkerEngine;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class MetricsService {

    private final JobRepository jobRepository;
    private final WorkerEngine workerEngine;
    private final ObjectMapper objectMapper;

    public MetricsService(JobRepository jobRepository, WorkerEngine workerEngine, ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.workerEngine = workerEngine;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public StatsOverviewResponse getOverview() {
        long queued = jobRepository.countByStatus(JobStatus.QUEUED);
        long running = jobRepository.countByStatus(JobStatus.RUNNING);
        long succeeded = jobRepository.countByStatus(JobStatus.SUCCEEDED);
        long failed = jobRepository.countByStatus(JobStatus.FAILED);
        long deadLetter = jobRepository.countByStatus(JobStatus.DEAD_LETTER);
        long cancelled = jobRepository.countByStatus(JobStatus.CANCELLED);
        long retryWait = jobRepository.countByStatus(JobStatus.RETRY_WAIT);
        long total = jobRepository.count();

        return new StatsOverviewResponse(
                total, queued, running, succeeded, failed, deadLetter, cancelled, retryWait
        );
    }

    @Transactional(readOnly = true)
    public List<TimeseriesPoint> getTimeseries(int totalMinutes, int bucketSeconds) {
        if (totalMinutes <= 0) totalMinutes = 30;
        if (bucketSeconds <= 0) bucketSeconds = 60;

        Instant end = Instant.now();
        Instant start = end.minus(totalMinutes, ChronoUnit.MINUTES);

        List<TimeseriesPoint> points = new ArrayList<>();
        Instant current = start;

        while (current.isBefore(end)) {
            Instant next = current.plusSeconds(bucketSeconds);
            if (next.isAfter(end)) {
                next = end;
            }

            long submitted = jobRepository.countCreatedBetween(current, next);
            long succ = jobRepository.countCompletedWithStatusBetween(JobStatus.SUCCEEDED, current, next);
            long fail = jobRepository.countCompletedWithStatusBetween(JobStatus.FAILED, current, next)
                    + jobRepository.countCompletedWithStatusBetween(JobStatus.DEAD_LETTER, current, next);

            Double avgDuration = jobRepository.averageExecutionDurationBetween(current, next);
            double avg = (avgDuration != null) ? Math.round(avgDuration * 10.0) / 10.0 : 0.0;

            points.add(new TimeseriesPoint(current, submitted, succ, fail, avg));
            current = next;
        }

        return points;
    }

    @Transactional(readOnly = true)
    public WorkerStatsResponse getWorkerStats() {
        int configured = workerEngine.getConfiguredPoolSize();
        int active = workerEngine.getActiveWorkerCount();
        int idle = workerEngine.getIdleWorkerCount();
        long queueDepth = jobRepository.countByStatus(JobStatus.QUEUED)
                + jobRepository.countByStatus(JobStatus.RETRY_WAIT);

        List<Job> runningEntities = jobRepository.findRunningJobsOrderByStartedAtDesc(
                JobStatus.RUNNING, PageRequest.of(0, 20)
        );

        List<JobResponse> runningJobs = runningEntities.stream()
                .map(j -> JobResponse.fromEntity(j, objectMapper))
                .toList();

        return new WorkerStatsResponse(configured, active, idle, queueDepth, runningJobs);
    }
}
