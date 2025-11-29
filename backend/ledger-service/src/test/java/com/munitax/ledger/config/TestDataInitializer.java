package com.munitax.ledger.config;

import com.munitax.ledger.enums.AccountType;
import com.munitax.ledger.enums.NormalBalance;
import com.munitax.ledger.model.ChartOfAccounts;
import com.munitax.ledger.repository.ChartOfAccountsRepository;
import com.munitax.ledger.util.TestConstants;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Initialize test chart of accounts for integration tests
 */
@Component
@Profile("test")
@RequiredArgsConstructor
public class TestDataInitializer {
    
    private final ChartOfAccountsRepository chartOfAccountsRepository;
    
    public UUID getTestTenantId() {
        return TestConstants.TEST_TENANT_ID;
    }
    
    @PostConstruct
    public void initTestData() {
        // Filer accounts
        createAccount("1000", "Cash", AccountType.ASSET, NormalBalance.DEBIT);
        createAccount("1200", "Refund Receivable", AccountType.ASSET, NormalBalance.DEBIT);
        createAccount("2100", "Tax Liability (Current Year)", AccountType.LIABILITY, NormalBalance.CREDIT);
        createAccount("2110", "Tax Liability (Prior Years)", AccountType.LIABILITY, NormalBalance.CREDIT);
        createAccount("2120", "Penalty Liability", AccountType.LIABILITY, NormalBalance.CREDIT);
        createAccount("2130", "Interest Liability", AccountType.LIABILITY, NormalBalance.CREDIT);
        createAccount("6100", "Tax Expense", AccountType.EXPENSE, NormalBalance.DEBIT);
        
        // Municipality accounts
        createAccount("1200-MUNI", "Accounts Receivable", AccountType.ASSET, NormalBalance.DEBIT);
        createAccount("2200", "Refunds Payable", AccountType.LIABILITY, NormalBalance.CREDIT);
        createAccount("4100", "Tax Revenue", AccountType.REVENUE, NormalBalance.CREDIT);
        createAccount("4200", "Penalty Revenue", AccountType.REVENUE, NormalBalance.CREDIT);
        createAccount("4300", "Interest Revenue", AccountType.REVENUE, NormalBalance.CREDIT);
        createAccount("5200", "Refund Expense", AccountType.EXPENSE, NormalBalance.DEBIT);
    }
    
    private void createAccount(String accountNumber, String accountName, AccountType accountType, NormalBalance normalBalance) {
        if (chartOfAccountsRepository.findByAccountNumber(accountNumber).isEmpty()) {
            ChartOfAccounts account = ChartOfAccounts.builder()
                    .accountNumber(accountNumber)
                    .accountName(accountName)
                    .accountType(accountType)
                    .normalBalance(normalBalance)
                    .tenantId(TestConstants.TEST_TENANT_ID)
                    .active(true)
                    .build();
            chartOfAccountsRepository.save(account);
        }
    }
}
