package com.munitax.taxengine.domain.withholding;

/**
 * Enum representing W-1 filing frequencies as per FR-013.
 * Different businesses file at different intervals based on withholding volume.
 */
public enum FilingFrequency {
    /**
     * Daily filing - typically for large employers with significant daily withholding.
     * Due date: Next business day after filing period.
     */
    DAILY,
    
    /**
     * Semi-monthly filing - twice per month (e.g., 15th and end of month).
     * Due date: 15th of following month.
     */
    SEMI_MONTHLY,
    
    /**
     * Monthly filing - once per month.
     * Due date: 15th of following month.
     */
    MONTHLY,
    
    /**
     * Quarterly filing - four times per year (Q1, Q2, Q3, Q4).
     * Due date: 30 days after quarter end.
     */
    QUARTERLY
}
