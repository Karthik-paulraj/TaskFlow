import React from 'react';

interface PriorityBadgeProps {
  priority: number;
}

export const PriorityBadge: React.FC<PriorityBadgeProps> = ({ priority }) => {
  let color = 'bg-slate-800 text-slate-300 border-slate-700';

  if (priority >= 9) {
    color = 'bg-red-500/10 text-red-400 border-red-500/20';
  } else if (priority >= 7) {
    color = 'bg-amber-500/10 text-amber-400 border-amber-500/20';
  } else if (priority >= 5) {
    color = 'bg-blue-500/10 text-blue-400 border-blue-500/20';
  } else {
    color = 'bg-slate-800 text-slate-400 border-slate-700';
  }

  return (
    <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold border ${color}`}>
      P{priority}
    </span>
  );
};
