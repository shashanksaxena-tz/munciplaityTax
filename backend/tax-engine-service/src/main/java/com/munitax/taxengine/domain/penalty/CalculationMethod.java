package com.munitax.taxengine.domain.penalty;

/**
 * Calculation methods for estimated tax underpayment penalties.
 * 
 * Functional Requirements:
 * - FR-020: Standard method (25% of annual tax per quarter)
 * - FR-021: Annualized income method (for uneven income throughout year)
 */
public enum CalculationMethod {
    /**
     * Standard method: 25% of annual tax due per quarter.
     * Simplest approach, used by most taxpayers.
     * Q1 (Apr 15), Q2 (Jun 15), Q3 (Sep 15), Q4 (Jan 15).
     */
    STANDARD,
    
    /**
     * Annualized income method: Based on income earned through each quarter.
     * Used when income is uneven throughout year (e.g., seasonal business).
     * Allows lower required payments in early quarters if income earned later.
     */
    ANNUALIZED_INCOME
}
