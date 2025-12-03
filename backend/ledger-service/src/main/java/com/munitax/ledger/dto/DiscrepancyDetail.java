package com.munitax.ledger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscrepancyDetail {
    private String filerId;
    private String filerName;
    private String transactionType;
    private LocalDate transactionDate;
    private BigDecimal filerAmount;
    private BigDecimal municipalityAmount;
    private BigDecimal variance;
    private String description;
}
