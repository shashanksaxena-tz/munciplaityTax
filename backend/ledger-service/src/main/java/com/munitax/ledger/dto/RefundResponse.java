package com.munitax.ledger.dto;

import com.munitax.ledger.enums.RefundMethod;
import com.munitax.ledger.enums.RefundStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for refund response
 * T062: Create RefundResponse DTO
 * T059: Support refund approval workflow per FR-039
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundResponse {
    private UUID refundId;
    private UUID refundRequestId;
    private UUID filerId;
    private BigDecimal amount;
    private BigDecimal overpaymentAmount;  // T057: Overpayment detection per FR-036
    private RefundStatus status;           // T059: Approval workflow per FR-039
    private RefundMethod refundMethod;
    private String reason;
    private UUID journalEntryId;           // Filer journal entry ID
    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;      // T059: Approval tracking
    private LocalDateTime issuedAt;
    private UUID requestedBy;
    private UUID approvedBy;               // T059: Who approved the refund
    private UUID issuedBy;
    private String confirmationNumber;     // T042: FR-042 - Transaction ID
    private String message;
}
