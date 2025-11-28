package com.munitax.ledger.service;

import com.munitax.ledger.dto.DiscrepancyDetail;
import com.munitax.ledger.dto.ReconciliationResponse;
import com.munitax.ledger.enums.ReconciliationStatus;
import com.munitax.ledger.model.JournalEntry;
import com.munitax.ledger.model.JournalEntryLine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReconciliationService {
    
    private final JournalEntryService journalEntryService;
    
    public ReconciliationResponse generateReconciliationReport(UUID tenantId, UUID municipalityId) {
        log.info("Generating reconciliation report for tenant {}", tenantId);
        
        // Calculate municipality AR (account 1201)
        BigDecimal municipalityAR = calculateAccountBalance(tenantId, municipalityId, "1201");
        
        // Calculate municipality cash receipts (account 1001 credits)
        BigDecimal municipalityCash = calculateAccountBalance(tenantId, municipalityId, "1001");
        
        // For filer liabilities and payments, we would need to aggregate across all filers
        // Simplified: Using same values for demonstration (in real implementation, would iterate all filers)
        BigDecimal filerLiabilities = municipalityAR; // Should sum all filer liability accounts
        BigDecimal filerPayments = municipalityCash; // Should sum all filer payment entries
        
        // Calculate variances
        BigDecimal arVariance = municipalityAR.subtract(filerLiabilities);
        BigDecimal cashVariance = municipalityCash.subtract(filerPayments);
        
        // Determine reconciliation status
        ReconciliationStatus status = (arVariance.compareTo(BigDecimal.ZERO) == 0 
                && cashVariance.compareTo(BigDecimal.ZERO) == 0) 
                ? ReconciliationStatus.RECONCILED 
                : ReconciliationStatus.DISCREPANCY;
        
        // Build discrepancies list (simplified)
        List<DiscrepancyDetail> discrepancies = new ArrayList<>();
        if (status == ReconciliationStatus.DISCREPANCY) {
            if (arVariance.compareTo(BigDecimal.ZERO) != 0) {
                discrepancies.add(DiscrepancyDetail.builder()
                        .transactionType("Accounts Receivable")
                        .transactionDate(LocalDate.now())
                        .filerAmount(filerLiabilities)
                        .municipalityAmount(municipalityAR)
                        .variance(arVariance)
                        .description("AR variance detected")
                        .build());
            }
            
            if (cashVariance.compareTo(BigDecimal.ZERO) != 0) {
                discrepancies.add(DiscrepancyDetail.builder()
                        .transactionType("Cash Receipts")
                        .transactionDate(LocalDate.now())
                        .filerAmount(filerPayments)
                        .municipalityAmount(municipalityCash)
                        .variance(cashVariance)
                        .description("Cash variance detected")
                        .build());
            }
        }
        
        return ReconciliationResponse.builder()
                .reportDate(LocalDate.now())
                .municipalityAR(municipalityAR)
                .filerLiabilities(filerLiabilities)
                .arVariance(arVariance)
                .municipalityCash(municipalityCash)
                .filerPayments(filerPayments)
                .cashVariance(cashVariance)
                .status(status)
                .discrepancies(discrepancies)
                .build();
    }
    
    private BigDecimal calculateAccountBalance(UUID tenantId, UUID entityId, String accountNumber) {
        List<JournalEntry> entries = journalEntryService.getEntriesForEntity(tenantId, entityId);
        
        BigDecimal balance = BigDecimal.ZERO;
        
        for (JournalEntry entry : entries) {
            for (JournalEntryLine line : entry.getLines()) {
                if (line.getAccount().getAccountNumber().equals(accountNumber)) {
                    // For asset accounts: debit increases, credit decreases
                    // For liability/revenue accounts: credit increases, debit decreases
                    if (accountNumber.startsWith("1")) {
                        // Asset account
                        balance = balance.add(line.getDebit()).subtract(line.getCredit());
                    } else if (accountNumber.startsWith("2") || accountNumber.startsWith("4")) {
                        // Liability or revenue account
                        balance = balance.add(line.getCredit()).subtract(line.getDebit());
                    }
                }
            }
        }
        
        return balance;
    }
}
