package com.munitax.taxengine.domain.withholding;

/**
 * Status lifecycle for year-end W-1 to W-2/W-3 reconciliation.
 * Tracks reconciliation progress and discrepancy resolution (FR-009).
 */
public enum ReconciliationStatus {
    /**
     * Reconciliation has not been initiated for this tax year.
     */
    NOT_STARTED,
    
    /**
     * W-2 PDFs are being uploaded and processed.
     * Reconciliation calculation in progress.
     */
    IN_PROGRESS,
    
    /**
     * Variance detected between W-1 totals and W-2 totals exceeding threshold.
     * Requires business explanation or amended W-1 filing (FR-006, FR-008).
     */
    DISCREPANCY,
    
    /**
     * W-1 and W-2 totals match within acceptable threshold, or
     * discrepancy has been resolved with explanation.
     * Year-end reconciliation complete.
     */
    RECONCILED
}
