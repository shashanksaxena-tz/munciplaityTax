package com.munitax.taxengine.domain.penalty;

/**
 * Status of penalty abatement request.
 * 
 * Functional Requirements:
 * - FR-038: Track abatement request status through workflow
 */
public enum AbatementStatus {
    /**
     * Submitted, awaiting review by auditor.
     */
    PENDING,
    
    /**
     * Full abatement approved - penalty removed entirely.
     */
    APPROVED,
    
    /**
     * Partial abatement approved - some penalty removed, some upheld.
     */
    PARTIAL,
    
    /**
     * Full penalty upheld - request denied.
     */
    DENIED,
    
    /**
     * Taxpayer withdrew request before decision.
     */
    WITHDRAWN
}
