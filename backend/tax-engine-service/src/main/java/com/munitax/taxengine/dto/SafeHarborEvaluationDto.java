package com.munitax.taxengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for Safe Harbor Evaluation response.
 * 
 * Functional Requirements:
 * - FR-015 to FR-019: Safe harbor evaluation (90% current year OR 100%/110% prior year)
 * - FR-016: Safe Harbor 1 - Paid >= 90% of current year tax
 * - FR-017: Safe Harbor 2 - Paid >= 100% of prior year tax (110% if AGI > $150K)
 * 
 * @see com.munitax.taxengine.domain.penalty.EstimatedTaxPenalty
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SafeHarborEvaluationDto {
    
    /**
     * Safe Harbor 1: Paid >= 90% of current year tax.
     */
    private Boolean safeHarbor1Met;
    
    /**
     * Amount paid toward current year tax.
     */
    private BigDecimal currentYearPaid;
    
    /**
     * Required amount for Safe Harbor 1 (90% of current year tax).
     */
    private BigDecimal safeHarbor1Required;
    
    /**
     * Percentage paid of current year tax.
     */
    private BigDecimal currentYearPercentage;
    
    /**
     * Safe Harbor 2: Paid >= 100%/110% of prior year tax.
     */
    private Boolean safeHarbor2Met;
    
    /**
     * Amount paid toward prior year liability.
     */
    private BigDecimal priorYearPaid;
    
    /**
     * Required amount for Safe Harbor 2 (100% or 110% of prior year).
     */
    private BigDecimal safeHarbor2Required;
    
    /**
     * Prior year safe harbor percentage (100% or 110%).
     */
    private BigDecimal priorYearPercentage;
    
    /**
     * Adjusted Gross Income (determines 100% vs 110% threshold).
     */
    private BigDecimal agi;
    
    /**
     * AGI threshold for 110% requirement ($150,000).
     */
    @Builder.Default
    private BigDecimal agiThreshold = new BigDecimal("150000.00");
    
    /**
     * Whether any safe harbor was met (no penalty due).
     */
    private Boolean anySafeHarborMet;
    
    /**
     * Explanation of safe harbor evaluation.
     */
    private String explanation;
    
    /**
     * Calculate if any safe harbor was met.
     * 
     * @return true if either safe harbor 1 or 2 is met
     */
    public Boolean calculateAnySafeHarborMet() {
        return Boolean.TRUE.equals(safeHarbor1Met) || Boolean.TRUE.equals(safeHarbor2Met);
    }
    
    /**
     * Generate explanation based on safe harbor results.
     * 
     * @return human-readable explanation
     */
    public String generateExplanation() {
        if (Boolean.TRUE.equals(safeHarbor1Met)) {
            return String.format("Safe Harbor 1 met: Paid %.2f%% of current year tax (>= 90%% required)",
                    currentYearPercentage);
        } else if (Boolean.TRUE.equals(safeHarbor2Met)) {
            boolean highAgi = agi.compareTo(agiThreshold) > 0;
            return String.format("Safe Harbor 2 met: Paid %.2f%% of prior year tax (>= %d%% required for AGI %s $150K)",
                    priorYearPercentage.multiply(BigDecimal.valueOf(100)),
                    highAgi ? 110 : 100,
                    highAgi ? ">" : "<=");
        } else {
            return String.format("No safe harbor met: Paid %.2f%% of current year (< 90%%) and %.2f%% of prior year (< %d%%)",
                    currentYearPercentage,
                    priorYearPercentage.multiply(BigDecimal.valueOf(100)),
                    agi.compareTo(agiThreshold) > 0 ? 110 : 100);
        }
    }
}
