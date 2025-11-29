package com.munitax.ledger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountStatementResponse {
    private String accountName;
    private LocalDate statementDate;
    private BigDecimal beginningBalance;
    private BigDecimal endingBalance;
    private BigDecimal totalDebits;
    private BigDecimal totalCredits;
    private List<StatementTransaction> transactions;
}
