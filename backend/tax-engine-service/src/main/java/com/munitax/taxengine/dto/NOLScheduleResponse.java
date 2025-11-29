package com.munitax.taxengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for NOL schedule.
 * 
 * Contains consolidated NOL schedule information including vintages.
 * 
 * @see com.munitax.taxengine.domain.nol.NOLSchedule
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NOLScheduleResponse {
    
    private UUID id;
    private UUID businessId;
    private UUID returnId;
    private Integer taxYear;
    
    private BigDecimal totalBeginningBalance;
    private BigDecimal newNOLGenerated;
    private BigDecimal totalAvailableNOL;
    private BigDecimal nolDeduction;
    private BigDecimal expiredNOL;
    private BigDecimal totalEndingBalance;
    
    private BigDecimal limitationPercentage;
    private BigDecimal taxableIncomeBeforeNOL;
    private BigDecimal taxableIncomeAfterNOL;
    
    private List<NOLVintageResponse> vintages;
    
    private LocalDateTime createdAt;
    
    /**
     * NOL vintage detail in schedule response.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NOLVintageResponse {
        private Integer taxYear;
        private BigDecimal originalAmount;
        private BigDecimal previouslyUsed;
        private BigDecimal expired;
        private BigDecimal availableThisYear;
        private BigDecimal usedThisYear;
        private BigDecimal remainingForFuture;
        private String expirationDate;
        private Boolean isCarriedBack;
        private BigDecimal carrybackAmount;
    }
}
