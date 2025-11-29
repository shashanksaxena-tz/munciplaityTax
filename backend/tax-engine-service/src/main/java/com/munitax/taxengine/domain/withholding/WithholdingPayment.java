package com.munitax.taxengine.domain.withholding;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * WithholdingPayment Entity - Tracks payments made against W-1 withholding filings.
 * 
 * Functional Requirements:
 * - FR-020: Payment tracking integration with payment gateway
 * - FR-012: Underpayment penalty calculation based on payment status
 * 
 * Constitution Compliance:
 * - Principle II: Multi-tenant data isolation via tenant_id
 * - Principle V: Security - transaction IDs never exposed in logs
 * 
 * @see W1Filing
 * @see PaymentMethod
 * @see PaymentStatus
 */
@Entity
@Table(name = "withholding_payments", schema = "dublin")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WithholdingPayment {
    
    /**
     * Unique identifier for this payment.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /**
     * Multi-tenant isolation - tenant owning this payment.
     * MUST be set on all operations (Constitution Principle II).
     */
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    /**
     * W-1 filing ID that this payment is applied to.
     * Foreign key to w1_filings table.
     */
    @Column(name = "w1_filing_id", nullable = false)
    private UUID w1FilingId;
    
    /**
     * Timestamp when payment was received/processed.
     */
    @Column(name = "payment_date", nullable = false)
    private LocalDateTime paymentDate;
    
    /**
     * Amount paid in this transaction.
     * CHECK constraint: payment_amount > 0
     */
    @Column(name = "payment_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal paymentAmount;
    
    /**
     * Payment method used.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 50)
    private PaymentMethod paymentMethod;
    
    /**
     * Payment gateway transaction identifier.
     * Used for reconciliation with payment system.
     */
    @Column(name = "transaction_id", nullable = false, length = 100)
    private String transactionId;
    
    /**
     * User-facing confirmation number.
     * Displayed to business owner for their records.
     */
    @Column(name = "confirmation_number", nullable = false, length = 50)
    private String confirmationNumber;
    
    /**
     * Payment status (PENDING, COMPLETED, FAILED, REFUNDED).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;
    
    /**
     * Reason for payment failure (if status = FAILED).
     * Populated by payment gateway error message.
     */
    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;
    
    /**
     * Immutable timestamp when payment record was created.
     * Constitution Principle III: Audit trail immutability.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}
