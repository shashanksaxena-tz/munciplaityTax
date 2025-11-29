package com.munitax.taxengine.domain.penalty;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA Entity for interest calculations on unpaid tax.
 * 
 * Functional Requirements:
 * - FR-027 to FR-032: Interest calculation with quarterly compounding
 * - FR-028: Interest rate retrieved from rule-engine-service (federal short-term rate + 3%)
 * 
 * Multi-tenant Isolation: Constitution II
 * - All queries MUST filter by tenant_id
 * 
 * Audit Trail: Constitution III
 * - created_at, created_by immutable
 * - All changes logged to PenaltyAuditLog
 */
@Entity
@Table(name = "interests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Interest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /**
     * Tenant ID for multi-tenant data isolation (Constitution II).
     */
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    /**
     * Associated tax return.
     * Foreign key to tax_returns table.
     */
    @Column(name = "return_id", nullable = false)
    private UUID returnId;
    
    /**
     * Original due date for the tax return.
     */
    @Column(name = "tax_due_date", nullable = false)
    private LocalDate taxDueDate;
    
    /**
     * Amount of unpaid tax subject to interest.
     */
    @Column(name = "unpaid_tax_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal unpaidTaxAmount;
    
    /**
     * Annual interest rate retrieved from rule-engine-service.
     * FR-028: Federal short-term rate + 3%, typically 3-8%.
     */
    @Column(name = "annual_interest_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal annualInterestRate;
    
    /**
     * Compounding frequency (always QUARTERLY per IRS standard).
     * FR-029: Interest compounds quarterly.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "compounding_frequency", nullable = false, length = 20)
    @Builder.Default
    private CompoundingFrequency compoundingFrequency = CompoundingFrequency.QUARTERLY;
    
    /**
     * Interest calculation start date (typically tax due date).
     */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    
    /**
     * Interest calculation end date (typically payment date or current date).
     */
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;
    
    /**
     * Total number of days in interest period.
     * Formula: end_date - start_date
     */
    @Column(name = "total_days", nullable = false)
    private Integer totalDays;
    
    /**
     * Total interest calculated across all quarters.
     * Sum of interest from quarterly_interests table.
     * FR-031: Display total interest breakdown by quarter.
     */
    @Column(name = "total_interest", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalInterest;
    
    /**
     * Quarterly interest breakdown (one-to-many relationship).
     * Not mapped as JPA relationship - accessed via repository queries.
     */
    @Transient
    @Builder.Default
    private List<QuarterlyInterest> quarterlyInterests = new ArrayList<>();
    
    /**
     * Audit trail: When interest was created (immutable).
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    /**
     * Audit trail: Who created the interest calculation (user ID or SYSTEM).
     */
    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;
    
    /**
     * Audit trail: Last modification timestamp.
     */
    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    /**
     * Calculate effective annual rate as a percentage.
     * 
     * @return annual rate as percentage (e.g., 5.0 for 5%)
     */
    public BigDecimal getAnnualRatePercentage() {
        return annualInterestRate.multiply(new BigDecimal("100"));
    }
    
    /**
     * Calculate quarterly interest rate.
     * FR-029: Quarterly compounding rate = annual rate / 4
     * 
     * @return quarterly rate
     */
    public BigDecimal getQuarterlyRate() {
        return annualInterestRate.divide(new BigDecimal("4"), 6, java.math.RoundingMode.HALF_UP);
    }
    
    /**
     * Validate that date range is correct.
     * 
     * @return true if end_date >= start_date
     */
    public boolean isDateRangeValid() {
        return !endDate.isBefore(startDate);
    }
}
