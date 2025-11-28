package com.munitax.taxengine.dto;

import com.munitax.taxengine.domain.withholding.FilingFrequency;
import com.munitax.taxengine.domain.withholding.W1FilingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for W-1 filing operations.
 * Includes filing details and cumulative totals for dashboard display.
 * 
 * Returned from:
 * - POST /api/v1/w1-filings (file new W-1)
 * - GET /api/v1/w1-filings/{id} (get filing details)
 * - POST /api/v1/w1-filings/{id}/amend (file amended W-1)
 * 
 * @see W1FilingRequest
 * @see com.munitax.taxengine.domain.withholding.W1Filing
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class W1FilingResponse {
    
    /**
     * Unique W-1 filing ID.
     */
    private UUID id;
    
    /**
     * Business profile ID.
     */
    private UUID businessId;
    
    /**
     * Tax year.
     */
    private Integer taxYear;
    
    /**
     * Filing frequency.
     */
    private FilingFrequency filingFrequency;
    
    /**
     * Period identifier (Q1, M01, etc.).
     */
    private String period;
    
    /**
     * Period date range.
     */
    private LocalDate periodStartDate;
    private LocalDate periodEndDate;
    
    /**
     * Calculated due date for this filing.
     */
    private LocalDate dueDate;
    
    /**
     * Actual filing timestamp.
     */
    private LocalDateTime filingDate;
    
    /**
     * Wage amounts.
     */
    private BigDecimal grossWages;
    private BigDecimal taxableWages;
    
    /**
     * Tax calculation.
     */
    private BigDecimal taxRate;
    private BigDecimal taxDue;
    
    /**
     * Adjustments and penalties.
     */
    private BigDecimal adjustments;
    private BigDecimal lateFilingPenalty;
    private BigDecimal underpaymentPenalty;
    
    /**
     * Total amount due = taxDue + adjustments + penalties.
     */
    private BigDecimal totalAmountDue;
    
    /**
     * Amendment information.
     */
    private Boolean isAmended;
    private UUID amendsFilingId; // ID only for API response
    private String amendmentReason;
    
    /**
     * Employee count.
     */
    private Integer employeeCount;
    
    /**
     * Current status.
     */
    private W1FilingStatus status;
    
    /**
     * Audit trail.
     */
    private LocalDateTime createdAt;
    private UUID createdBy;
    private LocalDateTime updatedAt;
    
    /**
     * Cumulative totals for this business + tax year (optional).
     * Included in response to POST /api/v1/w1-filings for dashboard display.
     * Success Criteria: Display cumulative totals within 2 seconds of filing (FR-002).
     */
    private CumulativeTotalsDTO cumulativeTotals;
    
    /**
     * Nested DTO for cumulative year-to-date totals.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CumulativeTotalsDTO {
        /**
         * Count of W-1 filings for tax year.
         */
        private Integer periodsFiled;
        
        /**
         * Sum of gross wages YTD.
         */
        private BigDecimal cumulativeWagesYtd;
        
        /**
         * Sum of tax due YTD.
         */
        private BigDecimal cumulativeTaxYtd;
        
        /**
         * Projected annual wages based on run rate (FR-005).
         */
        private BigDecimal projectedAnnualWages;
        
        /**
         * Is business on track to meet estimated wages? (FR-005)
         */
        private Boolean onTrackIndicator;
        
        /**
         * Human-readable explanation of on-track status.
         * Example: "On pace - 75% of year complete, 75% of estimated wages filed"
         * Example: "Behind pace - 50% of year complete, 30% of estimated wages filed"
         */
        private String onTrackExplanation;
    }
}
