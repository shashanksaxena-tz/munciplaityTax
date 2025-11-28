package com.munitax.ledger.dto;

import com.munitax.ledger.enums.ReconciliationStatus;
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
public class ReconciliationResponse {
    private LocalDate reportDate;
    private BigDecimal municipalityAR;
    private BigDecimal filerLiabilities;
    private BigDecimal arVariance;
    private BigDecimal municipalityCash;
    private BigDecimal filerPayments;
    private BigDecimal cashVariance;
    private ReconciliationStatus status;
    private List<DiscrepancyDetail> discrepancies;
}
