package com.munitax.submission.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubmissionRequest {
    private String tenantId;
    private String userId;
    private String taxpayerId;
    private Integer taxYear;
    private String returnType;
    private Double taxDue;
    private String taxpayerName;
    private String taxpayerFEIN;
    private Double grossReceipts;
    private Double netProfit;
    private LocalDate dueDate;
    
    // Document attachment support
    private List<DocumentAttachment> documents;
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DocumentAttachment {
        private String documentId;       // Reference to document in storage
        private String fileName;
        private String formType;         // W-2, 1099-NEC, etc.
        private Long fileSize;
        private String mimeType;
        private String extractionResult; // JSON string of extraction results
        private Double extractionConfidence;
    }
}
