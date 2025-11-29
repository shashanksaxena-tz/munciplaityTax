package com.munitax.taxengine.domain.nol;

/**
 * Entity Type Enum - Represents the type of entity for NOL tracking.
 * 
 * Functional Requirements:
 * - FR-027: Track entity type for each NOL
 * - FR-028: C-Corps retain NOL at entity level
 * - FR-029: S-Corps pass NOL to shareholders
 * - FR-030: Partnerships allocate NOL to partners
 * 
 * @see NOL
 */
public enum EntityType {
    /**
     * C-Corporation: NOL retained at entity level, not passed to shareholders
     */
    C_CORP,
    
    /**
     * S-Corporation: NOL passed to shareholders pro-rata by ownership %
     */
    S_CORP,
    
    /**
     * Partnership: NOL allocated to partners per partnership agreement
     */
    PARTNERSHIP,
    
    /**
     * Sole Proprietorship: NOL on individual return (Schedule C)
     */
    SOLE_PROP
}
