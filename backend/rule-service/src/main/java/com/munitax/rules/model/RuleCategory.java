package com.munitax.rules.model;

/**
 * Categories of tax rules for organizational purposes.
 * Each category represents a distinct domain of tax calculation logic.
 */
public enum RuleCategory {
    /**
     * Tax rates (municipal rate, state rate, etc.)
     */
    TAX_RATES("TaxRates"),
    
    /**
     * Income inclusion rules (what counts as taxable income)
     */
    INCOME_INCLUSION("IncomeInclusion"),
    
    /**
     * Deduction rules (standard deduction, itemized deductions)
     */
    DEDUCTIONS("Deductions"),
    
    /**
     * Penalty calculation rules (late filing, underpayment)
     */
    PENALTIES("Penalties"),
    
    /**
     * Filing requirement rules (who must file, thresholds)
     */
    FILING("Filing"),
    
    /**
     * Allocation rules (apportionment formulas, nexus)
     */
    ALLOCATION("Allocation"),
    
    /**
     * Withholding rules (employer withholding, estimated tax)
     */
    WITHHOLDING("Withholding"),
    
    /**
     * Validation rules (data quality, consistency checks)
     */
    VALIDATION("Validation");
    
    private final String displayName;
    
    RuleCategory(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
