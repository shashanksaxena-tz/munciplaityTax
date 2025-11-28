package com.munitax.taxengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for property factor in apportionment calculation.
 * Represents owned and rented property values in Ohio vs everywhere.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyFactorDto {

    /**
     * Unique identifier for the property factor record.
     */
    private UUID id;

    /**
     * Value of owned property located in Ohio.
     */
    @NotNull(message = "Ohio property value is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Ohio property value must be non-negative")
    private BigDecimal ohioPropertyValue;

    /**
     * Value of owned property located everywhere (total).
     */
    @NotNull(message = "Total property value is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Total property value must be non-negative")
    private BigDecimal totalPropertyValue;

    /**
     * Annual rent paid for property in Ohio (8x multiplier applied for capitalization).
     */
    @DecimalMin(value = "0.0", inclusive = true, message = "Rented property value must be non-negative")
    @Builder.Default
    private BigDecimal rentedPropertyValue = BigDecimal.ZERO;

    /**
     * Annual rent paid for property everywhere (8x multiplier applied for capitalization).
     */
    @DecimalMin(value = "0.0", inclusive = true, message = "Total rented property value must be non-negative")
    @Builder.Default
    private BigDecimal totalRentedPropertyValue = BigDecimal.ZERO;

    /**
     * Calculated property factor percentage (0-100).
     * Formula: (Ohio property + 8x Ohio rent) / (Total property + 8x Total rent)
     */
    private BigDecimal propertyFactorPercentage;

    /**
     * Ohio property numerator (including capitalized rent).
     */
    private BigDecimal ohioPropertyNumerator;

    /**
     * Everywhere property denominator (including capitalized rent).
     */
    private BigDecimal everywherePropertyDenominator;

    /**
     * Notes about property factor calculation.
     */
    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;

    // Alias methods for backward compatibility with controller
    public BigDecimal getPropertyInOhio() {
        return ohioPropertyValue;
    }

    public void setPropertyInOhio(BigDecimal propertyInOhio) {
        this.ohioPropertyValue = propertyInOhio;
    }

    public BigDecimal getTotalPropertyEverywhere() {
        return totalPropertyValue;
    }

    public void setTotalPropertyEverywhere(BigDecimal totalPropertyEverywhere) {
        this.totalPropertyValue = totalPropertyEverywhere;
    }

    public BigDecimal getRentedPropertyInOhio() {
        return rentedPropertyValue;
    }

    public void setRentedPropertyInOhio(BigDecimal rentedPropertyInOhio) {
        this.rentedPropertyValue = rentedPropertyInOhio;
    }

    public BigDecimal getRentedPropertyEverywhere() {
        return totalRentedPropertyValue;
    }

    public void setRentedPropertyEverywhere(BigDecimal rentedPropertyEverywhere) {
        this.totalRentedPropertyValue = rentedPropertyEverywhere;
    }
}
