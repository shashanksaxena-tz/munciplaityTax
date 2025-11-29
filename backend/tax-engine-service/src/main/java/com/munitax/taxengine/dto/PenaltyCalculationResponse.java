package com.munitax.taxengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for Penalty Calculation response.
 * 
 * Functional Requirements:
 * - FR-011: Calculate late-filing penalties (5% per month, max 25%, min $50 if tax > $200)
 * - FR-012: Calculate underpayment penalties (90% safe harbor rule)
 * 
 * Research R4: Late Filing Penalty Edge Cases
 * - Partial months: Round up to nearest month
 * - Safe harbor exceptions: First-time filer waiver, reasonable cause
 * - Minimum penalty: $50 if tax due > $200, otherwise waived
 * 
 * @see com.munitax.taxengine.domain.withholding.W1Filing
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PenaltyCalculationResponse {
    
    /**
     * W-1 filing ID that penalties apply to.
     */
    private String filingId;
    
    /**
     * Tax year and period (e.g., "2024-Q1").
     */
    private String taxYearAndPeriod;
    
    /**
     * Original due date for this W-1 filing.
     */
    private LocalDate dueDate;
    
    /**
     * Actual filing date (or current date if not yet filed).
     */
    private LocalDate filingDate;
    
    /**
     * Number of days late (0 if filed on time).
     */
    private Integer daysLate;
    
    /**
     * Tax amount due for this filing (before penalties).
     */
    private BigDecimal taxDue;
    
    /**
     * Late filing penalty amount (FR-011).
     * Formula:
     * - Months late (rounded up) × 5% × tax due
     * - Maximum: 25% of tax due
     * - Minimum: $50 if tax due > $200, otherwise $0
     */
    private BigDecimal lateFilingPenalty;
    
    /**
     * Late filing penalty rate applied (as percentage).
     * Example: 15 days late = 1 month × 5% = 5.0%
     */
    private BigDecimal lateFilingPenaltyRate;
    
    /**
     * Explanation of late filing penalty calculation.
     * Example: "Filed 45 days late (2 months). Penalty: 2 months × 5% × $2,500 = $250"
     */
    private String lateFilingPenaltyExplanation;
    
    /**
     * Underpayment penalty amount (FR-012).
     * Formula:
     * - If paid < 90% of current year liability: (shortfall × 15% annual / 365) × days outstanding
     * - Safe harbor: If paid >= 100% of prior year liability, no penalty
     */
    private BigDecimal underpaymentPenalty;
    
    /**
     * Underpayment penalty rate applied (annualized).
     * Fixed at 15% annual rate per municipal tax code.
     */
    @Builder.Default
    private BigDecimal underpaymentPenaltyRate = new BigDecimal("0.15");
    
    /**
     * Explanation of underpayment penalty calculation.
     * Example: "Paid $1,800 on $2,500 due (72% < 90% safe harbor). Penalty: $700 shortfall × 15% annual × 30 days / 365 = $8.63"
     */
    private String underpaymentPenaltyExplanation;
    
    /**
     * Total penalties (late filing + underpayment).
     */
    private BigDecimal totalPenalties;
    
    /**
     * Whether safe harbor exception applies.
     * TRUE if business qualifies for penalty waiver:
     * - First-time filer (no prior year filings)
     * - Reasonable cause (e.g., natural disaster, illness)
     * - Paid >= 100% of prior year liability
     */
    private Boolean safeHarborApplies;
    
    /**
     * Safe harbor reason (if applicable).
     * Examples:
     * - "First-time filer - penalty waived"
     * - "Paid 100% of prior year liability ($2,400) - safe harbor met"
     * - "Reasonable cause: Hurricane damage - penalty waived"
     */
    private String safeHarborReason;
}
