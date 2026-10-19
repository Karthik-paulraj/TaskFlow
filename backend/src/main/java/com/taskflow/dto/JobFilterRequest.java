package com.taskflow.dto;

import com.taskflow.enums.JobStatus;

public class JobFilterRequest {
    private JobStatus status;
    private String jobType;
    private Integer priority;
    private String search;
    private int page = 0;
    private int size = 20;
    private String sortBy = "createdAt";
    private String sortDirection = "desc";

    public JobFilterRequest() {
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public int getPage() {
        return Math.max(0, page);
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return Math.min(Math.max(1, size), 100);
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getSortBy() {
        return sortBy != null ? sortBy : "createdAt";
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortDirection() {
        return sortDirection != null ? sortDirection : "desc";
    }

    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }
}
