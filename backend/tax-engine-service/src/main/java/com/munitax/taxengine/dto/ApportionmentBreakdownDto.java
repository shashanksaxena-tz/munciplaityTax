package com.munitax.taxengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * DTO for detailed apportionment factor breakdown.
 * Provides transparency into how final apportionment percentage was calculated.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApportionmentBreakdownDto {

    /**
     * Property factor percentage (0-100).
     */
    private BigDecimal propertyFactorPercentage;

    /**
     * Property factor weight in formula (e.g., 0.25 for 25%).
     */
    private BigDecimal propertyFactorWeight;

    /**
     * Weighted property factor contribution to final apportionment.
     */
    private BigDecimal propertyFactorWeightedContribution;

    /**
     * Payroll factor percentage (0-100).
     */
    private BigDecimal payrollFactorPercentage;

    /**
     * Payroll factor weight in formula (e.g., 0.25 for 25%).
     */
    private BigDecimal payrollFactorWeight;

    /**
     * Weighted payroll factor contribution to final apportionment.
     */
    private BigDecimal payrollFactorWeightedContribution;

    /**
     * Sales factor percentage (0-100).
     */
    private BigDecimal salesFactorPercentage;

    /**
     * Sales factor weight in formula (e.g., 0.50 for 50% in double-weighted sales).
     */
    private BigDecimal salesFactorWeight;

    /**
     * Weighted sales factor contribution to final apportionment.
     */
    private BigDecimal salesFactorWeightedContribution;

    /**
     * Sum of all factor weights (should equal 1.0 or 100%).
     */
    private BigDecimal totalWeight;

    /**
     * Final calculated apportionment percentage.
     */
    private BigDecimal finalApportionmentPercentage;

    /**
     * Throwback adjustments applied (state -> adjustment amount).
     */
    private Map<String, BigDecimal> throwbackAdjustments;

    /**
     * Service sourcing adjustments applied (transaction ID -> adjustment).
     */
    private Map<String, BigDecimal> serviceSourcingAdjustments;

    /**
     * Number of sale transactions included.
     */
    private Integer totalSaleTransactions;

    /**
     * Number of transactions with throwback applied.
     */
    private Integer throwbackTransactionCount;

    /**
     * Number of service transactions with market-based sourcing.
     */
    private Integer marketBasedServiceCount;

    /**
     * Formula description (e.g., "Four-Factor Double-Weighted Sales").
     */
    private String formulaDescription;

    /**
     * Detailed calculation explanation.
     */
    private String calculationExplanation;
}
