package com.munitax.ledger.dto;

import com.munitax.ledger.enums.SourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatementTransaction {
    private LocalDate transactionDate;
    private SourceType transactionType;
    private String description;
    private BigDecimal debitAmount;
    private BigDecimal creditAmount;
    private BigDecimal runningBalance;
    private String entryNumber;
}
