import {
  BulkCreateJobResponse,
  CreateJobInput,
  Job,
  JobAttempt,
  PagedResponse,
  StatsOverview,
  TimeseriesPoint,
  WorkerStats,
} from '../types';

const API_BASE = import.meta.env.VITE_API_BASE_URL || '';

async function request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const url = `${API_BASE}${endpoint}`;
  const headers = new Headers(options.headers || {});

  if (!headers.has('Content-Type') && !(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json');
  }

  const response = await fetch(url, {
    ...options,
    headers,
  });

  if (!response.ok) {
    let errorMsg = `HTTP Error ${response.status}: ${response.statusText}`;
    try {
      const errorJson = await response.json();
      if (errorJson.message) {
        errorMsg = errorJson.message;
      }
      if (errorJson.validationErrors) {
        const details = Object.entries(errorJson.validationErrors)
          .map(([k, v]) => `${k}: ${v}`)
          .join(', ');
        errorMsg += ` (${details})`;
      }
    } catch {
      // Use fallback errorMsg
    }
    throw new Error(errorMsg);
  }

  // Handle empty responses (e.g. 204)
  if (response.status === 204) {
    return {} as T;
  }

  return response.json();
}

export const api = {
  // Statistics
  getStatsOverview: () => request<StatsOverview>('/api/stats/overview'),
  getTimeseries: (minutes = 30, bucketSeconds = 60) =>
    request<TimeseriesPoint[]>(`/api/stats/timeseries?minutes=${minutes}&bucketSeconds=${bucketSeconds}`),
  getWorkerStats: () => request<WorkerStats>('/api/stats/workers'),
  getHealth: () => request<{ status: string }>('/actuator/health'),

  // Jobs
  listJobs: (params: {
    status?: string;
    jobType?: string;
    priority?: number;
    search?: string;
    page?: number;
    size?: number;
    sortBy?: string;
    sortDirection?: string;
  }) => {
    const query = new URLSearchParams();
    if (params.status && params.status !== 'ALL') query.set('status', params.status);
    if (params.jobType && params.jobType !== 'ALL') query.set('jobType', params.jobType);
    if (params.priority) query.set('priority', params.priority.toString());
    if (params.search) query.set('search', params.search);
    if (params.page !== undefined) query.set('page', params.page.toString());
    if (params.size !== undefined) query.set('size', params.size.toString());
    if (params.sortBy) query.set('sortBy', params.sortBy);
    if (params.sortDirection) query.set('sortDirection', params.sortDirection);

    return request<PagedResponse<Job>>(`/api/jobs?${query.toString()}`);
  },

  getJob: (id: string) => request<Job>(`/api/jobs/${id}`),

  getJobAttempts: (id: string) => request<JobAttempt[]>(`/api/jobs/${id}/attempts`),

  createJob: (input: CreateJobInput) =>
    request<Job>('/api/jobs', {
      method: 'POST',
      body: JSON.stringify(input),
      headers: input.idempotencyKey ? { 'Idempotency-Key': input.idempotencyKey } : {},
    }),

  createBulkJobs: (jobs: CreateJobInput[]) =>
    request<BulkCreateJobResponse>('/api/jobs/bulk', {
      method: 'POST',
      body: JSON.stringify({ jobs }),
    }),

  cancelJob: (id: string) =>
    request<Job>(`/api/jobs/${id}/cancel`, {
      method: 'POST',
    }),

  retryJob: (id: string) =>
    request<Job>(`/api/jobs/${id}/retry`, {
      method: 'POST',
    }),
};
