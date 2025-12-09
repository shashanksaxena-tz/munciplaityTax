package com.munitax.submission.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "submission_documents")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubmissionDocument {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false, name = "submission_id")
    private String submissionId;
    
    @Column(nullable = false, name = "document_id")
    private String documentId; // Reference to document in storage/extraction service
    
    @Column(nullable = false, name = "file_name")
    private String fileName;
    
    @Column(name = "form_type")
    private String formType; // W-2, 1099-NEC, 1099-MISC, etc.
    
    @Column(name = "file_size")
    private Long fileSize; // Size in bytes
    
    @Column(name = "mime_type")
    private String mimeType; // application/pdf, image/jpeg, etc.
    
    @Column(name = "upload_date")
    private Instant uploadDate;
    
    @Column(columnDefinition = "TEXT", name = "extraction_result")
    private String extractionResult; // JSON string of extraction results
    
    @Column(name = "extraction_confidence")
    private Double extractionConfidence;
    
    @Column(name = "tenant_id")
    private String tenantId;
}
