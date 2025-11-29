package com.munitax.taxengine.domain.penalty;

/**
 * Reasons for requesting penalty abatement.
 * 
 * Functional Requirements:
 * - FR-035: Supported abatement reasons (reasonable cause)
 */
public enum AbatementReason {
    /**
     * Death in immediate family.
     */
    DEATH,
    
    /**
     * Serious illness or incapacitation of taxpayer or family member.
     * Requires supporting documentation (hospital records, doctor's note).
     */
    ILLNESS,
    
    /**
     * Natural disaster (fire, flood, earthquake, hurricane).
     * Requires FEMA declaration or equivalent proof.
     */
    DISASTER,
    
    /**
     * Unable to obtain necessary records.
     * Example: Records destroyed in fire, accountant disappeared with records.
     */
    MISSING_RECORDS,
    
    /**
     * Erroneous written advice from tax authority.
     * Taxpayer relied on incorrect guidance from municipality staff.
     */
    ERRONEOUS_ADVICE,
    
    /**
     * First-time penalty abatement (FPA).
     * Automatic approval if clean 3-year history (no prior penalties).
     */
    FIRST_TIME,
    
    /**
     * Other reasonable cause not covered by standard categories.
     * Requires detailed explanation.
     */
    OTHER
}
