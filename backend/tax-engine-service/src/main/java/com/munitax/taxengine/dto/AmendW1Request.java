package com.munitax.taxengine.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for Amended W-1 Filing request.
 * 
 * Functional Requirements:
 * - FR-003: Support amended W-1 filings with cascade updates
 * - FR-017: Archive amended returns while preserving original
 * 
 * User Story 3 (P2):
 * Business discovers error in earlier period and files amended W-1.
 * System automatically recalculates all subsequent cumulative totals.
 * 
 * @see com.munitax.taxengine.domain.withholding.W1Filing
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AmendW1Request {
    
    /**
     * ID of the original W-1 filing to amend.
     * Must reference an existing W-1 filing with is_amended = FALSE.
     */
    @NotNull(message = "Original filing ID is required")
    private UUID originalFilingId;
    
    /**
     * Reason for filing amendment (required per FR-003).
     * Examples:
     * - "Corrected employee bonus amount"
     * - "Added missing employees"
     * - "Fixed payroll calculation error"
     * - "Corrected W-2 for employee SSN error"
     */
    @NotBlank(message = "Amendment reason is required")
    @Size(max = 500, message = "Amendment reason must be 500 characters or less")
    private String amendmentReason;
    
    /**
     * Corrected gross wages for the period.
     * Must be >= 0.
     */
    @NotNull(message = "Gross wages is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Gross wages must be non-negative")
    @Digits(integer = 13, fraction = 2, message = "Gross wages must have at most 13 digits before decimal and 2 after")
    private BigDecimal grossWages;
    
    /**
     * Corrected taxable wages for the period.
     * If not provided, defaults to gross wages.
     * Must be >= 0 and <= gross wages.
     */
    @DecimalMin(value = "0.0", inclusive = true, message = "Taxable wages must be non-negative")
    @Digits(integer = 13, fraction = 2, message = "Taxable wages must have at most 13 digits before decimal and 2 after")
    private BigDecimal taxableWages;
    
    /**
     * Corrected adjustments for the period.
     * Can be negative (e.g., bonus reversal).
     * Examples:
     * - Prior overpayment credit: -500.00
     * - Additional tax from audit: +250.00
     */
    @Digits(integer = 13, fraction = 2, message = "Adjustments must have at most 13 digits before decimal and 2 after")
    private BigDecimal adjustments;
    
    /**
     * Corrected employee count for the period.
     * Optional - only needed if original filing included employee count.
     * Must be >= 0.
     */
    @Min(value = 0, message = "Employee count must be non-negative")
    private Integer employeeCount;
    
    /**
     * Whether to automatically recalculate cumulative totals for subsequent periods.
     * Default: TRUE (recommended per FR-003).
     * Set to FALSE only if manual review needed before cascade update.
     */
    @Builder.Default
    private Boolean autoRecalculateCumulative = true;
}
