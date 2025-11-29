package com.munitax.taxengine.domain.withholding;

/**
 * Enum for payment status tracking in withholding payment processing.
 * 
 * Functional Requirements:
 * - FR-020: Payment tracking integration with payment gateway
 * 
 * Payment Status Flow:
 * 1. PENDING: Payment initiated but not yet processed by gateway
 * 2. COMPLETED: Payment successfully processed and funds received
 * 3. FAILED: Payment failed due to insufficient funds, expired card, etc.
 * 4. REFUNDED: Payment was completed but later refunded
 */
public enum PaymentStatus {
    /**
     * Payment initiated but not yet processed by payment gateway.
     * Business owner should see "Payment processing..." message.
     */
    PENDING,
    
    /**
     * Payment successfully processed and funds received.
     * W-1 filing status should be updated to PAID.
     */
    COMPLETED,
    
    /**
     * Payment failed due to gateway error.
     * Common reasons: insufficient funds, expired card, invalid account.
     * Business owner should be prompted to retry with different payment method.
     */
    FAILED,
    
    /**
     * Payment was completed but later refunded.
     * Reasons: overpayment, duplicate payment, amended filing correction.
     * W-1 filing status may revert to FILED or OVERDUE.
     */
    REFUNDED
}
