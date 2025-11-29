package com.munitax.taxengine.dto;

import com.munitax.taxengine.domain.apportionment.ApportionmentFormula;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for formula comparison between traditional and single-sales-factor formulas.
 * Task: T134 [US5]
 * 
 * Helps taxpayers understand the difference between apportionment formulas
 * and choose the option that minimizes their tax liability.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormulaComparisonDto {

    /**
     * Traditional formula name (e.g., FOUR_FACTOR_DOUBLE_SALES).
     */
    private String traditionalFormula;

    /**
     * Calculated apportionment percentage using traditional formula.
     */
    private BigDecimal traditionalApportionment;

    /**
     * Property factor weighted contribution in traditional formula.
     */
    private BigDecimal traditionalPropertyContribution;

    /**
     * Payroll factor weighted contribution in traditional formula.
     */
    private BigDecimal traditionalPayrollContribution;

    /**
     * Sales factor weighted contribution in traditional formula.
     */
    private BigDecimal traditionalSalesContribution;

    /**
     * Single-sales-factor formula name.
     */
    private String singleSalesFormula;

    /**
     * Calculated apportionment percentage using single-sales-factor.
     */
    private BigDecimal singleSalesApportionment;

    /**
     * Property factor weighted contribution in single-sales formula (always 0).
     */
    private BigDecimal singleSalesPropertyContribution;

    /**
     * Payroll factor weighted contribution in single-sales formula (always 0).
     */
    private BigDecimal singleSalesPayrollContribution;

    /**
     * Sales factor weighted contribution in single-sales formula (100% weight).
     */
    private BigDecimal singleSalesSalesContribution;

    /**
     * Recommended formula based on lower apportionment.
     */
    private String recommendedFormula;

    /**
     * Explanation of why this formula is recommended.
     */
    private String recommendationReason;

    /**
     * Absolute difference in apportionment percentage between the two formulas.
     */
    private BigDecimal savingsPercentage;

    /**
     * Whether the taxpayer used the recommended formula.
     */
    private Boolean usedRecommendation;
}
