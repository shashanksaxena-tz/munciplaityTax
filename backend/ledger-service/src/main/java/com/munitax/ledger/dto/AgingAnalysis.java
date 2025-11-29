package com.munitax.ledger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for aging analysis of outstanding balances.
 * Breaks down balances by age: 0-30, 31-60, 61-90, 90+ days.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgingAnalysis {
    private BigDecimal current;         // 0-30 days
    private BigDecimal thirtyDays;      // 31-60 days
    private BigDecimal sixtyDays;       // 61-90 days
    private BigDecimal ninetyPlus;      // 90+ days
    private BigDecimal totalOutstanding;
    private LocalDate analysisDate;
}
