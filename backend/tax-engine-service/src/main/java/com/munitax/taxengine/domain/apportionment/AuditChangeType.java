package com.munitax.taxengine.domain.apportionment;

/**
 * Type of audit log change for apportionment calculations.
 * Used to track all changes to Schedule Y and related entities.
 */
public enum AuditChangeType {
    
    /**
     * Sourcing method election changed (Finnigan ↔ Joyce, Throwback ↔ Throwout)
     */
    ELECTION_CHANGED("Election Changed"),
    
    /**
     * Factor recalculated (property, payroll, or sales factor)
     */
    FACTOR_RECALCULATED("Factor Recalculated"),
    
    /**
     * New sale transaction added
     */
    TRANSACTION_ADDED("Transaction Added"),
    
    /**
     * Existing sale transaction modified
     */
    TRANSACTION_MODIFIED("Transaction Modified"),
    
    /**
     * Nexus status changed in a state
     */
    NEXUS_CHANGED("Nexus Status Changed"),
    
    /**
     * Schedule Y filed (submitted to municipality)
     */
    SCHEDULE_FILED("Schedule Y Filed"),
    
    /**
     * Schedule Y amended (supersedes previous filing)
     */
    SCHEDULE_AMENDED("Schedule Y Amended"),
    
    /**
     * Manual calculation adjustment by auditor
     */
    CALCULATION_ADJUSTMENT("Calculation Adjusted");
    
    private final String displayName;
    
    AuditChangeType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Determines if this change type affects final apportionment percentage.
     */
    public boolean affectsApportionment() {
        return this == ELECTION_CHANGED 
            || this == FACTOR_RECALCULATED 
            || this == TRANSACTION_ADDED 
            || this == TRANSACTION_MODIFIED 
            || this == NEXUS_CHANGED
            || this == CALCULATION_ADJUSTMENT;
    }
    
    /**
     * Determines if this change type requires auditor approval.
     */
    public boolean requiresAuditorApproval() {
        return this == CALCULATION_ADJUSTMENT || this == SCHEDULE_AMENDED;
    }
}
