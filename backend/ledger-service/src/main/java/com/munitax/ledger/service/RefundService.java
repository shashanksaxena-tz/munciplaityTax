package com.munitax.ledger.service;

import com.munitax.ledger.dto.JournalEntryLineRequest;
import com.munitax.ledger.dto.JournalEntryRequest;
import com.munitax.ledger.enums.SourceType;
import com.munitax.ledger.model.JournalEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundService {
    
    private final JournalEntryService journalEntryService;
    private final AuditLogService auditLogService;
    
    @Transactional
    public JournalEntry processRefundRequest(UUID tenantId, UUID filerId, BigDecimal refundAmount, 
                                            String reason, UUID requestedBy) {
        log.info("Processing refund request for filer {}: amount={}", filerId, refundAmount);
        
        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Refund amount must be positive");
        }
        
        // Use a fixed municipality entity ID (deterministic based on tenant ID)
        UUID municipalityEntityId = UUID.nameUUIDFromBytes(
                ("MUNICIPALITY-" + tenantId.toString()).getBytes());
        
        // Create filer journal entry for refund request
        JournalEntryRequest filerEntry = JournalEntryRequest.builder()
                .entryDate(LocalDate.now())
                .description(String.format("Refund Request - %s", reason))
                .sourceType(SourceType.REFUND)
                .sourceId(UUID.randomUUID()) // Refund request ID
                .tenantId(tenantId)
                .entityId(filerId)
                .createdBy(requestedBy)
                .lines(new ArrayList<>())
                .build();
        
        // Filer: DEBIT Refund Receivable (asset increases), CREDIT Tax Liability (reduces overpayment)
        filerEntry.getLines().add(JournalEntryLineRequest.builder()
                .accountNumber("1200")
                .debit(refundAmount)
                .credit(BigDecimal.ZERO)
                .description("Refund receivable from municipality")
                .build());
        
        filerEntry.getLines().add(JournalEntryLineRequest.builder()
                .accountNumber("2100")
                .debit(BigDecimal.ZERO)
                .credit(refundAmount)
                .description("Reduce overpayment")
                .build());
        
        JournalEntry filerJournalEntry = journalEntryService.createJournalEntry(filerEntry);
        
        // Create municipality journal entry for refund
        JournalEntryRequest municipalityEntry = JournalEntryRequest.builder()
                .entryDate(LocalDate.now())
                .description(String.format("Refund to filer %s - %s", filerId, reason))
                .sourceType(SourceType.REFUND)
                .sourceId(filerJournalEntry.getSourceId())
                .tenantId(tenantId)
                .entityId(municipalityEntityId)
                .createdBy(requestedBy)
                .lines(new ArrayList<>())
                .build();
        
        // Municipality: DEBIT Refund Expense (expense increases), CREDIT Refunds Payable (liability increases)
        municipalityEntry.getLines().add(JournalEntryLineRequest.builder()
                .accountNumber("5200")
                .debit(refundAmount)
                .credit(BigDecimal.ZERO)
                .description("Refund expense")
                .build());
        
        municipalityEntry.getLines().add(JournalEntryLineRequest.builder()
                .accountNumber("2200")
                .debit(BigDecimal.ZERO)
                .credit(refundAmount)
                .description("Refund payable")
                .build());
        
        journalEntryService.createJournalEntry(municipalityEntry);
        
        // Audit log
        auditLogService.logAction(
                filerJournalEntry.getEntryId(),
                "REFUND",
                "REQUEST",
                requestedBy,
                tenantId,
                String.format("Refund requested: amount=%s, reason=%s", refundAmount, reason)
        );
        
        log.info("Refund request processed successfully for filer {}", filerId);
        return filerJournalEntry;
    }
    
    @Transactional
    public void issueRefund(UUID tenantId, UUID filerId, UUID refundRequestId, 
                           BigDecimal refundAmount, UUID issuedBy) {
        log.info("Issuing refund for filer {}: amount={}", filerId, refundAmount);
        
        // Use a fixed municipality entity ID (deterministic based on tenant ID)
        UUID municipalityEntityId = UUID.nameUUIDFromBytes(
                ("MUNICIPALITY-" + tenantId.toString()).getBytes());
        
        // Create filer journal entry for refund issuance
        JournalEntryRequest filerEntry = JournalEntryRequest.builder()
                .entryDate(LocalDate.now())
                .description("Refund Issued")
                .sourceType(SourceType.REFUND)
                .sourceId(refundRequestId)
                .tenantId(tenantId)
                .entityId(filerId)
                .createdBy(issuedBy)
                .lines(new ArrayList<>())
                .build();
        
        // Filer: DEBIT Cash (asset increases), CREDIT Refund Receivable (asset decreases)
        filerEntry.getLines().add(JournalEntryLineRequest.builder()
                .accountNumber("1000")
                .debit(refundAmount)
                .credit(BigDecimal.ZERO)
                .description("Cash refund received")
                .build());
        
        filerEntry.getLines().add(JournalEntryLineRequest.builder()
                .accountNumber("1200")
                .debit(BigDecimal.ZERO)
                .credit(refundAmount)
                .description("Refund receivable cleared")
                .build());
        
        JournalEntry filerJournalEntry = journalEntryService.createJournalEntry(filerEntry);
        
        // Create municipality journal entry for refund issuance
        JournalEntryRequest municipalityEntry = JournalEntryRequest.builder()
                .entryDate(LocalDate.now())
                .description(String.format("Refund Issued to filer %s", filerId))
                .sourceType(SourceType.REFUND)
                .sourceId(refundRequestId)
                .tenantId(tenantId)
                .entityId(municipalityEntityId)
                .createdBy(issuedBy)
                .lines(new ArrayList<>())
                .build();
        
        // Municipality: DEBIT Refunds Payable (liability decreases), CREDIT Cash (asset decreases)
        municipalityEntry.getLines().add(JournalEntryLineRequest.builder()
                .accountNumber("2200")
                .debit(refundAmount)
                .credit(BigDecimal.ZERO)
                .description("Refund payable cleared")
                .build());
        
        municipalityEntry.getLines().add(JournalEntryLineRequest.builder()
                .accountNumber("1001")
                .debit(BigDecimal.ZERO)
                .credit(refundAmount)
                .description("Cash refund paid")
                .build());
        
        journalEntryService.createJournalEntry(municipalityEntry);
        
        // Audit log
        auditLogService.logAction(
                filerJournalEntry.getEntryId(),
                "REFUND",
                "ISSUED",
                issuedBy,
                tenantId,
                String.format("Refund issued: amount=%s", refundAmount)
        );
        
        log.info("Refund issued successfully for filer {}", filerId);
    }
}
