package com.munitax.taxengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for Year-End Reconciliation Report.
 * 
 * Functional Requirements:
 * - FR-007: Generate reconciliation report with detailed breakdown
 * - FR-008: Allow explanation text for discrepancies
 * - FR-009: Track reconciliation status
 * - FR-018: Employee count validation
 * 
 * Report Sections:
 * 1. W-1 Summary: Total wages and tax from all W-1 filings
 * 2. W-2 Summary: Total wages and tax from uploaded W-2 forms
 * 3. Variance Analysis: Difference between W-1 and W-2 totals
 * 4. Employee Count Validation: W-1 reported vs W-2 issued count
 * 5. Reconciliation Status and Action Items
 * 
 * @see com.munitax.taxengine.domain.withholding.WithholdingReconciliation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReconciliationReportDto {
    
    /**
     * Reconciliation ID.
     */
    private String reconciliationId;
    
    /**
     * Tax year being reconciled (e.g., 2024).
     */
    private Integer taxYear;
    
    /**
     * Total gross wages from all W-1 filings for the year.
     */
    private BigDecimal w1TotalWages;
    
    /**
     * Total tax due from all W-1 filings for the year.
     */
    private BigDecimal w1TotalTax;
    
    /**
     * Total gross wages from all uploaded W-2 forms (Box 18: Local wages).
     */
    private BigDecimal w2TotalWages;
    
    /**
     * Total tax withheld from all uploaded W-2 forms (Box 19: Local tax).
     */
    private BigDecimal w2TotalTax;
    
    /**
     * Wage variance: w1TotalWages - w2TotalWages.
     * - Positive: W-1 reported more wages than W-2 (possible under-reporting on W-2)
     * - Negative: W-2 reported more wages than W-1 (possible under-filing of W-1)
     */
    private BigDecimal varianceWages;
    
    /**
     * Tax variance: w1TotalTax - w2TotalTax.
     * - Positive: W-1 reported more tax than W-2 (possible over-withholding)
     * - Negative: W-2 reported more tax than W-1 (possible under-withholding)
     */
    private BigDecimal varianceTax;
    
    /**
     * Variance as percentage of W-2 total wages.
     * Formula: (varianceWages / w2TotalWages) × 100
     * 
     * Used for threshold detection:
     * - < 1%: Minor variance (may accept without action)
     * - >= 1%: Significant variance (requires explanation or amendment)
     */
    private BigDecimal variancePercentage;
    
    /**
     * Number of W-2 forms uploaded and successfully extracted.
     */
    private Integer w2Count;
    
    /**
     * Number of distinct employees from W-2 forms (based on SSN).
     */
    private Integer w2EmployeeCount;
    
    /**
     * Number of W-2 forms ignored (not included in reconciliation).
     * Reasons: wrong EIN, duplicate, extraction error, incomplete data.
     */
    private Integer ignoredW2Count;
    
    /**
     * Average employee count from W-1 filings.
     * Used for FR-018 employee count validation.
     */
    private Integer w1AverageEmployeeCount;
    
    /**
     * Employee count variance: w1AverageEmployeeCount - w2EmployeeCount.
     * Tolerance: ±20% (FR-018).
     */
    private Integer employeeCountVariance;
    
    /**
     * Employee count warning message (if variance > 20%).
     * Example: "Employee count mismatch - 15 average reported vs 12 W-2s issued"
     */
    private String employeeCountWarning;
    
    /**
     * Reconciliation status:
     * - NOT_STARTED: No W-2s uploaded yet
     * - IN_PROGRESS: W-2s being uploaded/extracted
     * - DISCREPANCY: Variance exceeds threshold (requires action)
     * - RECONCILED: Variance within threshold or explained
     */
    private String status;
    
    /**
     * Explanation text if discrepancy was accepted (FR-008).
     * Required if status = RECONCILED and variance > 0.
     */
    private String resolutionNotes;
    
    /**
     * Date when reconciliation was completed.
     * Null if status = NOT_STARTED or IN_PROGRESS.
     */
    private LocalDateTime reconciliationDate;
    
    /**
     * Recommended action based on variance:
     * - "ACCEPT": Variance within threshold, no action needed
     * - "AMEND": File amended W-1 to correct discrepancy
     * - "EXPLAIN": Provide explanation for discrepancy
     * - "REVIEW_IGNORED": Review ignored W-2s and re-upload if needed
     */
    private String recommendedAction;
    
    /**
     * List of ignored W-2 summaries (for quick review).
     * Full details available via GET /reconciliations/{id}/ignored-w2s.
     */
    private List<IgnoredW2Summary> ignoredW2Summaries;
    
    /**
     * Summary of an ignored W-2 for reconciliation report.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IgnoredW2Summary {
        /**
         * Employer name from W-2 (if extracted).
         */
        private String employerName;
        
        /**
         * Employer EIN from W-2 (if extracted).
         */
        private String employerEin;
        
        /**
         * Reason why W-2 was ignored.
         */
        private String ignoredReason;
        
        /**
         * Last 4 digits of employee SSN (for duplicate detection).
         */
        private String employeeSsnLast4;
    }
}
