package com.munitax.ledger.repository;

import com.munitax.ledger.model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {
    List<PaymentTransaction> findByFilerIdOrderByTimestampDesc(UUID filerId);
    List<PaymentTransaction> findByTenantIdOrderByTimestampDesc(UUID tenantId);
    Optional<PaymentTransaction> findByPaymentId(UUID paymentId);
}
