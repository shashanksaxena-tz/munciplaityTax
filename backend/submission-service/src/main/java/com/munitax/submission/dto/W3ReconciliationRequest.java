package com.munitax.submission.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO for creating a W-3 year-end reconciliation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class W3ReconciliationRequest {
    
    /**
     * Business ID (FEIN) filing this W-3.
     */
    @NotBlank(message = "Business ID is required")
    private String businessId;
    
    /**
     * Tax year for this reconciliation (e.g., 2024).
     */
    @NotNull(message = "Tax year is required")
    @Min(value = 2020, message = "Tax year must be 2020 or later")
    @Max(value = 2099, message = "Tax year must be valid")
    private Integer taxYear;
    
    /**
     * Total tax from W-2 forms reported to employees.
     * Sum of Box 19 (Local Income Tax) from all employee W-2s.
     */
    @NotNull(message = "Total W-2 tax is required")
    @DecimalMin(value = "0.00", message = "Total W-2 tax must be non-negative")
    @Digits(integer = 15, fraction = 2, message = "Total W-2 tax must have at most 2 decimal places")
    private BigDecimal totalW2Tax;
    
    /**
     * Total number of W-2 forms included.
     */
    @Min(value = 0, message = "W-2 form count must be non-negative")
    private Integer w2FormCount;
    
    /**
     * Total number of employees reported on W-2s.
     */
    @Min(value = 0, message = "Total employees must be non-negative")
    private Integer totalEmployees;
    
    /**
     * Optional notes about the reconciliation.
     */
    private String notes;
    
    /**
     * Optional list of specific W-1 filing IDs to include.
     * If not provided, all W-1 filings for the year will be included.
     */
    private List<String> w1FilingIds;
}
