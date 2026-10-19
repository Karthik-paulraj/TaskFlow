package com.taskflow.dto;

import java.util.List;

public class WorkerStatsResponse {
    private int configuredWorkers;
    private int activeWorkers;
    private int idleWorkers;
    private long queueDepth;
    private List<JobResponse> currentlyRunningJobs;

    public WorkerStatsResponse() {
    }

    public WorkerStatsResponse(int configuredWorkers, int activeWorkers, int idleWorkers, long queueDepth, List<JobResponse> currentlyRunningJobs) {
        this.configuredWorkers = configuredWorkers;
        this.activeWorkers = activeWorkers;
        this.idleWorkers = idleWorkers;
        this.queueDepth = queueDepth;
        this.currentlyRunningJobs = currentlyRunningJobs;
    }

    public int getConfiguredWorkers() {
        return configuredWorkers;
    }

    public void setConfiguredWorkers(int configuredWorkers) {
        this.configuredWorkers = configuredWorkers;
    }

    public int getActiveWorkers() {
        return activeWorkers;
    }

    public void setActiveWorkers(int activeWorkers) {
        this.activeWorkers = activeWorkers;
    }

    public int getIdleWorkers() {
        return idleWorkers;
    }

    public void setIdleWorkers(int idleWorkers) {
        this.idleWorkers = idleWorkers;
    }

    public long getQueueDepth() {
        return queueDepth;
    }

    public void setQueueDepth(long queueDepth) {
        this.queueDepth = queueDepth;
    }

    public List<JobResponse> getCurrentlyRunningJobs() {
        return currentlyRunningJobs;
    }

    public void setCurrentlyRunningJobs(List<JobResponse> currentlyRunningJobs) {
        this.currentlyRunningJobs = currentlyRunningJobs;
    }
}
