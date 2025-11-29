package com.munitax.taxengine.domain.penalty;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity for immutable audit trail of penalty and interest actions.
 * 
 * Functional Requirements:
 * - Constitution III: Audit Trail Immutability
 * - FR-045: Audit log for all penalty/interest actions
 * 
 * Multi-tenant Isolation: Constitution II
 * - All queries MUST filter by tenant_id
 * 
 * IMPORTANT: This table is APPEND-ONLY. No UPDATE or DELETE operations allowed.
 */
@Entity
@Table(name = "penalty_audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PenaltyAuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /**
     * Tenant ID for multi-tenant data isolation (Constitution II).
     */
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    /**
     * Type of entity being audited.
     * FR-045: PENALTY, INTEREST, ESTIMATED_TAX, ABATEMENT, PAYMENT_ALLOCATION
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 50)
    private PenaltyAuditEntityType entityType;
    
    /**
     * ID of the entity being audited.
     */
    @Column(name = "entity_id", nullable = false)
    private UUID entityId;
    
    /**
     * Action performed on the entity.
     * FR-045: ASSESSED, CALCULATED, ABATED, PAYMENT_APPLIED, RECALCULATED
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 50)
    private PenaltyAuditAction action;
    
    /**
     * ID of user/system that performed the action.
     */
    @Column(name = "actor_id", nullable = false)
    private UUID actorId;
    
    /**
     * Role of the actor: TAXPAYER, AUDITOR (municipality staff), SYSTEM (automated).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "actor_role", nullable = false, length = 20)
    private ActorRole actorRole;
    
    /**
     * Human-readable description of the action.
     * Example: "Late filing penalty assessed: $125.00 for 3 months late"
     */
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;
    
    /**
     * Previous state of the entity as JSON (if update operation).
     * NULL for create operations.
     */
    @Column(name = "old_value", columnDefinition = "JSONB")
    private String oldValue;
    
    /**
     * New state of the entity as JSON.
     */
    @Column(name = "new_value", columnDefinition = "JSONB")
    private String newValue;
    
    /**
     * IP address of the request (if applicable).
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    /**
     * User agent of the request (if applicable).
     */
    @Column(name = "user_agent", length = 255)
    private String userAgent;
    
    /**
     * Immutable timestamp when this audit entry was created.
     * Constitution III: NEVER UPDATE OR DELETE.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    /**
     * Check if this is a create operation (no old value).
     * 
     * @return true if oldValue is null
     */
    public boolean isCreateOperation() {
        return oldValue == null || oldValue.isEmpty();
    }
    
    /**
     * Check if this is an update operation (has both old and new values).
     * 
     * @return true if both oldValue and newValue are present
     */
    public boolean isUpdateOperation() {
        return oldValue != null && !oldValue.isEmpty() 
            && newValue != null && !newValue.isEmpty();
    }
    
    /**
     * Check if action was performed by system (automated).
     * 
     * @return true if actorRole is SYSTEM
     */
    public boolean isSystemAction() {
        return ActorRole.SYSTEM.equals(actorRole);
    }
    
    /**
     * Check if action was performed by municipality auditor.
     * 
     * @return true if actorRole is AUDITOR
     */
    public boolean isAuditorAction() {
        return ActorRole.AUDITOR.equals(actorRole);
    }
}
