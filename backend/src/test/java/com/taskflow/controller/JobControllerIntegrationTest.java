package com.taskflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.dto.BulkCreateJobRequest;
import com.taskflow.dto.CreateJobRequest;
import com.taskflow.entity.Job;
import com.taskflow.enums.JobStatus;
import com.taskflow.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JobControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JobRepository jobRepository;

    @BeforeEach
    void clean() {
        jobRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/jobs submits job and returns 201 CREATED")
    void testCreateJobApi() throws Exception {
        CreateJobRequest request = new CreateJobRequest(
                "EMAIL_SIMULATION",
                objectMapper.createObjectNode()
                        .put("to", "user@taskflow.dev")
                        .put("subject", "Queue Activation")
                        .put("body", "Your job is now processing."),
                8,
                3,
                null,
                "api-test-key-1"
        );

        mockMvc.perform(post("/api/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.jobType").value("EMAIL_SIMULATION"))
                .andExpect(jsonPath("$.priority").value(8))
                .andExpect(jsonPath("$.status").value("QUEUED"));
    }

    @Test
    @DisplayName("POST /api/jobs/bulk submits multiple jobs with partial failure handling")
    void testBulkCreateJobApi() throws Exception {
        CreateJobRequest valid1 = new CreateJobRequest(
                "EMAIL_SIMULATION",
                objectMapper.createObjectNode().put("to", "first@test.com").put("subject", "Sub 1"),
                5, 3, null, null
        );
        CreateJobRequest invalid2 = new CreateJobRequest(
                "EMAIL_SIMULATION",
                objectMapper.createObjectNode().put("to", "invalid-email").put("subject", "Sub 2"),
                5, 3, null, null
        );

        BulkCreateJobRequest bulk = new BulkCreateJobRequest(List.of(valid1, invalid2));

        mockMvc.perform(post("/api/jobs/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bulk)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSubmitted").value(1))
                .andExpect(jsonPath("$.totalFailed").value(1))
                .andExpect(jsonPath("$.failures[0].index").value(1));
    }

    @Test
    @DisplayName("GET /api/jobs lists jobs with pagination and filter")
    void testListJobsApi() throws Exception {
        Job job = new Job(UUID.randomUUID(), "REPORT_GENERATION", "{\"reportType\":\"DAILY_SUMMARY\"}", 5, 3, null, null);
        jobRepository.save(job);

        mockMvc.perform(get("/api/jobs")
                        .param("status", "QUEUED")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/stats/overview returns job status distribution")
    void testStatsOverviewApi() throws Exception {
        Job job = new Job(UUID.randomUUID(), "DELAYED_TASK", "{\"durationMs\":50}", 5, 3, null, null);
        jobRepository.save(job);

        mockMvc.perform(get("/api/stats/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalJobs").value(1))
                .andExpect(jsonPath("$.queuedJobs").value(1));
    }

    @Test
    @DisplayName("POST /api/jobs/{id}/cancel cancels an eligible job")
    void testCancelJobApi() throws Exception {
        Job job = new Job(UUID.randomUUID(), "DELAYED_TASK", "{\"durationMs\":50}", 5, 3, null, null);
        Job saved = jobRepository.save(job);

        mockMvc.perform(post("/api/jobs/" + saved.getId() + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
