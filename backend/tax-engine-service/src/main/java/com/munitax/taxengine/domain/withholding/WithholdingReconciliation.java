package com.munitax.taxengine.domain.withholding;

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
 * WithholdingReconciliation Entity - Year-end reconciliation comparing W-1 totals to W-2 totals.
 * 
 * Purpose:
 * IRS/municipal compliance requirement: W-1 quarterly/monthly totals MUST reconcile to 
 * annual W-2/W-3 totals. Discrepancies trigger audits (User Story 2).
 * 
 * Functional Requirements:
 * - FR-006: Perform year-end reconciliation by comparing cumulative W-1 vs uploaded W-2s
 * - FR-007: Generate reconciliation report showing variance and recommended action
 * - FR-008: Allow business to attach explanation text for discrepancies
 * - FR-009: Track reconciliation status per tax year
 * - FR-010: Prevent next year's W-1 filing if prior year reconciliation incomplete
 * 
 * Variance Threshold (FR-006):
 * - Flag discrepancy if: abs(variance) > $100 OR variance_percentage > 1%
 * - Example: $200,000 W-1 vs $202,500 W-2 = $2,500 variance (1.25%) → DISCREPANCY status
 * - Example: $200,000 W-1 vs $200,050 W-2 = $50 variance (0.025%) → RECONCILED status
 * 
 * @see W1Filing
 * @see CumulativeWithholdingTotals
 * @see ReconciliationStatus
 */
@Entity
@Table(name = "withholding_reconciliations", schema = "dublin")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WithholdingReconciliation {
    
    /**
     * Unique identifier for this reconciliation record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /**
     * Multi-tenant isolation.
     */
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    /**
     * Business profile being reconciled.
     */
    @Column(name = "business_id", nullable = false)
    private UUID businessId;
    
    /**
     * Tax year for reconciliation (e.g., 2024).
     * UNIQUE constraint: One reconciliation per business + tax_year.
     */
    @Column(name = "tax_year", nullable = false)
    private Integer taxYear;
    
    /**
     * Sum of gross wages from all W-1 filings for tax year.
     * Copied from CumulativeWithholdingTotals at reconciliation initiation.
     */
    @Column(name = "w1_total_wages", nullable = false, precision = 15, scale = 2)
    private BigDecimal w1TotalWages;
    
    /**
     * Sum of Box 18 (local wages) from all uploaded W-2s.
     * Extracted by extraction-service (Gemini AI) from W-2 PDFs (FR-014).
     */
    @Column(name = "w2_total_wages", nullable = false, precision = 15, scale = 2)
    private BigDecimal w2TotalWages;
    
    /**
     * Sum of tax due from all W-1 filings.
     */
    @Column(name = "w1_total_tax", nullable = false, precision = 15, scale = 2)
    private BigDecimal w1TotalTax;
    
    /**
     * Sum of Box 19 (local tax withheld) from all uploaded W-2s.
     */
    @Column(name = "w2_total_tax", nullable = false, precision = 15, scale = 2)
    private BigDecimal w2TotalTax;
    
    /**
     * Wage variance: w1_total_wages - w2_total_wages.
     * Positive = W-1 over-reported (under-withheld).
     * Negative = W-1 under-reported (over-withheld).
     * CHECK constraint: variance_wages = (w1_total_wages - w2_total_wages) within $0.01 tolerance
     */
    @Column(name = "variance_wages", nullable = false, precision = 15, scale = 2)
    private BigDecimal varianceWages;
    
    /**
     * Tax variance: w1_total_tax - w2_total_tax.
     */
    @Column(name = "variance_tax", nullable = false, precision = 15, scale = 2)
    private BigDecimal varianceTax;
    
    /**
     * Variance percentage: (variance_wages / w2_total_wages) × 100.
     * Example: $2,500 variance on $200,000 W-2 wages = 1.25%
     */
    @Column(name = "variance_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal variancePercentage;
    
    /**
     * Current status of reconciliation workflow.
     * Lifecycle: NOT_STARTED → IN_PROGRESS → DISCREPANCY/RECONCILED
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ReconciliationStatus status = ReconciliationStatus.NOT_STARTED;
    
    /**
     * When reconciliation completed (status = RECONCILED).
     */
    @Column(name = "reconciliation_date")
    private LocalDateTime reconciliationDate;
    
    /**
     * Business explanation for discrepancy (FR-008).
     * Required if status = RECONCILED and variance exists.
     * Example: "Variance due to mid-year employee relocation (different locality). 
     *           Confirmed with payroll records."
     */
    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;
    
    /**
     * Count of W-2 PDFs uploaded for reconciliation.
     */
    @Column(name = "w2_count", nullable = false)
    @Builder.Default
    private Integer w2Count = 0;
    
    /**
     * Count of distinct employees from W-2s (unique SSNs).
     * Used for employee count validation (FR-018).
     */
    @Column(name = "w2_employee_count", nullable = false)
    @Builder.Default
    private Integer w2EmployeeCount = 0;
    
    /**
     * Count of W-2 PDFs ignored during reconciliation (Constitution IV - AI Transparency).
     * Example: 2 W-2s with wrong employer EIN → ignored_w2_count = 2.
     */
    @Column(name = "ignored_w2_count")
    @Builder.Default
    private Integer ignoredW2Count = 0;
    
    /**
     * Auditor who approved discrepancy resolution (optional).
     * Null if business self-resolved within safe harbor threshold.
     */
    @Column(name = "approved_by")
    private UUID approvedBy;
    
    /**
     * When auditor approved discrepancy resolution.
     */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    
    /**
     * Record creation timestamp.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    /**
     * User who initiated reconciliation (business owner/accountant).
     */
    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;
    
    /**
     * Last update timestamp (status changes, W-2 uploads).
     */
    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
