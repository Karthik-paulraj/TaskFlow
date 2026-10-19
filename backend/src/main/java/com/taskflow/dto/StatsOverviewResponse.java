package com.taskflow.dto;

public class StatsOverviewResponse {
    private long totalJobs;
    private long queuedJobs;
    private long runningJobs;
    private long succeededJobs;
    private long failedJobs;
    private long deadLetterJobs;
    private long cancelledJobs;
    private long retryWaitJobs;

    public StatsOverviewResponse() {
    }

    public StatsOverviewResponse(long totalJobs, long queuedJobs, long runningJobs, long succeededJobs, long failedJobs, long deadLetterJobs, long cancelledJobs, long retryWaitJobs) {
        this.totalJobs = totalJobs;
        this.queuedJobs = queuedJobs;
        this.runningJobs = runningJobs;
        this.succeededJobs = succeededJobs;
        this.failedJobs = failedJobs;
        this.deadLetterJobs = deadLetterJobs;
        this.cancelledJobs = cancelledJobs;
        this.retryWaitJobs = retryWaitJobs;
    }

    public long getTotalJobs() {
        return totalJobs;
    }

    public void setTotalJobs(long totalJobs) {
        this.totalJobs = totalJobs;
    }

    public long getQueuedJobs() {
        return queuedJobs;
    }

    public void setQueuedJobs(long queuedJobs) {
        this.queuedJobs = queuedJobs;
    }

    public long getRunningJobs() {
        return runningJobs;
    }

    public void setRunningJobs(long runningJobs) {
        this.runningJobs = runningJobs;
    }

    public long getSucceededJobs() {
        return succeededJobs;
    }

    public void setSucceededJobs(long succeededJobs) {
        this.succeededJobs = succeededJobs;
    }

    public long getFailedJobs() {
        return failedJobs;
    }

    public void setFailedJobs(long failedJobs) {
        this.failedJobs = failedJobs;
    }

    public long getDeadLetterJobs() {
        return deadLetterJobs;
    }

    public void setDeadLetterJobs(long deadLetterJobs) {
        this.deadLetterJobs = deadLetterJobs;
    }

    public long getCancelledJobs() {
        return cancelledJobs;
    }

    public void setCancelledJobs(long cancelledJobs) {
        this.cancelledJobs = cancelledJobs;
    }

    public long getRetryWaitJobs() {
        return retryWaitJobs;
    }

    public void setRetryWaitJobs(long retryWaitJobs) {
        this.retryWaitJobs = retryWaitJobs;
    }
}
