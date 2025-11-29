package com.munitax.taxengine.domain.penalty;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity for tracking payment allocations to tax, penalties, and interest.
 * 
 * Functional Requirements:
 * - FR-040 to FR-043: Payment allocation order (Tax → Penalties → Interest)
 * - FR-041: IRS standard allocation order
 * 
 * Multi-tenant Isolation: Constitution II
 * - All queries MUST filter by tenant_id
 * 
 * Audit Trail: Constitution III
 * - created_at, created_by immutable
 * - All changes logged to PenaltyAuditLog
 */
@Entity
@Table(name = "payment_allocations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentAllocation {
    
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
     * Date when payment was received.
     */
    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;
    
    /**
     * Total payment amount received.
     */
    @Column(name = "payment_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal paymentAmount;
    
    /**
     * Amount applied to tax principal.
     * FR-041: First priority in allocation order.
     */
    @Column(name = "applied_to_tax", nullable = false, precision = 15, scale = 2)
    private BigDecimal appliedToTax;
    
    /**
     * Amount applied to penalties.
     * FR-041: Second priority in allocation order.
     */
    @Column(name = "applied_to_penalties", nullable = false, precision = 15, scale = 2)
    private BigDecimal appliedToPenalties;
    
    /**
     * Amount applied to interest.
     * FR-041: Third priority in allocation order.
     */
    @Column(name = "applied_to_interest", nullable = false, precision = 15, scale = 2)
    private BigDecimal appliedToInterest;
    
    /**
     * Remaining tax balance after this payment.
     */
    @Column(name = "remaining_tax_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal remainingTaxBalance;
    
    /**
     * Remaining penalty balance after this payment.
     */
    @Column(name = "remaining_penalty_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal remainingPenaltyBalance;
    
    /**
     * Remaining interest balance after this payment.
     */
    @Column(name = "remaining_interest_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal remainingInterestBalance;
    
    /**
     * Allocation strategy (always TAX_FIRST per IRS standard).
     * FR-041: Tax principal → Penalties → Interest
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "allocation_order", nullable = false, length = 20)
    @Builder.Default
    private AllocationOrder allocationOrder = AllocationOrder.TAX_FIRST;
    
    /**
     * Audit trail: When allocation was created (immutable).
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    /**
     * Audit trail: Who created the allocation (user ID or SYSTEM).
     */
    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;
    
    /**
     * Validate that payment amount equals sum of allocations.
     * 
     * @return true if payment = tax + penalties + interest (within 1 cent)
     */
    public boolean isAllocationValid() {
        BigDecimal total = appliedToTax.add(appliedToPenalties).add(appliedToInterest);
        return paymentAmount.subtract(total).abs().compareTo(new BigDecimal("0.01")) < 0;
    }
    
    /**
     * Check if payment fully covered tax balance.
     * 
     * @return true if remaining_tax_balance is zero
     */
    public boolean isTaxFullyPaid() {
        return remainingTaxBalance.compareTo(BigDecimal.ZERO) == 0;
    }
    
    /**
     * Check if payment fully covered all balances.
     * 
     * @return true if all remaining balances are zero
     */
    public boolean isFullyPaid() {
        return remainingTaxBalance.compareTo(BigDecimal.ZERO) == 0
            && remainingPenaltyBalance.compareTo(BigDecimal.ZERO) == 0
            && remainingInterestBalance.compareTo(BigDecimal.ZERO) == 0;
    }
    
    /**
     * Calculate total remaining balance across all categories.
     * 
     * @return sum of remaining tax, penalty, and interest
     */
    public BigDecimal getTotalRemainingBalance() {
        return remainingTaxBalance.add(remainingPenaltyBalance).add(remainingInterestBalance);
    }
}
