package com.munitax.ledger.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID logId;
    
    @Column(nullable = false)
    private UUID entityId;
    
    @Column(nullable = false)
    private String entityType; // JOURNAL_ENTRY, PAYMENT, etc.
    
    @Column(nullable = false)
    private String action; // CREATE, UPDATE, POST, REVERSE, VIEW
    
    @Column(nullable = false)
    private UUID userId;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    @Column(columnDefinition = "TEXT")
    private String details;
    
    private String oldValue;
    
    private String newValue;
    
    private String reason;
    
    @Column(nullable = false)
    private UUID tenantId;
}
