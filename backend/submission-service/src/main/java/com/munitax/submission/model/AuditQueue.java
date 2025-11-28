package com.munitax.submission.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "audit_queue")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuditQueue {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String queueId;
    
    @Column(nullable = false)
    private String returnId; // Foreign key to Submission
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority = Priority.MEDIUM;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditStatus status = AuditStatus.PENDING;
    
    @Column(nullable = false)
    private Instant submissionDate;
    
    private String assignedAuditorId; // Foreign key to User (auditor)
    
    private Instant assignmentDate;
    
    private Instant reviewStartedDate;
    
    private Instant reviewCompletedDate;
    
    @Column(nullable = false)
    private Integer riskScore = 0; // 0-100
    
    @Column(nullable = false)
    private Integer flaggedIssuesCount = 0;
    
    private String tenantId;
    
    // Calculated field - computed in service layer
    @Transient
    private Long daysInQueue;
    
    public enum Priority {
        LOW, MEDIUM, HIGH
    }
    
    public enum AuditStatus {
        PENDING,
        IN_REVIEW,
        AWAITING_DOCUMENTATION,
        APPROVED,
        REJECTED,
        AMENDED
    }
    
    // Helper methods
    public void startReview(String auditorId) {
        this.assignedAuditorId = auditorId;
        this.status = AuditStatus.IN_REVIEW;
        this.reviewStartedDate = Instant.now();
        if (this.assignmentDate == null) {
            this.assignmentDate = Instant.now();
        }
    }
    
    public void completeReview() {
        this.reviewCompletedDate = Instant.now();
    }
    
    public Long calculateDaysInQueue() {
        if (submissionDate == null) return 0L;
        Instant now = reviewCompletedDate != null ? reviewCompletedDate : Instant.now();
        return (now.toEpochMilli() - submissionDate.toEpochMilli()) / (1000 * 60 * 60 * 24);
    }
}
