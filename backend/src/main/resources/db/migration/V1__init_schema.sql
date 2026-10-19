-- TaskFlow Database Schema Migration V1
-- Production-style Job Queue and Execution History

CREATE TABLE IF NOT EXISTS jobs (
    id UUID PRIMARY KEY,
    job_type VARCHAR(64) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    priority INT NOT NULL DEFAULT 5,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    scheduled_at TIMESTAMP WITH TIME ZONE NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 3,
    next_retry_at TIMESTAMP WITH TIME ZONE,
    result TEXT,
    last_error TEXT,
    idempotency_key VARCHAR(128),
    worker_id VARCHAR(128),
    execution_duration_ms BIGINT,
    version BIGINT NOT NULL DEFAULT 0
);

-- Index for scheduler polling and atomic claiming
CREATE INDEX IF NOT EXISTS idx_jobs_scheduler_claim 
ON jobs (status, scheduled_at, priority DESC, created_at ASC);

-- Index for retry waiting polling
CREATE INDEX IF NOT EXISTS idx_jobs_retry_poll 
ON jobs (status, next_retry_at, priority DESC);

-- Index for ordering and filtering
CREATE INDEX IF NOT EXISTS idx_jobs_created_at ON jobs (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_jobs_status ON jobs (status);
CREATE INDEX IF NOT EXISTS idx_jobs_type ON jobs (job_type);

-- Unique index for idempotency keys (standard SQL allows multiple NULL values)
CREATE UNIQUE INDEX IF NOT EXISTS idx_jobs_idempotency_key 
ON jobs (idempotency_key);

-- Execution attempts history table
CREATE TABLE IF NOT EXISTS job_attempts (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL,
    attempt_number INT NOT NULL,
    worker_id VARCHAR(128) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    outcome VARCHAR(32) NOT NULL,
    error_details TEXT,
    execution_duration_ms BIGINT NOT NULL,
    CONSTRAINT fk_job_attempts_job FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_job_attempts_job_id ON job_attempts (job_id, attempt_number ASC);
CREATE INDEX IF NOT EXISTS idx_job_attempts_started ON job_attempts (started_at DESC);
