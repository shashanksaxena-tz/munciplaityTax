package com.munitax.taxengine.domain.penalty;

/**
 * Types of entities tracked in penalty audit log.
 * 
 * Functional Requirements:
 * - Constitution III: Audit trail immutability
 * - FR-045: Audit log for all penalty/interest actions
 */
public enum PenaltyAuditEntityType {
    /**
     * Penalty entity (late filing, late payment, underpayment).
     */
    PENALTY,
    
    /**
     * Interest entity (quarterly compound interest).
     */
    INTEREST,
    
    /**
     * Estimated tax penalty entity (quarterly underpayment with safe harbor).
     */
    ESTIMATED_TAX,
    
    /**
     * Penalty abatement entity (reasonable cause waiver).
     */
    ABATEMENT,
    
    /**
     * Payment allocation entity (how payment distributed to tax/penalties/interest).
     */
    PAYMENT_ALLOCATION
}
