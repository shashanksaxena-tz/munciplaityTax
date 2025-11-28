package com.munitax.taxengine.domain.nol;

/**
 * NOL Ordering Method Enum - Determines how NOLs are prioritized for usage.
 * 
 * Functional Requirements:
 * - FR-022: Apply FIFO ordering by default (use oldest NOLs first)
 * - FR-025: Allow manual NOL utilization ordering override
 * 
 * @see NOLUsage
 */
public enum NOLOrderingMethod {
    /**
     * First In First Out: Use oldest NOLs first to avoid expiration
     * (Default and recommended method)
     */
    FIFO,
    
    /**
     * Manual override: User specifies which NOL vintage to use first
     * (Requires justification in override_reason field)
     */
    MANUAL_OVERRIDE
}
