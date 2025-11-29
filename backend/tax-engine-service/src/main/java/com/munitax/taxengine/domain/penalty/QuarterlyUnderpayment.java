package com.munitax.taxengine.domain.penalty;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * JPA Entity for quarterly underpayment details (embedded in EstimatedTaxPenalty).
 * 
 * Functional Requirements:
 * - FR-022: Calculate underpayment per quarter (Required - Actual)
 * - FR-024: Apply overpayments from later quarters to earlier underpayments
 * 
 * Multi-tenant Isolation: Constitution II
 * - All queries MUST filter by tenant_id
 * 
 * @see EstimatedTaxPenalty
 */
@Entity
@Table(name = "quarterly_underpayments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuarterlyUnderpayment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /**
     * Tenant ID for multi-tenant data isolation (Constitution II).
     */
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    /**
     * Reference to parent EstimatedTaxPenalty.
     */
    @Column(name = "estimated_penalty_id", nullable = false)
    private UUID estimatedPenaltyId;
    
    /**
     * Quarter identification: Q1, Q2, Q3, Q4.
     * Q1: Apr 15, Q2: Jun 15, Q3: Sep 15, Q4: Jan 15 (next year)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "quarter", nullable = false, length = 2)
    private Quarter quarter;
    
    /**
     * Due date for this quarter's estimated payment.
     */
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;
    
    /**
     * Required payment for this quarter (typically 25% of annual tax).
     */
    @Column(name = "required_payment", nullable = false, precision = 15, scale = 2)
    private BigDecimal requiredPayment;
    
    /**
     * Actual payment made for this quarter.
     */
    @Column(name = "actual_payment", nullable = false, precision = 15, scale = 2)
    private BigDecimal actualPayment;
    
    /**
     * Underpayment amount (required - actual).
     * Negative value indicates overpayment.
     */
    @Column(name = "underpayment", nullable = false, precision = 15, scale = 2)
    private BigDecimal underpayment;
    
    /**
     * Number of quarters from due date to filing date.
     * Used to calculate penalty duration.
     */
    @Column(name = "quarters_unpaid", nullable = false)
    private Integer quartersUnpaid;
    
    /**
     * Penalty rate per quarter (typically derived from federal short-term rate).
     */
    @Column(name = "penalty_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal penaltyRate;
    
    /**
     * Calculated penalty for this quarter.
     * Formula: underpayment × penalty_rate × quarters_unpaid
     */
    @Column(name = "penalty_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal penaltyAmount;
    
    /**
     * Calculate if this quarter has an underpayment (not overpayment).
     * 
     * @return true if underpayment > 0
     */
    public boolean hasUnderpayment() {
        return underpayment.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Calculate if this quarter has an overpayment.
     * 
     * @return true if underpayment < 0
     */
    public boolean hasOverpayment() {
        return underpayment.compareTo(BigDecimal.ZERO) < 0;
    }
    
    /**
     * Get absolute value of overpayment (for applying to other quarters).
     * 
     * @return absolute value of underpayment if negative, otherwise zero
     */
    public BigDecimal getOverpaymentAmount() {
        return hasOverpayment() ? underpayment.abs() : BigDecimal.ZERO;
    }
}
