package com.munitax.taxengine.domain.nol;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * NOLUsage Entity - Represents NOL utilized in a specific tax year.
 * 
 * Functional Requirements:
 * - FR-002: Track NOL usage across years
 * - FR-008: Calculate maximum NOL deduction
 * - FR-009: Apply NOL deduction to taxable income
 * - FR-022: Apply FIFO ordering by default
 * - FR-025: Allow manual NOL utilization ordering override
 * 
 * Constitution Compliance:
 * - Principle II: Multi-tenant data isolation via tenant_id
 * - Principle III: Immutable audit trail
 * 
 * @see NOL
 * @see NOLOrderingMethod
 */
@Entity
@Table(name = "nol_usages", schema = "dublin", indexes = {
    @Index(name = "idx_nol_usage_nol", columnList = "nol_id"),
    @Index(name = "idx_nol_usage_return", columnList = "return_id"),
    @Index(name = "idx_nol_usage_year", columnList = "usage_year")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NOLUsage {
    
    /**
     * Unique identifier for this NOL usage record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /**
     * Multi-tenant isolation - tenant owning this usage record.
     */
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    /**
     * Reference to the NOL being used.
     */
    @Column(name = "nol_id", nullable = false)
    private UUID nolId;
    
    /**
     * Reference to the tax return where this NOL was used.
     * References tax_returns table (not defined in this service).
     */
    @Column(name = "return_id", nullable = false)
    private UUID returnId;
    
    /**
     * Tax year when this NOL was used (e.g., 2023, 2024).
     * CHECK constraint: usage_year >= tax_year of referenced NOL
     */
    @Column(name = "usage_year", nullable = false)
    private Integer usageYear;
    
    /**
     * Taxable income before NOL deduction was applied.
     * CHECK constraint: taxable_income_before_nol >= 0
     */
    @Column(name = "taxable_income_before_nol", nullable = false, precision = 15, scale = 2)
    private BigDecimal taxableIncomeBeforeNOL;
    
    /**
     * NOL limitation percentage (80 or 100).
     * Post-2017: 80% (TCJA rule)
     * Pre-2018: 100% (legacy rule)
     */
    @Column(name = "nol_limitation_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal nolLimitationPercentage;
    
    /**
     * Maximum NOL deduction allowed.
     * = Min(Available NOL balance, limitation% × taxable income before NOL)
     * CHECK constraint: maximum_nol_deduction >= 0
     */
    @Column(name = "maximum_nol_deduction", nullable = false, precision = 15, scale = 2)
    private BigDecimal maximumNOLDeduction;
    
    /**
     * Actual NOL deduction applied (may be less than maximum).
     * User may choose to use less than maximum for tax planning.
     * CHECK constraint: actual_nol_deduction >= 0
     * CHECK constraint: actual_nol_deduction <= maximum_nol_deduction
     */
    @Column(name = "actual_nol_deduction", nullable = false, precision = 15, scale = 2)
    private BigDecimal actualNOLDeduction;
    
    /**
     * Taxable income after NOL deduction.
     * = taxable_income_before_nol - actual_nol_deduction
     * CHECK constraint: taxable_income_after_nol >= 0
     */
    @Column(name = "taxable_income_after_nol", nullable = false, precision = 15, scale = 2)
    private BigDecimal taxableIncomeAfterNOL;
    
    /**
     * Tax savings from using this NOL.
     * = actual_nol_deduction × tax_rate
     * CHECK constraint: tax_savings >= 0
     */
    @Column(name = "tax_savings", nullable = false, precision = 15, scale = 2)
    private BigDecimal taxSavings;
    
    /**
     * Ordering method used for this NOL application.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "ordering_method", nullable = false, length = 20)
    @Builder.Default
    private NOLOrderingMethod orderingMethod = NOLOrderingMethod.FIFO;
    
    /**
     * Reason for manual override if ordering_method = MANUAL_OVERRIDE.
     * Required when not using FIFO ordering.
     * Example: "Using post-TCJA NOL first because pre-TCJA NOL can offset 100% in future high-income year"
     */
    @Column(name = "override_reason", columnDefinition = "TEXT")
    private String overrideReason;
    
    /**
     * Audit trail: When this NOL usage was recorded (immutable).
     */
    @Column(name = "usage_date", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime usageDate;
}
