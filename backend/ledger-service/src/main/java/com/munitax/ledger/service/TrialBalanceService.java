package com.munitax.ledger.service;

import com.munitax.ledger.dto.AccountBalanceSummary;
import com.munitax.ledger.dto.TrialBalanceResponse;
import com.munitax.ledger.enums.AccountType;
import com.munitax.ledger.enums.NormalBalance;
import com.munitax.ledger.model.ChartOfAccounts;
import com.munitax.ledger.model.JournalEntry;
import com.munitax.ledger.model.JournalEntryLine;
import com.munitax.ledger.repository.ChartOfAccountsRepository;
import com.munitax.ledger.repository.JournalEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating trial balance reports
 * T044: Create TrialBalanceService with generateTrialBalance method
 * T047: Implement account balance calculation from journal entries
 * T048: Add account hierarchy grouping (assets, liabilities, revenue, expense)
 * T049: Add date range filtering for trial balance (month-end, quarter-end, year-end) per FR-035
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TrialBalanceService {
    
    private final JournalEntryRepository journalEntryRepository;
    private final ChartOfAccountsRepository chartOfAccountsRepository;
    
    /**
     * T044: Generate trial balance for municipality
     * FR-031: System MUST generate trial balance for municipality general ledger
     * FR-032: System MUST list all accounts with debit balance, credit balance, net balance
     * FR-033: System MUST calculate total debits, total credits, difference
     * FR-034: System MUST flag if unbalanced (debits ≠ credits)
     * 
     * @param tenantId The tenant ID
     * @param asOfDate The date to generate trial balance as of (null = current date)
     * @return TrialBalanceResponse with all accounts and balances
     */
    public TrialBalanceResponse generateTrialBalance(UUID tenantId, LocalDate asOfDate) {
        log.info("Generating trial balance for tenant {} as of {}", tenantId, asOfDate);
        
        // Default to current date if not specified
        if (asOfDate == null) {
            asOfDate = LocalDate.now();
        }
        
        // Get municipality entity ID (deterministic based on tenant ID)
        UUID municipalityEntityId = UUID.nameUUIDFromBytes(
                ("MUNICIPALITY-" + tenantId.toString()).getBytes(StandardCharsets.UTF_8));
        
        // T047: Calculate account balances from journal entries
        Map<String, AccountBalanceSummary> accountBalances = calculateAccountBalances(
                tenantId, municipalityEntityId, asOfDate);
        
        // Get list of accounts
        List<AccountBalanceSummary> accounts = new ArrayList<>(accountBalances.values());
        
        // Calculate totals
        BigDecimal totalDebits = accounts.stream()
                .map(AccountBalanceSummary::getDebitBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalCredits = accounts.stream()
                .map(AccountBalanceSummary::getCreditBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal difference = totalDebits.subtract(totalCredits);
        boolean isBalanced = difference.compareTo(BigDecimal.ZERO) == 0;
        String status = isBalanced ? "BALANCED" : "UNBALANCED";
        
        // T048: Group accounts by type (hierarchy)
        Map<String, List<AccountBalanceSummary>> accountsByType = groupAccountsByType(accounts);
        Map<String, BigDecimal> totalsByType = calculateTotalsByType(accountsByType);
        
        log.info("Trial balance generated: {} accounts, total debits={}, total credits={}, difference={}, status={}",
                accounts.size(), totalDebits, totalCredits, difference, status);
        
        return TrialBalanceResponse.builder()
                .asOfDate(asOfDate)
                .accounts(accounts)
                .totalDebits(totalDebits)
                .totalCredits(totalCredits)
                .difference(difference)
                .isBalanced(isBalanced)
                .status(status)
                .accountsByType(accountsByType)
                .totalsByType(totalsByType)
                .accountCount(accounts.size())
                .tenantId(tenantId.toString())
                .entityId(municipalityEntityId.toString())
                .generatedAt(LocalDateTime.now().toString())
                .build();
    }
    
    /**
     * T047: Calculate account balances from journal entries
     * For each account, sum all debits and credits from journal entry lines
     * 
     * @param tenantId The tenant ID
     * @param entityId The municipality entity ID
     * @param asOfDate Calculate balances as of this date
     * @return Map of account number to AccountBalanceSummary
     */
    private Map<String, AccountBalanceSummary> calculateAccountBalances(
            UUID tenantId, UUID entityId, LocalDate asOfDate) {
        
        // Get all chart of accounts for municipality
        List<ChartOfAccounts> accounts = chartOfAccountsRepository.findByTenantId(tenantId);
        
        // Initialize balance map
        Map<String, AccountBalanceSummary> balances = new HashMap<>();
        for (ChartOfAccounts account : accounts) {
            balances.put(account.getAccountNumber(), AccountBalanceSummary.builder()
                    .accountNumber(account.getAccountNumber())
                    .accountName(account.getAccountName())
                    .accountType(account.getAccountType().name())
                    .normalBalance(account.getNormalBalance().name())
                    .debitBalance(BigDecimal.ZERO)
                    .creditBalance(BigDecimal.ZERO)
                    .netBalance(BigDecimal.ZERO)
                    .build());
        }
        
        // Get all journal entries for municipality up to asOfDate
        List<JournalEntry> entries = journalEntryRepository.findByEntityId(entityId);
        
        // Sum debits and credits for each account
        for (JournalEntry entry : entries) {
            // T049: Filter by date
            if (entry.getEntryDate().isAfter(asOfDate)) {
                continue;  // Skip entries after asOfDate
            }
            
            for (JournalEntryLine line : entry.getLines()) {
                String accountNumber = line.getAccountNumber();
                AccountBalanceSummary balance = balances.get(accountNumber);
                
                if (balance != null) {
                    // Add to debit or credit balance
                    balance.setDebitBalance(balance.getDebitBalance().add(line.getDebit()));
                    balance.setCreditBalance(balance.getCreditBalance().add(line.getCredit()));
                    
                    // Calculate net balance based on account type
                    // ASSET and EXPENSE: net = debits - credits
                    // LIABILITY and REVENUE: net = credits - debits
                    if (balance.getAccountType().equals("ASSET") || balance.getAccountType().equals("EXPENSE")) {
                        balance.setNetBalance(balance.getDebitBalance().subtract(balance.getCreditBalance()));
                    } else {
                        balance.setNetBalance(balance.getCreditBalance().subtract(balance.getDebitBalance()));
                    }
                }
            }
        }
        
        // Filter out accounts with zero balance (optional - keep all for completeness)
        return balances;
    }
    
    /**
     * T048: Group accounts by type for hierarchy display
     * 
     * @param accounts List of all accounts
     * @return Map of account type to list of accounts
     */
    private Map<String, List<AccountBalanceSummary>> groupAccountsByType(
            List<AccountBalanceSummary> accounts) {
        
        return accounts.stream()
                .collect(Collectors.groupingBy(AccountBalanceSummary::getAccountType));
    }
    
    /**
     * T048: Calculate totals by account type
     * 
     * @param accountsByType Accounts grouped by type
     * @return Map of account type to total net balance
     */
    private Map<String, BigDecimal> calculateTotalsByType(
            Map<String, List<AccountBalanceSummary>> accountsByType) {
        
        Map<String, BigDecimal> totalsByType = new HashMap<>();
        
        for (Map.Entry<String, List<AccountBalanceSummary>> entry : accountsByType.entrySet()) {
            BigDecimal total = entry.getValue().stream()
                    .map(AccountBalanceSummary::getNetBalance)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            totalsByType.put(entry.getKey(), total);
        }
        
        return totalsByType;
    }
    
    /**
     * T049: Generate trial balance for specific period (month-end, quarter-end, year-end)
     * FR-035: System MUST support trial balance as of specific date
     * 
     * @param tenantId The tenant ID
     * @param year The year
     * @param period The period: "Q1", "Q2", "Q3", "Q4", "M1"-"M12", "YEAR"
     * @return TrialBalanceResponse for the specified period
     */
    public TrialBalanceResponse generateTrialBalanceForPeriod(UUID tenantId, int year, String period) {
        log.info("Generating trial balance for tenant {} for period {} {}", tenantId, period, year);
        
        LocalDate asOfDate;
        
        // Determine end date based on period
        if (period.startsWith("Q")) {
            // Quarter-end
            int quarter = Integer.parseInt(period.substring(1));
            int month = quarter * 3;
            asOfDate = LocalDate.of(year, month, 1).plusMonths(1).minusDays(1);
        } else if (period.startsWith("M")) {
            // Month-end
            int month = Integer.parseInt(period.substring(1));
            asOfDate = LocalDate.of(year, month, 1).plusMonths(1).minusDays(1);
        } else if (period.equals("YEAR")) {
            // Year-end
            asOfDate = LocalDate.of(year, 12, 31);
        } else {
            throw new IllegalArgumentException("Invalid period: " + period + ". Use Q1-Q4, M1-M12, or YEAR");
        }
        
        return generateTrialBalance(tenantId, asOfDate);
    }
}
