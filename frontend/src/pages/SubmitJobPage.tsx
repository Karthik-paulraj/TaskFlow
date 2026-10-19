import React, { useState } from 'react';
import { api } from '../api/client';
import { useToast } from '../components/Toast';
import {
  Send,
  Sparkles,
  Layers,
  Key,
  Calendar,
  AlertTriangle,
  CheckCircle2,
  ListPlus,
  PlayCircle,
} from 'lucide-react';

interface SubmitJobPageProps {
  onJobCreated: () => void;
}

const SAMPLE_PAYLOADS: Record<string, any> = {
  EMAIL_SIMULATION: {
    to: 'engineering@taskflow.dev',
    subject: 'Batch Pipeline Notification',
    body: 'Automated background task processed successfully.',
  },
  REPORT_GENERATION: {
    reportType: 'DAILY_SUMMARY',
    parameters: {
      includeMetrics: true,
      timeWindowHours: 24,
    },
  },
  DATA_PROCESSING: {
    batchSize: 500,
    records: [120, 450, 890, 1200, 3400],
  },
  DEMO_FAILURE: {
    failureMode: 'FAIL_N_TIMES',
    failUntilAttempt: 2,
    errorMessage: 'Simulated downstream timeout (recoverable)',
  },
  DELAYED_TASK: {
    durationMs: 1500,
  },
};

export const SubmitJobPage: React.FC<SubmitJobPageProps> = ({ onJobCreated }) => {
  const [mode, setMode] = useState<'single' | 'bulk'>('single');

  // Single job state
  const [jobType, setJobType] = useState('EMAIL_SIMULATION');
  const [payloadStr, setPayloadStr] = useState(
    JSON.stringify(SAMPLE_PAYLOADS.EMAIL_SIMULATION, null, 2)
  );
  const [priority, setPriority] = useState(5);
  const [maxAttempts, setMaxAttempts] = useState(3);
  const [scheduledAt, setScheduledAt] = useState('');
  const [idempotencyKey, setIdempotencyKey] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [jsonError, setJsonError] = useState<string | null>(null);

  // Bulk state
  const [bulkPreset, setBulkPreset] = useState<'benchmark' | 'mixed' | 'failures'>('benchmark');
  const [isBulkSubmitting, setIsBulkSubmitting] = useState(false);

  const { showToast } = useToast();

  const handleJobTypeChange = (newType: string) => {
    setJobType(newType);
    if (SAMPLE_PAYLOADS[newType]) {
      setPayloadStr(JSON.stringify(SAMPLE_PAYLOADS[newType], null, 2));
      setJsonError(null);
    }
  };

  const handlePayloadChange = (val: string) => {
    setPayloadStr(val);
    try {
      JSON.parse(val);
      setJsonError(null);
    } catch (e: any) {
      setJsonError(e.message);
    }
  };

  const handleGenerateKey = () => {
    const key = 'idem-' + Math.random().toString(36).substring(2, 10) + '-' + Date.now();
    setIdempotencyKey(key);
  };

  const handleSubmitSingle = async (e: React.FormEvent) => {
    e.preventDefault();

    let parsedPayload = {};
    try {
      parsedPayload = JSON.parse(payloadStr);
    } catch {
      setJsonError('Please fix JSON formatting before submitting.');
      return;
    }

    setIsSubmitting(true);
    try {
      const created = await api.createJob({
        jobType,
        payload: parsedPayload,
        priority,
        maxAttempts,
        scheduledAt: scheduledAt ? new Date(scheduledAt).toISOString() : undefined,
        idempotencyKey: idempotencyKey.trim() || undefined,
      });

      showToast(`Submitted job ${created.id.substring(0, 8)}... (${created.jobType})`, 'success');
      onJobCreated();
    } catch (err: any) {
      showToast(err.message, 'error');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleSubmitBulkPreset = async () => {
    setIsBulkSubmitting(true);
    try {
      const jobsToSubmit: any[] = [];

      if (bulkPreset === 'benchmark') {
        // 20 DATA_PROCESSING jobs
        for (let i = 1; i <= 20; i++) {
          jobsToSubmit.push({
            jobType: 'DATA_PROCESSING',
            payload: { batchSize: 50 * i },
            priority: (i % 10) + 1,
            maxAttempts: 3,
          });
        }
      } else if (bulkPreset === 'mixed') {
        // 15 mixed jobs
        for (let i = 1; i <= 5; i++) {
          jobsToSubmit.push({
            jobType: 'EMAIL_SIMULATION',
            payload: { to: `user${i}@taskflow.dev`, subject: `Notification #${i}` },
            priority: 7,
            maxAttempts: 3,
          });
          jobsToSubmit.push({
            jobType: 'REPORT_GENERATION',
            payload: { reportType: 'DAILY_SUMMARY' },
            priority: 5,
            maxAttempts: 3,
          });
          jobsToSubmit.push({
            jobType: 'DELAYED_TASK',
            payload: { durationMs: 400 },
            priority: 4,
            maxAttempts: 3,
          });
        }
      } else if (bulkPreset === 'failures') {
        // 6 jobs configured to fail and retry
        for (let i = 1; i <= 6; i++) {
          jobsToSubmit.push({
            jobType: 'DEMO_FAILURE',
            payload: { failureMode: 'FAIL_N_TIMES', failUntilAttempt: 2 },
            priority: 9,
            maxAttempts: 3,
          });
        }
      }

      const res = await api.createBulkJobs(jobsToSubmit);
      showToast(`Bulk submitted ${res.totalSubmitted} jobs successfully`, 'success');
      onJobCreated();
    } catch (err: any) {
      showToast(err.message, 'error');
    } finally {
      setIsBulkSubmitting(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      {/* Mode Switcher */}
      <div className="flex items-center gap-2 border-b border-slate-800 pb-4">
        <button
          onClick={() => setMode('single')}
          className={`flex items-center gap-2 px-4 py-2 rounded-lg text-xs font-semibold transition-all ${
            mode === 'single'
              ? 'bg-brand-600 text-white shadow-md shadow-brand-600/30'
              : 'text-slate-400 hover:text-slate-200 bg-slate-900 border border-slate-800'
          }`}
        >
          <Send className="w-4 h-4" />
          <span>Single Job Submission</span>
        </button>

        <button
          onClick={() => setMode('bulk')}
          className={`flex items-center gap-2 px-4 py-2 rounded-lg text-xs font-semibold transition-all ${
            mode === 'bulk'
              ? 'bg-brand-600 text-white shadow-md shadow-brand-600/30'
              : 'text-slate-400 hover:text-slate-200 bg-slate-900 border border-slate-800'
          }`}
        >
          <ListPlus className="w-4 h-4" />
          <span>Bulk Wave Submission</span>
        </button>
      </div>

      {mode === 'single' ? (
        <form onSubmit={handleSubmitSingle} className="p-6 rounded-xl bg-slate-900 border border-slate-800 space-y-6">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-bold text-slate-100 flex items-center gap-2">
              <Layers className="w-4 h-4 text-brand-400" />
              Configure Background Job
            </h3>
            <button
              type="button"
              onClick={() => handleJobTypeChange(jobType)}
              className="flex items-center gap-1.5 text-xs text-brand-400 hover:text-brand-300 font-medium"
            >
              <Sparkles className="w-3.5 h-3.5" />
              Reset Sample Payload
            </button>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {/* Job Type Selector */}
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                Job Handler Type *
              </label>
              <select
                value={jobType}
                onChange={(e) => handleJobTypeChange(e.target.value)}
                className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-brand-500"
              >
                <option value="EMAIL_SIMULATION">EMAIL_SIMULATION</option>
                <option value="REPORT_GENERATION">REPORT_GENERATION</option>
                <option value="DATA_PROCESSING">DATA_PROCESSING</option>
                <option value="DEMO_FAILURE">DEMO_FAILURE (Retry Testing)</option>
                <option value="DELAYED_TASK">DELAYED_TASK</option>
              </select>
            </div>

            {/* Priority Slider */}
            <div>
              <div className="flex items-center justify-between mb-1.5">
                <label className="text-xs font-semibold text-slate-300">
                  Priority (1 - 10)
                </label>
                <span className="text-xs font-bold text-brand-400">P{priority}</span>
              </div>
              <input
                type="range"
                min="1"
                max="10"
                value={priority}
                onChange={(e) => setPriority(Number(e.target.value))}
                className="w-full h-2 bg-slate-950 rounded-lg appearance-none cursor-pointer accent-brand-500"
              />
              <div className="flex justify-between text-[10px] text-slate-500 mt-1">
                <span>1 (Lowest)</span>
                <span>5 (Standard)</span>
                <span>10 (Urgent)</span>
              </div>
            </div>

            {/* Max Attempts */}
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                Max Execution Attempts
              </label>
              <input
                type="number"
                min="1"
                max="10"
                value={maxAttempts}
                onChange={(e) => setMaxAttempts(Number(e.target.value))}
                className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-brand-500"
              />
            </div>

            {/* Scheduled At */}
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                Scheduled Execution (Optional)
              </label>
              <div className="relative">
                <input
                  type="datetime-local"
                  value={scheduledAt}
                  onChange={(e) => setScheduledAt(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-brand-500"
                />
              </div>
            </div>
          </div>

          {/* Idempotency Key */}
          <div>
            <div className="flex items-center justify-between mb-1.5">
              <label className="text-xs font-semibold text-slate-300 flex items-center gap-1.5">
                <Key className="w-3.5 h-3.5 text-slate-400" />
                Idempotency Key (Optional)
              </label>
              <button
                type="button"
                onClick={handleGenerateKey}
                className="text-[11px] text-slate-400 hover:text-slate-200 underline"
              >
                Generate Key
              </button>
            </div>
            <input
              type="text"
              placeholder="e.g. order-payment-9842"
              value={idempotencyKey}
              onChange={(e) => setIdempotencyKey(e.target.value)}
              className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-200 font-mono focus:outline-none focus:border-brand-500"
            />
            <span className="text-[11px] text-slate-500 mt-1 block">
              Repeated submissions with the same key return the existing job without re-enqueuing.
            </span>
          </div>

          {/* JSON Payload Editor */}
          <div>
            <div className="flex items-center justify-between mb-1.5">
              <label className="text-xs font-semibold text-slate-300">
                Payload JSON *
              </label>
              {jsonError && (
                <span className="text-[11px] text-rose-400 flex items-center gap-1">
                  <AlertTriangle className="w-3 h-3" />
                  Invalid JSON
                </span>
              )}
            </div>
            <textarea
              rows={6}
              value={payloadStr}
              onChange={(e) => handlePayloadChange(e.target.value)}
              className={`w-full bg-slate-950 border rounded-lg p-3 text-xs font-mono text-slate-200 focus:outline-none ${
                jsonError ? 'border-rose-500/80' : 'border-slate-800 focus:border-brand-500'
              }`}
            />
            {jsonError && (
              <p className="text-[11px] text-rose-400 mt-1 font-mono">{jsonError}</p>
            )}
          </div>

          {/* Submit Button */}
          <div className="pt-2 flex justify-end">
            <button
              type="submit"
              disabled={isSubmitting || !!jsonError}
              className="flex items-center gap-2 px-6 py-2.5 rounded-lg text-xs font-bold bg-brand-600 hover:bg-brand-500 text-white shadow-lg shadow-brand-600/30 transition-all disabled:opacity-50"
            >
              {isSubmitting ? (
                <>
                  <div className="animate-spin rounded-full h-3.5 w-3.5 border-b-2 border-white"></div>
                  <span>Enqueueing Job...</span>
                </>
              ) : (
                <>
                  <Send className="w-3.5 h-3.5" />
                  <span>Enqueue Background Job</span>
                </>
              )}
            </button>
          </div>
        </form>
      ) : (
        /* Bulk Wave Submission View */
        <div className="p-6 rounded-xl bg-slate-900 border border-slate-800 space-y-6">
          <div>
            <h3 className="text-sm font-bold text-slate-100 flex items-center gap-2">
              <PlayCircle className="w-4 h-4 text-brand-400" />
              Dispatch Bulk Job Waves
            </h3>
            <p className="text-xs text-slate-400 mt-1">
              Test queue ingestion throughput and concurrent worker execution under heavy batch loads.
            </p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div
              onClick={() => setBulkPreset('benchmark')}
              className={`p-4 rounded-xl border cursor-pointer transition-all ${
                bulkPreset === 'benchmark'
                  ? 'bg-slate-850 border-brand-500 text-slate-100 shadow-md shadow-brand-500/10'
                  : 'bg-slate-950/60 border-slate-800 text-slate-400 hover:border-slate-700'
              }`}
            >
              <div className="font-bold text-sm text-slate-100 mb-1">
                Benchmark Wave
              </div>
              <p className="text-xs text-slate-400">
                20 DATA_PROCESSING tasks with stepped dataset sizes and variable priorities.
              </p>
            </div>

            <div
              onClick={() => setBulkPreset('mixed')}
              className={`p-4 rounded-xl border cursor-pointer transition-all ${
                bulkPreset === 'mixed'
                  ? 'bg-slate-850 border-brand-500 text-slate-100 shadow-md shadow-brand-500/10'
                  : 'bg-slate-950/60 border-slate-800 text-slate-400 hover:border-slate-700'
              }`}
            >
              <div className="font-bold text-sm text-slate-100 mb-1">
                Mixed Production Pipeline
              </div>
              <p className="text-xs text-slate-400">
                15 interleaved jobs: Emails, Aggregation Reports, and Delayed tasks.
              </p>
            </div>

            <div
              onClick={() => setBulkPreset('failures')}
              className={`p-4 rounded-xl border cursor-pointer transition-all ${
                bulkPreset === 'failures'
                  ? 'bg-slate-850 border-brand-500 text-slate-100 shadow-md shadow-brand-500/10'
                  : 'bg-slate-950/60 border-slate-800 text-slate-400 hover:border-slate-700'
              }`}
            >
              <div className="font-bold text-sm text-slate-100 mb-1">
                Failure & Recovery Wave
              </div>
              <p className="text-xs text-slate-400">
                6 jobs configured to fail initially, demonstrating exponential backoff and successful retry.
              </p>
            </div>
          </div>

          <div className="pt-2 flex justify-end">
            <button
              onClick={handleSubmitBulkPreset}
              disabled={isBulkSubmitting}
              className="flex items-center gap-2 px-6 py-2.5 rounded-lg text-xs font-bold bg-brand-600 hover:bg-brand-500 text-white shadow-lg shadow-brand-600/30 transition-all disabled:opacity-50"
            >
              {isBulkSubmitting ? (
                <>
                  <div className="animate-spin rounded-full h-3.5 w-3.5 border-b-2 border-white"></div>
                  <span>Dispatching Wave...</span>
                </>
              ) : (
                <>
                  <PlayCircle className="w-3.5 h-3.5" />
                  <span>Dispatch Selected Wave</span>
                </>
              )}
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
