package com.munitax.submission.controller;

import com.munitax.submission.dto.SubmissionRequest;
import com.munitax.submission.dto.SubmissionResponse;
import com.munitax.submission.model.Submission;
import com.munitax.submission.model.SubmissionDocument;
import com.munitax.submission.repository.SubmissionRepository;
import com.munitax.submission.service.SubmissionDocumentService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/submissions")
public class SubmissionController {

    private final SubmissionRepository repository;
    private final SubmissionDocumentService documentService;

    public SubmissionController(SubmissionRepository repository, SubmissionDocumentService documentService) {
        this.repository = repository;
        this.documentService = documentService;
    }

    @PostMapping
    public SubmissionResponse submitReturn(@RequestBody SubmissionRequest request) {
        // Create submission entity
        Submission submission = new Submission();
        submission.setId(UUID.randomUUID().toString());
        submission.setTenantId(request.getTenantId());
        submission.setUserId(request.getUserId());
        submission.setTaxpayerId(request.getTaxpayerId());
        submission.setTaxYear(request.getTaxYear());
        submission.setReturnType(request.getReturnType());
        submission.setTaxDue(request.getTaxDue());
        submission.setTaxpayerName(request.getTaxpayerName());
        submission.setTaxpayerFEIN(request.getTaxpayerFEIN());
        submission.setGrossReceipts(request.getGrossReceipts());
        submission.setNetProfit(request.getNetProfit());
        submission.setDueDate(request.getDueDate());
        submission.setStatus("SUBMITTED");
        submission.setSubmittedAt(Instant.now());
        
        // Save submission
        submission = repository.save(submission);
        
        // Save documents if provided
        List<SubmissionDocument> savedDocuments = new ArrayList<>();
        if (request.getDocuments() != null && !request.getDocuments().isEmpty()) {
            String submissionId = submission.getId();
            List<SubmissionDocument> documents = request.getDocuments().stream()
                .map(doc -> {
                    SubmissionDocument submissionDoc = new SubmissionDocument();
                    submissionDoc.setSubmissionId(submissionId);
                    submissionDoc.setDocumentId(doc.getDocumentId());
                    submissionDoc.setFileName(doc.getFileName());
                    submissionDoc.setFormType(doc.getFormType());
                    submissionDoc.setFileSize(doc.getFileSize());
                    submissionDoc.setMimeType(doc.getMimeType());
                    submissionDoc.setUploadDate(Instant.now());
                    submissionDoc.setExtractionResult(doc.getExtractionResult());
                    submissionDoc.setExtractionConfidence(doc.getExtractionConfidence());
                    submissionDoc.setTenantId(request.getTenantId());
                    return submissionDoc;
                })
                .collect(Collectors.toList());
            
            savedDocuments = documentService.saveDocuments(documents);
        }
        
        return SubmissionResponse.fromEntity(submission, savedDocuments);
    }

    @GetMapping
    public List<Submission> getSubmissions(@RequestParam(required = false) String tenantId) {
        if (tenantId != null) {
            return repository.findByTenantId(tenantId);
        }
        return repository.findAll();
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Submission> approveSubmission(@PathVariable String id, @RequestParam String auditorId) {
        return repository.findById(id)
                .map(submission -> {
                    submission.setStatus("APPROVED");
                    submission.setReviewedAt(Instant.now());
                    submission.setReviewedBy(auditorId);
                    return ResponseEntity.ok(repository.save(submission));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Submission> rejectSubmission(@PathVariable String id, @RequestParam String auditorId,
            @RequestBody String comments) {
        return repository.findById(id)
                .map(submission -> {
                    submission.setStatus("REJECTED");
                    submission.setAuditorComments(comments);
                    submission.setReviewedAt(Instant.now());
                    submission.setReviewedBy(auditorId);
                    return ResponseEntity.ok(repository.save(submission));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all documents for a submission
     */
    @GetMapping("/{id}/documents")
    public ResponseEntity<List<SubmissionDocument>> getSubmissionDocuments(
            @PathVariable String id,
            @RequestParam(required = false) String tenantId) {
        
        // Verify submission exists
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        List<SubmissionDocument> documents;
        if (tenantId != null) {
            documents = documentService.getDocumentsBySubmissionIdAndTenant(id, tenantId);
        } else {
            documents = documentService.getDocumentsBySubmissionId(id);
        }
        
        return ResponseEntity.ok(documents);
    }

    /**
     * Get a specific document by ID
     * In a real implementation, this would fetch the actual file from storage
     * and return it as a downloadable resource
     */
    @GetMapping("/{id}/documents/{docId}")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable String id,
            @PathVariable String docId) {
        
        // Verify submission exists
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        // Get document metadata
        return documentService.getDocumentById(docId)
            .filter(doc -> doc.getSubmissionId().equals(id))
            .map(doc -> {
                // In a real implementation, fetch the actual file from storage service
                // For now, return metadata as JSON for demonstration
                byte[] content = String.format(
                    "{\"message\":\"Document download placeholder\",\"documentId\":\"%s\",\"fileName\":\"%s\",\"formType\":\"%s\"}",
                    doc.getDocumentId(),
                    doc.getFileName(),
                    doc.getFormType()
                ).getBytes();
                
                ByteArrayResource resource = new ByteArrayResource(content);
                
                return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getFileName() + "\"")
                    .contentType(MediaType.parseMediaType(doc.getMimeType() != null ? doc.getMimeType() : "application/octet-stream"))
                    .contentLength(content.length)
                    .body((Resource) resource);
            })
            .orElse(ResponseEntity.notFound().build());
    }
}
