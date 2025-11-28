package com.munitax.taxengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for NOL operations.
 * 
 * Contains full NOL details including balance, expiration, and usage information.
 * 
 * @see com.munitax.taxengine.domain.nol.NOL
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NOLResponse {
    
    private UUID id;
    private UUID businessId;
    private Integer taxYear;
    private String jurisdiction;
    private String municipalityCode;
    private String entityType;
    
    private BigDecimal originalNOLAmount;
    private BigDecimal currentNOLBalance;
    private BigDecimal usedAmount;
    private BigDecimal expiredAmount;
    
    private LocalDate expirationDate;
    private Integer carryforwardYears;
    private Boolean isExpired;
    private Boolean hasRemainingBalance;
    
    private Boolean isCarriedBack;
    private BigDecimal carrybackAmount;
    private BigDecimal carrybackRefund;
    
    private BigDecimal apportionmentPercentage;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
