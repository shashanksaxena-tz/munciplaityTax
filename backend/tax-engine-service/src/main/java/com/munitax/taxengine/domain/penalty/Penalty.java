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
 * JPA Entity for penalty assessments on tax returns.
 * 
 * Functional Requirements:
 * - FR-001 to FR-006: Late filing penalty (5% per month, max 25%)
 * - FR-007 to FR-011: Late payment penalty (1% per month, max 25%)
 * - FR-012 to FR-014: Combined penalty cap (max 5% per month when both apply)
 * 
 * Multi-tenant Isolation: Constitution II
 * - All queries MUST filter by tenant_id
 * - Use @Filter annotation for automatic tenant scoping
 * 
 * Audit Trail: Constitution III
 * - created_at, created_by immutable
 * - All changes logged to PenaltyAuditLog
 */
@Entity
@Table(name = "penalties")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Penalty {
    
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
     * Type of penalty: LATE_FILING, LATE_PAYMENT, ESTIMATED_UNDERPAYMENT, OTHER.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "penalty_type", nullable = false, length = 50)
    private PenaltyType penaltyType;
    
    /**
     * When penalty was calculated and assessed.
     */
    @Column(name = "assessment_date", nullable = false)
    private LocalDate assessmentDate;
    
    /**
     * Original due date for this return.
     */
    @Column(name = "tax_due_date", nullable = false)
    private LocalDate taxDueDate;
    
    /**
     * Actual filing or payment date.
     */
    @Column(name = "actual_date", nullable = false)
    private LocalDate actualDate;
    
    /**
     * Calculated months late (rounded up to next full month).
     * FR-005: Partial months rounded up per research R5.
     * Maximum: 120 months (10 years per business rule).
     */
    @Column(name = "months_late", nullable = false)
    private Integer monthsLate;
    
    /**
     * Tax balance subject to penalty at time of assessment.
     */
    @Column(name = "unpaid_tax_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal unpaidTaxAmount;
    
    /**
     * Rate per month:
     * - 0.05 (5%) for late filing
     * - 0.01 (1%) for late payment
     */
    @Column(name = "penalty_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal penaltyRate;
    
    /**
     * Calculated penalty amount.
     * Formula: unpaid_tax_amount × penalty_rate × months_late
     * Capped at maximum_penalty (25% of unpaid tax).
     */
    @Column(name = "penalty_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal penaltyAmount;
    
    /**
     * Maximum penalty cap (25% of unpaid tax).
     * - Late filing: 5 months max (5% × 5 = 25%)
     * - Late payment: 25 months max (1% × 25 = 25%)
     */
    @Column(name = "maximum_penalty", nullable = false, precision = 15, scale = 2)
    private BigDecimal maximumPenalty;
    
    /**
     * Whether penalty was abated for reasonable cause.
     */
    @Column(name = "is_abated", nullable = false)
    @Builder.Default
    private Boolean isAbated = false;
    
    /**
     * Reason for abatement (if abated).
     */
    @Column(name = "abatement_reason", columnDefinition = "TEXT")
    private String abatementReason;
    
    /**
     * When abatement was approved (if abated).
     */
    @Column(name = "abatement_date")
    private LocalDate abatementDate;
    
    /**
     * Audit trail: When penalty was created (immutable).
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    /**
     * Audit trail: Who created the penalty (user ID or SYSTEM).
     */
    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;
    
    /**
     * Audit trail: Last modification timestamp.
     */
    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    /**
     * Calculate effective penalty amount (after abatement).
     * 
     * @return penalty_amount if not abated, 0 if abated
     */
    public BigDecimal getEffectivePenaltyAmount() {
        return Boolean.TRUE.equals(isAbated) ? BigDecimal.ZERO : penaltyAmount;
    }
    
    /**
     * Check if penalty has reached maximum cap.
     * 
     * @return true if penalty_amount >= maximum_penalty
     */
    public boolean isAtMaximumCap() {
        return penaltyAmount.compareTo(maximumPenalty) >= 0;
    }
}
