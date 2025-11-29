package com.munitax.taxengine.domain.penalty;

/**
 * Frequency for interest compounding on unpaid tax.
 * 
 * Functional Requirements:
 * - FR-029: Quarterly compounding (IRS standard)
 */
public enum CompoundingFrequency {
    /**
     * Quarterly compounding: Interest compounds every 3 months.
     * Accrued interest added to principal at end of each quarter.
     * IRS standard for underpayment and overpayment interest.
     */
    QUARTERLY
}
