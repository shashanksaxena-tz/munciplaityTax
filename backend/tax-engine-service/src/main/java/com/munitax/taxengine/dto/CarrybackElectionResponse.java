package com.munitax.taxengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for NOL carryback election.
 * 
 * Contains carryback details including refund amounts by year.
 * 
 * @see com.munitax.taxengine.domain.nol.NOLCarryback
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarrybackElectionResponse {
    
    private UUID nolId;
    private Integer nolTaxYear;
    private BigDecimal totalNOLCarriedBack;
    private BigDecimal totalRefund;
    private BigDecimal remainingNOL;
    
    private List<CarrybackYearDetail> carrybackDetails;
    
    /**
     * Carryback detail for a single prior year.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CarrybackYearDetail {
        private UUID carrybackId;
        private Integer carrybackYear;
        private BigDecimal priorYearTaxableIncome;
        private BigDecimal nolApplied;
        private BigDecimal priorYearTaxRate;
        private BigDecimal refundAmount;
        private String refundStatus;
        private LocalDate filedDate;
        private LocalDate refundDate;
    }
}
