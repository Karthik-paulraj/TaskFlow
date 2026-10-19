import React, { useState, useEffect, useCallback } from 'react';
import { Navbar } from './components/Navbar';
import { OverviewPage } from './pages/OverviewPage';
import { JobsPage } from './pages/JobsPage';
import { SubmitJobPage } from './pages/SubmitJobPage';
import { WorkersPage } from './pages/WorkersPage';
import { JobDetailsModal } from './components/JobDetailsModal';
import { ToastProvider, useToast } from './components/Toast';
import { api } from './api/client';

export const AppContent: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'overview' | 'jobs' | 'submit' | 'workers'>('overview');
  const [jobsInitialStatusFilter, setJobsInitialStatusFilter] = useState('ALL');
  const [autoRefreshInterval, setAutoRefreshInterval] = useState(5000); // 5s default
  const [refreshTrigger, setRefreshTrigger] = useState(0);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [isHealthy, setIsHealthy] = useState(true);
  const [inspectJobId, setInspectJobId] = useState<string | null>(null);

  // Manual refresh trigger
  const handleManualRefresh = useCallback(() => {
    setIsRefreshing(true);
    setRefreshTrigger((prev) => prev + 1);
    api.getHealth()
      .then(() => setIsHealthy(true))
      .catch(() => setIsHealthy(false))
      .finally(() => {
        setTimeout(() => setIsRefreshing(false), 300);
      });
  }, []);

  // Periodic Polling
  useEffect(() => {
    if (autoRefreshInterval <= 0) return;

    const timer = setInterval(() => {
      setRefreshTrigger((prev) => prev + 1);
      api.getHealth()
        .then(() => setIsHealthy(true))
        .catch(() => setIsHealthy(false));
    }, autoRefreshInterval);

    return () => clearInterval(timer);
  }, [autoRefreshInterval]);

  const navigateToJobs = (statusFilter?: string) => {
    if (statusFilter) {
      setJobsInitialStatusFilter(statusFilter);
    }
    setActiveTab('jobs');
  };

  const handleJobCreated = () => {
    setRefreshTrigger((prev) => prev + 1);
    setActiveTab('jobs');
  };

  return (
    <div className="min-h-screen bg-slate-950 flex flex-col font-sans">
      <Navbar
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        autoRefreshInterval={autoRefreshInterval}
        setAutoRefreshInterval={setAutoRefreshInterval}
        onManualRefresh={handleManualRefresh}
        isRefreshing={isRefreshing}
        isHealthy={isHealthy}
      />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-6">
        {activeTab === 'overview' && (
          <OverviewPage
            onNavigateToSubmit={() => setActiveTab('submit')}
            onNavigateToJobs={navigateToJobs}
            refreshTrigger={refreshTrigger}
          />
        )}

        {activeTab === 'jobs' && (
          <JobsPage
            initialStatusFilter={jobsInitialStatusFilter}
            refreshTrigger={refreshTrigger}
          />
        )}

        {activeTab === 'submit' && (
          <SubmitJobPage onJobCreated={handleJobCreated} />
        )}

        {activeTab === 'workers' && (
          <WorkersPage
            refreshTrigger={refreshTrigger}
            onInspectJob={(id) => setInspectJobId(id)}
          />
        )}
      </main>

      <footer className="border-t border-slate-900 bg-slate-950/80 py-4 text-center text-xs text-slate-500">
        TaskFlow Distributed Background Job Queue Platform &bull; Java 21 LTS &bull; Spring Boot 3 &bull; PostgreSQL
      </footer>

      {inspectJobId && (
        <JobDetailsModal
          jobId={inspectJobId}
          onClose={() => setInspectJobId(null)}
          onJobUpdated={() => setRefreshTrigger((prev) => prev + 1)}
        />
      )}
    </div>
  );
};

export const App: React.FC = () => {
  return (
    <ToastProvider>
      <AppContent />
    </ToastProvider>
  );
};

export default App;
