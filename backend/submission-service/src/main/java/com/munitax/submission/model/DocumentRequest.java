package com.munitax.submission.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "document_requests")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String requestId;
    
    @Column(nullable = false)
    private String returnId;
    
    @Column(nullable = false)
    private String auditorId;
    
    @Column(nullable = false)
    private Instant requestDate = Instant.now();
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentType documentType;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;
    
    @Column(nullable = false)
    private LocalDate deadline;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.PENDING;
    
    private Instant receivedDate;
    
    @ElementCollection
    @CollectionTable(name = "document_request_files", joinColumns = @JoinColumn(name = "request_id"))
    @Column(name = "file_reference")
    private List<String> uploadedFiles = new ArrayList<>();
    
    private String tenantId;
    
    public enum DocumentType {
        GENERAL_LEDGER,
        BANK_STATEMENTS,
        DEPRECIATION_SCHEDULE,
        CONTRACTS,
        INVOICES,
        RECEIPTS,
        PAYROLL_RECORDS,
        TAX_RETURNS_PRIOR_YEAR,
        OTHER
    }
    
    public enum RequestStatus {
        PENDING,
        RECEIVED,
        OVERDUE,
        WAIVED
    }
    
    // Helper methods
    public boolean isOverdue() {
        return status == RequestStatus.PENDING && 
               LocalDate.now().isAfter(deadline);
    }
    
    public void markReceived() {
        this.status = RequestStatus.RECEIVED;
        this.receivedDate = Instant.now();
    }
    
    public void addUploadedFile(String fileReference) {
        if (this.uploadedFiles == null) {
            this.uploadedFiles = new ArrayList<>();
        }
        this.uploadedFiles.add(fileReference);
    }
}
