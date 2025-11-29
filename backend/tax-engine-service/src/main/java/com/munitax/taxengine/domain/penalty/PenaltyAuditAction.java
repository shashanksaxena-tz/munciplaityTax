package com.munitax.taxengine.domain.penalty;

/**
 * Actions that trigger penalty audit log entries.
 * 
 * Functional Requirements:
 * - Constitution III: Audit trail immutability
 * - FR-045: Audit log for all penalty/interest actions
 */
public enum PenaltyAuditAction {
    /**
     * Penalty or interest assessed on return.
     */
    ASSESSED,
    
    /**
     * Penalty or interest calculated (initial or recalculation).
     */
    CALCULATED,
    
    /**
     * Penalty abated (removed for reasonable cause).
     */
    ABATED,
    
    /**
     * Payment applied to tax, penalties, or interest.
     */
    PAYMENT_APPLIED,
    
    /**
     * Penalty or interest recalculated after payment or abatement.
     */
    RECALCULATED
}
