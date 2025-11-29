package com.munitax.taxengine.domain.nol;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * NOLSchedule Entity - Consolidated NOL schedule for a tax return (all NOL vintages).
 * 
 * Functional Requirements:
 * - FR-004: Display NOL schedule on tax return
 * - FR-012: Display calculation breakdown
 * - FR-036: Generate Form 27-NOL
 * - FR-045: Reconcile NOL balance across years
 * 
 * Constitution Compliance:
 * - Principle II: Multi-tenant data isolation via tenant_id
 * - Principle III: Immutable audit trail
 * 
 * @see NOL
 * @see NOLUsage
 */
@Entity
@Table(name = "nol_schedules", schema = "dublin", indexes = {
    @Index(name = "idx_nol_schedule_return", columnList = "return_id"),
    @Index(name = "idx_nol_schedule_year", columnList = "tax_year"),
    @Index(name = "idx_nol_schedule_business", columnList = "business_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NOLSchedule {
    
    /**
     * Unique identifier for this NOL schedule.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /**
     * Multi-tenant isolation - tenant owning this schedule.
     */
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    /**
     * Business profile ID for this schedule.
     */
    @Column(name = "business_id", nullable = false)
    private UUID businessId;
    
    /**
     * Reference to the tax return for this schedule.
     * References tax_returns table (not defined in this service).
     */
    @Column(name = "return_id", nullable = false)
    private UUID returnId;
    
    /**
     * Tax year for this schedule (e.g., 2024).
     */
    @Column(name = "tax_year", nullable = false)
    private Integer taxYear;
    
    /**
     * Sum of all NOL balances at the start of this tax year.
     * Carried forward from prior year ending balance.
     * CHECK constraint: total_beginning_balance >= 0
     */
    @Column(name = "total_beginning_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalBeginningBalance;
    
    /**
     * New NOL generated in current year (if current year has loss).
     * Zero if current year has positive taxable income.
     * CHECK constraint: new_nol_generated >= 0
     */
    @Column(name = "new_nol_generated", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal newNOLGenerated = BigDecimal.ZERO;
    
    /**
     * Total NOL available for use in current year.
     * = total_beginning_balance + new_nol_generated
     * CHECK constraint: total_available_nol >= 0
     */
    @Column(name = "total_available_nol", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAvailableNOL;
    
    /**
     * Total NOL deduction applied in current year.
     * Sum of all NOLUsage records for this return.
     * CHECK constraint: nol_deduction >= 0
     * CHECK constraint: nol_deduction <= total_available_nol
     */
    @Column(name = "nol_deduction", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal nolDeduction = BigDecimal.ZERO;
    
    /**
     * Total NOL expired in current year.
     * Sum of expired amounts from all NOL records.
     * CHECK constraint: expired_nol >= 0
     */
    @Column(name = "expired_nol", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal expiredNOL = BigDecimal.ZERO;
    
    /**
     * Total NOL balance at the end of this tax year.
     * = total_available_nol - nol_deduction - expired_nol
     * Carried forward to next year as beginning balance.
     * CHECK constraint: total_ending_balance >= 0
     */
    @Column(name = "total_ending_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalEndingBalance;
    
    /**
     * NOL limitation percentage applicable for this year (80 or 100).
     * Determined by tax year and jurisdiction rules.
     * Post-2017: 80%, Pre-2018: 100%
     */
    @Column(name = "limitation_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal limitationPercentage;
    
    /**
     * Taxable income before NOL deduction.
     * Retrieved from tax return calculation.
     * CHECK constraint: taxable_income_before_nol >= 0
     */
    @Column(name = "taxable_income_before_nol", nullable = false, precision = 15, scale = 2)
    private BigDecimal taxableIncomeBeforeNOL;
    
    /**
     * Taxable income after NOL deduction.
     * = taxable_income_before_nol - nol_deduction
     * CHECK constraint: taxable_income_after_nol >= 0
     */
    @Column(name = "taxable_income_after_nol", nullable = false, precision = 15, scale = 2)
    private BigDecimal taxableIncomeAfterNOL;
    
    /**
     * Audit trail: When this schedule was created (immutable).
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    /**
     * Last modification timestamp.
     */
    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
