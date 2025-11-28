package com.munitax.ledger.repository;

import com.munitax.ledger.enums.EntryStatus;
import com.munitax.ledger.model.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID> {
    List<JournalEntry> findByTenantIdAndEntityIdOrderByEntryDateDesc(UUID tenantId, UUID entityId);
    List<JournalEntry> findByTenantIdAndEntityIdAndEntryDateBetweenOrderByEntryDateDesc(
            UUID tenantId, UUID entityId, LocalDate startDate, LocalDate endDate);
    List<JournalEntry> findByTenantIdAndStatus(UUID tenantId, EntryStatus status);
    
    @Query("SELECT MAX(e.entryNumber) FROM JournalEntry e WHERE e.tenantId = :tenantId AND e.entryNumber LIKE :prefix%")
    String findMaxEntryNumberByPrefix(UUID tenantId, String prefix);
}
