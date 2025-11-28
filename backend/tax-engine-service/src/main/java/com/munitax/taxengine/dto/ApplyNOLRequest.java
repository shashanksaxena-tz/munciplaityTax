package com.munitax.taxengine.dto;

import com.munitax.taxengine.domain.nol.Jurisdiction;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for applying NOL deduction to a tax return.
 * 
 * Validation Rules:
 * - businessId: Required
 * - returnId: Required
 * - taxYear: Required, >= 2000
 * - taxableIncomeBeforeNOL: Required, >= 0
 * - nolDeductionAmount: Required, >= 0
 * - taxRate: Required, > 0
 * 
 * @see com.munitax.taxengine.domain.nol.NOLUsage
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplyNOLRequest {
    
    @NotNull(message = "Business ID is required")
    private UUID businessId;
    
    @NotNull(message = "Return ID is required")
    private UUID returnId;
    
    @NotNull(message = "Tax year is required")
    @Min(value = 2000, message = "Tax year must be >= 2000")
    private Integer taxYear;
    
    @NotNull(message = "Taxable income before NOL is required")
    @DecimalMin(value = "0.00", message = "Taxable income must be >= 0")
    private BigDecimal taxableIncomeBeforeNOL;
    
    @NotNull(message = "NOL deduction amount is required")
    @DecimalMin(value = "0.00", message = "NOL deduction must be >= 0")
    private BigDecimal nolDeductionAmount;
    
    @NotNull(message = "Tax rate is required")
    @DecimalMin(value = "0.0001", message = "Tax rate must be > 0")
    @DecimalMax(value = "100.00", message = "Tax rate must be <= 100")
    private BigDecimal taxRate;
    
    private Jurisdiction jurisdiction;
}
