package com.munitax.taxengine.dto;

import com.munitax.taxengine.domain.apportionment.SaleType;
import com.munitax.taxengine.domain.apportionment.ServiceSourcingMethod;
import com.munitax.taxengine.domain.apportionment.SourcingMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for individual sale transaction in sales factor calculation.
 * Represents a single sale with sourcing and throwback information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaleTransactionDto {

    /**
     * Unique identifier for the sale transaction.
     */
    private UUID id;

    /**
     * Type of sale (tangible goods, service, rental, interest, royalty).
     */
    @NotNull(message = "Sale type is required")
    private SaleType saleType;

    /**
     * Transaction amount.
     */
    @NotNull(message = "Sale amount is required")
    @DecimalMin(value = "0.01", message = "Sale amount must be greater than zero")
    private BigDecimal amount;

    /**
     * Destination state where goods were shipped or service was performed.
     */
    @NotBlank(message = "Destination state is required")
    @Size(min = 2, max = 2, message = "State code must be 2 characters")
    @Pattern(regexp = "^[A-Z]{2}$", message = "State code must be uppercase letters")
    private String destinationState;

    /**
     * Origin state where goods were shipped from (for throwback calculation).
     */
    @Size(min = 2, max = 2, message = "State code must be 2 characters")
    @Pattern(regexp = "^[A-Z]{2}$", message = "State code must be uppercase letters")
    private String originState;

    /**
     * Customer location for market-based sourcing (service transactions).
     */
    @Size(max = 200, message = "Customer location cannot exceed 200 characters")
    private String customerLocation;

    /**
     * Customer state for market-based sourcing (service transactions).
     * T097 [US3]: Added for service revenue sourcing.
     */
    @Size(min = 2, max = 2, message = "State code must be 2 characters")
    @Pattern(regexp = "^[A-Z]{2}$", message = "State code must be uppercase letters")
    private String customerState;

    /**
     * Service sourcing method used for this transaction (market-based or cost-of-performance).
     * T097 [US3]: Added for service revenue breakdown display.
     */
    private ServiceSourcingMethod serviceSourcingMethod;

    /**
     * Sourcing method applied to this transaction.
     */
    private SourcingMethod sourcingMethod;

    /**
     * Whether throwback rule was applied to this transaction.
     */
    @Builder.Default
    private Boolean throwbackApplied = false;

    /**
     * Amount thrown back to origin state (if throwback applied).
     */
    @DecimalMin(value = "0.0", inclusive = true, message = "Throwback amount must be non-negative")
    @Builder.Default
    private BigDecimal throwbackAmount = BigDecimal.ZERO;

    /**
     * Amount sourced to Ohio after sourcing and throwback rules.
     */
    @DecimalMin(value = "0.0", inclusive = true, message = "Ohio sourced amount must be non-negative")
    @Builder.Default
    private BigDecimal ohioSourcedAmount = BigDecimal.ZERO;

    /**
     * Description or reference for this transaction.
     */
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    /**
     * External transaction reference or invoice number.
     */
    @Size(max = 100, message = "Transaction reference cannot exceed 100 characters")
    private String transactionReference;
}
