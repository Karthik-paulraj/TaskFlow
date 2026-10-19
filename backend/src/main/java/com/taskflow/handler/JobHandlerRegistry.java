package com.taskflow.handler;

import com.taskflow.exception.UnsupportedJobTypeException;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class JobHandlerRegistry {

    private final Map<String, JobHandler> handlers = new HashMap<>();

    public JobHandlerRegistry(List<JobHandler> handlerBeans) {
        for (JobHandler handler : handlerBeans) {
            handlers.put(handler.getJobType().toUpperCase(), handler);
        }
    }

    public JobHandler getHandler(String jobType) {
        if (jobType == null) {
            throw new UnsupportedJobTypeException("null");
        }
        JobHandler handler = handlers.get(jobType.toUpperCase());
        if (handler == null) {
            throw new UnsupportedJobTypeException(jobType);
        }
        return handler;
    }

    public boolean isSupported(String jobType) {
        return jobType != null && handlers.containsKey(jobType.toUpperCase());
    }

    public Set<String> getSupportedJobTypes() {
        return Collections.unmodifiableSet(handlers.keySet());
    }
}
