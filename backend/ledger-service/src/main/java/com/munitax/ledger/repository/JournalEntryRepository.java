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
    List<JournalEntry> findByTenantId(String tenantId);
    List<JournalEntry> findByTenantIdAndEntityIdOrderByEntryDateDesc(String tenantId, String entityId);
    List<JournalEntry> findByTenantIdAndEntityIdAndEntryDateBetweenOrderByEntryDateDesc(
            String tenantId, String entityId, LocalDate startDate, LocalDate endDate);
    List<JournalEntry> findByTenantIdAndStatus(String tenantId, EntryStatus status);
    List<JournalEntry> findBySourceIdOrderByEntryDateDesc(UUID sourceId);
    
    @Query("SELECT MAX(e.entryNumber) FROM JournalEntry e WHERE e.tenantId = :tenantId AND e.entryNumber LIKE CONCAT(:prefix, '%')")
    String findMaxEntryNumberByPrefix(String tenantId, String prefix);
}
