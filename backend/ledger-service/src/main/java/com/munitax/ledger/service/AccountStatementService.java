package com.munitax.ledger.service;

import com.munitax.ledger.dto.AccountStatementResponse;
import com.munitax.ledger.dto.StatementTransaction;
import com.munitax.ledger.model.ChartOfAccounts;
import com.munitax.ledger.model.JournalEntry;
import com.munitax.ledger.model.JournalEntryLine;
import com.munitax.ledger.repository.ChartOfAccountsRepository;
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
public class AccountStatementService {
    
    private final JournalEntryService journalEntryService;
    private final ChartOfAccountsRepository chartOfAccountsRepository;
    
    public AccountStatementResponse generateFilerStatement(UUID tenantId, UUID filerId, 
                                                           LocalDate startDate, LocalDate endDate) {
        // Get tax liability account
        ChartOfAccounts taxLiabilityAccount = chartOfAccountsRepository
                .findByAccountNumber("2100")
                .orElseThrow(() -> new IllegalArgumentException("Tax liability account not found"));
        
        // Get all journal entries for filer
        List<JournalEntry> entries = journalEntryService.getEntriesForEntity(tenantId, filerId);
        
        // Build statement transactions
        List<StatementTransaction> transactions = new ArrayList<>();
        BigDecimal runningBalance = BigDecimal.ZERO;
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;
        
        for (JournalEntry entry : entries) {
            // Filter by date range
            if (startDate != null && entry.getEntryDate().isBefore(startDate)) {
                continue;
            }
            if (endDate != null && entry.getEntryDate().isAfter(endDate)) {
                continue;
            }
            
            // Find lines affecting tax liability or related accounts
            for (JournalEntryLine line : entry.getLines()) {
                // Only show liability and payment accounts
                String accountNumber = line.getAccount().getAccountNumber();
                if (accountNumber.startsWith("2") || accountNumber.equals("1000")) {
                    
                    BigDecimal debit = line.getDebit();
                    BigDecimal credit = line.getCredit();
                    
                    // For liability accounts, credit increases balance owed, debit decreases
                    // For asset accounts (cash), debit increases, credit decreases
                    if (accountNumber.startsWith("2")) {
                        // Liability account
                        runningBalance = runningBalance.add(credit).subtract(debit);
                        totalDebits = totalDebits.add(debit);
                        totalCredits = totalCredits.add(credit);
                    }
                    
                    StatementTransaction transaction = StatementTransaction.builder()
                            .transactionDate(entry.getEntryDate())
                            .transactionType(entry.getSourceType())
                            .description(line.getDescription() != null ? line.getDescription() : entry.getDescription())
                            .debitAmount(debit)
                            .creditAmount(credit)
                            .runningBalance(runningBalance)
                            .entryNumber(entry.getEntryNumber())
                            .build();
                    
                    transactions.add(transaction);
                }
            }
        }
        
        return AccountStatementResponse.builder()
                .accountName("Tax Liability Account")
                .statementDate(LocalDate.now())
                .beginningBalance(BigDecimal.ZERO)
                .endingBalance(runningBalance)
                .totalDebits(totalDebits)
                .totalCredits(totalCredits)
                .transactions(transactions)
                .build();
    }
}
