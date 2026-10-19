package com.taskflow.controller;

import com.taskflow.dto.StatsOverviewResponse;
import com.taskflow.dto.TimeseriesPoint;
import com.taskflow.dto.WorkerStatsResponse;
import com.taskflow.service.MetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stats")
@Tag(name = "Statistics", description = "Real-time queue depth, execution timeseries, and worker utilization metrics")
public class StatsController {

    private final MetricsService metricsService;

    public StatsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping("/overview")
    @Operation(summary = "Get queue status overview", description = "Returns counts for all job statuses (total, queued, running, succeeded, failed, dead-letter, cancelled).")
    public ResponseEntity<StatsOverviewResponse> getOverview() {
        StatsOverviewResponse overview = metricsService.getOverview();
        return ResponseEntity.ok(overview);
    }

    @GetMapping("/timeseries")
    @Operation(summary = "Get time-bucketed job metrics", description = "Returns historical submission, completion, and failure counts broken into discrete time intervals.")
    public ResponseEntity<List<TimeseriesPoint>> getTimeseries(
            @RequestParam(name = "minutes", defaultValue = "30") int minutes,
            @RequestParam(name = "bucketSeconds", defaultValue = "60") int bucketSeconds) {
        List<TimeseriesPoint> points = metricsService.getTimeseries(minutes, bucketSeconds);
        return ResponseEntity.ok(points);
    }

    @GetMapping("/workers")
    @Operation(summary = "Get worker pool and running jobs", description = "Returns pool capacity, active/idle worker count, current queue depth, and currently running tasks.")
    public ResponseEntity<WorkerStatsResponse> getWorkerStats() {
        WorkerStatsResponse stats = metricsService.getWorkerStats();
        return ResponseEntity.ok(stats);
    }
}
