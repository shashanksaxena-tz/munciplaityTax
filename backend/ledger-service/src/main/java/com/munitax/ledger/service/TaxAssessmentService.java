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
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaxAssessmentService {
    
    private final JournalEntryService journalEntryService;
    private final AuditLogService auditLogService;
    
    @Transactional
    public JournalEntry recordTaxAssessment(UUID tenantId, UUID filerId, UUID returnId,
                                           BigDecimal taxAmount, BigDecimal penaltyAmount,
                                           BigDecimal interestAmount, String taxYear,
                                           String taxPeriod) {
        log.info("Recording tax assessment for filer {}: tax={}, penalty={}, interest={}",
                filerId, taxAmount, penaltyAmount, interestAmount);
        
        BigDecimal totalAmount = taxAmount.add(penaltyAmount).add(interestAmount);
        
        // Use a fixed municipality entity ID (deterministic based on tenant ID)
        UUID municipalityEntityId = UUID.nameUUIDFromBytes(
                ("MUNICIPALITY-" + tenantId.toString()).getBytes(StandardCharsets.UTF_8));
        
        // Create filer journal entry for tax assessment
        JournalEntryRequest filerEntry = JournalEntryRequest.builder()
                .entryDate(LocalDate.now())
                .description(String.format("%s %s Tax Assessment", taxYear, taxPeriod))
                .sourceType(SourceType.TAX_ASSESSMENT)
                .sourceId(returnId)
                .tenantId(tenantId)
                .entityId(filerId)
                .createdBy(filerId)
                .lines(new ArrayList<>())
                .build();
        
        // Filer: CREDIT Tax Liability (liability increases), DEBIT Tax Expense
        if (taxAmount.compareTo(BigDecimal.ZERO) > 0) {
            filerEntry.getLines().add(JournalEntryLineRequest.builder()
                    .accountNumber("2100")
                    .debit(BigDecimal.ZERO)
                    .credit(taxAmount)
                    .description(String.format("Tax liability - %s %s", taxYear, taxPeriod))
                    .build());
            
            filerEntry.getLines().add(JournalEntryLineRequest.builder()
                    .accountNumber("6100")
                    .debit(taxAmount)
                    .credit(BigDecimal.ZERO)
                    .description("Tax expense")
                    .build());
        }
        
        // Add penalty if applicable
        if (penaltyAmount.compareTo(BigDecimal.ZERO) > 0) {
            filerEntry.getLines().add(JournalEntryLineRequest.builder()
                    .accountNumber("2120")
                    .debit(BigDecimal.ZERO)
                    .credit(penaltyAmount)
                    .description("Penalty liability")
                    .build());
            
            filerEntry.getLines().add(JournalEntryLineRequest.builder()
                    .accountNumber("6100")
                    .debit(penaltyAmount)
                    .credit(BigDecimal.ZERO)
                    .description("Penalty expense")
                    .build());
        }
        
        // Add interest if applicable
        if (interestAmount.compareTo(BigDecimal.ZERO) > 0) {
            filerEntry.getLines().add(JournalEntryLineRequest.builder()
                    .accountNumber("2130")
                    .debit(BigDecimal.ZERO)
                    .credit(interestAmount)
                    .description("Interest liability")
                    .build());
            
            filerEntry.getLines().add(JournalEntryLineRequest.builder()
                    .accountNumber("6100")
                    .debit(interestAmount)
                    .credit(BigDecimal.ZERO)
                    .description("Interest expense")
                    .build());
        }
        
        JournalEntry filerJournalEntry = journalEntryService.createJournalEntry(filerEntry);
        
        // Create municipality journal entry for tax assessment
        JournalEntryRequest municipalityEntry = JournalEntryRequest.builder()
                .entryDate(LocalDate.now())
                .description(String.format("Filer %s - %s %s Tax Assessment", filerId, taxYear, taxPeriod))
                .sourceType(SourceType.TAX_ASSESSMENT)
                .sourceId(returnId)
                .tenantId(tenantId)
                .entityId(municipalityEntityId)
                .createdBy(filerId)
                .lines(new ArrayList<>())
                .build();
        
        // Municipality: DEBIT AR (asset increases), CREDIT Revenue
        if (taxAmount.compareTo(BigDecimal.ZERO) > 0) {
            municipalityEntry.getLines().add(JournalEntryLineRequest.builder()
                    .accountNumber("1201")
                    .debit(taxAmount)
                    .credit(BigDecimal.ZERO)
                    .description("Accounts receivable - tax")
                    .build());
            
            municipalityEntry.getLines().add(JournalEntryLineRequest.builder()
                    .accountNumber("4100")
                    .debit(BigDecimal.ZERO)
                    .credit(taxAmount)
                    .description("Tax revenue")
                    .build());
        }
        
        if (penaltyAmount.compareTo(BigDecimal.ZERO) > 0) {
            municipalityEntry.getLines().add(JournalEntryLineRequest.builder()
                    .accountNumber("1201")
                    .debit(penaltyAmount)
                    .credit(BigDecimal.ZERO)
                    .description("Accounts receivable - penalty")
                    .build());
            
            municipalityEntry.getLines().add(JournalEntryLineRequest.builder()
                    .accountNumber("4200")
                    .debit(BigDecimal.ZERO)
                    .credit(penaltyAmount)
                    .description("Penalty revenue")
                    .build());
        }
        
        if (interestAmount.compareTo(BigDecimal.ZERO) > 0) {
            municipalityEntry.getLines().add(JournalEntryLineRequest.builder()
                    .accountNumber("1201")
                    .debit(interestAmount)
                    .credit(BigDecimal.ZERO)
                    .description("Accounts receivable - interest")
                    .build());
            
            municipalityEntry.getLines().add(JournalEntryLineRequest.builder()
                    .accountNumber("4300")
                    .debit(BigDecimal.ZERO)
                    .credit(interestAmount)
                    .description("Interest revenue")
                    .build());
        }
        
        journalEntryService.createJournalEntry(municipalityEntry);
        
        // Audit log
        auditLogService.logAction(
                filerJournalEntry.getEntryId(),
                "TAX_ASSESSMENT",
                "CREATE",
                filerId,
                tenantId,
                String.format("Tax assessed: total=%s (tax=%s, penalty=%s, interest=%s)",
                        totalAmount, taxAmount, penaltyAmount, interestAmount)
        );
        
        log.info("Tax assessment recorded successfully for filer {}", filerId);
        return filerJournalEntry;
    }
}
