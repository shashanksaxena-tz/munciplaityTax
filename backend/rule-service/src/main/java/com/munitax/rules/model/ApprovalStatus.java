package com.munitax.rules.model;

/**
 * Approval workflow status for tax rules.
 * Ensures proper review and authorization before rules become active.
 */
public enum ApprovalStatus {
    /**
     * Rule has been created but not yet approved.
     * Cannot be used in tax calculations.
     */
    PENDING,
    
    /**
     * Rule has been approved and is active (subject to effective date).
     * Can be used in tax calculations once effective date is reached.
     */
    APPROVED,
    
    /**
     * Rule has been reviewed and rejected.
     * Cannot be modified or activated.
     */
    REJECTED,
    
    /**
     * Rule has been withdrawn or canceled before becoming effective.
     * Used for rules that are no longer needed.
     */
    VOIDED
}
