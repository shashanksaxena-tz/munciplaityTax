package com.munitax.ledger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentAllocation {
    private BigDecimal toTax;
    private BigDecimal toPenalty;
    private BigDecimal toInterest;
}
