package com.munitax.taxengine.domain.penalty;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA Entity for estimated tax penalty assessments.
 * 
 * Functional Requirements:
 * - FR-015 to FR-019: Safe harbor evaluation (90% current year OR 100%/110% prior year)
 * - FR-020 to FR-026: Quarterly underpayment penalty calculation
 * 
 * Multi-tenant Isolation: Constitution II
 * - All queries MUST filter by tenant_id
 * 
 * Audit Trail: Constitution III
 * - created_at, created_by immutable
 * - All changes logged to PenaltyAuditLog
 */
@Entity
@Table(name = "estimated_tax_penalties")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstimatedTaxPenalty {
    
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
    @Column(name = "return_id", nullable = false, unique = true)
    private UUID returnId;
    
    /**
     * Tax year for this estimated tax penalty.
     */
    @Column(name = "tax_year", nullable = false)
    private Integer taxYear;
    
    /**
     * Annual tax liability for current year.
     */
    @Column(name = "annual_tax_liability", nullable = false, precision = 15, scale = 2)
    private BigDecimal annualTaxLiability;
    
    /**
     * Prior year tax liability (for safe harbor calculation).
     */
    @Column(name = "prior_year_tax_liability", nullable = false, precision = 15, scale = 2)
    private BigDecimal priorYearTaxLiability;
    
    /**
     * Adjusted Gross Income (AGI) for the taxpayer.
     * Used to determine 100% vs 110% prior year safe harbor threshold.
     */
    @Column(name = "agi", nullable = false, precision = 15, scale = 2)
    private BigDecimal agi;
    
    /**
     * Safe Harbor 1: Paid >= 90% of current year tax.
     * FR-016: If met, no penalty assessed.
     */
    @Column(name = "safe_harbor_1_met", nullable = false)
    @Builder.Default
    private Boolean safeHarbor1Met = false;
    
    /**
     * Safe Harbor 2: Paid >= 100% of prior year tax (110% if AGI > $150K).
     * FR-017: If met, no penalty assessed.
     */
    @Column(name = "safe_harbor_2_met", nullable = false)
    @Builder.Default
    private Boolean safeHarbor2Met = false;
    
    /**
     * Calculation method used: STANDARD (25% per quarter) or ANNUALIZED_INCOME.
     * FR-023: Standard method assumes equal quarterly income.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_method", nullable = false, length = 20)
    private CalculationMethod calculationMethod;
    
    /**
     * Total penalty amount across all quarters.
     * Sum of penalties from quarterly_underpayments table.
     */
    @Column(name = "total_penalty", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalPenalty = BigDecimal.ZERO;
    
    /**
     * Quarterly underpayment details (one-to-many relationship).
     * Not mapped as JPA relationship - accessed via repository queries.
     */
    @Transient
    @Builder.Default
    private List<QuarterlyUnderpayment> quarterlyUnderpayments = new ArrayList<>();
    
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
     * Check if any safe harbor is met (no penalty due).
     * 
     * @return true if either safe harbor condition is met
     */
    public boolean isSafeHarborMet() {
        return Boolean.TRUE.equals(safeHarbor1Met) || Boolean.TRUE.equals(safeHarbor2Met);
    }
    
    /**
     * Get the required prior year payment percentage based on AGI.
     * FR-017: 100% if AGI <= $150K, 110% if AGI > $150K
     * 
     * @return 1.00 or 1.10
     */
    public BigDecimal getPriorYearSafeHarborPercentage() {
        BigDecimal agiThreshold = new BigDecimal("150000.00");
        return agi.compareTo(agiThreshold) > 0 
            ? new BigDecimal("1.10") 
            : BigDecimal.ONE;
    }
    
    /**
     * Calculate required prior year payment for safe harbor 2.
     * 
     * @return prior year liability × (100% or 110%)
     */
    public BigDecimal getRequiredPriorYearPayment() {
        return priorYearTaxLiability.multiply(getPriorYearSafeHarborPercentage());
    }
}
