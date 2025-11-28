package com.munitax.taxengine.domain.withholding;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * CumulativeWithholdingTotals Entity - Year-to-date cumulative totals for business withholding.
 * 
 * Purpose:
 * Cached cumulative totals for performance (Research R2 - Option B: Cached table).
 * Event-driven updates on W-1 filing provide O(1) dashboard queries vs O(n) SUM() queries.
 * 
 * Functional Requirements:
 * - FR-002: Calculate cumulative YTD totals automatically on new W-1 filing
 * - FR-004: Validate W-1 wage amounts against filing frequency patterns
 * - FR-005: Project annual totals and display "on track" indicators
 * 
 * Update Strategy:
 * - INSERT: When first W-1 filed for business + tax_year
 * - UPDATE: When subsequent W-1 filed (triggered by W1FiledEvent)
 * - CASCADE UPDATE: When W-1 amended (Research R3 - Batch UPDATE all periods after amendment)
 * 
 * Performance:
 * - Success Criteria: Dashboard displays cumulative totals within 2 seconds
 * - Benchmark: <80ms query time (Research R2)
 * - Redis cache: 5-minute TTL with fallback to database
 * 
 * @see W1Filing
 */
@Entity
@Table(name = "cumulative_withholding_totals", schema = "dublin")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CumulativeWithholdingTotals {
    
    /**
     * Unique identifier for this cumulative record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /**
     * Multi-tenant isolation.
     */
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    /**
     * Business profile this cumulative total belongs to.
     */
    @Column(name = "business_id", nullable = false)
    private UUID businessId;
    
    /**
     * Tax year for cumulative totals (e.g., 2024).
     * UNIQUE constraint: One cumulative record per business + tax_year.
     */
    @Column(name = "tax_year", nullable = false)
    private Integer taxYear;
    
    /**
     * Count of W-1 filings submitted for this tax year.
     * Example: 4 for quarterly filer, 12 for monthly filer.
     * CHECK constraint: periods_filed >= 0
     */
    @Column(name = "periods_filed", nullable = false)
    @Builder.Default
    private Integer periodsFiled = 0;
    
    /**
     * Sum of gross wages from all W-1 filings for tax year.
     * SELECT SUM(gross_wages) FROM w1_filings WHERE business_id = ? AND tax_year = ?
     * CHECK constraint: cumulative_wages_ytd >= 0
     */
    @Column(name = "cumulative_wages_ytd", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal cumulativeWagesYtd = BigDecimal.ZERO;
    
    /**
     * Sum of tax due from all W-1 filings for tax year.
     * CHECK constraint: cumulative_tax_ytd >= 0
     */
    @Column(name = "cumulative_tax_ytd", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal cumulativeTaxYtd = BigDecimal.ZERO;
    
    /**
     * Sum of adjustments from all W-1 filings (can be negative).
     */
    @Column(name = "cumulative_adjustments_ytd", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal cumulativeAdjustmentsYtd = BigDecimal.ZERO;
    
    /**
     * Most recent W-1 filing date for this business + tax year.
     * Used to determine staleness for self-healing job.
     */
    @Column(name = "last_filing_date")
    private LocalDateTime lastFilingDate;
    
    /**
     * Estimated annual wages from business registration (optional).
     * Used to calculate "on track" indicator (FR-004, FR-005).
     * Example: Business estimated $500,000 annual wages at registration.
     */
    @Column(name = "estimated_annual_wages", precision = 15, scale = 2)
    private BigDecimal estimatedAnnualWages;
    
    /**
     * Projected annual wages based on current run rate (FR-005).
     * Calculation: (cumulative_wages_ytd / periods_filed) × expected_periods_for_year
     * Example: $250,000 YTD after Q1+Q2 → projected $500,000 annual (4 quarters).
     */
    @Column(name = "projected_annual_wages", precision = 15, scale = 2)
    private BigDecimal projectedAnnualWages;
    
    /**
     * Is business on track to meet estimated annual wages? (FR-005)
     * True if within 15% of estimated wages based on current YTD progress.
     * Example: Q1+Q2 cumulative $240,000 vs $250,000 expected (50% of $500K estimate) = true
     * Example: Q1+Q2 cumulative $100,000 vs $250,000 expected (50% of $500K estimate) = false
     */
    @Column(name = "on_track_indicator")
    @Builder.Default
    private Boolean onTrackIndicator = true;
    
    /**
     * Record creation timestamp.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    /**
     * Last update timestamp.
     * Updated on every W-1 filing or amendment.
     */
    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
