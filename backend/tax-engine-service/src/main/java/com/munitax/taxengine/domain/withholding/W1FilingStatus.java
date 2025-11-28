package com.munitax.taxengine.domain.withholding;

/**
 * Status lifecycle for W-1 withholding return filings.
 * Tracks payment status and amendment state.
 */
public enum W1FilingStatus {
    /**
     * W-1 return has been submitted but payment not yet received.
     * Initial state after filing.
     */
    FILED,
    
    /**
     * Payment has been received in full for this W-1 filing.
     */
    PAID,
    
    /**
     * W-1 filing is past due date and remains unpaid.
     * Penalties may be applied.
     */
    OVERDUE,
    
    /**
     * This W-1 filing has been superseded by an amended filing.
     * Original filing preserved for audit trail but no longer active.
     */
    AMENDED
}
