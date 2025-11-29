package com.munitax.ledger.service;

import com.munitax.ledger.dto.AccountStatementResponse;
import com.munitax.ledger.dto.AgingAnalysis;
import com.munitax.ledger.dto.StatementTransaction;
import com.munitax.ledger.enums.SourceType;
import com.munitax.ledger.model.ChartOfAccounts;
import com.munitax.ledger.model.JournalEntry;
import com.munitax.ledger.model.JournalEntryLine;
import com.munitax.ledger.repository.ChartOfAccountsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountStatementService {
    
    private final JournalEntryService journalEntryService;
    private final ChartOfAccountsRepository chartOfAccountsRepository;
    
    /**
     * Generate comprehensive filer account statement.
     * 
     * @param tenantId The tenant identifier
     * @param filerId The filer identifier
     * @param startDate Optional start date for filtering
     * @param endDate Optional end date for filtering
     * @return AccountStatementResponse with full transaction history
     */
    public AccountStatementResponse generateFilerStatement(UUID tenantId, UUID filerId, 
                                                           LocalDate startDate, LocalDate endDate) {
        return generateFilerStatement(tenantId, filerId, startDate, endDate, null, null);
    }
    
    /**
     * T032-T034: Generate filer statement with transaction type and tax year filtering.
     * 
     * @param tenantId The tenant identifier
     * @param filerId The filer identifier
     * @param startDate Optional start date for filtering
     * @param endDate Optional end date for filtering
     * @param transactionType Optional transaction type filter (TAX_ASSESSMENT, PAYMENT, etc.)
     * @param taxYear Optional tax year filter (e.g., "2024")
     * @return Filtered AccountStatementResponse
     */
    public AccountStatementResponse generateFilerStatement(UUID tenantId, UUID filerId, 
                                                           LocalDate startDate, LocalDate endDate,
                                                           SourceType transactionType, String taxYear) {
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
            // T033: Filter by transaction type
            if (transactionType != null && !entry.getSourceType().equals(transactionType)) {
                continue;
            }
            
            // T034: Filter by tax year (simple check - looks for year in description)
            if (taxYear != null && (entry.getDescription() == null || 
                    !entry.getDescription().contains(taxYear))) {
                continue;
            }
            
            // Filter by date range (T030)
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
    
    /**
     * T037: Calculate aging analysis for outstanding balance.
     * 
     * @param tenantId The tenant identifier
     * @param filerId The filer identifier
     * @return AgingAnalysis with breakdown by age buckets
     */
    public AgingAnalysis calculateAgingAnalysis(UUID tenantId, UUID filerId) {
        // Get all journal entries for filer
        List<JournalEntry> entries = journalEntryService.getEntriesForEntity(tenantId, filerId);
        
        // Buckets: 0-30, 31-60, 61-90, 90+ days
        BigDecimal current = BigDecimal.ZERO;        // 0-30 days
        BigDecimal thirtyDays = BigDecimal.ZERO;     // 31-60 days
        BigDecimal sixtyDays = BigDecimal.ZERO;      // 61-90 days
        BigDecimal ninetyPlus = BigDecimal.ZERO;     // 90+ days
        
        LocalDate today = LocalDate.now();
        
        for (JournalEntry entry : entries) {
            // Calculate days old
            long daysOld = Period.between(entry.getEntryDate(), today).getDays();
            
            // Sum up outstanding amounts from liability accounts
            for (JournalEntryLine line : entry.getLines()) {
                String accountNumber = line.getAccount().getAccountNumber();
                if (accountNumber.startsWith("2")) { // Liability accounts
                    BigDecimal amount = line.getCredit().subtract(line.getDebit());
                    
                    if (amount.compareTo(BigDecimal.ZERO) > 0) {
                        // Outstanding liability - categorize by age
                        if (daysOld <= 30) {
                            current = current.add(amount);
                        } else if (daysOld <= 60) {
                            thirtyDays = thirtyDays.add(amount);
                        } else if (daysOld <= 90) {
                            sixtyDays = sixtyDays.add(amount);
                        } else {
                            ninetyPlus = ninetyPlus.add(amount);
                        }
                    }
                }
            }
        }
        
        BigDecimal totalOutstanding = current.add(thirtyDays).add(sixtyDays).add(ninetyPlus);
        
        return AgingAnalysis.builder()
                .current(current)
                .thirtyDays(thirtyDays)
                .sixtyDays(sixtyDays)
                .ninetyPlus(ninetyPlus)
                .totalOutstanding(totalOutstanding)
                .analysisDate(today)
                .build();
    }
    
    /**
     * T035: Export statement to PDF format.
     * TODO: Implement PDF generation using a library like iText or Apache PDFBox
     * 
     * @param tenantId The tenant identifier
     * @param filerId The filer identifier
     * @return byte[] PDF content
     */
    public byte[] exportStatementToPdf(UUID tenantId, UUID filerId) {
        AccountStatementResponse statement = generateFilerStatement(tenantId, filerId, null, null);
        
        // TODO: Implement PDF generation
        // For now, return placeholder
        log.warn("PDF export not yet implemented - returning placeholder");
        String placeholder = "Account Statement PDF for Filer: " + filerId;
        return placeholder.getBytes();
    }
    
    /**
     * T036: Export statement to CSV format.
     * 
     * @param tenantId The tenant identifier
     * @param filerId The filer identifier
     * @return String CSV content
     */
    public String exportStatementToCsv(UUID tenantId, UUID filerId) {
        AccountStatementResponse statement = generateFilerStatement(tenantId, filerId, null, null);
        
        StringBuilder csv = new StringBuilder();
        
        // CSV Header
        csv.append("Date,Transaction Type,Description,Debit,Credit,Balance,Entry Number\n");
        
        // CSV Rows
        for (StatementTransaction transaction : statement.getTransactions()) {
            csv.append(transaction.getTransactionDate()).append(",");
            csv.append(transaction.getTransactionType()).append(",");
            csv.append("\"").append(transaction.getDescription()).append("\",");
            csv.append(transaction.getDebitAmount()).append(",");
            csv.append(transaction.getCreditAmount()).append(",");
            csv.append(transaction.getRunningBalance()).append(",");
            csv.append(transaction.getEntryNumber()).append("\n");
        }
        
        return csv.toString();
    }
}
