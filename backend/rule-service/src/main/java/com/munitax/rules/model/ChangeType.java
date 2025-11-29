package com.munitax.rules.model;

/**
 * Types of changes that can be made to tax rules.
 * Used in audit trail to track rule lifecycle.
 */
public enum ChangeType {
    /**
     * New rule created
     */
    CREATE,
    
    /**
     * Existing rule modified (value, effective date, etc.)
     */
    UPDATE,
    
    /**
     * Rule soft-deleted (status changed to VOIDED)
     */
    DELETE,
    
    /**
     * Rule approved by authorized user
     */
    APPROVE,
    
    /**
     * Rule rejected by authorized user
     */
    REJECT,
    
    /**
     * Rule voided (withdrawn before effective date)
     */
    VOID,
    
    /**
     * Rule rolled back to previous version (emergency undo)
     */
    ROLLBACK
}
