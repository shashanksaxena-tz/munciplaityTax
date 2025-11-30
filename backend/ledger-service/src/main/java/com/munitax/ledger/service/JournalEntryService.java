package com.munitax.ledger.service;

import com.munitax.ledger.dto.JournalEntryLineRequest;
import com.munitax.ledger.dto.JournalEntryRequest;
import com.munitax.ledger.enums.EntryStatus;
import com.munitax.ledger.model.ChartOfAccounts;
import com.munitax.ledger.model.JournalEntry;
import com.munitax.ledger.model.JournalEntryLine;
import com.munitax.ledger.repository.ChartOfAccountsRepository;
import com.munitax.ledger.repository.JournalEntryRepository;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * T096-T097: Added retry logic for database operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JournalEntryService {
    
    private final JournalEntryRepository journalEntryRepository;
    private final ChartOfAccountsRepository chartOfAccountsRepository;
    private final AuditLogService auditLogService;
    
    /**
     * T096: Retry logic added for database operations
     * Retries up to 3 times with exponential backoff on transient database errors
     */
    @Transactional
    @Retry(name = "database", fallbackMethod = "createJournalEntryFallback")
    public JournalEntry createJournalEntry(JournalEntryRequest request) {
        log.debug("Creating journal entry for entity {}", request.getEntityId());
        
        // Validate double-entry balance
        BigDecimal totalDebits = request.getLines().stream()
                .map(JournalEntryLineRequest::getDebit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalCredits = request.getLines().stream()
                .map(JournalEntryLineRequest::getCredit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new IllegalArgumentException(
                    String.format("Journal entry is not balanced. Debits: %s, Credits: %s", 
                            totalDebits, totalCredits));
        }
        
        // Generate entry number
        String entryNumber = generateEntryNumber(request.getTenantId(), request.getEntryDate());
        
        // Create journal entry
        JournalEntry entry = JournalEntry.builder()
                .entryNumber(entryNumber)
                .entryDate(request.getEntryDate())
                .description(request.getDescription())
                .sourceType(request.getSourceType())
                .sourceId(request.getSourceId())
                .status(EntryStatus.POSTED) // Auto-post for now
                .tenantId(request.getTenantId())
                .entityId(request.getEntityId())
                .createdBy(request.getCreatedBy())
                .createdAt(LocalDateTime.now())
                .postedBy(request.getCreatedBy())
                .postedAt(LocalDateTime.now())
                .lines(new ArrayList<>())
                .build();
        
        // Create journal entry lines
        int lineNumber = 1;
        for (JournalEntryLineRequest lineRequest : request.getLines()) {
            ChartOfAccounts account = chartOfAccountsRepository
                    .findByAccountNumber(lineRequest.getAccountNumber())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Account not found: " + lineRequest.getAccountNumber()));
            
            JournalEntryLine line = JournalEntryLine.builder()
                    .journalEntry(entry)
                    .account(account)
                    .lineNumber(lineNumber++)
                    .debit(lineRequest.getDebit() != null ? lineRequest.getDebit() : BigDecimal.ZERO)
                    .credit(lineRequest.getCredit() != null ? lineRequest.getCredit() : BigDecimal.ZERO)
                    .description(lineRequest.getDescription())
                    .build();
            
            entry.getLines().add(line);
        }
        
        // Save entry with retry on transient database errors
        JournalEntry savedEntry = journalEntryRepository.save(entry);
        
        log.info("Journal entry created successfully: {}", savedEntry.getEntryNumber());
        
        // Audit log
        auditLogService.logAction(
                savedEntry.getEntryId(),
                "JOURNAL_ENTRY",
                "CREATE",
                request.getCreatedBy(),
                request.getTenantId(),
                String.format("Created journal entry %s: %s", entryNumber, request.getDescription())
        );
        
        log.info("Created journal entry {}: debits={}, credits={}", 
                entryNumber, totalDebits, totalCredits);
        
        return savedEntry;
    }
    
    @Transactional
    public JournalEntry reverseEntry(UUID entryId, UUID userId, String reason) {
        JournalEntry originalEntry = journalEntryRepository.findById(entryId)
                .orElseThrow(() -> new IllegalArgumentException("Journal entry not found"));
        
        if (originalEntry.getStatus() == EntryStatus.REVERSED) {
            throw new IllegalStateException("Entry already reversed");
        }
        
        // T070 - Prevent deletion of posted entries, use reversal instead
        if (originalEntry.getStatus() == EntryStatus.POSTED) {
            log.info("Reversing posted entry {} instead of deleting", originalEntry.getEntryNumber());
        }
        
        // Create reversing entry request
        JournalEntryRequest reversingRequest = JournalEntryRequest.builder()
                .entryDate(originalEntry.getEntryDate())
                .description("REVERSAL: " + originalEntry.getDescription() + " - " + reason)
                .sourceType(originalEntry.getSourceType())
                .sourceId(originalEntry.getSourceId())
                .tenantId(originalEntry.getTenantId())
                .entityId(originalEntry.getEntityId())
                .createdBy(userId)
                .lines(new ArrayList<>())
                .build();
        
        // Reverse each line (swap debits and credits)
        for (JournalEntryLine line : originalEntry.getLines()) {
            reversingRequest.getLines().add(
                    JournalEntryLineRequest.builder()
                            .accountNumber(line.getAccount().getAccountNumber())
                            .debit(line.getCredit())  // Swap
                            .credit(line.getDebit())  // Swap
                            .description("Reversal: " + line.getDescription())
                            .build()
            );
        }
        
        // Create reversing entry
        JournalEntry reversingEntry = createJournalEntry(reversingRequest);
        
        // Update original entry
        originalEntry.setStatus(EntryStatus.REVERSED);
        originalEntry.setReversedBy(userId);
        originalEntry.setReversedAt(LocalDateTime.now());
        originalEntry.setReversalEntryId(reversingEntry.getEntryId());
        journalEntryRepository.save(originalEntry);
        
        // T070 - Audit log with old and new values
        auditLogService.logModification(
                entryId,
                "JOURNAL_ENTRY",
                "REVERSE",
                userId,
                originalEntry.getTenantId(),
                "POSTED",
                "REVERSED",
                reason
        );
        
        log.info("Reversed journal entry {}", originalEntry.getEntryNumber());
        
        return reversingEntry;
    }
    
    /**
     * T069 - Prevent deletion of posted entries per FR-049
     * Posted journal entries cannot be deleted, only reversed
     */
    @Transactional
    public void deleteJournalEntry(UUID entryId, UUID userId) {
        JournalEntry entry = journalEntryRepository.findById(entryId)
                .orElseThrow(() -> new IllegalArgumentException("Journal entry not found"));
        
        if (entry.getStatus() == EntryStatus.POSTED) {
            throw new IllegalStateException(
                    "Cannot delete posted journal entry. Use reverseEntry() instead. Entry: " 
                    + entry.getEntryNumber());
        }
        
        // Only draft entries can be deleted
        auditLogService.logAction(
                entryId,
                "JOURNAL_ENTRY",
                "DELETE",
                userId,
                entry.getTenantId(),
                String.format("Deleted draft entry %s", entry.getEntryNumber())
        );
        
        journalEntryRepository.delete(entry);
        log.info("Deleted draft journal entry {}", entry.getEntryNumber());
    }
    
    public List<JournalEntry> getEntriesForEntity(UUID tenantId, UUID entityId) {
        return journalEntryRepository.findByTenantIdAndEntityIdOrderByEntryDateDesc(tenantId, entityId);
    }
    
    public JournalEntry getEntryById(UUID entryId) {
        return journalEntryRepository.findById(entryId)
                .orElseThrow(() -> new IllegalArgumentException("Journal entry not found"));
    }
    
    private String generateEntryNumber(UUID tenantId, java.time.LocalDate entryDate) {
        String prefix = "JE-" + entryDate.format(DateTimeFormatter.ofPattern("yyyy"));
        String maxNumber = journalEntryRepository.findMaxEntryNumberByPrefix(tenantId, prefix);
        
        int nextNumber = 1;
        if (maxNumber != null) {
            try {
                String numberPart = maxNumber.substring(maxNumber.lastIndexOf('-') + 1);
                nextNumber = Integer.parseInt(numberPart) + 1;
            } catch (NumberFormatException e) {
                log.warn("Failed to parse entry number from {}, using 1 as next number", maxNumber, e);
                nextNumber = 1;
            }
        }
        
        return String.format("%s-%05d", prefix, nextNumber);
    }
}
