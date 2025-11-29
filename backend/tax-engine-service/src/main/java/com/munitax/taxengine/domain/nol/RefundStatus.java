package com.munitax.taxengine.domain.nol;

/**
 * Refund Status Enum - Tracks the status of NOL carryback refund claims.
 * 
 * Functional Requirements:
 * - FR-017: Calculate refund amount for each carryback year
 * - FR-018: Generate Form 27-NOL-CB (Carryback Application)
 * 
 * @see NOLCarryback
 */
public enum RefundStatus {
    /**
     * Carryback claim has been filed but not yet processed
     */
    CLAIMED,
    
    /**
     * Carryback refund approved by tax authority
     */
    APPROVED,
    
    /**
     * Carryback refund denied
     */
    DENIED,
    
    /**
     * Refund payment received by taxpayer
     */
    PAID
}
