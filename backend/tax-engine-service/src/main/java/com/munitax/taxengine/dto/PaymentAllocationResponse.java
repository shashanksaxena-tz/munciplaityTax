package com.munitax.taxengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for Payment Allocation response.
 * 
 * Functional Requirements:
 * - FR-040 to FR-043: Payment allocation order (Tax → Penalties → Interest)
 * - FR-041: IRS standard allocation order
 * - FR-042: Display allocation breakdown
 * 
 * @see com.munitax.taxengine.domain.penalty.PaymentAllocation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentAllocationResponse {
    
    /**
     * Payment allocation ID (UUID).
     */
    private String allocationId;
    
    /**
     * Tax return ID.
     */
    private String returnId;
    
    /**
     * Date when payment was received.
     */
    private LocalDate paymentDate;
    
    /**
     * Total payment amount received.
     */
    private BigDecimal paymentAmount;
    
    /**
     * Amount applied to tax principal.
     */
    private BigDecimal appliedToTax;
    
    /**
     * Amount applied to penalties.
     */
    private BigDecimal appliedToPenalties;
    
    /**
     * Amount applied to interest.
     */
    private BigDecimal appliedToInterest;
    
    /**
     * Remaining tax balance after this payment.
     */
    private BigDecimal remainingTaxBalance;
    
    /**
     * Remaining penalty balance after this payment.
     */
    private BigDecimal remainingPenaltyBalance;
    
    /**
     * Remaining interest balance after this payment.
     */
    private BigDecimal remainingInterestBalance;
    
    /**
     * Total remaining balance (tax + penalties + interest).
     */
    private BigDecimal totalRemainingBalance;
    
    /**
     * Allocation strategy used (always TAX_FIRST).
     */
    @Builder.Default
    private String allocationOrder = "TAX_FIRST";
    
    /**
     * Whether all balances are fully paid.
     */
    private Boolean fullyPaid;
    
    /**
     * Whether tax balance is fully paid.
     */
    private Boolean taxFullyPaid;
    
    /**
     * When allocation was created.
     */
    private LocalDateTime createdAt;
    
    /**
     * Allocation breakdown explanation.
     */
    private String allocationExplanation;
    
    /**
     * Payment method (if provided).
     */
    private String paymentMethod;
    
    /**
     * Payment reference (if provided).
     */
    private String paymentReference;
    
    /**
     * Generate allocation explanation.
     * 
     * @return human-readable explanation of allocation
     */
    public String generateAllocationExplanation() {
        StringBuilder explanation = new StringBuilder();
        explanation.append(String.format("Payment of $%,.2f allocated as follows:\n", paymentAmount));
        
        if (appliedToTax.compareTo(BigDecimal.ZERO) > 0) {
            explanation.append(String.format("  1. Tax Principal: $%,.2f", appliedToTax));
            if (taxFullyPaid) {
                explanation.append(" (PAID IN FULL)");
            }
            explanation.append("\n");
        }
        
        if (appliedToPenalties.compareTo(BigDecimal.ZERO) > 0) {
            explanation.append(String.format("  2. Penalties: $%,.2f", appliedToPenalties));
            if (remainingPenaltyBalance.compareTo(BigDecimal.ZERO) == 0) {
                explanation.append(" (PAID IN FULL)");
            }
            explanation.append("\n");
        }
        
        if (appliedToInterest.compareTo(BigDecimal.ZERO) > 0) {
            explanation.append(String.format("  3. Interest: $%,.2f", appliedToInterest));
            if (remainingInterestBalance.compareTo(BigDecimal.ZERO) == 0) {
                explanation.append(" (PAID IN FULL)");
            }
            explanation.append("\n");
        }
        
        if (fullyPaid) {
            explanation.append("\nAccount is PAID IN FULL. No remaining balance.");
        } else {
            explanation.append(String.format("\nRemaining Balance: $%,.2f", totalRemainingBalance));
            explanation.append(String.format("\n  - Tax: $%,.2f", remainingTaxBalance));
            explanation.append(String.format("\n  - Penalties: $%,.2f", remainingPenaltyBalance));
            explanation.append(String.format("\n  - Interest: $%,.2f", remainingInterestBalance));
        }
        
        return explanation.toString();
    }
}
