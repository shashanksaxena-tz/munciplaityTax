package com.munitax.taxengine.domain.penalty;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * JPA Entity for quarterly interest breakdown (embedded in Interest).
 * 
 * Functional Requirements:
 * - FR-029: Quarterly compounding (add accrued interest to principal each quarter)
 * - FR-031: Display interest breakdown by quarter
 * 
 * Multi-tenant Isolation: Constitution II
 * - All queries MUST filter by tenant_id
 * 
 * @see Interest
 */
@Entity
@Table(name = "quarterly_interests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuarterlyInterest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /**
     * Tenant ID for multi-tenant data isolation (Constitution II).
     */
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    /**
     * Reference to parent Interest entity.
     */
    @Column(name = "interest_id", nullable = false)
    private UUID interestId;
    
    /**
     * Quarter label (e.g., "Q1 2024", "Q2 2024").
     */
    @Column(name = "quarter", nullable = false, length = 10)
    private String quarter;
    
    /**
     * Start date of this quarter.
     */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    
    /**
     * End date of this quarter.
     */
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;
    
    /**
     * Number of days in this quarter.
     * Formula: (end_date - start_date + 1)
     */
    @Column(name = "days", nullable = false)
    private Integer days;
    
    /**
     * Principal balance at start of quarter (includes prior interest).
     * FR-029: Compounding means previous quarters' interest becomes new principal.
     */
    @Column(name = "beginning_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal beginningBalance;
    
    /**
     * Interest accrued during this quarter.
     * Formula: beginning_balance × annual_rate × days / 365
     */
    @Column(name = "interest_accrued", nullable = false, precision = 15, scale = 2)
    private BigDecimal interestAccrued;
    
    /**
     * Balance at end of quarter (beginning + interest).
     * Becomes next quarter's beginning_balance.
     */
    @Column(name = "ending_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal endingBalance;
    
    /**
     * Validate that ending balance equals beginning balance plus interest accrued.
     * 
     * @return true if ending = beginning + accrued (within 1 cent tolerance)
     */
    public boolean isBalanceValid() {
        BigDecimal calculated = beginningBalance.add(interestAccrued);
        return endingBalance.subtract(calculated).abs().compareTo(new BigDecimal("0.01")) < 0;
    }
    
    /**
     * Calculate daily interest rate for this quarter.
     * 
     * @param annualRate annual interest rate (e.g., 0.05 for 5%)
     * @return interest accrued divided by beginning balance and days
     */
    public BigDecimal getDailyRate(BigDecimal annualRate) {
        if (beginningBalance.compareTo(BigDecimal.ZERO) == 0 || days == 0) {
            return BigDecimal.ZERO;
        }
        // Daily rate = annual rate / 365
        return annualRate.divide(new BigDecimal("365"), 8, java.math.RoundingMode.HALF_UP);
    }
}
