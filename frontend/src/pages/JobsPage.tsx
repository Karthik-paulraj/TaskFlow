import React, { useEffect, useState, useCallback } from 'react';
import { Job, PagedResponse } from '../types';
import { api } from '../api/client';
import { StatusBadge } from '../components/StatusBadge';
import { PriorityBadge } from '../components/PriorityBadge';
import { JobDetailsModal } from '../components/JobDetailsModal';
import { useToast } from '../components/Toast';
import {
  Search,
  Filter,
  Eye,
  RotateCcw,
  Ban,
  ChevronLeft,
  ChevronRight,
  ArrowUpDown,
  Copy,
  Check,
} from 'lucide-react';

interface JobsPageProps {
  initialStatusFilter?: string;
  refreshTrigger: number;
}

export const JobsPage: React.FC<JobsPageProps> = ({
  initialStatusFilter = 'ALL',
  refreshTrigger,
}) => {
  const [jobs, setJobs] = useState<Job[]>([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(15);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [isLoading, setIsLoading] = useState(true);

  // Filters
  const [statusFilter, setStatusFilter] = useState(initialStatusFilter);
  const [jobTypeFilter, setJobTypeFilter] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [sortBy, setSortBy] = useState('createdAt');
  const [sortDirection, setSortDirection] = useState<'desc' | 'asc'>('desc');

  // Selected job for modal
  const [selectedJobId, setSelectedJobId] = useState<string | null>(null);
  const [copiedId, setCopiedId] = useState<string | null>(null);

  const { showToast } = useToast();

  const fetchJobs = useCallback(() => {
    setIsLoading(true);
    api.listJobs({
      status: statusFilter,
      jobType: jobTypeFilter,
      search: searchQuery,
      page,
      size,
      sortBy,
      sortDirection,
    })
      .then((data: PagedResponse<Job>) => {
        setJobs(data.content);
        setTotalPages(data.totalPages);
        setTotalElements(data.totalElements);
        setIsLoading(false);
      })
      .catch((err) => {
        showToast(err.message, 'error');
        setIsLoading(false);
      });
  }, [statusFilter, jobTypeFilter, searchQuery, page, size, sortBy, sortDirection, showToast]);

  useEffect(() => {
    fetchJobs();
  }, [fetchJobs, refreshTrigger]);

  const handleCopyId = (id: string, e: React.MouseEvent) => {
    e.stopPropagation();
    navigator.clipboard.writeText(id);
    setCopiedId(id);
    setTimeout(() => setCopiedId(null), 1500);
  };

  const handleCancelJob = async (id: string, e: React.MouseEvent) => {
    e.stopPropagation();
    if (!confirm('Are you sure you want to cancel this queued job?')) return;
    try {
      await api.cancelJob(id);
      showToast('Job cancelled successfully', 'info');
      fetchJobs();
    } catch (err: any) {
      showToast(err.message, 'error');
    }
  };

  const handleRetryJob = async (id: string, e: React.MouseEvent) => {
    e.stopPropagation();
    if (!confirm('Re-queue this job for execution?')) return;
    try {
      await api.retryJob(id);
      showToast('Job manually re-queued for execution', 'success');
      fetchJobs();
    } catch (err: any) {
      showToast(err.message, 'error');
    }
  };

  return (
    <div className="space-y-4">
      {/* Search & Filters Card */}
      <div className="p-4 rounded-xl bg-slate-900 border border-slate-800 space-y-3">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
          {/* Search box */}
          <div className="relative">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Search by ID, error, or key..."
              value={searchQuery}
              onChange={(e) => {
                setSearchQuery(e.target.value);
                setPage(0);
              }}
              className="w-full bg-slate-950 border border-slate-800 rounded-lg pl-9 pr-3 py-1.5 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-brand-500 transition-colors"
            />
          </div>

          {/* Status filter */}
          <div className="flex items-center gap-2">
            <span className="text-xs text-slate-400 shrink-0">Status:</span>
            <select
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value);
                setPage(0);
              }}
              className="w-full bg-slate-950 border border-slate-800 rounded-lg px-2.5 py-1.5 text-xs text-slate-300 focus:outline-none focus:border-brand-500"
            >
              <option value="ALL">All Statuses</option>
              <option value="QUEUED">QUEUED</option>
              <option value="RUNNING">RUNNING</option>
              <option value="SUCCEEDED">SUCCEEDED</option>
              <option value="RETRY_WAIT">RETRY_WAIT</option>
              <option value="FAILED">FAILED</option>
              <option value="DEAD_LETTER">DEAD_LETTER</option>
              <option value="CANCELLED">CANCELLED</option>
            </select>
          </div>

          {/* Job Type filter */}
          <div className="flex items-center gap-2">
            <span className="text-xs text-slate-400 shrink-0">Type:</span>
            <select
              value={jobTypeFilter}
              onChange={(e) => {
                setJobTypeFilter(e.target.value);
                setPage(0);
              }}
              className="w-full bg-slate-950 border border-slate-800 rounded-lg px-2.5 py-1.5 text-xs text-slate-300 focus:outline-none focus:border-brand-500"
            >
              <option value="ALL">All Types</option>
              <option value="EMAIL_SIMULATION">EMAIL_SIMULATION</option>
              <option value="REPORT_GENERATION">REPORT_GENERATION</option>
              <option value="DATA_PROCESSING">DATA_PROCESSING</option>
              <option value="DEMO_FAILURE">DEMO_FAILURE</option>
              <option value="DELAYED_TASK">DELAYED_TASK</option>
            </select>
          </div>

          {/* Sort By */}
          <div className="flex items-center gap-2">
            <span className="text-xs text-slate-400 shrink-0">Sort:</span>
            <select
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value)}
              className="w-full bg-slate-950 border border-slate-800 rounded-lg px-2.5 py-1.5 text-xs text-slate-300 focus:outline-none focus:border-brand-500"
            >
              <option value="createdAt">Created Time</option>
              <option value="priority">Priority</option>
              <option value="status">Status</option>
              <option value="executionDurationMs">Execution Duration</option>
              <option value="updatedAt">Updated Time</option>
            </select>
            <button
              onClick={() => setSortDirection(sortDirection === 'desc' ? 'asc' : 'desc')}
              title={`Sorting: ${sortDirection.toUpperCase()}`}
              className="p-1.5 bg-slate-950 border border-slate-800 rounded-lg text-slate-400 hover:text-slate-200"
            >
              <ArrowUpDown className="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>

      {/* Jobs Table */}
      <div className="rounded-xl bg-slate-900 border border-slate-800 overflow-hidden shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs text-slate-300">
            <thead className="bg-slate-950/80 border-b border-slate-800 text-[11px] uppercase tracking-wider text-slate-400 font-semibold">
              <tr>
                <th className="py-3 px-4">Job ID</th>
                <th className="py-3 px-4">Type</th>
                <th className="py-3 px-4">Status</th>
                <th className="py-3 px-4">Priority</th>
                <th className="py-3 px-4">Attempts</th>
                <th className="py-3 px-4">Duration</th>
                <th className="py-3 px-4">Created At</th>
                <th className="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 font-medium">
              {isLoading ? (
                <tr>
                  <td colSpan={8} className="py-12 text-center text-slate-500">
                    <div className="inline-flex items-center gap-2">
                      <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-brand-500"></div>
                      <span>Loading background jobs...</span>
                    </div>
                  </td>
                </tr>
              ) : jobs.length === 0 ? (
                <tr>
                  <td colSpan={8} className="py-12 text-center text-slate-500">
                    No jobs found matching the selected filters.
                  </td>
                </tr>
              ) : (
                jobs.map((job) => (
                  <tr
                    key={job.id}
                    onClick={() => setSelectedJobId(job.id)}
                    className="hover:bg-slate-850/60 cursor-pointer transition-colors"
                  >
                    <td className="py-3 px-4 font-mono text-[11px]">
                      <div className="flex items-center gap-1.5">
                        <span className="text-slate-300 truncate max-w-[110px]" title={job.id}>
                          {job.id.substring(0, 8)}...
                        </span>
                        <button
                          onClick={(e) => handleCopyId(job.id, e)}
                          title="Copy Full UUID"
                          className="text-slate-500 hover:text-slate-300 p-0.5 rounded"
                        >
                          {copiedId === job.id ? (
                            <Check className="w-3 h-3 text-emerald-400" />
                          ) : (
                            <Copy className="w-3 h-3" />
                          )}
                        </button>
                      </div>
                    </td>
                    <td className="py-3 px-4 font-semibold text-slate-200">
                      {job.jobType}
                    </td>
                    <td className="py-3 px-4">
                      <StatusBadge status={job.status} size="sm" />
                    </td>
                    <td className="py-3 px-4">
                      <PriorityBadge priority={job.priority} />
                    </td>
                    <td className="py-3 px-4 text-slate-300">
                      {job.attemptCount} / {job.maxAttempts}
                    </td>
                    <td className="py-3 px-4 font-mono text-slate-300">
                      {job.executionDurationMs != null ? `${job.executionDurationMs} ms` : '-'}
                    </td>
                    <td className="py-3 px-4 text-slate-400 text-[11px]">
                      {new Date(job.createdAt).toLocaleTimeString()}
                    </td>
                    <td className="py-3 px-4 text-right">
                      <div className="flex items-center justify-end gap-1.5" onClick={(e) => e.stopPropagation()}>
                        <button
                          onClick={() => setSelectedJobId(job.id)}
                          title="Inspect Details"
                          className="p-1 rounded bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white transition-colors"
                        >
                          <Eye className="w-3.5 h-3.5" />
                        </button>

                        {(job.status === 'QUEUED' || job.status === 'RETRY_WAIT') && (
                          <button
                            onClick={(e) => handleCancelJob(job.id, e)}
                            title="Cancel Job"
                            className="p-1 rounded bg-rose-950/40 hover:bg-rose-900/60 text-rose-400 border border-rose-800/40 transition-colors"
                          >
                            <Ban className="w-3.5 h-3.5" />
                          </button>
                        )}

                        {(job.status === 'FAILED' || job.status === 'DEAD_LETTER') && (
                          <button
                            onClick={(e) => handleRetryJob(job.id, e)}
                            title="Retry Job"
                            className="p-1 rounded bg-emerald-950/40 hover:bg-emerald-900/60 text-emerald-400 border border-emerald-800/40 transition-colors"
                          >
                            <RotateCcw className="w-3.5 h-3.5" />
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Bar */}
        <div className="flex items-center justify-between p-3.5 bg-slate-950/60 border-t border-slate-800 text-xs text-slate-400">
          <div>
            Showing {jobs.length > 0 ? page * size + 1 : 0} to{' '}
            {Math.min((page + 1) * size, totalElements)} of {totalElements} jobs
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0 || isLoading}
              className="p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 disabled:opacity-40 disabled:hover:bg-slate-800 transition-colors"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <span className="px-2 font-medium text-slate-300">
              Page {totalPages > 0 ? page + 1 : 1} of {totalPages || 1}
            </span>
            <button
              onClick={() => setPage((p) => (p + 1 < totalPages ? p + 1 : p))}
              disabled={page + 1 >= totalPages || isLoading}
              className="p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 disabled:opacity-40 disabled:hover:bg-slate-800 transition-colors"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>

      {/* Details Modal */}
      {selectedJobId && (
        <JobDetailsModal
          jobId={selectedJobId}
          onClose={() => setSelectedJobId(null)}
          onJobUpdated={fetchJobs}
        />
      )}
    </div>
  );
};
