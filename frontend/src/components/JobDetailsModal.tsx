import React, { useEffect, useState } from 'react';
import { Job, JobAttempt } from '../types';
import { api } from '../api/client';
import { StatusBadge } from './StatusBadge';
import { PriorityBadge } from './PriorityBadge';
import { useToast } from './Toast';
import {
  X,
  Copy,
  Check,
  RotateCcw,
  Ban,
  Clock,
  Cpu,
  Layers,
  AlertCircle,
  FileCode,
} from 'lucide-react';

interface JobDetailsModalProps {
  jobId: string | null;
  onClose: () => void;
  onJobUpdated?: () => void;
}

export const JobDetailsModal: React.FC<JobDetailsModalProps> = ({
  jobId,
  onClose,
  onJobUpdated,
}) => {
  const [job, setJob] = useState<Job | null>(null);
  const [attempts, setAttempts] = useState<JobAttempt[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isActionLoading, setIsActionLoading] = useState(false);
  const [copiedKey, setCopiedKey] = useState<string | null>(null);
  const { showToast } = useToast();

  useEffect(() => {
    if (!jobId) return;

    let mounted = true;
    setIsLoading(true);

    Promise.all([api.getJob(jobId), api.getJobAttempts(jobId)])
      .then(([jobData, attemptsData]) => {
        if (mounted) {
          setJob(jobData);
          setAttempts(attemptsData);
          setIsLoading(false);
        }
      })
      .catch((err) => {
        if (mounted) {
          showToast(err.message, 'error');
          setIsLoading(false);
        }
      });

    return () => {
      mounted = false;
    };
  }, [jobId, showToast]);

  const handleCopy = (text: string, key: string) => {
    navigator.clipboard.writeText(text);
    setCopiedKey(key);
    setTimeout(() => setCopiedKey(null), 2000);
  };

  const handleCancel = async () => {
    if (!job) return;
    if (!confirm('Are you sure you want to cancel this queued job?')) return;

    setIsActionLoading(true);
    try {
      const updated = await api.cancelJob(job.id);
      setJob(updated);
      showToast('Job cancelled successfully', 'info');
      if (onJobUpdated) onJobUpdated();
    } catch (err: any) {
      showToast(err.message, 'error');
    } finally {
      setIsActionLoading(false);
    }
  };

  const handleRetry = async () => {
    if (!job) return;
    if (!confirm('Manually retry this job now?')) return;

    setIsActionLoading(true);
    try {
      const updated = await api.retryJob(job.id);
      setJob(updated);
      showToast('Job re-queued for execution', 'success');
      if (onJobUpdated) onJobUpdated();
      // Fetch fresh attempts
      const refreshedAttempts = await api.getJobAttempts(job.id);
      setAttempts(refreshedAttempts);
    } catch (err: any) {
      showToast(err.message, 'error');
    } finally {
      setIsActionLoading(false);
    }
  };

  if (!jobId) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm animate-in fade-in duration-150">
      <div className="relative w-full max-w-3xl bg-slate-900 border border-slate-800 rounded-xl shadow-2xl overflow-hidden flex flex-col max-h-[90vh]">
        {/* Header */}
        <div className="flex items-center justify-between p-5 border-b border-slate-800 bg-slate-850/50">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-slate-800 rounded-lg text-brand-400">
              <Layers className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h3 className="text-base font-bold text-slate-100 tracking-tight">
                  {job ? job.jobType : 'Loading Job Details...'}
                </h3>
                {job && <StatusBadge status={job.status} size="sm" />}
                {job && <PriorityBadge priority={job.priority} />}
              </div>
              <p className="text-xs text-slate-400 font-mono mt-0.5">ID: {jobId}</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-lg text-slate-400 hover:text-slate-200 hover:bg-slate-800 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Modal Body */}
        <div className="p-6 overflow-y-auto space-y-6 flex-1 text-sm">
          {isLoading ? (
            <div className="flex items-center justify-center py-12 text-slate-400">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-brand-500 mr-3"></div>
              <span>Fetching job data and execution history...</span>
            </div>
          ) : job ? (
            <>
              {/* Metadata Grid */}
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 p-4 rounded-lg bg-slate-950/60 border border-slate-800 text-xs">
                <div>
                  <span className="text-slate-500 block">Attempts</span>
                  <span className="font-semibold text-slate-200">
                    {job.attemptCount} / {job.maxAttempts}
                  </span>
                </div>
                <div>
                  <span className="text-slate-500 block">Worker Assigned</span>
                  <span className="font-semibold text-slate-200 font-mono">
                    {job.workerId || 'None'}
                  </span>
                </div>
                <div>
                  <span className="text-slate-500 block">Execution Time</span>
                  <span className="font-semibold text-slate-200">
                    {job.executionDurationMs != null ? `${job.executionDurationMs} ms` : 'N/A'}
                  </span>
                </div>
                <div>
                  <span className="text-slate-500 block">Created At</span>
                  <span className="font-semibold text-slate-200">
                    {new Date(job.createdAt).toLocaleTimeString()}
                  </span>
                </div>
                {job.idempotencyKey && (
                  <div className="col-span-2">
                    <span className="text-slate-500 block">Idempotency Key</span>
                    <span className="font-mono text-slate-300 text-[11px] truncate block">
                      {job.idempotencyKey}
                    </span>
                  </div>
                )}
                {job.nextRetryAt && (
                  <div className="col-span-2">
                    <span className="text-purple-400 block font-medium">Scheduled Next Retry</span>
                    <span className="font-semibold text-purple-300">
                      {new Date(job.nextRetryAt).toLocaleTimeString()}
                    </span>
                  </div>
                )}
              </div>

              {/* Error Message banner if failed */}
              {job.lastError && (
                <div className="p-3.5 rounded-lg bg-rose-500/10 border border-rose-500/20 text-rose-300 text-xs flex items-start gap-2.5">
                  <AlertCircle className="w-4 h-4 text-rose-400 shrink-0 mt-0.5" />
                  <div className="font-mono leading-relaxed break-all">
                    <strong className="block text-rose-200 font-sans mb-0.5">Last Error:</strong>
                    {job.lastError}
                  </div>
                </div>
              )}

              {/* Payload Section */}
              <div>
                <div className="flex items-center justify-between mb-1.5">
                  <span className="text-xs font-semibold text-slate-300 flex items-center gap-1.5">
                    <FileCode className="w-3.5 h-3.5 text-brand-400" />
                    Input Payload (JSON)
                  </span>
                  <button
                    onClick={() => handleCopy(JSON.stringify(job.payload, null, 2), 'payload')}
                    className="text-xs text-slate-400 hover:text-slate-200 flex items-center gap-1"
                  >
                    {copiedKey === 'payload' ? (
                      <Check className="w-3 h-3 text-emerald-400" />
                    ) : (
                      <Copy className="w-3 h-3" />
                    )}
                    <span>{copiedKey === 'payload' ? 'Copied' : 'Copy'}</span>
                  </button>
                </div>
                <pre className="p-3 bg-slate-950 rounded-lg border border-slate-800 text-slate-300 font-mono text-xs overflow-x-auto max-h-48">
                  {JSON.stringify(job.payload, null, 2)}
                </pre>
              </div>

              {/* Result Section (if present) */}
              {job.result && (
                <div>
                  <div className="flex items-center justify-between mb-1.5">
                    <span className="text-xs font-semibold text-slate-300 flex items-center gap-1.5">
                      <Check className="w-3.5 h-3.5 text-emerald-400" />
                      Execution Result (JSON)
                    </span>
                    <button
                      onClick={() => handleCopy(JSON.stringify(job.result, null, 2), 'result')}
                      className="text-xs text-slate-400 hover:text-slate-200 flex items-center gap-1"
                    >
                      {copiedKey === 'result' ? (
                        <Check className="w-3 h-3 text-emerald-400" />
                      ) : (
                        <Copy className="w-3 h-3" />
                      )}
                      <span>{copiedKey === 'result' ? 'Copied' : 'Copy'}</span>
                    </button>
                  </div>
                  <pre className="p-3 bg-slate-950 rounded-lg border border-slate-800 text-emerald-300 font-mono text-xs overflow-x-auto max-h-48">
                    {JSON.stringify(job.result, null, 2)}
                  </pre>
                </div>
              )}

              {/* Attempt History Section */}
              <div>
                <span className="text-xs font-semibold text-slate-300 flex items-center gap-1.5 mb-2.5">
                  <Clock className="w-3.5 h-3.5 text-brand-400" />
                  Execution History ({attempts.length} {attempts.length === 1 ? 'Attempt' : 'Attempts'})
                </span>

                {attempts.length === 0 ? (
                  <p className="text-xs text-slate-500 italic p-3 bg-slate-950/40 rounded-lg border border-slate-800/60">
                    No execution attempts recorded yet.
                  </p>
                ) : (
                  <div className="space-y-2">
                    {attempts.map((attempt) => (
                      <div
                        key={attempt.id}
                        className="p-3 rounded-lg bg-slate-950/60 border border-slate-800 flex flex-col gap-2 text-xs"
                      >
                        <div className="flex items-center justify-between">
                          <div className="flex items-center gap-2">
                            <span className="font-bold text-slate-200">
                              Attempt #{attempt.attemptNumber}
                            </span>
                            <StatusBadge status={attempt.outcome} size="sm" />
                          </div>
                          <span className="font-mono text-slate-400">
                            {attempt.executionDurationMs} ms
                          </span>
                        </div>

                        <div className="grid grid-cols-2 gap-2 text-[11px] text-slate-400">
                          <div>
                            <span className="text-slate-500">Worker:</span> {attempt.workerId}
                          </div>
                          <div>
                            <span className="text-slate-500">Started:</span>{' '}
                            {new Date(attempt.startedAt).toLocaleTimeString()}
                          </div>
                        </div>

                        {attempt.errorDetails && (
                          <div className="mt-1 p-2 bg-rose-950/30 border border-rose-900/40 rounded text-rose-300 text-[11px] font-mono break-all">
                            {attempt.errorDetails}
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </>
          ) : null}
        </div>

        {/* Footer Actions */}
        <div className="p-4 border-t border-slate-800 bg-slate-850/50 flex items-center justify-between">
          <div className="flex items-center gap-2">
            {(job?.status === 'QUEUED' || job?.status === 'RETRY_WAIT') && (
              <button
                onClick={handleCancel}
                disabled={isActionLoading}
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold bg-rose-600/20 text-rose-300 hover:bg-rose-600/30 border border-rose-500/30 transition-all disabled:opacity-50"
              >
                <Ban className="w-3.5 h-3.5" />
                <span>Cancel Job</span>
              </button>
            )}

            {(job?.status === 'FAILED' || job?.status === 'DEAD_LETTER') && (
              <button
                onClick={handleRetry}
                disabled={isActionLoading}
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold bg-brand-600 hover:bg-brand-500 text-white shadow-md shadow-brand-600/20 transition-all disabled:opacity-50"
              >
                <RotateCcw className="w-3.5 h-3.5" />
                <span>Retry Job Now</span>
              </button>
            )}
          </div>

          <button
            onClick={onClose}
            className="px-4 py-1.5 rounded-lg text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white transition-colors"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};
