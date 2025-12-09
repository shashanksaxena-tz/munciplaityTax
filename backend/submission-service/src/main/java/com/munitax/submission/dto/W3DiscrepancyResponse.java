package com.munitax.submission.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for W-3 reconciliation discrepancy details.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class W3DiscrepancyResponse {
    
    /**
     * Total tax from W-1 filings.
     */
    private BigDecimal totalW1Tax;
    
    /**
     * Total tax from W-2 forms.
     */
    private BigDecimal totalW2Tax;
    
    /**
     * Discrepancy amount (W1 - W2).
     */
    private BigDecimal discrepancy;
    
    /**
     * Percentage difference.
     */
    private BigDecimal discrepancyPercentage;
    
    /**
     * Status: BALANCED or UNBALANCED.
     */
    private String status;
    
    /**
     * Number of W-1 filings included.
     */
    private Integer w1FilingCount;
    
    /**
     * Number of W-2 forms included.
     */
    private Integer w2FormCount;
    
    /**
     * Missing W-1 filings (expected vs actual).
     */
    private Integer missingW1Filings;
    
    /**
     * Expected number of W-1 filings based on filing frequency.
     */
    private Integer expectedW1Filings;
    
    /**
     * Late filing penalty amount.
     */
    private BigDecimal lateFilingPenalty;
    
    /**
     * Missing filing penalty amount.
     */
    private BigDecimal missingFilingPenalty;
    
    /**
     * Total penalties.
     */
    private BigDecimal totalPenalties;
    
    /**
     * Human-readable description of issues found.
     */
    private String description;
    
    /**
     * Recommended actions to resolve discrepancies.
     */
    private String recommendedAction;
}
