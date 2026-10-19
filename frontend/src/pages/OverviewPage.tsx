import React, { useEffect, useState } from 'react';
import { StatsOverview, TimeseriesPoint } from '../types';
import { api } from '../api/client';
import { useToast } from '../components/Toast';
import {
  Layers,
  Clock,
  Play,
  CheckCircle2,
  XCircle,
  Skull,
  TrendingUp,
  Activity,
  Zap,
} from 'lucide-react';
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  BarChart,
  Bar,
} from 'recharts';

interface OverviewPageProps {
  onNavigateToSubmit: () => void;
  onNavigateToJobs: (statusFilter?: string) => void;
  refreshTrigger: number;
}

export const OverviewPage: React.FC<OverviewPageProps> = ({
  onNavigateToSubmit,
  onNavigateToJobs,
  refreshTrigger,
}) => {
  const [stats, setStats] = useState<StatsOverview | null>(null);
  const [timeseries, setTimeseries] = useState<TimeseriesPoint[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const { showToast } = useToast();

  useEffect(() => {
    let mounted = true;

    Promise.all([api.getStatsOverview(), api.getTimeseries(30, 60)])
      .then(([overviewData, tsData]) => {
        if (mounted) {
          setStats(overviewData);
          setTimeseries(tsData);
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

  const handleQuickDemo = async (type: string) => {
    try {
      if (type === 'EMAIL') {
        await api.createJob({
          jobType: 'EMAIL_SIMULATION',
          payload: { to: 'quickdemo@taskflow.dev', subject: 'Quick Demo Job' },
          priority: 8,
          maxAttempts: 3,
        });
        showToast('Created EMAIL_SIMULATION job', 'success');
      } else if (type === 'FAILURE') {
        await api.createJob({
          jobType: 'DEMO_FAILURE',
          payload: { failureMode: 'FAIL_N_TIMES', failUntilAttempt: 2 },
          priority: 9,
          maxAttempts: 3,
        });
        showToast('Created DEMO_FAILURE job (will retry 2 times then succeed)', 'warning');
      }
    } catch (e: any) {
      showToast(e.message, 'error');
    }
  };

  // Formatter for timestamp labels on charts
  const formatTime = (ts: string) => {
    const d = new Date(ts);
    return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  };

  return (
    <div className="space-y-6">
      {/* Metric Cards Grid */}
      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
        {/* Total */}
        <div
          onClick={() => onNavigateToJobs('ALL')}
          className="p-4 rounded-xl bg-slate-900 border border-slate-800 hover:border-slate-700 transition-all cursor-pointer shadow-sm"
        >
          <div className="flex items-center justify-between text-slate-400 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">Total</span>
            <Layers className="w-4 h-4 text-slate-400" />
          </div>
          <div className="text-2xl font-bold text-slate-100">
            {stats ? stats.totalJobs : '-'}
          </div>
          <span className="text-[11px] text-slate-500 mt-1 block">All registered jobs</span>
        </div>

        {/* Queued */}
        <div
          onClick={() => onNavigateToJobs('QUEUED')}
          className="p-4 rounded-xl bg-slate-900 border border-amber-500/20 hover:border-amber-500/40 transition-all cursor-pointer shadow-sm"
        >
          <div className="flex items-center justify-between text-amber-400 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">Queued</span>
            <Clock className="w-4 h-4 text-amber-400" />
          </div>
          <div className="text-2xl font-bold text-amber-300">
            {stats ? stats.queuedJobs : '-'}
          </div>
          <span className="text-[11px] text-slate-500 mt-1 block">Awaiting execution</span>
        </div>

        {/* Running */}
        <div
          onClick={() => onNavigateToJobs('RUNNING')}
          className="p-4 rounded-xl bg-slate-900 border border-blue-500/20 hover:border-blue-500/40 transition-all cursor-pointer shadow-sm"
        >
          <div className="flex items-center justify-between text-blue-400 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">Running</span>
            <Play className="w-4 h-4 text-blue-400" />
          </div>
          <div className="text-2xl font-bold text-blue-300">
            {stats ? stats.runningJobs : '-'}
          </div>
          <span className="text-[11px] text-slate-500 mt-1 block">Active worker execution</span>
        </div>

        {/* Succeeded */}
        <div
          onClick={() => onNavigateToJobs('SUCCEEDED')}
          className="p-4 rounded-xl bg-slate-900 border border-emerald-500/20 hover:border-emerald-500/40 transition-all cursor-pointer shadow-sm"
        >
          <div className="flex items-center justify-between text-emerald-400 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">Succeeded</span>
            <CheckCircle2 className="w-4 h-4 text-emerald-400" />
          </div>
          <div className="text-2xl font-bold text-emerald-300">
            {stats ? stats.succeededJobs : '-'}
          </div>
          <span className="text-[11px] text-slate-500 mt-1 block">Completed successfully</span>
        </div>

        {/* Failed */}
        <div
          onClick={() => onNavigateToJobs('FAILED')}
          className="p-4 rounded-xl bg-slate-900 border border-rose-500/20 hover:border-rose-500/40 transition-all cursor-pointer shadow-sm"
        >
          <div className="flex items-center justify-between text-rose-400 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">Failed</span>
            <XCircle className="w-4 h-4 text-rose-400" />
          </div>
          <div className="text-2xl font-bold text-rose-300">
            {stats ? stats.failedJobs : '-'}
          </div>
          <span className="text-[11px] text-slate-500 mt-1 block">Terminal failure</span>
        </div>

        {/* Dead Letter */}
        <div
          onClick={() => onNavigateToJobs('DEAD_LETTER')}
          className="p-4 rounded-xl bg-slate-900 border border-red-500/30 hover:border-red-500/50 transition-all cursor-pointer shadow-sm"
        >
          <div className="flex items-center justify-between text-red-400 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">Dead Letter</span>
            <Skull className="w-4 h-4 text-red-400" />
          </div>
          <div className="text-2xl font-bold text-red-400">
            {stats ? stats.deadLetterJobs : '-'}
          </div>
          <span className="text-[11px] text-slate-500 mt-1 block">Exhausted all retries</span>
        </div>
      </div>

      {/* Quick Launch Banner */}
      <div className="p-4 rounded-xl bg-gradient-to-r from-slate-900 via-slate-850 to-slate-900 border border-slate-800 flex flex-col sm:flex-row items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="p-2.5 rounded-lg bg-brand-500/10 text-brand-400 border border-brand-500/20">
            <Zap className="w-5 h-5" />
          </div>
          <div>
            <h4 className="text-sm font-bold text-slate-100">Live Engine Diagnostics & Demos</h4>
            <p className="text-xs text-slate-400">
              Submit realistic background jobs to observe concurrent worker claiming and failure recovery in real time.
            </p>
          </div>
        </div>
        <div className="flex items-center gap-2 w-full sm:w-auto">
          <button
            onClick={() => handleQuickDemo('EMAIL')}
            className="flex-1 sm:flex-none px-3 py-1.5 rounded-lg text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 transition-colors"
          >
            + Test Email Job
          </button>
          <button
            onClick={() => handleQuickDemo('FAILURE')}
            className="flex-1 sm:flex-none px-3 py-1.5 rounded-lg text-xs font-semibold bg-amber-500/20 hover:bg-amber-500/30 text-amber-300 border border-amber-500/30 transition-colors"
          >
            + Demo Retry Recovery
          </button>
          <button
            onClick={onNavigateToSubmit}
            className="flex-1 sm:flex-none px-3 py-1.5 rounded-lg text-xs font-semibold bg-brand-600 hover:bg-brand-500 text-white shadow-md shadow-brand-600/30 transition-colors"
          >
            Submit Custom Job
          </button>
        </div>
      </div>

      {/* Charts Section */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Submissions & Completions Timeseries AreaChart */}
        <div className="p-5 rounded-xl bg-slate-900 border border-slate-800">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h3 className="text-sm font-bold text-slate-100 flex items-center gap-2">
                <TrendingUp className="w-4 h-4 text-brand-400" />
                Job Throughput Activity
              </h3>
              <p className="text-xs text-slate-400">
                Jobs submitted vs jobs successfully processed in 60s windows
              </p>
            </div>
          </div>

          <div className="h-64 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={timeseries} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                <defs>
                  <linearGradient id="colorSubmitted" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.4} />
                    <stop offset="95%" stopColor="#3b82f6" stopOpacity={0.0} />
                  </linearGradient>
                  <linearGradient id="colorSucceeded" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#10b981" stopOpacity={0.4} />
                    <stop offset="95%" stopColor="#10b981" stopOpacity={0.0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
                <XAxis dataKey="timestamp" tickFormatter={formatTime} stroke="#64748b" textAnchor="end" fontSize={10} />
                <YAxis stroke="#64748b" fontSize={10} />
                <Tooltip
                  contentStyle={{
                    backgroundColor: '#0f172a',
                    borderColor: '#334155',
                    borderRadius: '0.5rem',
                    fontSize: '12px',
                  }}
                  labelFormatter={(ts) => `Time: ${new Date(ts).toLocaleTimeString()}`}
                />
                <Area
                  type="monotone"
                  dataKey="submitted"
                  name="Submitted"
                  stroke="#3b82f6"
                  fillOpacity={1}
                  fill="url(#colorSubmitted)"
                />
                <Area
                  type="monotone"
                  dataKey="succeeded"
                  name="Succeeded"
                  stroke="#10b981"
                  fillOpacity={1}
                  fill="url(#colorSucceeded)"
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Execution Duration Trend BarChart */}
        <div className="p-5 rounded-xl bg-slate-900 border border-slate-800">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h3 className="text-sm font-bold text-slate-100 flex items-center gap-2">
                <Activity className="w-4 h-4 text-purple-400" />
                Average Execution Duration (ms)
              </h3>
              <p className="text-xs text-slate-400">Mean processing time of finished jobs</p>
            </div>
          </div>

          <div className="h-64 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={timeseries} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
                <XAxis dataKey="timestamp" tickFormatter={formatTime} stroke="#64748b" fontSize={10} />
                <YAxis stroke="#64748b" fontSize={10} />
                <Tooltip
                  contentStyle={{
                    backgroundColor: '#0f172a',
                    borderColor: '#334155',
                    borderRadius: '0.5rem',
                    fontSize: '12px',
                  }}
                  labelFormatter={(ts) => `Time: ${new Date(ts).toLocaleTimeString()}`}
                  formatter={(val: any) => [`${val} ms`, 'Avg Duration']}
                />
                <Bar dataKey="avgDurationMs" name="Avg Duration (ms)" fill="#a855f7" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>
    </div>
  );
};
