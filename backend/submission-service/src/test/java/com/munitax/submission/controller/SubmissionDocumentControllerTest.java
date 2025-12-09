package com.munitax.submission.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.munitax.submission.dto.SubmissionRequest;
import com.munitax.submission.model.Submission;
import com.munitax.submission.model.SubmissionDocument;
import com.munitax.submission.repository.SubmissionRepository;
import com.munitax.submission.service.SubmissionDocumentService;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class SubmissionDocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SubmissionRepository submissionRepository;

    @MockBean
    private SubmissionDocumentService documentService;

    private Submission testSubmission;
    private SubmissionDocument testDocument;

    @BeforeEach
    void setUp() {
        testSubmission = new Submission();
        testSubmission.setId("sub-123");
        testSubmission.setTenantId("tenant-1");
        testSubmission.setUserId("user-1");
        testSubmission.setReturnType("W1");
        testSubmission.setTaxYear(2024);
        testSubmission.setStatus("SUBMITTED");
        testSubmission.setSubmittedAt(Instant.now());

        testDocument = new SubmissionDocument();
        testDocument.setId("doc-123");
        testDocument.setSubmissionId("sub-123");
        testDocument.setDocumentId("storage-doc-123");
        testDocument.setFileName("W2_2024.pdf");
        testDocument.setFormType("W-2");
        testDocument.setFileSize(102400L);
        testDocument.setMimeType("application/pdf");
        testDocument.setUploadDate(Instant.now());
        testDocument.setExtractionConfidence(0.95);
        testDocument.setPageCount(1);
        testDocument.setFieldProvenance("{\"fields\":[{\"fieldName\":\"wages\",\"pageNumber\":1,\"boundingBox\":{\"x\":0.1,\"y\":0.2,\"width\":0.3,\"height\":0.05},\"confidence\":0.95}]}");
        testDocument.setTenantId("tenant-1");
    }

    @Test
    void testSubmitReturnWithDocuments() throws Exception {
        when(submissionRepository.save(any(Submission.class))).thenReturn(testSubmission);
        when(documentService.saveDocuments(anyList())).thenReturn(Arrays.asList(testDocument));

        SubmissionRequest request = new SubmissionRequest();
        request.setTenantId("tenant-1");
        request.setUserId("user-1");
        request.setTaxYear(2024);
        request.setReturnType("W1");

        SubmissionRequest.DocumentAttachment docAttachment = new SubmissionRequest.DocumentAttachment();
        docAttachment.setDocumentId("storage-doc-123");
        docAttachment.setFileName("W2_2024.pdf");
        docAttachment.setFormType("W-2");
        docAttachment.setFileSize(102400L);
        docAttachment.setMimeType("application/pdf");
        docAttachment.setExtractionConfidence(0.95);
        docAttachment.setPageCount(1);
        docAttachment.setFieldProvenance("{\"fields\":[{\"fieldName\":\"wages\",\"pageNumber\":1,\"boundingBox\":{\"x\":0.1,\"y\":0.2,\"width\":0.3,\"height\":0.05},\"confidence\":0.95}]}");

        request.setDocuments(Arrays.asList(docAttachment));

        mockMvc.perform(post("/api/v1/submissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.documents").isArray())
                .andExpect(jsonPath("$.documents[0].fileName").value("W2_2024.pdf"))
                .andExpect(jsonPath("$.documents[0].formType").value("W-2"));
    }

    @Test
    void testGetSubmissionDocuments() throws Exception {
        when(submissionRepository.existsById("sub-123")).thenReturn(true);
        when(documentService.getDocumentsBySubmissionId("sub-123"))
                .thenReturn(Arrays.asList(testDocument));

        mockMvc.perform(get("/api/v1/submissions/sub-123/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("doc-123"))
                .andExpect(jsonPath("$[0].fileName").value("W2_2024.pdf"))
                .andExpect(jsonPath("$[0].formType").value("W-2"))
                .andExpect(jsonPath("$[0].extractionConfidence").value(0.95));
    }

    @Test
    void testGetSubmissionDocuments_WithTenantFilter() throws Exception {
        when(submissionRepository.existsById("sub-123")).thenReturn(true);
        when(documentService.getDocumentsBySubmissionIdAndTenant("sub-123", "tenant-1"))
                .thenReturn(Arrays.asList(testDocument));

        mockMvc.perform(get("/api/v1/submissions/sub-123/documents")
                .param("tenantId", "tenant-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tenantId").value("tenant-1"));
    }

    @Test
    void testGetSubmissionDocuments_SubmissionNotFound() throws Exception {
        when(submissionRepository.existsById("nonexistent")).thenReturn(false);

        mockMvc.perform(get("/api/v1/submissions/nonexistent/documents"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDownloadDocument() throws Exception {
        when(submissionRepository.existsById("sub-123")).thenReturn(true);
        when(documentService.getDocumentById("doc-123"))
                .thenReturn(Optional.of(testDocument));

        mockMvc.perform(get("/api/v1/submissions/sub-123/documents/doc-123"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"W2_2024.pdf\""));
    }

    @Test
    void testDownloadDocument_NotFound() throws Exception {
        when(submissionRepository.existsById("sub-123")).thenReturn(true);
        when(documentService.getDocumentById("nonexistent"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/submissions/sub-123/documents/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDownloadDocument_SubmissionNotFound() throws Exception {
        when(submissionRepository.existsById("nonexistent")).thenReturn(false);

        mockMvc.perform(get("/api/v1/submissions/nonexistent/documents/doc-123"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDownloadDocument_WrongSubmission() throws Exception {
        SubmissionDocument otherDocument = new SubmissionDocument();
        otherDocument.setId("doc-456");
        otherDocument.setSubmissionId("other-sub-123");
        otherDocument.setDocumentId("storage-doc-456");
        otherDocument.setFileName("1099_2024.pdf");

        when(submissionRepository.existsById("sub-123")).thenReturn(true);
        when(documentService.getDocumentById("doc-456"))
                .thenReturn(Optional.of(otherDocument));

        mockMvc.perform(get("/api/v1/submissions/sub-123/documents/doc-456"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetDocumentProvenance() throws Exception {
        when(submissionRepository.existsById("sub-123")).thenReturn(true);
        when(documentService.getDocumentById("doc-123"))
                .thenReturn(Optional.of(testDocument));

        mockMvc.perform(get("/api/v1/submissions/sub-123/documents/doc-123/provenance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("doc-123"))
                .andExpect(jsonPath("$.fileName").value("W2_2024.pdf"))
                .andExpect(jsonPath("$.pageCount").value(1))
                .andExpect(jsonPath("$.extractionConfidence").value(0.95))
                .andExpect(jsonPath("$.fieldProvenance").exists())
                .andExpect(jsonPath("$.fieldProvenance").isNotEmpty())
                .andExpect(jsonPath("$.fieldProvenance").value(org.hamcrest.Matchers.containsString("\"fields\":")));
    }

    @Test
    void testGetDocumentProvenance_NotFound() throws Exception {
        when(submissionRepository.existsById("sub-123")).thenReturn(true);
        when(documentService.getDocumentById("nonexistent"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/submissions/sub-123/documents/nonexistent/provenance"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetDocumentProvenance_TenantIsolation() throws Exception {
        // Document from a different tenant
        SubmissionDocument otherTenantDoc = new SubmissionDocument();
        otherTenantDoc.setId("doc-456");
        otherTenantDoc.setSubmissionId("sub-123");
        otherTenantDoc.setDocumentId("storage-doc-456");
        otherTenantDoc.setFileName("W2_2024.pdf");
        otherTenantDoc.setTenantId("tenant-2");

        when(submissionRepository.existsById("sub-123")).thenReturn(true);
        when(documentService.getDocumentById("doc-456"))
                .thenReturn(Optional.of(otherTenantDoc));

        // Attempting to access with tenant-1 should fail
        mockMvc.perform(get("/api/v1/submissions/sub-123/documents/doc-456/provenance")
                .param("tenantId", "tenant-1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDownloadDocument_TenantIsolation() throws Exception {
        // Document from a different tenant
        SubmissionDocument otherTenantDoc = new SubmissionDocument();
        otherTenantDoc.setId("doc-789");
        otherTenantDoc.setSubmissionId("sub-123");
        otherTenantDoc.setDocumentId("storage-doc-789");
        otherTenantDoc.setFileName("1099_2024.pdf");
        otherTenantDoc.setTenantId("tenant-2");

        when(submissionRepository.existsById("sub-123")).thenReturn(true);
        when(documentService.getDocumentById("doc-789"))
                .thenReturn(Optional.of(otherTenantDoc));

        // Attempting to download with tenant-1 should fail
        mockMvc.perform(get("/api/v1/submissions/sub-123/documents/doc-789")
                .param("tenantId", "tenant-1"))
                .andExpect(status().isNotFound());
    }
}
