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
 * DTO for Penalty Abatement request.
 * 
 * Functional Requirements:
 * - FR-033 to FR-039: Penalty abatement request, review, and approval workflow
 * - FR-034: Reasonable cause reasons (DEATH, ILLNESS, DISASTER, etc.)
 * 
 * @see com.munitax.taxengine.domain.penalty.PenaltyAbatement
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PenaltyAbatementRequest {
    
    /**
     * Tenant ID for multi-tenant isolation.
     */
    @NotNull(message = "Tenant ID is required")
    private UUID tenantId;
    
    /**
     * Tax return ID for which abatement is requested.
     */
    @NotNull(message = "Return ID is required")
    private UUID returnId;
    
    /**
     * Specific penalty ID to abate (optional - null means all penalties for return).
     */
    private UUID penaltyId;
    
    /**
     * Date when abatement is requested.
     * If null, uses current date.
     */
    private LocalDate requestDate;
    
    /**
     * Type of abatement: LATE_FILING, LATE_PAYMENT, ESTIMATED, ALL.
     */
    @NotBlank(message = "Abatement type is required")
    private String abatementType;
    
    /**
     * Amount of penalty requesting to abate.
     */
    @NotNull(message = "Requested amount is required")
    @DecimalMin(value = "0.01", message = "Requested amount must be positive")
    private BigDecimal requestedAmount;
    
    /**
     * Reason for requesting abatement.
     * Valid values: DEATH, ILLNESS, DISASTER, MISSING_RECORDS, ERRONEOUS_ADVICE, FIRST_TIME, OTHER
     */
    @NotBlank(message = "Reason is required")
    private String reason;
    
    /**
     * Detailed explanation of the reasonable cause.
     */
    @NotBlank(message = "Explanation is required")
    @Size(min = 10, max = 5000, message = "Explanation must be between 10 and 5000 characters")
    private String explanation;
    
    /**
     * Supporting documents metadata (JSON string).
     * Contains document IDs, types, upload dates, etc.
     */
    private String supportingDocuments;
    
    /**
     * User ID who submitted the abatement request.
     */
    @NotNull(message = "Submitter ID is required")
    private UUID submittedBy;
    
    /**
     * Contact email for follow-up.
     */
    @Email(message = "Valid email is required")
    private String contactEmail;
    
    /**
     * Contact phone for follow-up.
     */
    private String contactPhone;
}
