package com.munitax.taxengine.domain.penalty;

/**
 * Types of penalties that can be assessed on tax returns.
 * 
 * Functional Requirements:
 * - FR-001 to FR-006: Late filing penalty (5% per month, max 25%)
 * - FR-007 to FR-011: Late payment penalty (1% per month, max 25%)
 * - FR-015 to FR-026: Estimated tax underpayment penalty
 */
public enum PenaltyType {
    /**
     * Late filing penalty: 5% per month, maximum 25% after 5 months.
     * Applies when return filed after due date (including extensions).
     */
    LATE_FILING,
    
    /**
     * Late payment penalty: 1% per month, maximum 25% after 25 months.
     * Applies when tax paid after due date.
     */
    LATE_PAYMENT,
    
    /**
     * Estimated tax underpayment penalty.
     * Applies when quarterly estimated payments insufficient.
     * Subject to safe harbor rules (90% current year OR 100%/110% prior year).
     */
    ESTIMATED_UNDERPAYMENT,
    
    /**
     * Other penalty types not covered by standard categories.
     */
    OTHER
}
