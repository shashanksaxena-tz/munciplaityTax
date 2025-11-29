package com.munitax.ledger.dto;

import com.munitax.ledger.enums.PaymentMethod;
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
public class PaymentReceipt {
    private UUID paymentId;
    private UUID transactionId;
    private String receiptNumber;
    private String providerTransactionId;
    private String authorizationCode;
    private BigDecimal amount;
    private String currency;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private LocalDateTime paymentDate;
    private UUID filerId;
    private UUID tenantId;
    private String description;
    private UUID journalEntryId;
    private String journalEntryNumber;
    private boolean testMode;
}
