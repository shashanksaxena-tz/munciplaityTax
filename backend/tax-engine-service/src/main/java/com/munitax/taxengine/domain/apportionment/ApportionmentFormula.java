package com.munitax.taxengine.domain.apportionment;

/**
 * Apportionment formula types for multi-state income allocation.
 * Defines how property, payroll, and sales factors are weighted in the final calculation.
 *
 * @see <a href="https://www.mtc.gov/uniformity/project-teams/apportionment-formula">MTC Apportionment Formula</a>
 */
public enum ApportionmentFormula {
    
    /**
     * Traditional three-factor formula: (P + PY + S) / 3
     * Equal weighting of property, payroll, and sales factors.
     */
    TRADITIONAL_THREE_FACTOR("Traditional Three-Factor", 1, 1, 1),
    
    /**
     * Four-factor with double-weighted sales: (P + PY + S + S) / 4
     * This is Ohio's default formula (ORC 718.02).
     */
    FOUR_FACTOR_DOUBLE_SALES("Four-Factor Double-Weighted Sales", 1, 1, 2),
    
    /**
     * Single-sales-factor formula: S / 1
     * Only sales factor used, property and payroll ignored.
     * Adopted by some states to incentivize in-state employment/property.
     */
    SINGLE_SALES_FACTOR("Single-Sales-Factor", 0, 0, 1),
    
    /**
     * Custom formula with configurable weights.
     * Weights specified in formula_weights JSONB column.
     * Example: {"property": 0.5, "payroll": 0.25, "sales": 0.25}
     */
    CUSTOM("Custom Formula", null, null, null);
    
    private final String displayName;
    private final Integer propertyWeight;
    private final Integer payrollWeight;
    private final Integer salesWeight;
    
    ApportionmentFormula(String displayName, Integer propertyWeight, Integer payrollWeight, Integer salesWeight) {
        this.displayName = displayName;
        this.propertyWeight = propertyWeight;
        this.payrollWeight = payrollWeight;
        this.salesWeight = salesWeight;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public Integer getPropertyWeight() {
        return propertyWeight;
    }
    
    public Integer getPayrollWeight() {
        return payrollWeight;
    }
    
    public Integer getSalesWeight() {
        return salesWeight;
    }
    
    public int getTotalWeight() {
        if (this == CUSTOM) {
            throw new IllegalStateException("Custom formula weights must be specified in formula_weights JSONB");
        }
        return propertyWeight + payrollWeight + salesWeight;
    }
    
    public boolean isCustom() {
        return this == CUSTOM;
    }
}
