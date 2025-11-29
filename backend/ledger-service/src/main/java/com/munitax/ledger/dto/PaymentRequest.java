package com.munitax.ledger.dto;

import com.munitax.ledger.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
    private UUID filerId;
    private UUID tenantId;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private String description;
    
    // Credit Card fields
    private String cardNumber;
    private String cardExpiration;
    private String cardCvv;
    private String cardholderName;
    private Integer expirationMonth;
    private Integer expirationYear;
    private String cvv;
    
    // ACH fields
    private String achRouting;
    private String achAccount;
    
    // Check/Wire fields
    private String checkNumber;
    private String wireConfirmation;
    
    // Allocation (optional)
    private PaymentAllocation allocation;
}
