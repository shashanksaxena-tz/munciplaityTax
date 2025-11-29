package com.munitax.taxengine.repository;

import com.munitax.taxengine.domain.withholding.WithholdingPayment;
import com.munitax.taxengine.domain.withholding.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for WithholdingPayment entity.
 * 
 * Functional Requirements:
 * - FR-020: Payment tracking integration with payment gateway
 * - FR-012: Underpayment penalty calculation based on payment history
 * 
 * Query Methods:
 * - Find payments by W-1 filing ID
 * - Find payments by transaction ID
 * - Find unpaid W-1 filings (no COMPLETED payment)
 * - Calculate aging of unpaid liabilities
 * 
 * @see WithholdingPayment
 */
@Repository
public interface WithholdingPaymentRepository extends JpaRepository<WithholdingPayment, UUID> {
    
    /**
     * Find all payments for a specific W-1 filing.
     * Ordered by payment date descending (most recent first).
     * 
     * @param w1FilingId W-1 filing ID
     * @return List of payments for this filing
     */
    List<WithholdingPayment> findByW1FilingIdOrderByPaymentDateDesc(UUID w1FilingId);
    
    /**
     * Find a payment by transaction ID (from payment gateway).
     * Used for payment gateway webhooks and reconciliation.
     * 
     * @param transactionId Payment gateway transaction ID
     * @return Optional payment record
     */
    Optional<WithholdingPayment> findByTransactionId(String transactionId);
    
    /**
     * Find all payments for a specific business (across all W-1 filings).
     * Used for payment history dashboard.
     * 
     * Note: This requires custom query as payment only stores w1FilingId, not businessId.
     * 
     * @param tenantId Tenant ID (for multi-tenant isolation)
     * @param businessId Business profile ID
     * @return List of all payments for this business
     */
    @Query(value = """
        SELECT p.* FROM dublin.withholding_payments p
        INNER JOIN dublin.w1_filings f ON p.w1_filing_id = f.id
        WHERE p.tenant_id = :tenantId 
        AND f.business_id = :businessId
        ORDER BY p.payment_date DESC
        """, nativeQuery = true)
    List<WithholdingPayment> findByBusinessId(
        @Param("tenantId") UUID tenantId,
        @Param("businessId") UUID businessId
    );
    
    /**
     * Find all payments with a specific status.
     * Used for payment reconciliation and retry logic.
     * 
     * @param tenantId Tenant ID (for multi-tenant isolation)
     * @param status Payment status (PENDING, COMPLETED, FAILED, REFUNDED)
     * @return List of payments with this status
     */
    List<WithholdingPayment> findByTenantIdAndStatus(UUID tenantId, PaymentStatus status);
    
    /**
     * Find all pending payments older than a specific date.
     * Used for payment timeout detection (e.g., pending > 24 hours).
     * 
     * @param tenantId Tenant ID (for multi-tenant isolation)
     * @param cutoffDate Date before which payments are considered stale
     * @return List of stale pending payments
     */
    @Query("""
        SELECT p FROM WithholdingPayment p
        WHERE p.tenantId = :tenantId
        AND p.status = com.munitax.taxengine.domain.withholding.PaymentStatus.PENDING
        AND p.paymentDate < :cutoffDate
        """)
    List<WithholdingPayment> findStalePendingPayments(
        @Param("tenantId") UUID tenantId,
        @Param("cutoffDate") LocalDateTime cutoffDate
    );
    
    /**
     * Check if a W-1 filing has been fully paid.
     * A filing is fully paid if sum(COMPLETED payments) >= total_amount_due.
     * 
     * @param w1FilingId W-1 filing ID
     * @param status Payment status (should be COMPLETED)
     * @return True if at least one COMPLETED payment exists
     */
    boolean existsByW1FilingIdAndStatus(UUID w1FilingId, PaymentStatus status);
}
