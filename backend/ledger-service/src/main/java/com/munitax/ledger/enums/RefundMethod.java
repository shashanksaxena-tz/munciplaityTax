package com.munitax.ledger.enums;

/**
 * Refund method enum
 * T060: Support refund methods (ACH, Check, Wire) per FR-041
 */
public enum RefundMethod {
    ACH,          // Electronic ACH transfer
    CHECK,        // Physical check
    WIRE          // Wire transfer
}
