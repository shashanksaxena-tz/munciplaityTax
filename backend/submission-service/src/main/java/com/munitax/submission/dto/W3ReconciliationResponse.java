package com.munitax.submission.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for W-3 reconciliation operations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class W3ReconciliationResponse {
    
    private UUID id;
    private String tenantId;
    private String businessId;
    private Integer taxYear;
    private BigDecimal totalW1Tax;
    private BigDecimal totalW2Tax;
    private BigDecimal discrepancy;
    private String status;
    private Integer w1FilingCount;
    private Integer w2FormCount;
    private Integer totalEmployees;
    private BigDecimal lateFilingPenalty;
    private BigDecimal missingFilingPenalty;
    private BigDecimal totalPenalties;
    private LocalDate dueDate;
    private Instant filingDate;
    private Boolean isSubmitted;
    private String confirmationNumber;
    private String notes;
    private Instant createdAt;
    private String createdBy;
    private Instant updatedAt;
    private List<String> w1FilingIds;
}
