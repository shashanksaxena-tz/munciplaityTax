package com.munitax.ledger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * DTO for trial balance response
 * T045: Create TrialBalanceResponse DTO
 * T048: Support account hierarchy grouping
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrialBalanceResponse {
    private LocalDate asOfDate;                              // T049: Date for trial balance
    private List<AccountBalanceSummary> accounts;             // All accounts with balances
    private BigDecimal totalDebits;                           // Sum of all debit balances
    private BigDecimal totalCredits;                          // Sum of all credit balances
    private BigDecimal difference;                            // Difference (should be zero)
    private boolean isBalanced;                               // T042: Balance validation flag
    private String status;                                    // "BALANCED" or "UNBALANCED"
    
    // T048: Account hierarchy grouping
    private Map<String, List<AccountBalanceSummary>> accountsByType;  // Grouped by ASSET, LIABILITY, etc.
    private Map<String, BigDecimal> totalsByType;             // Totals for each account type
    
    // Metadata
    private int accountCount;                                 // Total number of accounts
    private String tenantId;
    private String entityId;                                  // Municipality entity ID
    private String generatedAt;                               // Timestamp when generated
}
