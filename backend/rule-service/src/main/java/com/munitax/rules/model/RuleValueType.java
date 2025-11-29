package com.munitax.rules.model;

/**
 * Types of values that can be stored in a tax rule.
 * Determines the structure of the JSONB value field.
 */
public enum RuleValueType {
    /**
     * Simple numeric value (e.g., 50000 for threshold amount)
     * JSONB structure: {"scalar": 50000}
     */
    NUMBER,
    
    /**
     * Percentage value (e.g., 2.5% for tax rate)
     * JSONB structure: {"scalar": 2.5, "unit": "percent"}
     */
    PERCENTAGE,
    
    /**
     * Enumerated value from predefined list (e.g., "BOX_5_MEDICARE")
     * JSONB structure: {"option": "BOX_5_MEDICARE", "allowedValues": [...]}
     */
    ENUM,
    
    /**
     * Boolean flag (e.g., true/false for a feature toggle)
     * JSONB structure: {"flag": true}
     */
    BOOLEAN,
    
    /**
     * Mathematical formula using SpEL (e.g., "wages * municipalRate")
     * JSONB structure: {"expression": "...", "variables": [...], "returnType": "number"}
     */
    FORMULA,
    
    /**
     * Conditional rule with if-then-else logic
     * JSONB structure: {"condition": "income > 1000000", "thenValue": 5000, "elseValue": 50}
     */
    CONDITIONAL
}
