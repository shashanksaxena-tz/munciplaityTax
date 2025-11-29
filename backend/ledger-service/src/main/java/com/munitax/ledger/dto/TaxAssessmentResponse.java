package com.munitax.ledger.dto;

import com.munitax.ledger.enums.SourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * T017 - Response DTO for tax assessment creation
 * Returns journal entry details and assessment summary
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxAssessmentResponse {
    
    /**
     * Journal entry ID created for filer books
     */
    private UUID filerJournalEntryId;
    
    /**
     * Journal entry number (e.g., JE-2024-00001)
     */
    private String entryNumber;
    
    /**
     * Filer entity identifier
     */
    private UUID filerId;
    
    /**
     * Tax return identifier
     */
    private UUID returnId;
    
    /**
     * Date of assessment
     */
    private LocalDate assessmentDate;
    
    /**
     * Tax amount assessed
     */
    private BigDecimal taxAmount;
    
    /**
     * Penalty amount (if applicable)
     */
    private BigDecimal penaltyAmount;
    
    /**
     * Interest amount (if applicable)
     */
    private BigDecimal interestAmount;
    
    /**
     * Total amount due
     */
    private BigDecimal totalAmount;
    
    /**
     * Tax year
     */
    private String taxYear;
    
    /**
     * Tax period (Q1, Q2, etc.)
     */
    private String taxPeriod;
    
    /**
     * Source type (should be TAX_ASSESSMENT)
     */
    private SourceType sourceType;
    
    /**
     * Timestamp when assessment was recorded
     */
    private LocalDateTime createdAt;
    
    /**
     * Description of the assessment
     */
    private String description;
    
    /**
     * Status message
     */
    private String message;
}
