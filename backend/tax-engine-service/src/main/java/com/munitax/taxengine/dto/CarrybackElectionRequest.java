package com.munitax.taxengine.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for NOL carryback election.
 * 
 * Contains NOL ID and prior year tax data for carryback calculation.
 * 
 * @see com.munitax.taxengine.domain.nol.NOLCarryback
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarrybackElectionRequest {
    
    @NotNull(message = "NOL ID is required")
    private UUID nolId;
    
    /**
     * Map of prior year → {taxableIncome, taxRate, taxPaid, returnId}
     * Must contain data for up to 5 prior years.
     */
    @NotNull(message = "Prior year data is required")
    @Size(min = 1, max = 5, message = "Must provide 1-5 prior years of data")
    private Map<Integer, PriorYearDataDTO> priorYearData;
    
    /**
     * Prior year tax data DTO.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PriorYearDataDTO {
        
        @NotNull(message = "Taxable income is required")
        @DecimalMin(value = "0.00", message = "Taxable income must be >= 0")
        private BigDecimal taxableIncome;
        
        @NotNull(message = "Tax rate is required")
        @DecimalMin(value = "0.0001", message = "Tax rate must be > 0")
        @DecimalMax(value = "100.00", message = "Tax rate must be <= 100")
        private BigDecimal taxRate;
        
        @NotNull(message = "Tax paid is required")
        @DecimalMin(value = "0.00", message = "Tax paid must be >= 0")
        private BigDecimal taxPaid;
        
        @NotNull(message = "Return ID is required")
        private UUID returnId;
    }
}
