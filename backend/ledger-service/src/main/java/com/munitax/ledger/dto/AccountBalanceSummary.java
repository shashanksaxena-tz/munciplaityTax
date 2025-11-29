package com.munitax.ledger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for trial balance line items
 * T046: Create AccountBalanceSummary DTO for trial balance line items
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountBalanceSummary {
    private String accountNumber;
    private String accountName;
    private String accountType;        // ASSET, LIABILITY, REVENUE, EXPENSE
    private BigDecimal debitBalance;   // Total debit balance
    private BigDecimal creditBalance;  // Total credit balance
    private BigDecimal netBalance;     // Net balance (debit - credit or credit - debit depending on account type)
    private String normalBalance;      // DEBIT or CREDIT
}
