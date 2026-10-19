package com.taskflow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "taskflow")
public class TaskFlowProperties {

    private Worker worker = new Worker();
    private Scheduler scheduler = new Scheduler();
    private Retry retry = new Retry();

    public static class Worker {
        private int poolSize = 6;
        private int queueCapacity = 200;

        public int getPoolSize() {
            return poolSize;
        }

        public void setPoolSize(int poolSize) {
            this.poolSize = poolSize;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }
    }

    public static class Scheduler {
        private long pollIntervalMs = 500;
        private int batchSize = 10;
        private long leaseTimeoutSeconds = 120;
        private long recoveryCheckIntervalMs = 15000;

        public long getPollIntervalMs() {
            return pollIntervalMs;
        }

        public void setPollIntervalMs(long pollIntervalMs) {
            this.pollIntervalMs = pollIntervalMs;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public long getLeaseTimeoutSeconds() {
            return leaseTimeoutSeconds;
        }

        public void setLeaseTimeoutSeconds(long leaseTimeoutSeconds) {
            this.leaseTimeoutSeconds = leaseTimeoutSeconds;
        }

        public long getRecoveryCheckIntervalMs() {
            return recoveryCheckIntervalMs;
        }

        public void setRecoveryCheckIntervalMs(long recoveryCheckIntervalMs) {
            this.recoveryCheckIntervalMs = recoveryCheckIntervalMs;
        }
    }

    public static class Retry {
        private long initialDelaySeconds = 2;
        private long maxDelaySeconds = 60;
        private double jitterFactor = 0.2;
        private int defaultMaxAttempts = 3;

        public long getInitialDelaySeconds() {
            return initialDelaySeconds;
        }

        public void setInitialDelaySeconds(long initialDelaySeconds) {
            this.initialDelaySeconds = initialDelaySeconds;
        }

        public long getMaxDelaySeconds() {
            return maxDelaySeconds;
        }

        public void setMaxDelaySeconds(long maxDelaySeconds) {
            this.maxDelaySeconds = maxDelaySeconds;
        }

        public double getJitterFactor() {
            return jitterFactor;
        }

        public void setJitterFactor(double jitterFactor) {
            this.jitterFactor = jitterFactor;
        }

        public int getDefaultMaxAttempts() {
            return defaultMaxAttempts;
        }

        public void setDefaultMaxAttempts(int defaultMaxAttempts) {
            this.defaultMaxAttempts = defaultMaxAttempts;
        }
    }

    public Worker getWorker() {
        return worker;
    }

    public void setWorker(Worker worker) {
        this.worker = worker;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public Retry getRetry() {
        return retry;
    }

    public void setRetry(Retry retry) {
        this.retry = retry;
    }
}
