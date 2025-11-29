package com.munitax.taxengine.domain.withholding;

/**
 * Enum for reasons why a W-2 was ignored during reconciliation.
 * 
 * Functional Requirements:
 * - Constitution IV: AI Transparency & Explainability
 * - Research R1: Ignored W-2 Detection Logic
 * 
 * Ignored Reasons:
 * - WRONG_EIN: Employer EIN does not match business profile EIN
 * - DUPLICATE: Same employee SSN appears in multiple uploaded W-2s
 * - EXTRACTION_ERROR: AI extraction service failed to parse W-2 data
 * - INCOMPLETE_DATA: W-2 missing required fields (Box 18, Box 19)
 */
public enum IgnoredW2Reason {
    /**
     * Employer EIN from W-2 Box b does not match business profile EIN.
     * 
     * Action Required: Business owner should verify this is their W-2 or remove it.
     * Common causes: Uploaded W-2 from wrong business, employee job change.
     */
    WRONG_EIN,
    
    /**
     * Same employee SSN (last 4 digits) appears in multiple uploaded W-2s.
     * 
     * Action Required: Review W-2s to determine which is correct.
     * Common causes: Re-uploaded corrected W-2, employee job change mid-year.
     */
    DUPLICATE,
    
    /**
     * AI extraction service (Gemini) failed to extract data from W-2 PDF.
     * 
     * Action Required: Re-upload W-2 with better scan quality or manually enter data.
     * Common causes: Low-resolution PDF, corrupted file, unsupported format.
     */
    EXTRACTION_ERROR,
    
    /**
     * W-2 is missing required fields for municipal withholding reconciliation.
     * 
     * Action Required: Re-upload complete W-2 or manually enter missing data.
     * Required fields: Box 18 (Local wages), Box 19 (Local tax withheld).
     */
    INCOMPLETE_DATA
}
