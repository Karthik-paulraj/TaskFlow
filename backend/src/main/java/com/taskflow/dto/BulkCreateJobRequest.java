package com.taskflow.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public class BulkCreateJobRequest {

    @NotEmpty(message = "Job list must not be empty")
    @Size(max = 50, message = "Bulk job creation is limited to 50 jobs per request")
    @Valid
    private List<CreateJobRequest> jobs;

    public BulkCreateJobRequest() {
    }

    public BulkCreateJobRequest(List<CreateJobRequest> jobs) {
        this.jobs = jobs;
    }

    public List<CreateJobRequest> getJobs() {
        return jobs;
    }

    public void setJobs(List<CreateJobRequest> jobs) {
        this.jobs = jobs;
    }
}
