package com.munitax.taxengine.domain.withholding;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * WithholdingAuditLog Entity - Immutable audit trail for all withholding actions.
 * 
 * Purpose:
 * Constitution Principle III (Audit Trail Immutability) - All W-1 filings, amendments,
 * reconciliation decisions must be logged immutably with 7-year retention (IRS IRC § 6001).
 * 
 * Entity Types:
 * - W1_FILING: W-1 filing actions
 * - RECONCILIATION: Year-end reconciliation actions
 * - CUMULATIVE_TOTALS: YTD cumulative updates
 * - PAYMENT: Payment received actions
 * 
 * Actions:
 * - FILED: New W-1 filed
 * - AMENDED: W-1 amended (creates new filing, logs amendment)
 * - RECONCILED: Reconciliation completed
 * - PAYMENT_RECEIVED: Payment applied to W-1
 * - CUMULATIVE_UPDATED: Cumulative totals recalculated
 * - DISCREPANCY_RESOLVED: Variance accepted with explanation
 * 
 * Actor Roles:
 * - BUSINESS: Business owner or accountant
 * - AUDITOR: Municipality auditor
 * - SYSTEM: Automated action (event-driven update)
 * 
 * @Immutable ensures Hibernate never issues UPDATE statements.
 * Records can only be inserted, never modified.
 */
@Entity
@Table(name = "withholding_audit_log", schema = "dublin")
@Immutable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WithholdingAuditLog {
    
    /**
     * Unique identifier for this audit log entry.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /**
     * Multi-tenant isolation.
     */
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;
    
    /**
     * Type of entity being audited.
     */
    @Column(name = "entity_type", nullable = false, length = 50, updatable = false)
    private String entityType; // W1_FILING, RECONCILIATION, CUMULATIVE_TOTALS, PAYMENT
    
    /**
     * ID of the affected entity.
     * Example: W1Filing.id if entity_type = W1_FILING.
     */
    @Column(name = "entity_id", nullable = false, updatable = false)
    private UUID entityId;
    
    /**
     * Action performed on entity.
     */
    @Column(name = "action", nullable = false, length = 50, updatable = false)
    private String action; // FILED, AMENDED, RECONCILED, PAYMENT_RECEIVED, CUMULATIVE_UPDATED, DISCREPANCY_RESOLVED
    
    /**
     * User who performed the action.
     */
    @Column(name = "actor_id", nullable = false, updatable = false)
    private UUID actorId;
    
    /**
     * Role of the actor.
     */
    @Column(name = "actor_role", nullable = false, length = 20, updatable = false)
    private String actorRole; // BUSINESS, AUDITOR, SYSTEM
    
    /**
     * Human-readable description of the action.
     * Example: "Business filed Q1 2024 W-1 with $125,000 wages"
     * Example: "System recalculated cumulative totals after amended March W-1"
     */
    @Column(name = "description", nullable = false, columnDefinition = "TEXT", updatable = false)
    private String description;
    
    /**
     * Previous state before action (JSON).
     * Null for CREATE actions.
     * Example: {"grossWages": 125000, "taxDue": 2812.50}
     */
    @Column(name = "old_value", columnDefinition = "jsonb", updatable = false)
    private String oldValue;
    
    /**
     * New state after action (JSON).
     * Null for DELETE actions.
     * Example: {"grossWages": 135000, "taxDue": 3037.50}
     */
    @Column(name = "new_value", columnDefinition = "jsonb", updatable = false)
    private String newValue;
    
    /**
     * Actor's IP address (IPv4 or IPv6).
     * Used for security audit trail.
     */
    @Column(name = "ip_address", length = 45, updatable = false)
    private String ipAddress;
    
    /**
     * Browser user agent string.
     * Used to identify client application.
     */
    @Column(name = "user_agent", updatable = false)
    private String userAgent;
    
    /**
     * Immutable timestamp when action occurred.
     * Never updated per Constitution Principle III.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}
