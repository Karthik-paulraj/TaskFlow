export type JobStatus =
  | 'QUEUED'
  | 'RUNNING'
  | 'SUCCEEDED'
  | 'RETRY_WAIT'
  | 'FAILED'
  | 'DEAD_LETTER'
  | 'CANCELLED';

export type ExecutionOutcome =
  | 'SUCCESS'
  | 'FAILURE'
  | 'RETRYABLE_FAILURE'
  | 'TIMEOUT';

export interface Job {
  id: string;
  jobType: string;
  payload: any;
  status: JobStatus;
  priority: number;
  createdAt: string;
  scheduledAt: string;
  startedAt?: string;
  completedAt?: string;
  updatedAt: string;
  attemptCount: number;
  maxAttempts: number;
  nextRetryAt?: string;
  result?: any;
  lastError?: string;
  idempotencyKey?: string;
  workerId?: string;
  executionDurationMs?: number;
}

export interface JobAttempt {
  id: string;
  jobId: string;
  attemptNumber: number;
  workerId: string;
  startedAt: string;
  completedAt: string;
  outcome: ExecutionOutcome;
  errorDetails?: string;
  executionDurationMs: number;
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface StatsOverview {
  totalJobs: number;
  queuedJobs: number;
  runningJobs: number;
  succeededJobs: number;
  failedJobs: number;
  deadLetterJobs: number;
  cancelledJobs: number;
  retryWaitJobs: number;
}

export interface TimeseriesPoint {
  timestamp: string;
  submitted: number;
  succeeded: number;
  failed: number;
  avgDurationMs: number;
}

export interface WorkerStats {
  configuredWorkers: number;
  activeWorkers: number;
  idleWorkers: number;
  queueDepth: number;
  currentlyRunningJobs: Job[];
}

export interface CreateJobInput {
  jobType: string;
  payload: any;
  priority: number;
  maxAttempts: number;
  scheduledAt?: string;
  idempotencyKey?: string;
}

export interface BulkCreateJobResponse {
  submittedJobs: Job[];
  failures: { index: number; jobType: string; error: string }[];
  totalSubmitted: number;
  totalFailed: number;
}
