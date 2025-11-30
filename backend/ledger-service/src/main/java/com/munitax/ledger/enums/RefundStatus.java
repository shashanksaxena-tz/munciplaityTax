package com.munitax.ledger.enums;

/**
 * Refund status enum
 * T060: Support refund methods (ACH, Check, Wire)
 */
public enum RefundStatus {
    REQUESTED,    // Refund request submitted
    APPROVED,     // Refund request approved
    REJECTED,     // Refund request rejected
    ISSUED,       // Refund has been issued
    COMPLETED     // Refund completed and received by filer
}
