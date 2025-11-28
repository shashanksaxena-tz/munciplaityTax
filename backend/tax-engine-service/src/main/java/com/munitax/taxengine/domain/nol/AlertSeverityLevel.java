package com.munitax.taxengine.domain.nol;

/**
 * Alert Severity Level Enum - Indicates urgency of NOL expiration alerts.
 * 
 * Functional Requirements:
 * - FR-024: Alert user of expiring NOLs with severity levels
 * 
 * @see NOLExpirationAlert
 */
public enum AlertSeverityLevel {
    /**
     * NOL expiring within 1 year - immediate action required
     */
    CRITICAL,
    
    /**
     * NOL expiring within 1-2 years - planning needed
     */
    WARNING,
    
    /**
     * NOL expiring within 2-3 years - informational
     */
    INFO
}
