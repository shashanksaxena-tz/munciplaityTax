package com.munitax.taxengine.domain.penalty;

/**
 * Types of penalties that can be abated.
 * 
 * Functional Requirements:
 * - FR-034: Penalty abatement form with penalty type selection
 */
public enum AbatementType {
    /**
     * Abate late filing penalty only.
     */
    LATE_FILING,
    
    /**
     * Abate late payment penalty only.
     */
    LATE_PAYMENT,
    
    /**
     * Abate estimated tax underpayment penalty only.
     */
    ESTIMATED,
    
    /**
     * Abate all penalties assessed on this return.
     */
    ALL
}
