package com.munitax.submission.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "audit_actions")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuditAction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String actionId;
    
    @Column(nullable = false)
    private String returnId;
    
    @Column(nullable = false)
    private String auditorId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionType actionType;
    
    @Column(nullable = false)
    private Instant actionDate = Instant.now();
    
    @Column(columnDefinition = "TEXT")
    private String actionDetails; // JSON format for context-specific data
    
    private String previousStatus;
    
    private String newStatus;
    
    private String ipAddress;
    
    private String userAgent;
    
    private String tenantId;
    
    public enum ActionType {
        ASSIGNED,
        REVIEW_STARTED,
        REVIEW_COMPLETED,
        APPROVED,
        REJECTED,
        DOCS_REQUESTED,
        ANNOTATED,
        ESCALATED,
        PRIORITY_CHANGED,
        REASSIGNED
    }
    
    // Constructor for easy creation
    public AuditAction(String returnId, String auditorId, ActionType actionType, 
                      String previousStatus, String newStatus, String actionDetails) {
        this.returnId = returnId;
        this.auditorId = auditorId;
        this.actionType = actionType;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.actionDetails = actionDetails;
        this.actionDate = Instant.now();
    }
}
