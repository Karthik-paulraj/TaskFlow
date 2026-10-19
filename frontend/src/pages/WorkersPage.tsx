import React, { useEffect, useState } from 'react';
import { WorkerStats } from '../types';
import { api } from '../api/client';
import { useToast } from '../components/Toast';
import { StatusBadge } from '../components/StatusBadge';
import { PriorityBadge } from '../components/PriorityBadge';
import {
  Cpu,
  Layers,
  Activity,
  Clock,
  CheckCircle2,
  AlertTriangle,
  Server,
  ShieldCheck,
} from 'lucide-react';

interface WorkersPageProps {
  refreshTrigger: number;
  onInspectJob: (id: string) => void;
}

export const WorkersPage: React.FC<WorkersPageProps> = ({
  refreshTrigger,
  onInspectJob,
}) => {
  const [stats, setStats] = useState<WorkerStats | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const { showToast } = useToast();

  useEffect(() => {
    let mounted = true;

    api.getWorkerStats()
      .then((data) => {
        if (mounted) {
          setStats(data);
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
  }, [refreshTrigger, showToast]);

  const poolSize = stats ? stats.configuredWorkers : 6;
  const activeWorkers = stats ? stats.activeWorkers : 0;
  const idleWorkers = stats ? stats.idleWorkers : poolSize;
  const utilizationPct = poolSize > 0 ? Math.round((activeWorkers / poolSize) * 100) : 0;

  return (
    <div className="space-y-6">
      {/* Metrics Row */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="p-4 rounded-xl bg-slate-900 border border-slate-800 shadow-sm">
          <div className="flex items-center justify-between text-slate-400 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">Pool Capacity</span>
            <Server className="w-4 h-4 text-slate-400" />
          </div>
          <div className="text-2xl font-bold text-slate-100">
            {stats ? stats.configuredWorkers : '-'}
          </div>
          <span className="text-[11px] text-slate-500 mt-1 block">Configured worker threads</span>
        </div>

        <div className="p-4 rounded-xl bg-slate-900 border border-blue-500/20 shadow-sm">
          <div className="flex items-center justify-between text-blue-400 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">Active Workers</span>
            <Activity className="w-4 h-4 text-blue-400" />
          </div>
          <div className="text-2xl font-bold text-blue-300">
            {stats ? stats.activeWorkers : '-'}
          </div>
          <span className="text-[11px] text-slate-500 mt-1 block">Live threads processing tasks</span>
        </div>

        <div className="p-4 rounded-xl bg-slate-900 border border-emerald-500/20 shadow-sm">
          <div className="flex items-center justify-between text-emerald-400 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">Idle Workers</span>
            <Cpu className="w-4 h-4 text-emerald-400" />
          </div>
          <div className="text-2xl font-bold text-emerald-300">
            {stats ? stats.idleWorkers : '-'}
          </div>
          <span className="text-[11px] text-slate-500 mt-1 block">Ready for immediate claim</span>
        </div>

        <div className="p-4 rounded-xl bg-slate-900 border border-amber-500/20 shadow-sm">
          <div className="flex items-center justify-between text-amber-400 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">Queue Depth</span>
            <Layers className="w-4 h-4 text-amber-400" />
          </div>
          <div className="text-2xl font-bold text-amber-300">
            {stats ? stats.queueDepth : '-'}
          </div>
          <span className="text-[11px] text-slate-500 mt-1 block">Queued & RetryWait jobs</span>
        </div>
      </div>

      {/* Utilization Bar */}
      <div className="p-5 rounded-xl bg-slate-900 border border-slate-800 space-y-3">
        <div className="flex items-center justify-between text-xs">
          <span className="font-semibold text-slate-200 flex items-center gap-2">
            <Cpu className="w-4 h-4 text-brand-400" />
            Worker Engine Utilization
          </span>
          <span className="font-bold text-brand-400">{utilizationPct}% Busy</span>
        </div>
        <div className="w-full bg-slate-950 h-3 rounded-full overflow-hidden border border-slate-800 p-0.5">
          <div
            className="h-full rounded-full bg-gradient-to-r from-brand-500 to-emerald-400 transition-all duration-300"
            style={{ width: `${utilizationPct}%` }}
          />
        </div>
        <div className="flex justify-between text-[11px] text-slate-500">
          <span>{activeWorkers} threads busy</span>
          <span>{idleWorkers} threads idle available</span>
        </div>
      </div>

      {/* Currently Running Tasks Table */}
      <div className="rounded-xl bg-slate-900 border border-slate-800 overflow-hidden shadow-sm">
        <div className="p-4 border-b border-slate-800 flex items-center justify-between">
          <div>
            <h3 className="text-sm font-bold text-slate-100 flex items-center gap-2">
              <Activity className="w-4 h-4 text-blue-400" />
              Active Running Executions ({stats ? stats.currentlyRunningJobs.length : 0})
            </h3>
            <p className="text-xs text-slate-400">
              Live background jobs being processed by workers right now
            </p>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs text-slate-300">
            <thead className="bg-slate-950/80 border-b border-slate-800 text-[11px] uppercase tracking-wider text-slate-400 font-semibold">
              <tr>
                <th className="py-3 px-4">Job ID</th>
                <th className="py-3 px-4">Type</th>
                <th className="py-3 px-4">Worker Thread</th>
                <th className="py-3 px-4">Priority</th>
                <th className="py-3 px-4">Started At</th>
                <th className="py-3 px-4 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 font-medium">
              {isLoading ? (
                <tr>
                  <td colSpan={6} className="py-10 text-center text-slate-500">
                    Loading worker execution states...
                  </td>
                </tr>
              ) : !stats || stats.currentlyRunningJobs.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-10 text-center text-slate-500">
                    <span className="block mb-1 text-slate-400">No active executions</span>
                    All worker threads are idle and waiting for incoming queue tasks.
                  </td>
                </tr>
              ) : (
                stats.currentlyRunningJobs.map((job) => (
                  <tr key={job.id} className="hover:bg-slate-850/60">
                    <td className="py-3 px-4 font-mono text-[11px] text-slate-200">
                      {job.id}
                    </td>
                    <td className="py-3 px-4 font-semibold text-slate-200">
                      {job.jobType}
                    </td>
                    <td className="py-3 px-4 font-mono text-blue-400 text-xs">
                      {job.workerId || 'Worker'}
                    </td>
                    <td className="py-3 px-4">
                      <PriorityBadge priority={job.priority} />
                    </td>
                    <td className="py-3 px-4 text-slate-400 text-[11px]">
                      {job.startedAt ? new Date(job.startedAt).toLocaleTimeString() : '-'}
                    </td>
                    <td className="py-3 px-4 text-right">
                      <button
                        onClick={() => onInspectJob(job.id)}
                        className="px-2.5 py-1 rounded bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white transition-colors text-xs"
                      >
                        Inspect
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Architectural Guarantee Card */}
      <div className="p-5 rounded-xl bg-slate-900 border border-slate-800 space-y-3">
        <h4 className="text-xs font-bold uppercase tracking-wider text-slate-300 flex items-center gap-2">
          <ShieldCheck className="w-4 h-4 text-emerald-400" />
          Concurrency & Fault Tolerance Architecture
        </h4>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-xs text-slate-400 leading-relaxed">
          <div className="p-3 bg-slate-950/60 rounded-lg border border-slate-800/80">
            <strong className="text-slate-200 block mb-1">Atomic Claiming</strong>
            Uses row-level database locking and state validation to ensure a job is claimed by at most one worker thread.
          </div>
          <div className="p-3 bg-slate-950/60 rounded-lg border border-slate-800/80">
            <strong className="text-slate-200 block mb-1">Bounded Resource Consumption</strong>
            Worker pool uses bounded queues with backpressure, preventing memory exhaustion under burst traffic.
          </div>
          <div className="p-3 bg-slate-950/60 rounded-lg border border-slate-800/80">
            <strong className="text-slate-200 block mb-1">Crash & Lease Recovery</strong>
            The background orphan monitor detects abandoned jobs whose worker lease timed out and re-queues them safely.
          </div>
        </div>
      </div>
    </div>
  );
};
