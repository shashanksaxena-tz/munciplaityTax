package com.munitax.taxengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * DTO for sales factor in apportionment calculation.
 * Represents sales revenue sourced to Ohio vs everywhere, with throwback adjustments.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesFactorDto {

    /**
     * Unique identifier for the sales factor record.
     */
    private UUID id;

    /**
     * Total sales sourced to Ohio after all adjustments.
     */
    @NotNull(message = "Ohio sales is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Ohio sales must be non-negative")
    private BigDecimal ohioSales;

    /**
     * Total sales everywhere (includes all affiliated group sales if Finnigan elected).
     */
    @NotNull(message = "Total sales is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Total sales must be non-negative")
    private BigDecimal totalSales;

    /**
     * Amount of sales thrown back to Ohio due to no-nexus destination states.
     */
    @DecimalMin(value = "0.0", inclusive = true, message = "Throwback adjustment must be non-negative")
    @Builder.Default
    private BigDecimal throwbackAdjustment = BigDecimal.ZERO;

    /**
     * Service revenue portion of total sales.
     */
    @DecimalMin(value = "0.0", inclusive = true, message = "Service revenue must be non-negative")
    @Builder.Default
    private BigDecimal serviceRevenue = BigDecimal.ZERO;

    /**
     * Tangible goods sales portion of total sales.
     */
    @DecimalMin(value = "0.0", inclusive = true, message = "Tangible goods sales must be non-negative")
    @Builder.Default
    private BigDecimal tangibleGoodsSales = BigDecimal.ZERO;

    /**
     * Calculated sales factor percentage (0-100).
     * Formula: (Ohio sales + throwback adjustment) / Total sales
     */
    private BigDecimal salesFactorPercentage;

    /**
     * List of individual sale transactions.
     */
    @Valid
    private List<SaleTransactionDto> saleTransactions;

    /**
     * Total number of sale transactions.
     */
    private Integer transactionCount;

    /**
     * Number of transactions with throwback applied.
     */
    private Integer throwbackTransactionCount;

    /**
     * List of throwback adjustment line items showing detailed breakdown.
     * Task: T079 [US2]
     */
    private List<ThrowbackAdjustmentDto> throwbackAdjustments;

    /**
     * Notes about sales factor calculation or adjustments.
     */
    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;

    // Alias methods for backward compatibility with controller
    public BigDecimal getSalesInOhio() {
        return ohioSales;
    }

    public void setSalesInOhio(BigDecimal salesInOhio) {
        this.ohioSales = salesInOhio;
    }

    public BigDecimal getTotalSalesEverywhere() {
        return totalSales;
    }

    public void setTotalSalesEverywhere(BigDecimal totalSalesEverywhere) {
        this.totalSales = totalSalesEverywhere;
    }
}
