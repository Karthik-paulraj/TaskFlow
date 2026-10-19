import React from 'react';
import { JobStatus, ExecutionOutcome } from '../types';
import {
  Clock,
  Play,
  CheckCircle2,
  RotateCcw,
  XCircle,
  Skull,
  Ban,
} from 'lucide-react';

interface StatusBadgeProps {
  status: JobStatus | ExecutionOutcome | string;
  size?: 'sm' | 'md';
}

export const StatusBadge: React.FC<StatusBadgeProps> = ({ status, size = 'md' }) => {
  const s = status.toUpperCase();
  const isSm = size === 'sm';

  switch (s) {
    case 'QUEUED':
      return (
        <span
          className={`inline-flex items-center gap-1.5 font-medium rounded-full bg-amber-500/10 text-amber-400 border border-amber-500/20 ${
            isSm ? 'px-2 py-0.5 text-xs' : 'px-2.5 py-1 text-xs'
          }`}
        >
          <Clock className={isSm ? 'w-3 h-3' : 'w-3.5 h-3.5'} />
          QUEUED
        </span>
      );
    case 'RUNNING':
      return (
        <span
          className={`inline-flex items-center gap-1.5 font-medium rounded-full bg-blue-500/10 text-blue-400 border border-blue-500/20 ${
            isSm ? 'px-2 py-0.5 text-xs' : 'px-2.5 py-1 text-xs'
          }`}
        >
          <span className="relative flex h-2 w-2">
            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-blue-400 opacity-75"></span>
            <span className="relative inline-flex rounded-full h-2 w-2 bg-blue-500"></span>
          </span>
          RUNNING
        </span>
      );
    case 'SUCCEEDED':
    case 'SUCCESS':
      return (
        <span
          className={`inline-flex items-center gap-1.5 font-medium rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 ${
            isSm ? 'px-2 py-0.5 text-xs' : 'px-2.5 py-1 text-xs'
          }`}
        >
          <CheckCircle2 className={isSm ? 'w-3 h-3' : 'w-3.5 h-3.5'} />
          SUCCEEDED
        </span>
      );
    case 'RETRY_WAIT':
    case 'RETRYABLE_FAILURE':
      return (
        <span
          className={`inline-flex items-center gap-1.5 font-medium rounded-full bg-purple-500/10 text-purple-400 border border-purple-500/20 ${
            isSm ? 'px-2 py-0.5 text-xs' : 'px-2.5 py-1 text-xs'
          }`}
        >
          <RotateCcw className={isSm ? 'w-3 h-3' : 'w-3.5 h-3.5'} />
          RETRY_WAIT
        </span>
      );
    case 'FAILED':
    case 'FAILURE':
      return (
        <span
          className={`inline-flex items-center gap-1.5 font-medium rounded-full bg-rose-500/10 text-rose-400 border border-rose-500/20 ${
            isSm ? 'px-2 py-0.5 text-xs' : 'px-2.5 py-1 text-xs'
          }`}
        >
          <XCircle className={isSm ? 'w-3 h-3' : 'w-3.5 h-3.5'} />
          FAILED
        </span>
      );
    case 'DEAD_LETTER':
      return (
        <span
          className={`inline-flex items-center gap-1.5 font-medium rounded-full bg-red-600/15 text-red-400 border border-red-500/30 ${
            isSm ? 'px-2 py-0.5 text-xs' : 'px-2.5 py-1 text-xs'
          }`}
        >
          <Skull className={isSm ? 'w-3 h-3' : 'w-3.5 h-3.5'} />
          DEAD_LETTER
        </span>
      );
    case 'CANCELLED':
      return (
        <span
          className={`inline-flex items-center gap-1.5 font-medium rounded-full bg-slate-800 text-slate-400 border border-slate-700 ${
            isSm ? 'px-2 py-0.5 text-xs' : 'px-2.5 py-1 text-xs'
          }`}
        >
          <Ban className={isSm ? 'w-3 h-3' : 'w-3.5 h-3.5'} />
          CANCELLED
        </span>
      );
    default:
      return (
        <span className="px-2 py-0.5 rounded text-xs bg-slate-800 text-slate-300">
          {s}
        </span>
      );
  }
};
