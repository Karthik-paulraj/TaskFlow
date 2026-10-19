package com.taskflow.dto;

import java.time.Instant;

public class TimeseriesPoint {
    private Instant timestamp;
    private long submitted;
    private long succeeded;
    private long failed;
    private double avgDurationMs;

    public TimeseriesPoint() {
    }

    public TimeseriesPoint(Instant timestamp, long submitted, long succeeded, long failed, double avgDurationMs) {
        this.timestamp = timestamp;
        this.submitted = submitted;
        this.succeeded = succeeded;
        this.failed = failed;
        this.avgDurationMs = avgDurationMs;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public long getSubmitted() {
        return submitted;
    }

    public void setSubmitted(long submitted) {
        this.submitted = submitted;
    }

    public long getSucceeded() {
        return succeeded;
    }

    public void setSucceeded(long succeeded) {
        this.succeeded = succeeded;
    }

    public long getFailed() {
        return failed;
    }

    public void setFailed(long failed) {
        this.failed = failed;
    }

    public double getAvgDurationMs() {
        return avgDurationMs;
    }

    public void setAvgDurationMs(double avgDurationMs) {
        this.avgDurationMs = avgDurationMs;
    }
}
