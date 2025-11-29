package com.munitax.rules.model;

/**
 * Categories of tax rules for organizational purposes.
 * Each category represents a distinct domain of tax calculation logic.
 */
public enum RuleCategory {
    /**
     * Tax rates (municipal rate, state rate, etc.)
     */
    TAX_RATES,
    
    /**
     * Income inclusion rules (what counts as taxable income)
     */
    INCOME_INCLUSION,
    
    /**
     * Deduction rules (standard deduction, itemized deductions)
     */
    DEDUCTIONS,
    
    /**
     * Penalty calculation rules (late filing, underpayment)
     */
    PENALTIES,
    
    /**
     * Filing requirement rules (who must file, thresholds)
     */
    FILING,
    
    /**
     * Allocation rules (apportionment formulas, nexus)
     */
    ALLOCATION,
    
    /**
     * Withholding rules (employer withholding, estimated tax)
     */
    WITHHOLDING,
    
    /**
     * Validation rules (data quality, consistency checks)
     */
    VALIDATION
}
