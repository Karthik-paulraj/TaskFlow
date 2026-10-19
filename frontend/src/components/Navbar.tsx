import React from 'react';
import {
  Layers,
  LayoutDashboard,
  ListTodo,
  PlusCircle,
  Cpu,
  RefreshCw,
  Activity,
} from 'lucide-react';

interface NavbarProps {
  activeTab: 'overview' | 'jobs' | 'submit' | 'workers';
  setActiveTab: (tab: 'overview' | 'jobs' | 'submit' | 'workers') => void;
  autoRefreshInterval: number;
  setAutoRefreshInterval: (interval: number) => void;
  onManualRefresh: () => void;
  isRefreshing: boolean;
  isHealthy: boolean;
}

export const Navbar: React.FC<NavbarProps> = ({
  activeTab,
  setActiveTab,
  autoRefreshInterval,
  setAutoRefreshInterval,
  onManualRefresh,
  isRefreshing,
  isHealthy,
}) => {
  return (
    <header className="sticky top-0 z-40 bg-slate-900/80 backdrop-blur-md border-b border-slate-800">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo & Platform Info */}
          <div className="flex items-center gap-3">
            <div className="flex items-center justify-center w-9 h-9 rounded-lg bg-gradient-to-br from-brand-500 to-emerald-700 shadow-lg shadow-brand-500/20 text-white font-bold">
              <Layers className="w-5 h-5 text-white" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="font-bold text-lg text-slate-100 tracking-tight">TaskFlow</span>
                <span className="px-1.5 py-0.5 text-[10px] font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 rounded">
                  v1.0
                </span>
              </div>
              <p className="text-[11px] text-slate-400 font-medium hidden sm:block">
                Distributed Job Engine
              </p>
            </div>
          </div>

          {/* Navigation Links */}
          <nav className="flex items-center gap-1 bg-slate-950/60 p-1 rounded-xl border border-slate-800/80">
            <button
              onClick={() => setActiveTab('overview')}
              className={`flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
                activeTab === 'overview'
                  ? 'bg-brand-600 text-white shadow-md shadow-brand-600/30'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
              }`}
            >
              <LayoutDashboard className="w-4 h-4" />
              <span>Overview</span>
            </button>

            <button
              onClick={() => setActiveTab('jobs')}
              className={`flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
                activeTab === 'jobs'
                  ? 'bg-brand-600 text-white shadow-md shadow-brand-600/30'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
              }`}
            >
              <ListTodo className="w-4 h-4" />
              <span>Jobs</span>
            </button>

            <button
              onClick={() => setActiveTab('submit')}
              className={`flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
                activeTab === 'submit'
                  ? 'bg-brand-600 text-white shadow-md shadow-brand-600/30'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
              }`}
            >
              <PlusCircle className="w-4 h-4" />
              <span>Submit Job</span>
            </button>

            <button
              onClick={() => setActiveTab('workers')}
              className={`flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
                activeTab === 'workers'
                  ? 'bg-brand-600 text-white shadow-md shadow-brand-600/30'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
              }`}
            >
              <Cpu className="w-4 h-4" />
              <span>Workers</span>
            </button>
          </nav>

          {/* Right Toolbar: Backend Health, Auto-Refresh, Manual Refresh */}
          <div className="flex items-center gap-3">
            {/* Health pill */}
            <div
              className={`hidden md:flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium border ${
                isHealthy
                  ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                  : 'bg-rose-500/10 text-rose-400 border-rose-500/20'
              }`}
              title={isHealthy ? 'Connected to backend server' : 'Cannot reach backend server'}
            >
              <Activity className="w-3.5 h-3.5" />
              <span>{isHealthy ? 'Engine Online' : 'Engine Offline'}</span>
            </div>

            {/* Auto refresh dropdown */}
            <div className="flex items-center gap-1.5 bg-slate-950/60 px-2 py-1 rounded-lg border border-slate-800 text-xs">
              <span className="text-slate-400 text-[11px] hidden lg:inline">Poll:</span>
              <select
                value={autoRefreshInterval}
                onChange={(e) => setAutoRefreshInterval(Number(e.target.value))}
                aria-label="Polling interval"
                className="bg-transparent text-slate-300 font-medium text-xs focus:outline-none cursor-pointer"
              >
                <option value={0} className="bg-slate-900 text-slate-300">Off</option>
                <option value={2000} className="bg-slate-900 text-slate-300">2s</option>
                <option value={5000} className="bg-slate-900 text-slate-300">5s</option>
                <option value={10000} className="bg-slate-900 text-slate-300">10s</option>
              </select>
            </div>

            {/* Manual refresh button */}
            <button
              onClick={onManualRefresh}
              disabled={isRefreshing}
              title="Refresh Data"
              className="p-2 rounded-lg bg-slate-800/80 hover:bg-slate-700 text-slate-300 hover:text-white border border-slate-700/60 transition-all disabled:opacity-50"
            >
              <RefreshCw className={`w-4 h-4 ${isRefreshing ? 'animate-spin text-brand-400' : ''}`} />
            </button>
          </div>
        </div>
      </div>
    </header>
  );
};
