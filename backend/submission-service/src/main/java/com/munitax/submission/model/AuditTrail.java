package com.munitax.submission.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "audit_trail")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuditTrail {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String trailId;
    
    @Column(nullable = false)
    private String returnId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType eventType;
    
    @Column(nullable = false)
    private String userId; // Who performed the action
    
    @Column(nullable = false)
    private Instant timestamp = Instant.now();
    
    private String ipAddress;
    
    @Column(columnDefinition = "TEXT")
    private String eventDetails; // JSON format for full context
    
    private String digitalSignature; // E-signature hash for approvals
    
    @Column(nullable = false)
    private Boolean immutable = true; // Always true, cannot edit
    
    private String tenantId;
    
    public enum EventType {
        SUBMISSION,
        ASSIGNMENT,
        REVIEW_STARTED,
        REVIEW_COMPLETED,
        APPROVAL,
        REJECTION,
        AMENDMENT,
        PAYMENT,
        COMMUNICATION,
        ESCALATION,
        DOCUMENT_REQUEST,
        DOCUMENT_RECEIVED,
        PRIORITY_CHANGE,
        STATUS_CHANGE,
        ANNOTATION_ADDED
    }
    
    // Constructor for easy creation
    public AuditTrail(String returnId, String userId, EventType eventType, String eventDetails) {
        this.returnId = returnId;
        this.userId = userId;
        this.eventType = eventType;
        this.eventDetails = eventDetails;
        this.timestamp = Instant.now();
        this.immutable = true;
    }
    
    public AuditTrail(String returnId, String userId, EventType eventType, 
                     String eventDetails, String digitalSignature) {
        this(returnId, userId, eventType, eventDetails);
        this.digitalSignature = digitalSignature;
    }
}
