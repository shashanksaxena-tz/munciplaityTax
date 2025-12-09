package com.munitax.submission.dto;

import com.munitax.submission.model.Submission;
import com.munitax.submission.model.SubmissionDocument;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubmissionResponse {
    private String id;
    private String tenantId;
    private String userId;
    private String taxpayerId;
    private Integer taxYear;
    private String status;
    private String auditorComments;
    private Instant submittedAt;
    private Instant reviewedAt;
    private String reviewedBy;
    private String returnType;
    private Double taxDue;
    private String taxpayerName;
    private String taxpayerFEIN;
    private String priority;
    private Integer riskScore;
    private Boolean hasDiscrepancies;
    private Integer discrepancyCount;
    private String rejectionReason;
    private String digitalSignature;
    private Double grossReceipts;
    private Double netProfit;
    private Instant filedDate;
    private LocalDate dueDate;
    private List<DocumentInfo> documents;
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DocumentInfo {
        private String id;
        private String documentId;
        private String fileName;
        private String formType;
        private Long fileSize;
        private String mimeType;
        private Instant uploadDate;
        private Double extractionConfidence;
    }
    
    /**
     * Convert from entity to DTO
     */
    public static SubmissionResponse fromEntity(Submission submission, List<SubmissionDocument> documents) {
        SubmissionResponse response = new SubmissionResponse();
        response.setId(submission.getId());
        response.setTenantId(submission.getTenantId());
        response.setUserId(submission.getUserId());
        response.setTaxpayerId(submission.getTaxpayerId());
        response.setTaxYear(submission.getTaxYear());
        response.setStatus(submission.getStatus());
        response.setAuditorComments(submission.getAuditorComments());
        response.setSubmittedAt(submission.getSubmittedAt());
        response.setReviewedAt(submission.getReviewedAt());
        response.setReviewedBy(submission.getReviewedBy());
        response.setReturnType(submission.getReturnType());
        response.setTaxDue(submission.getTaxDue());
        response.setTaxpayerName(submission.getTaxpayerName());
        response.setTaxpayerFEIN(submission.getTaxpayerFEIN());
        response.setPriority(submission.getPriority());
        response.setRiskScore(submission.getRiskScore());
        response.setHasDiscrepancies(submission.getHasDiscrepancies());
        response.setDiscrepancyCount(submission.getDiscrepancyCount());
        response.setRejectionReason(submission.getRejectionReason());
        response.setDigitalSignature(submission.getDigitalSignature());
        response.setGrossReceipts(submission.getGrossReceipts());
        response.setNetProfit(submission.getNetProfit());
        response.setFiledDate(submission.getFiledDate());
        response.setDueDate(submission.getDueDate());
        
        if (documents != null) {
            response.setDocuments(documents.stream()
                .map(doc -> new DocumentInfo(
                    doc.getId(),
                    doc.getDocumentId(),
                    doc.getFileName(),
                    doc.getFormType(),
                    doc.getFileSize(),
                    doc.getMimeType(),
                    doc.getUploadDate(),
                    doc.getExtractionConfidence()
                ))
                .collect(Collectors.toList()));
        }
        
        return response;
    }
}
