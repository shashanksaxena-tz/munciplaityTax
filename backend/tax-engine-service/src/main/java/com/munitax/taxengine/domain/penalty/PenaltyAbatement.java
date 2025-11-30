package com.munitax.taxengine.domain.penalty;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity for penalty abatement requests and approvals.
 * 
 * Functional Requirements:
 * - FR-033 to FR-039: Penalty abatement request, review, and approval workflow
 * - FR-036: Form 27-PA generation via pdf-service
 * 
 * Multi-tenant Isolation: Constitution II
 * - All queries MUST filter by tenant_id
 * 
 * Audit Trail: Constitution III
 * - created_at immutable
 * - All changes logged to PenaltyAuditLog
 */
@Entity
@Table(name = "penalty_abatements")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PenaltyAbatement {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /**
     * Tenant ID for multi-tenant data isolation (Constitution II).
     */
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    /**
     * Associated tax return.
     * Foreign key to tax_returns table.
     */
    @Column(name = "return_id", nullable = false)
    private UUID returnId;
    
    /**
     * Specific penalty to abate (NULL if requesting abatement for all penalties).
     * Foreign key to penalties table.
     */
    @Column(name = "penalty_id")
    private UUID penaltyId;
    
    /**
     * Date when abatement was requested.
     */
    @Column(name = "request_date", nullable = false)
    private LocalDate requestDate;
    
    /**
     * Type of abatement: LATE_FILING, LATE_PAYMENT, ESTIMATED, ALL.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "abatement_type", nullable = false, length = 50)
    private AbatementType abatementType;
    
    /**
     * Amount of penalty requesting to abate.
     */
    @Column(name = "requested_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal requestedAmount;
    
    /**
     * Reason for requesting abatement.
     * FR-034: Valid reasons include DEATH, ILLNESS, DISASTER, etc.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 50)
    private AbatementReason reason;
    
    /**
     * Detailed explanation of the reasonable cause.
     */
    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;
    
    /**
     * Supporting documents metadata (stored as JSONB).
     * Contains document IDs, types, upload dates, etc.
     */
    @Column(name = "supporting_documents", columnDefinition = "JSONB")
    private String supportingDocuments;
    
    /**
     * Status of abatement request.
     * FR-037: Workflow states: PENDING → APPROVED/PARTIAL/DENIED/WITHDRAWN
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AbatementStatus status = AbatementStatus.PENDING;
    
    /**
     * User ID of reviewer who made decision.
     */
    @Column(name = "reviewed_by")
    private UUID reviewedBy;
    
    /**
     * Date when review decision was made.
     */
    @Column(name = "review_date")
    private LocalDate reviewDate;
    
    /**
     * Amount of penalty actually approved for abatement.
     * May be less than requested_amount (partial approval).
     */
    @Column(name = "approved_amount", precision = 15, scale = 2)
    private BigDecimal approvedAmount;
    
    /**
     * Reason for denial (if status = DENIED).
     */
    @Column(name = "denial_reason", columnDefinition = "TEXT")
    private String denialReason;
    
    /**
     * Notes from reviewer about the decision.
     */
    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;
    
    /**
     * User ID of the person who requested the abatement.
     */
    @Column(name = "requested_by")
    private UUID requestedBy;
    
    /**
     * User ID of the person who created the abatement record.
     */
    @Column(name = "created_by")
    private UUID createdBy;
    
    /**
     * Whether this is a first-time abatement request.
     */
    @Column(name = "is_first_time_abatement")
    @Builder.Default
    private Boolean isFirstTimeAbatement = false;
    
    /**
     * Path to generated Form 27-PA PDF.
     * FR-036: Generated via pdf-service integration.
     */
    @Column(name = "form_generated", length = 500)
    private String formGenerated;
    
    /**
     * Audit trail: When abatement request was created (immutable).
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    /**
     * Audit trail: Last modification timestamp.
     */
    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    /**
     * Check if abatement is pending review.
     * 
     * @return true if status is PENDING
     */
    public boolean isPending() {
        return AbatementStatus.PENDING.equals(status);
    }
    
    /**
     * Check if abatement was approved (fully or partially).
     * 
     * @return true if status is APPROVED or PARTIAL
     */
    public boolean isApproved() {
        return AbatementStatus.APPROVED.equals(status) || AbatementStatus.PARTIAL.equals(status);
    }
    
    /**
     * Check if abatement was denied.
     * 
     * @return true if status is DENIED
     */
    public boolean isDenied() {
        return AbatementStatus.DENIED.equals(status);
    }
    
    /**
     * Get effective approved amount (0 if not approved).
     * 
     * @return approved_amount if approved, otherwise 0
     */
    public BigDecimal getEffectiveApprovedAmount() {
        return isApproved() && approvedAmount != null ? approvedAmount : BigDecimal.ZERO;
    }
    
    /**
     * Calculate approval percentage.
     * 
     * @return (approved / requested) × 100, or 0 if not approved
     */
    public BigDecimal getApprovalPercentage() {
        if (!isApproved() || approvedAmount == null || requestedAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return approvedAmount.divide(requestedAmount, 4, java.math.RoundingMode.HALF_UP)
                             .multiply(new BigDecimal("100"));
    }
}
