package com.munitax.taxengine.domain.penalty;

/**
 * Quarters for estimated tax payments.
 * 
 * Functional Requirements:
 * - FR-020: Quarterly payment schedule for estimated taxes
 */
public enum Quarter {
    /**
     * Quarter 1: January 1 - March 31, due April 15.
     */
    Q1,
    
    /**
     * Quarter 2: April 1 - May 31, due June 15.
     */
    Q2,
    
    /**
     * Quarter 3: June 1 - August 31, due September 15.
     */
    Q3,
    
    /**
     * Quarter 4: September 1 - December 31, due January 15 (next year).
     */
    Q4
}
