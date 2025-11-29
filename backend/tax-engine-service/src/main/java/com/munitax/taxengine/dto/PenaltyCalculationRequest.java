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
 * DTO for Penalty Calculation request.
 * 
 * Functional Requirements:
 * - FR-001 to FR-006: Late filing penalty calculation (5% per month, max 25%)
 * - FR-007 to FR-011: Late payment penalty calculation (1% per month, max 25%)
 * - FR-012 to FR-014: Combined penalty cap (max 5% per month when both apply)
 * 
 * @see com.munitax.taxengine.domain.penalty.Penalty
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PenaltyCalculationRequest {
    
    /**
     * Tenant ID for multi-tenant isolation.
     */
    @NotNull(message = "Tenant ID is required")
    private UUID tenantId;
    
    /**
     * Tax return ID for which penalty is being calculated.
     */
    @NotNull(message = "Return ID is required")
    private UUID returnId;
    
    /**
     * Type of penalty to calculate: LATE_FILING, LATE_PAYMENT, ESTIMATED_UNDERPAYMENT.
     */
    @NotBlank(message = "Penalty type is required")
    private String penaltyType;
    
    /**
     * Original due date for this tax return.
     */
    @NotNull(message = "Tax due date is required")
    private LocalDate taxDueDate;
    
    /**
     * Actual filing or payment date.
     * If null, uses current date.
     */
    private LocalDate actualDate;
    
    /**
     * Unpaid tax amount subject to penalty.
     */
    @NotNull(message = "Unpaid tax amount is required")
    @DecimalMin(value = "0.00", message = "Unpaid tax amount must be non-negative")
    private BigDecimal unpaidTaxAmount;
    
    /**
     * User ID who triggered the calculation (for audit trail).
     */
    @NotNull(message = "Created by is required")
    private UUID createdBy;
    
    /**
     * Whether to apply combined penalty cap (FR-012 to FR-014).
     * If true and both filing and payment penalties exist, apply 5%/month cap.
     */
    @Builder.Default
    private Boolean applyCombinedCap = false;
    
    /**
     * Whether to check for existing penalties before creating new ones.
     * If true, returns existing penalty if one already exists for this return and type.
     */
    @Builder.Default
    private Boolean checkExisting = true;
}
