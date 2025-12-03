package com.munitax.ledger.model;

import com.munitax.ledger.enums.PaymentMethod;
import com.munitax.ledger.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID transactionId;
    
    @Column(nullable = false)
    private UUID paymentId;
    
    @Column(nullable = false)
    private UUID filerId;
    
    @Column(nullable = false)
    private String tenantId;
    
    private String providerTransactionId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    
    @Column(nullable = false)
    private String currency = "USD";
    
    private String authorizationCode;
    
    private String failureReason;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    @Column(nullable = false)
    private Boolean isTestMode = false;
    
    // Payment details (encrypted in production)
    private String cardLast4;
    private String cardBrand;
    private String achLast4;
    
    private UUID journalEntryId;
}
