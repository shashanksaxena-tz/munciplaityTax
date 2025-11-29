package com.munitax.taxengine.domain.nol;

/**
 * Jurisdiction Enum - Represents the tax jurisdiction for NOL tracking.
 * 
 * Functional Requirements:
 * - FR-032: Calculate state NOL separately from federal NOL
 * - FR-035: Display separate NOL schedules for federal and state
 * 
 * @see NOL
 */
public enum Jurisdiction {
    /**
     * Federal level NOL (total business loss)
     */
    FEDERAL,
    
    /**
     * Ohio state level NOL (apportioned loss)
     */
    STATE_OHIO,
    
    /**
     * Municipality level NOL (further apportioned)
     */
    MUNICIPALITY
}
