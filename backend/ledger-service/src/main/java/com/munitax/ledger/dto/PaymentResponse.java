package com.munitax.ledger.dto;

import com.munitax.ledger.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private UUID transactionId;
    private PaymentStatus status;
    private BigDecimal amount;
    private String providerTransactionId;
    private String authorizationCode;
    private String failureReason;
    private LocalDateTime timestamp;
    private boolean isTestMode;
    private String receiptNumber;
    private UUID journalEntryId;
}
