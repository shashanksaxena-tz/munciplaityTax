package com.munitax.taxengine.domain.penalty;

/**
 * Order for applying payments to tax, penalties, and interest.
 * 
 * Functional Requirements:
 * - FR-040: Standard IRS payment allocation order
 */
public enum AllocationOrder {
    /**
     * TAX_FIRST: Standard IRS ordering.
     * 1. Apply to tax liability (principal) first
     * 2. Then to penalties (late filing, late payment, underpayment)
     * 3. Then to interest
     * 
     * Example: $10,000 payment on $8K tax + $1.5K penalties + $500 interest
     * → $8K to tax, $1.5K to penalties, $500 to interest (fully paid)
     */
    TAX_FIRST
}
