package com.munitax.taxengine.domain.withholding;

/**
 * Enum for payment methods used in withholding payment processing.
 * 
 * Functional Requirements:
 * - FR-020: Payment tracking integration with payment gateway
 * 
 * Payment Method Types:
 * - ACH: Automated Clearing House (direct bank transfer)
 * - CHECK: Paper check or electronic check
 * - CREDIT_CARD: Credit card payment
 * - WIRE_TRANSFER: Wire transfer (typically for large amounts)
 */
public enum PaymentMethod {
    /**
     * Automated Clearing House (ACH) electronic transfer.
     * Typical processing time: 1-3 business days.
     */
    ACH,
    
    /**
     * Paper check or electronic check payment.
     * Typical processing time: 5-7 business days.
     */
    CHECK,
    
    /**
     * Credit card payment (Visa, MasterCard, Amex, Discover).
     * Typical processing time: Immediate.
     * May incur processing fees.
     */
    CREDIT_CARD,
    
    /**
     * Wire transfer (typically for large amounts > $10,000).
     * Typical processing time: Same day or next business day.
     */
    WIRE_TRANSFER
}
