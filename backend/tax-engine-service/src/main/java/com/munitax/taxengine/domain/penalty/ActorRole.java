package com.munitax.taxengine.domain.penalty;

/**
 * Role of actor performing penalty/interest actions.
 * 
 * Functional Requirements:
 * - FR-045: Audit log includes actor role
 */
public enum ActorRole {
    /**
     * Taxpayer or their authorized representative (CPA, attorney).
     */
    TAXPAYER,
    
    /**
     * Municipality auditor or tax administrator.
     * Can review and approve penalty abatement requests.
     */
    AUDITOR,
    
    /**
     * System-automated action (e.g., nightly penalty calculation job).
     */
    SYSTEM
}
