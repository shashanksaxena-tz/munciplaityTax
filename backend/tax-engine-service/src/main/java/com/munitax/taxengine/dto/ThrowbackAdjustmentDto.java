package com.munitax.taxengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for throwback adjustment line item details.
 * Shows individual sales thrown back to origin state.
 * Task: T079 [US2]
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThrowbackAdjustmentDto {

    /**
     * The sale transaction ID.
     */
    private UUID transactionId;

    /**
     * The destination state where goods were shipped (no nexus).
     */
    private String destinationState;

    /**
     * The origin state where goods were shipped from.
     */
    private String originState;

    /**
     * The sale amount.
     */
    private BigDecimal saleAmount;

    /**
     * The amount thrown back to origin state.
     */
    private BigDecimal throwbackAmount;

    /**
     * Description of the throwback adjustment.
     * Example: "Sales to CA - No nexus - Thrown back: $100,000"
     */
    private String description;

    /**
     * Whether this sale was thrown out (excluded from numerator and denominator).
     */
    @Builder.Default
    private Boolean thrownOut = false;
}
