package com.munitax.ledger.repository;

import com.munitax.ledger.model.ChartOfAccounts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChartOfAccountsRepository extends JpaRepository<ChartOfAccounts, UUID> {
    Optional<ChartOfAccounts> findByAccountNumber(String accountNumber);
    List<ChartOfAccounts> findByTenantIdAndActiveTrue(String tenantId);
    List<ChartOfAccounts> findByTenantId(String tenantId);
}
