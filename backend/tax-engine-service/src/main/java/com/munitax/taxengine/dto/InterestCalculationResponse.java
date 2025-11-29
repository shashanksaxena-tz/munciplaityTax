package com.munitax.taxengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for Interest Calculation response.
 * 
 * Functional Requirements:
 * - FR-027 to FR-032: Interest calculation with quarterly compounding
 * - FR-031: Display interest breakdown by quarter
 * 
 * @see com.munitax.taxengine.domain.penalty.Interest
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterestCalculationResponse {
    
    /**
     * Interest calculation ID (UUID).
     */
    private String interestId;
    
    /**
     * Tax return ID.
     */
    private String returnId;
    
    /**
     * Original due date for the tax return.
     */
    private LocalDate taxDueDate;
    
    /**
     * Unpaid tax amount subject to interest.
     */
    private BigDecimal unpaidTaxAmount;
    
    /**
     * Annual interest rate used in calculation.
     */
    private BigDecimal annualInterestRate;
    
    /**
     * Annual interest rate as percentage (for display).
     */
    private BigDecimal annualInterestRatePercentage;
    
    /**
     * Quarterly interest rate (annual / 4).
     */
    private BigDecimal quarterlyInterestRate;
    
    /**
     * Interest calculation start date.
     */
    private LocalDate startDate;
    
    /**
     * Interest calculation end date.
     */
    private LocalDate endDate;
    
    /**
     * Total number of days in interest period.
     */
    private Integer totalDays;
    
    /**
     * Total interest calculated across all quarters.
     */
    private BigDecimal totalInterest;
    
    /**
     * Compounding frequency (always QUARTERLY).
     */
    @Builder.Default
    private String compoundingFrequency = "QUARTERLY";
    
    /**
     * Quarterly interest breakdown.
     */
    @Builder.Default
    private List<QuarterlyInterestDto> quarterlyBreakdown = new ArrayList<>();
    
    /**
     * Explanation of interest calculation.
     */
    private String explanation;
    
    /**
     * DTO for quarterly interest details.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuarterlyInterestDto {
        /**
         * Quarter label (e.g., "Q1 2024").
         */
        private String quarter;
        
        /**
         * Start date of this quarter.
         */
        private LocalDate startDate;
        
        /**
         * End date of this quarter.
         */
        private LocalDate endDate;
        
        /**
         * Number of days in this quarter.
         */
        private Integer days;
        
        /**
         * Principal balance at start of quarter.
         */
        private BigDecimal beginningBalance;
        
        /**
         * Interest accrued during this quarter.
         */
        private BigDecimal interestAccrued;
        
        /**
         * Balance at end of quarter (beginning + interest).
         */
        private BigDecimal endingBalance;
    }
    
    /**
     * Generate explanation of interest calculation.
     * 
     * @return human-readable explanation
     */
    public String generateExplanation() {
        return String.format("Interest calculated on $%,.2f unpaid tax from %s to %s (%d days) " +
                "at %.4f%% annual rate with quarterly compounding. Total interest: $%,.2f across %d quarters.",
                unpaidTaxAmount, startDate, endDate, totalDays,
                annualInterestRatePercentage, totalInterest, quarterlyBreakdown.size());
    }
}
