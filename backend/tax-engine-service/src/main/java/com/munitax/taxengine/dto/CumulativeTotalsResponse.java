package com.munitax.taxengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for Cumulative Withholding Totals response.
 * 
 * Functional Requirements:
 * - FR-002: Display cumulative year-to-date totals
 * - FR-004: Project annual totals based on run rate
 * - FR-005: On-track indicator for business filing compliance
 * 
 * Success Criteria:
 * - Business owners can view cumulative totals within 2 seconds
 * 
 * @see com.munitax.taxengine.domain.withholding.CumulativeWithholdingTotals
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CumulativeTotalsResponse {
    
    /**
     * Tax year for these cumulative totals (e.g., 2024).
     */
    private Integer taxYear;
    
    /**
     * Number of W-1 filings submitted for this year.
     */
    private Integer periodsFiled;
    
    /**
     * Cumulative gross wages year-to-date.
     * Sum of all W-1 gross wages for the year.
     */
    private BigDecimal cumulativeWagesYtd;
    
    /**
     * Cumulative tax withheld year-to-date.
     * Sum of all W-1 tax due for the year.
     */
    private BigDecimal cumulativeTaxYtd;
    
    /**
     * Cumulative adjustments year-to-date.
     * Sum of all adjustments (credits, overpayments).
     */
    private BigDecimal cumulativeAdjustmentsYtd;
    
    /**
     * Date of most recent W-1 filing.
     */
    private LocalDateTime lastFilingDate;
    
    /**
     * Estimated annual wages from business registration.
     * Null if business did not provide estimate during registration.
     */
    private BigDecimal estimatedAnnualWages;
    
    /**
     * Projected annual wages based on current run rate.
     * Formula: (cumulativeWagesYtd / days elapsed) × 365
     * Used for "on track" calculation (FR-004).
     */
    private BigDecimal projectedAnnualWages;
    
    /**
     * On-track indicator: Are cumulative wages within 15% of estimated annual pace?
     * - TRUE: On track (cumulative wages ± 15% of expected at this point in year)
     * - FALSE: Off track (significantly higher or lower than expected)
     * 
     * Displayed as:
     * - ✓ "On track" (green) if TRUE
     * - ⚠ "Behind pace" or "Ahead of pace" (yellow) if FALSE
     */
    private Boolean onTrackIndicator;
    
    /**
     * Human-readable on-track message for UI display.
     * Examples:
     * - "On track - 67% of estimated annual wages through 50% of year"
     * - "Behind pace - 40% of estimated wages through 50% of year"
     * - "Ahead of pace - 75% of estimated wages through 50% of year"
     */
    private String onTrackMessage;
    
    /**
     * Last time cumulative totals were recalculated.
     * Used for cache invalidation and debugging.
     */
    private LocalDateTime updatedAt;
}
