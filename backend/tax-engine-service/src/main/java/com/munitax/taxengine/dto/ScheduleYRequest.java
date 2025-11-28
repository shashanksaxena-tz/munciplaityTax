package com.munitax.taxengine.dto;

import com.munitax.taxengine.domain.apportionment.SourcingMethodElection;
import com.munitax.taxengine.domain.apportionment.ThrowbackElection;
import com.munitax.taxengine.domain.apportionment.ServiceSourcingMethod;
import com.munitax.taxengine.domain.apportionment.ApportionmentFormula;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for creating or updating Schedule Y multi-state sourcing filing.
 * Contains all necessary information for apportionment calculation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleYRequest {

    /**
     * The business ID for which Schedule Y is being filed.
     */
    @NotNull(message = "Business ID is required")
    private UUID businessId;

    /**
     * The return ID this Schedule Y is associated with.
     */
    @NotNull(message = "Return ID is required")
    private UUID returnId;

    /**
     * The tax year for this filing (e.g., 2024).
     */
    @NotNull(message = "Tax year is required")
    @Min(value = 2000, message = "Tax year must be 2000 or later")
    @Max(value = 2100, message = "Tax year must be 2100 or earlier")
    private Integer taxYear;

    /**
     * The apportionment formula to use (e.g., THREE_FACTOR, FOUR_FACTOR_DOUBLE_WEIGHTED_SALES).
     */
    @NotNull(message = "Apportionment formula is required")
    private ApportionmentFormula apportionmentFormula;

    /**
     * Sourcing method election for affiliated group sales (Finnigan vs Joyce).
     */
    @NotNull(message = "Sourcing method election is required")
    private SourcingMethodElection sourcingMethodElection;

    /**
     * Throwback rule election for sales to no-nexus states.
     */
    @NotNull(message = "Throwback election is required")
    private ThrowbackElection throwbackElection;

    /**
     * Service sourcing method election (market-based vs cost-of-performance).
     */
    private ServiceSourcingMethod serviceSourcingMethod;

    /**
     * Property factor details.
     */
    @Valid
    private PropertyFactorDto propertyFactor;

    /**
     * Payroll factor details.
     */
    @Valid
    private PayrollFactorDto payrollFactor;

    /**
     * Sales factor details.
     */
    @NotNull(message = "Sales factor is required")
    @Valid
    private SalesFactorDto salesFactor;

    /**
     * Notes or comments about this filing.
     */
    @Size(max = 2000, message = "Notes cannot exceed 2000 characters")
    private String notes;

    /**
     * Whether this is an amended filing.
     */
    @Builder.Default
    private Boolean isAmended = false;

    /**
     * The original Schedule Y ID if this is an amended filing.
     */
    private UUID originalScheduleYId;
}
