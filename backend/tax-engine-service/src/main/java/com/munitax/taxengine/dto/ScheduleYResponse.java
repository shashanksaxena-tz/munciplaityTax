package com.munitax.taxengine.dto;

import com.munitax.taxengine.domain.apportionment.SourcingMethodElection;
import com.munitax.taxengine.domain.apportionment.ThrowbackElection;
import com.munitax.taxengine.domain.apportionment.ServiceSourcingMethod;
import com.munitax.taxengine.domain.apportionment.ApportionmentFormula;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for Schedule Y multi-state sourcing filing.
 * Contains complete filing information including calculated apportionment percentage.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleYResponse {

    /**
     * The Schedule Y unique identifier.
     */
    private UUID id;

    /**
     * The business ID for which Schedule Y was filed.
     */
    private UUID businessId;

    /**
     * The return ID this Schedule Y is associated with.
     */
    private UUID returnId;

    /**
     * The tax year for this filing.
     */
    private Integer taxYear;

    /**
     * The apportionment formula used.
     */
    private ApportionmentFormula apportionmentFormula;

    /**
     * Sourcing method election for affiliated group sales.
     */
    private SourcingMethodElection sourcingMethodElection;

    /**
     * Throwback rule election for sales to no-nexus states.
     */
    private ThrowbackElection throwbackElection;

    /**
     * Service sourcing method election.
     */
    private ServiceSourcingMethod serviceSourcingMethod;

    /**
     * Calculated apportionment percentage (0-100).
     */
    private BigDecimal apportionmentPercentage;

    /**
     * Property factor details and calculation.
     */
    private PropertyFactorDto propertyFactor;

    /**
     * Payroll factor details and calculation.
     */
    private PayrollFactorDto payrollFactor;

    /**
     * Sales factor details and calculation.
     */
    private SalesFactorDto salesFactor;

    /**
     * Detailed breakdown of apportionment calculation.
     */
    private ApportionmentBreakdownDto apportionmentBreakdown;

    /**
     * Nexus status information for relevant states.
     */
    private NexusStatusDto nexusStatus;

    /**
     * Notes or comments about this filing.
     */
    private String notes;

    /**
     * Whether this is an amended filing.
     */
    private Boolean isAmended;

    /**
     * The original Schedule Y ID if this is an amended filing.
     */
    private UUID originalScheduleYId;

    /**
     * Filing date and time.
     */
    private LocalDateTime filedAt;

    /**
     * User who filed the Schedule Y.
     */
    private UUID filedBy;

    /**
     * Last modification date and time.
     */
    private LocalDateTime updatedAt;

    /**
     * User who last modified the Schedule Y.
     */
    private UUID updatedBy;

    /**
     * Creation timestamp.
     */
    private LocalDateTime createdAt;

    /**
     * Formula comparison details (Task T134 [US5]).
     * Contains traditional vs single-sales-factor comparison and recommendation.
     * Only populated if formula comparison was requested.
     */
    private FormulaComparisonDto formulaComparison;
}
