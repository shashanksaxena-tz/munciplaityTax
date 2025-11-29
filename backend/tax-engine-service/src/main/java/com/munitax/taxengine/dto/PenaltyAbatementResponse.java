package com.munitax.taxengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for Penalty Abatement response.
 * 
 * Functional Requirements:
 * - FR-033 to FR-039: Penalty abatement request, review, and approval workflow
 * - FR-036: Form 27-PA generation via pdf-service
 * - FR-037: Workflow states (PENDING, APPROVED, PARTIAL, DENIED, WITHDRAWN)
 * 
 * @see com.munitax.taxengine.domain.penalty.PenaltyAbatement
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PenaltyAbatementResponse {
    
    /**
     * Abatement request ID (UUID).
     */
    private String abatementId;
    
    /**
     * Tax return ID.
     */
    private String returnId;
    
    /**
     * Specific penalty ID (if applicable).
     */
    private String penaltyId;
    
    /**
     * Date when abatement was requested.
     */
    private LocalDate requestDate;
    
    /**
     * Type of abatement: LATE_FILING, LATE_PAYMENT, ESTIMATED, ALL.
     */
    private String abatementType;
    
    /**
     * Amount of penalty requested for abatement.
     */
    private BigDecimal requestedAmount;
    
    /**
     * Reason for requesting abatement.
     */
    private String reason;
    
    /**
     * Detailed explanation.
     */
    private String explanation;
    
    /**
     * Status: PENDING, APPROVED, PARTIAL, DENIED, WITHDRAWN.
     */
    private String status;
    
    /**
     * Reviewer user ID.
     */
    private String reviewedBy;
    
    /**
     * Date when review decision was made.
     */
    private LocalDate reviewDate;
    
    /**
     * Amount approved for abatement.
     */
    private BigDecimal approvedAmount;
    
    /**
     * Approval percentage (approved / requested × 100).
     */
    private BigDecimal approvalPercentage;
    
    /**
     * Reason for denial (if status = DENIED).
     */
    private String denialReason;
    
    /**
     * Path to generated Form 27-PA PDF.
     */
    private String formGenerated;
    
    /**
     * When abatement request was created.
     */
    private LocalDateTime createdAt;
    
    /**
     * Last modification timestamp.
     */
    private LocalDateTime updatedAt;
    
    /**
     * Status message for UI display.
     */
    private String statusMessage;
    
    /**
     * Whether request can be withdrawn.
     */
    private Boolean canWithdraw;
    
    /**
     * Whether request can be edited.
     */
    private Boolean canEdit;
    
    /**
     * Generate status message based on current status.
     * 
     * @return human-readable status message
     */
    public String generateStatusMessage() {
        switch (status) {
            case "PENDING":
                return "Your abatement request is pending review by municipal staff.";
            case "APPROVED":
                return String.format("Your abatement request was approved for the full amount of $%,.2f.", approvedAmount);
            case "PARTIAL":
                return String.format("Your abatement request was partially approved for $%,.2f of the requested $%,.2f (%.1f%%).",
                        approvedAmount, requestedAmount, approvalPercentage);
            case "DENIED":
                return String.format("Your abatement request was denied. Reason: %s", denialReason);
            case "WITHDRAWN":
                return "You have withdrawn your abatement request.";
            default:
                return "Status unknown.";
        }
    }
}
