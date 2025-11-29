package com.munitax.ledger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * T016 - Request DTO for tax assessment creation
 * Used when a tax return is filed and needs to be recorded in the ledger
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxAssessmentRequest {
    
    /**
     * Tenant identifier (municipality ID)
     */
    private UUID tenantId;
    
    /**
     * Filer entity identifier (business or individual)
     */
    private UUID filerId;
    
    /**
     * Tax return identifier that triggered this assessment
     */
    private UUID returnId;
    
    /**
     * Base tax amount assessed
     */
    private BigDecimal taxAmount;
    
    /**
     * Penalty amount (if applicable, defaults to 0)
     */
    private BigDecimal penaltyAmount;
    
    /**
     * Interest amount (if applicable, defaults to 0)
     */
    private BigDecimal interestAmount;
    
    /**
     * Tax year (e.g., "2024")
     */
    private String taxYear;
    
    /**
     * Tax period (e.g., "Q1", "Q2", "Annual")
     */
    private String taxPeriod;
    
    /**
     * Calculate total amount due
     */
    public BigDecimal getTotalAmount() {
        BigDecimal total = taxAmount != null ? taxAmount : BigDecimal.ZERO;
        if (penaltyAmount != null) {
            total = total.add(penaltyAmount);
        }
        if (interestAmount != null) {
            total = total.add(interestAmount);
        }
        return total;
    }
}
