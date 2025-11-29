package com.munitax.taxengine.dto;

import com.munitax.taxengine.domain.nol.EntityType;
import com.munitax.taxengine.domain.nol.Jurisdiction;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for creating a new NOL record.
 * 
 * Validation Rules:
 * - businessId: Required
 * - taxYear: Required, >= 2000
 * - lossAmount: Required, > 0
 * - jurisdiction: Required
 * - entityType: Required
 * 
 * @see com.munitax.taxengine.domain.nol.NOL
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateNOLRequest {
    
    @NotNull(message = "Business ID is required")
    private UUID businessId;
    
    @NotNull(message = "Tax year is required")
    @Min(value = 2000, message = "Tax year must be >= 2000")
    @Max(value = 2100, message = "Tax year must be <= 2100")
    private Integer taxYear;
    
    @NotNull(message = "Loss amount is required")
    @DecimalMin(value = "0.01", message = "Loss amount must be positive")
    private BigDecimal lossAmount;
    
    @NotNull(message = "Jurisdiction is required")
    private Jurisdiction jurisdiction;
    
    @NotNull(message = "Entity type is required")
    private EntityType entityType;
    
    /**
     * Ohio apportionment percentage (0-100).
     * Required if jurisdiction is STATE_OHIO.
     */
    @DecimalMin(value = "0.00", message = "Apportionment percentage must be >= 0")
    @DecimalMax(value = "100.00", message = "Apportionment percentage must be <= 100")
    private BigDecimal apportionmentPercentage;
    
    /**
     * Municipality code if jurisdiction is MUNICIPALITY.
     */
    @Size(max = 10, message = "Municipality code must be <= 10 characters")
    private String municipalityCode;
}
