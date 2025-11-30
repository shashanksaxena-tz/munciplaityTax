package com.munitax.submission.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.munitax.submission.model.Submission;
import com.munitax.submission.repository.SubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class SubmissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SubmissionRepository submissionRepository;

    private Submission testSubmission;

    @BeforeEach
    void setUp() {
        testSubmission = new Submission();
        testSubmission.setId("test-id-123");
        testSubmission.setTenantId("tenant-1");
        testSubmission.setUserId("user-1");
        testSubmission.setReturnType("W1");
        testSubmission.setTaxYear(2024);
        testSubmission.setStatus("SUBMITTED");
        testSubmission.setSubmittedAt(Instant.now());
    }

    @Test
    void testSubmitReturn() throws Exception {
        when(submissionRepository.save(any(Submission.class))).thenReturn(testSubmission);

        Submission newSubmission = new Submission();
        newSubmission.setTenantId("tenant-1");
        newSubmission.setUserId("user-1");
        newSubmission.setReturnType("W1");
        newSubmission.setTaxYear(2024);

        mockMvc.perform(post("/api/v1/submissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newSubmission)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    void testGetSubmissions_All() throws Exception {
        List<Submission> submissions = Arrays.asList(testSubmission);
        when(submissionRepository.findAll()).thenReturn(submissions);

        mockMvc.perform(get("/api/v1/submissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("test-id-123"))
                .andExpect(jsonPath("$[0].tenantId").value("tenant-1"));
    }

    @Test
    void testGetSubmissions_ByTenant() throws Exception {
        List<Submission> submissions = Arrays.asList(testSubmission);
        when(submissionRepository.findByTenantId("tenant-1")).thenReturn(submissions);

        mockMvc.perform(get("/api/v1/submissions")
                .param("tenantId", "tenant-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tenantId").value("tenant-1"));
    }

    @Test
    void testApproveSubmission() throws Exception {
        testSubmission.setStatus("APPROVED");
        testSubmission.setReviewedBy("auditor-1");
        testSubmission.setReviewedAt(Instant.now());
        
        when(submissionRepository.findById("test-id-123")).thenReturn(Optional.of(testSubmission));
        when(submissionRepository.save(any(Submission.class))).thenReturn(testSubmission);

        mockMvc.perform(post("/api/v1/submissions/test-id-123/approve")
                .param("auditorId", "auditor-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.reviewedBy").value("auditor-1"));
    }

    @Test
    void testRejectSubmission() throws Exception {
        testSubmission.setStatus("REJECTED");
        testSubmission.setReviewedBy("auditor-1");
        testSubmission.setReviewedAt(Instant.now());
        testSubmission.setAuditorComments("Missing documentation");
        
        when(submissionRepository.findById("test-id-123")).thenReturn(Optional.of(testSubmission));
        when(submissionRepository.save(any(Submission.class))).thenReturn(testSubmission);

        mockMvc.perform(post("/api/v1/submissions/test-id-123/reject")
                .param("auditorId", "auditor-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("\"Missing documentation\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.auditorComments").value("Missing documentation"));
    }

    @Test
    void testApproveSubmission_NotFound() throws Exception {
        when(submissionRepository.findById("nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/submissions/nonexistent/approve")
                .param("auditorId", "auditor-1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testRejectSubmission_NotFound() throws Exception {
        when(submissionRepository.findById("nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/submissions/nonexistent/reject")
                .param("auditorId", "auditor-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("\"Comments\""))
                .andExpect(status().isNotFound());
    }
}
