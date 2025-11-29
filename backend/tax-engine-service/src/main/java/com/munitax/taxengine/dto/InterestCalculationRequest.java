package com.munitax.taxengine.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for Interest Calculation request.
 * 
 * Functional Requirements:
 * - FR-027 to FR-032: Interest calculation with quarterly compounding
 * - FR-028: Interest rate retrieved from rule-engine-service
 * - FR-029: Quarterly compounding
 * 
 * @see com.munitax.taxengine.domain.penalty.Interest
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterestCalculationRequest {
    
    /**
     * Tenant ID for multi-tenant isolation.
     */
    @NotNull(message = "Tenant ID is required")
    private UUID tenantId;
    
    /**
     * Tax return ID for which interest is being calculated.
     */
    @NotNull(message = "Return ID is required")
    private UUID returnId;
    
    /**
     * Original due date for the tax return.
     */
    @NotNull(message = "Tax due date is required")
    private LocalDate taxDueDate;
    
    /**
     * Unpaid tax amount subject to interest.
     */
    @NotNull(message = "Unpaid tax amount is required")
    @DecimalMin(value = "0.00", message = "Unpaid tax amount must be non-negative")
    private BigDecimal unpaidTaxAmount;
    
    /**
     * Interest calculation start date (typically tax due date).
     * If null, uses taxDueDate.
     */
    private LocalDate startDate;
    
    /**
     * Interest calculation end date (typically payment date or current date).
     * If null, uses current date.
     */
    private LocalDate endDate;
    
    /**
     * Annual interest rate (if provided, otherwise retrieved from rule-engine-service).
     * FR-028: Federal short-term rate + 3%, typically 3-8%.
     */
    @DecimalMin(value = "0.0000", message = "Annual interest rate must be non-negative")
    @DecimalMax(value = "1.0000", message = "Annual interest rate must be <= 1.0000 (100%)")
    private BigDecimal annualInterestRate;
    
    /**
     * User ID who triggered the calculation (for audit trail).
     */
    @NotNull(message = "Created by is required")
    private UUID createdBy;
    
    /**
     * Whether to retrieve current interest rate from rule-engine-service.
     * If true, ignores annualInterestRate parameter.
     */
    @Builder.Default
    private Boolean retrieveCurrentRate = true;
    
    /**
     * Whether to include quarterly breakdown in response.
     */
    @Builder.Default
    private Boolean includeQuarterlyBreakdown = true;
}
