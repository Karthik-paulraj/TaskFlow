package com.taskflow.dto;

import java.util.List;

public class BulkCreateJobResponse {
    private List<JobResponse> submittedJobs;
    private List<BulkJobFailure> failures;
    private int totalSubmitted;
    private int totalFailed;

    public BulkCreateJobResponse() {
    }

    public BulkCreateJobResponse(List<JobResponse> submittedJobs, List<BulkJobFailure> failures) {
        this.submittedJobs = submittedJobs;
        this.failures = failures;
        this.totalSubmitted = submittedJobs != null ? submittedJobs.size() : 0;
        this.totalFailed = failures != null ? failures.size() : 0;
    }

    public static class BulkJobFailure {
        private int index;
        private String jobType;
        private String error;

        public BulkJobFailure() {
        }

        public BulkJobFailure(int index, String jobType, String error) {
            this.index = index;
            this.jobType = jobType;
            this.error = error;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public String getJobType() {
            return jobType;
        }

        public void setJobType(String jobType) {
            this.jobType = jobType;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }

    public List<JobResponse> getSubmittedJobs() {
        return submittedJobs;
    }

    public void setSubmittedJobs(List<JobResponse> submittedJobs) {
        this.submittedJobs = submittedJobs;
        this.totalSubmitted = submittedJobs != null ? submittedJobs.size() : 0;
    }

    public List<BulkJobFailure> getFailures() {
        return failures;
    }

    public void setFailures(List<BulkJobFailure> failures) {
        this.failures = failures;
        this.totalFailed = failures != null ? failures.size() : 0;
    }

    public int getTotalSubmitted() {
        return totalSubmitted;
    }

    public int getTotalFailed() {
        return totalFailed;
    }
}
