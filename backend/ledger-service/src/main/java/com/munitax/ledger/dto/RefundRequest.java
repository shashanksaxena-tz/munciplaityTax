package com.munitax.ledger.dto;

import com.munitax.ledger.enums.RefundMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for refund request
 * T061: Create RefundRequest DTO
 * T060: Support refund method selection per FR-041
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequest {
    private UUID filerId;
    private UUID tenantId;
    private BigDecimal amount;
    private String reason;
    private RefundMethod refundMethod;  // T060: FR-041 - ACH, Check, Wire
    private UUID requestedBy;
    
    // ACH-specific fields
    private String achRouting;
    private String achAccount;
    private String achAccountHolderName;
    
    // Check-specific fields
    private String checkMailingAddress;
    private String checkPayeeName;
    
    // Wire-specific fields
    private String wireBankName;
    private String wireRoutingNumber;
    private String wireAccountNumber;
    private String wireSwiftCode;
}
