package com.munitax.taxengine.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for Payment Allocation request.
 * 
 * Functional Requirements:
 * - FR-040 to FR-043: Payment allocation order (Tax → Penalties → Interest)
 * - FR-041: IRS standard allocation order
 * 
 * @see com.munitax.taxengine.domain.penalty.PaymentAllocation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentAllocationRequest {
    
    /**
     * Tenant ID for multi-tenant isolation.
     */
    @NotNull(message = "Tenant ID is required")
    private UUID tenantId;
    
    /**
     * Tax return ID for which payment is being allocated.
     */
    @NotNull(message = "Return ID is required")
    private UUID returnId;
    
    /**
     * Date when payment was received.
     */
    @NotNull(message = "Payment date is required")
    private LocalDate paymentDate;
    
    /**
     * Total payment amount received.
     */
    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "0.01", message = "Payment amount must be positive")
    private BigDecimal paymentAmount;
    
    /**
     * Current outstanding tax balance (before this payment).
     */
    @NotNull(message = "Current tax balance is required")
    @DecimalMin(value = "0.00", message = "Tax balance must be non-negative")
    private BigDecimal currentTaxBalance;
    
    /**
     * Current outstanding penalty balance (before this payment).
     */
    @NotNull(message = "Current penalty balance is required")
    @DecimalMin(value = "0.00", message = "Penalty balance must be non-negative")
    private BigDecimal currentPenaltyBalance;
    
    /**
     * Current outstanding interest balance (before this payment).
     */
    @NotNull(message = "Current interest balance is required")
    @DecimalMin(value = "0.00", message = "Interest balance must be non-negative")
    private BigDecimal currentInterestBalance;
    
    /**
     * User ID who recorded the payment (for audit trail).
     */
    @NotNull(message = "Created by is required")
    private UUID createdBy;
    
    /**
     * Payment allocation order (default: TAX_FIRST per IRS standard).
     * Valid values: TAX_FIRST
     */
    @Builder.Default
    private String allocationOrder = "TAX_FIRST";
    
    /**
     * Payment method (for reference).
     * Examples: CHECK, ACH, CREDIT_CARD, CASH
     */
    private String paymentMethod;
    
    /**
     * Payment reference number (check number, transaction ID, etc.).
     */
    private String paymentReference;
    
    /**
     * Whether to recalculate penalties and interest after payment.
     * If true, recalculates based on new remaining balance.
     */
    @Builder.Default
    private Boolean recalculateAfterPayment = false;
}
