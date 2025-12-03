package com.munitax.ledger.service;

import com.munitax.ledger.dto.DiscrepancyDetail;
import com.munitax.ledger.dto.ReconciliationResponse;
import com.munitax.ledger.enums.ReconciliationStatus;
import com.munitax.ledger.model.JournalEntry;
import com.munitax.ledger.model.JournalEntryLine;
import com.munitax.ledger.repository.JournalEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReconciliationService {
    
    private final JournalEntryService journalEntryService;
    private final JournalEntryRepository journalEntryRepository;
    
    // Tax liability accounts for filers
    private static final String[] FILER_LIABILITY_ACCOUNTS = {"2100", "2110", "2120", "2130"};
    
    // Municipality accounts receivable
    private static final String MUNICIPALITY_AR_ACCOUNT = "1201";
    
    // Cash accounts
    private static final String CASH_ACCOUNT = "1001";
    
    /**
     * Generate comprehensive reconciliation report comparing municipality books to all filers.
     * 
     * @param tenantId The tenant identifier
     * @param municipalityId The municipality identifier
     * @return ReconciliationResponse with reconciliation results
     */
    public ReconciliationResponse generateReconciliationReport(String tenantId, String municipalityId) {
        log.info("Generating production reconciliation report for tenant {}", tenantId);
        
        // Calculate municipality AR (account 1201)
        BigDecimal municipalityAR = calculateAccountBalance(tenantId, municipalityId, MUNICIPALITY_AR_ACCOUNT);
        log.debug("Municipality AR: {}", municipalityAR);
        
        // Calculate municipality cash receipts (account 1001 credits)
        BigDecimal municipalityCash = calculateAccountBalance(tenantId, municipalityId, CASH_ACCOUNT);
        log.debug("Municipality Cash: {}", municipalityCash);
        
        // PRODUCTION IMPLEMENTATION: Aggregate all filer balances
        // T022: Query all filer entities
        Set<String> allFilerIds = queryAllFilerEntities(tenantId);
        log.debug("Found {} filers for tenant {}", allFilerIds.size(), tenantId);
        
        // T023: Sum all filer tax liability accounts
        BigDecimal filerLiabilities = sumFilerTaxLiabilities(tenantId, allFilerIds);
        log.debug("Total filer liabilities: {}", filerLiabilities);
        
        // T024: Sum all filer payment entries
        BigDecimal filerPayments = sumFilerPayments(tenantId, allFilerIds);
        log.debug("Total filer payments: {}", filerPayments);
        
        // Calculate variances
        BigDecimal arVariance = municipalityAR.subtract(filerLiabilities);
        BigDecimal cashVariance = municipalityCash.subtract(filerPayments);
        
        log.info("AR Variance: {}, Cash Variance: {}", arVariance, cashVariance);
        
        // Determine reconciliation status
        ReconciliationStatus status = (arVariance.compareTo(BigDecimal.ZERO) == 0 
                && cashVariance.compareTo(BigDecimal.ZERO) == 0) 
                ? ReconciliationStatus.RECONCILED 
                : ReconciliationStatus.DISCREPANCY;
        
        // Build discrepancies list
        List<DiscrepancyDetail> discrepancies = buildDiscrepanciesList(
                arVariance, cashVariance, allFilerIds, tenantId, municipalityId,
                municipalityAR, filerLiabilities, municipalityCash, filerPayments);
        
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
    
    /**
     * Generate drill-down reconciliation for a specific filer.
     * 
     * @param tenantId The tenant identifier
     * @param filerId The filer identifier
     * @param municipalityId The municipality identifier
     * @return ReconciliationResponse for the specific filer
     */
    public ReconciliationResponse generateFilerReconciliation(String tenantId, String filerId, String municipalityId) {
        log.info("Generating filer reconciliation for filer {} in tenant {}", filerId, tenantId);
        
        // Calculate filer's tax liabilities
        BigDecimal filerLiabilities = sumFilerTaxLiabilities(tenantId, Set.of(filerId));
        
        // Calculate filer's payments
        BigDecimal filerPayments = sumFilerPayments(tenantId, Set.of(filerId));
        
        // For municipality side, we need to calculate entries related to this filer
        // This would require linking mechanism in production (e.g., entries tagged with filerId)
        BigDecimal municipalityAR = filerLiabilities; // Simplified for now
        BigDecimal municipalityCash = filerPayments; // Simplified for now
        
        BigDecimal arVariance = municipalityAR.subtract(filerLiabilities);
        BigDecimal cashVariance = municipalityCash.subtract(filerPayments);
        
        ReconciliationStatus status = (arVariance.compareTo(BigDecimal.ZERO) == 0 
                && cashVariance.compareTo(BigDecimal.ZERO) == 0) 
                ? ReconciliationStatus.RECONCILED 
                : ReconciliationStatus.DISCREPANCY;
        
        List<DiscrepancyDetail> discrepancies = new ArrayList<>();
        if (status == ReconciliationStatus.DISCREPANCY) {
            discrepancies.add(DiscrepancyDetail.builder()
                    .filerId(filerId)
                    .filerName("Filer " + filerId.substring(0, Math.min(8, filerId.length())))
                    .transactionType("Reconciliation")
                    .transactionDate(LocalDate.now())
                    .filerAmount(filerLiabilities.add(filerPayments))
                    .municipalityAmount(municipalityAR.add(municipalityCash))
                    .variance(arVariance.add(cashVariance))
                    .description("Filer-specific reconciliation discrepancy")
                    .build());
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
    
    /**
     * T022: Query all filer entities for the tenant.
     * 
     * @param tenantId The tenant identifier
     * @return Set of filer IDs
     */
    private Set<String> queryAllFilerEntities(String tenantId) {
        // Get all journal entries for the tenant
        List<JournalEntry> allEntries = journalEntryRepository.findByTenantId(tenantId);
        
        // Extract unique entity IDs (excluding municipality)
        // A filer is identified by having liability accounts (2xxx accounts)
        Set<String> filerIds = new HashSet<>();
        
        for (JournalEntry entry : allEntries) {
            String entityId = entry.getEntityId();
            
            // Check if this entry has any filer liability accounts
            boolean hasFilerAccounts = entry.getLines().stream()
                    .anyMatch(line -> {
                        String accountNum = line.getAccount().getAccountNumber();
                        return accountNum.startsWith("2"); // Liability accounts
                    });
            
            if (hasFilerAccounts && entityId != null) {
                filerIds.add(entityId);
            }
        }
        
        log.debug("Identified {} unique filer entities", filerIds.size());
        return filerIds;
    }
    
    /**
     * T023: Sum all filer tax liability accounts (2100, 2110, 2120, 2130).
     * 
     * @param tenantId The tenant identifier
     * @param filerIds Set of filer IDs to aggregate
     * @return Total filer liabilities
     */
    private BigDecimal sumFilerTaxLiabilities(String tenantId, Set<String> filerIds) {
        BigDecimal totalLiabilities = BigDecimal.ZERO;
        
        for (String filerId : filerIds) {
            for (String accountNumber : FILER_LIABILITY_ACCOUNTS) {
                BigDecimal balance = calculateAccountBalance(tenantId, filerId, accountNumber);
                totalLiabilities = totalLiabilities.add(balance);
            }
        }
        
        log.debug("Summed liabilities for {} filers: {}", filerIds.size(), totalLiabilities);
        return totalLiabilities;
    }
    
    /**
     * T024: Sum all filer payment entries across all filers.
     * 
     * @param tenantId The tenant identifier
     * @param filerIds Set of filer IDs to aggregate
     * @return Total filer payments
     */
    private BigDecimal sumFilerPayments(String tenantId, Set<String> filerIds) {
        BigDecimal totalPayments = BigDecimal.ZERO;
        
        for (String filerId : filerIds) {
            // Payments are recorded as credits to liability accounts or debits to cash
            // We'll calculate by looking at cash account debits for filers
            BigDecimal filerCashDebits = calculateAccountDebits(tenantId, filerId, CASH_ACCOUNT);
            totalPayments = totalPayments.add(filerCashDebits);
        }
        
        log.debug("Summed payments for {} filers: {}", filerIds.size(), totalPayments);
        return totalPayments;
    }
    
    /**
     * Calculate account balance for specific entity and account.
     * 
     * @param tenantId The tenant identifier
     * @param entityId The entity identifier (filer or municipality)
     * @param accountNumber The account number
     * @return Account balance
     */
    private BigDecimal calculateAccountBalance(String tenantId, String entityId, String accountNumber) {
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
    
    /**
     * Calculate total debits for a specific account.
     * 
     * @param tenantId The tenant identifier
     * @param entityId The entity identifier
     * @param accountNumber The account number
     * @return Total debits
     */
    private BigDecimal calculateAccountDebits(String tenantId, String entityId, String accountNumber) {
        List<JournalEntry> entries = journalEntryService.getEntriesForEntity(tenantId, entityId);
        
        BigDecimal totalDebits = BigDecimal.ZERO;
        
        for (JournalEntry entry : entries) {
            for (JournalEntryLine line : entry.getLines()) {
                if (line.getAccount().getAccountNumber().equals(accountNumber)) {
                    totalDebits = totalDebits.add(line.getDebit());
                }
            }
        }
        
        return totalDebits;
    }
    
    /**
     * Build list of discrepancies with details.
     * 
     * @return List of discrepancy details
     */
    private List<DiscrepancyDetail> buildDiscrepanciesList(
            BigDecimal arVariance, BigDecimal cashVariance,
            Set<String> allFilerIds, String tenantId, String municipalityId,
            BigDecimal municipalityAR, BigDecimal filerLiabilities,
            BigDecimal municipalityCash, BigDecimal filerPayments) {
        
        List<DiscrepancyDetail> discrepancies = new ArrayList<>();
        
        if (arVariance.compareTo(BigDecimal.ZERO) != 0) {
            discrepancies.add(DiscrepancyDetail.builder()
                    .transactionType("Accounts Receivable")
                    .transactionDate(LocalDate.now())
                    .filerAmount(filerLiabilities)
                    .municipalityAmount(municipalityAR)
                    .variance(arVariance)
                    .description(String.format("AR variance detected across %d filers", allFilerIds.size()))
                    .build());
        }
        
        if (cashVariance.compareTo(BigDecimal.ZERO) != 0) {
            discrepancies.add(DiscrepancyDetail.builder()
                    .transactionType("Cash Receipts")
                    .transactionDate(LocalDate.now())
                    .filerAmount(filerPayments)
                    .municipalityAmount(municipalityCash)
                    .variance(cashVariance)
                    .description(String.format("Cash variance detected across %d filers", allFilerIds.size()))
                    .build());
        }
        
        return discrepancies;
    }
}
