package com.munitax.taxengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO representing a single reconciliation issue found during W-1 filing reconciliation.
 * 
 * Reconciliation checks include:
 * - W-1 wages vs W-2 Box 1 (federal wages) mismatch
 * - W-1 local wages vs W-2 Box 18 (local wages) mismatch
 * - Withholding rate outside valid range (0-3.0%)
 * - Quarterly/period totals not matching cumulative
 * - Missing required filings
 * - Duplicate EIN filings for same period
 * - Late filings detected
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReconciliationIssue {
    
    /**
     * Unique identifier for this issue.
     */
    private UUID id;
    
    /**
     * Employer/Business ID this issue relates to.
     */
    private UUID employerId;
    
    /**
     * Tax year for the issue.
     */
    private Integer taxYear;
    
    /**
     * Filing period (e.g., "Q1", "Q2", "M01").
     */
    private String period;
    
    /**
     * Type of reconciliation issue.
     */
    private IssueType issueType;
    
    /**
     * Severity level of the issue.
     */
    private IssueSeverity severity;
    
    /**
     * Human-readable description of the issue.
     */
    private String description;
    
    /**
     * Expected value (e.g., W-2 reported wages).
     */
    private BigDecimal expectedValue;
    
    /**
     * Actual value (e.g., W-1 filed wages).
     */
    private BigDecimal actualValue;
    
    /**
     * Variance amount (actualValue - expectedValue).
     */
    private BigDecimal variance;
    
    /**
     * Variance percentage.
     */
    private BigDecimal variancePercentage;
    
    /**
     * Due date for the filing (if applicable).
     */
    private LocalDate dueDate;
    
    /**
     * Actual filing date (if applicable).
     */
    private LocalDate filingDate;
    
    /**
     * Recommended action to resolve the issue.
     */
    private String recommendedAction;
    
    /**
     * Whether this issue has been resolved.
     */
    @Builder.Default
    private Boolean resolved = false;
    
    /**
     * Types of reconciliation issues.
     */
    public enum IssueType {
        WAGE_MISMATCH_FEDERAL,
        WAGE_MISMATCH_LOCAL,
        WITHHOLDING_RATE_INVALID,
        CUMULATIVE_MISMATCH,
        MISSING_FILING,
        DUPLICATE_FILING,
        LATE_FILING
    }
    
    /**
     * Severity levels for issues.
     */
    public enum IssueSeverity {
        CRITICAL,  // Requires immediate attention
        HIGH,      // Should be resolved soon
        MEDIUM,    // Should be reviewed
        LOW        // Informational
    }
}
