package com.munitax.taxengine.domain.nol;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * NOLCarryback Entity - Represents NOL carried back to prior years for refund.
 * 
 * Functional Requirements:
 * - FR-013: Support NOL carryback election for 2018-2020 losses (CARES Act)
 * - FR-016: Calculate carryback amount using FIFO ordering
 * - FR-017: Calculate refund amount for each carryback year
 * - FR-018: Generate Form 27-NOL-CB
 * - FR-020: Support state-specific carryback rules
 * 
 * Constitution Compliance:
 * - Principle II: Multi-tenant data isolation via tenant_id
 * - Principle III: Immutable audit trail
 * 
 * @see NOL
 * @see RefundStatus
 */
@Entity
@Table(name = "nol_carrybacks", schema = "dublin", indexes = {
    @Index(name = "idx_nol_carryback_nol", columnList = "nol_id"),
    @Index(name = "idx_nol_carryback_year", columnList = "carryback_year"),
    @Index(name = "idx_nol_carryback_status", columnList = "refund_status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NOLCarryback {
    
    /**
     * Unique identifier for this carryback record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /**
     * Multi-tenant isolation - tenant owning this carryback.
     */
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    /**
     * Reference to the NOL being carried back.
     */
    @Column(name = "nol_id", nullable = false)
    private UUID nolId;
    
    /**
     * Tax year to which NOL is carried back (e.g., 2015, 2016).
     * Must be within 5 years prior to NOL year for CARES Act.
     * CHECK constraint: carryback_year < NOL.tax_year
     */
    @Column(name = "carryback_year", nullable = false)
    private Integer carrybackYear;
    
    /**
     * Taxable income in the carryback year before NOL application.
     * Retrieved from prior year tax return.
     * CHECK constraint: prior_year_taxable_income >= 0
     */
    @Column(name = "prior_year_taxable_income", nullable = false, precision = 15, scale = 2)
    private BigDecimal priorYearTaxableIncome;
    
    /**
     * Amount of NOL applied to this carryback year.
     * Limited to prior_year_taxable_income (cannot create negative income).
     * CHECK constraint: nol_applied > 0
     * CHECK constraint: nol_applied <= prior_year_taxable_income
     */
    @Column(name = "nol_applied", nullable = false, precision = 15, scale = 2)
    private BigDecimal nolApplied;
    
    /**
     * Tax rate in the carryback year (percentage).
     * Used to calculate refund: refund_amount = nol_applied × prior_year_tax_rate.
     * CHECK constraint: prior_year_tax_rate > 0
     */
    @Column(name = "prior_year_tax_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal priorYearTaxRate;
    
    /**
     * Refund amount calculated for this carryback year.
     * = nol_applied × prior_year_tax_rate
     * CHECK constraint: refund_amount >= 0
     */
    @Column(name = "refund_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal refundAmount;
    
    /**
     * Reference to prior year tax return.
     * References tax_returns table (not defined in this service).
     */
    @Column(name = "prior_year_return_id", nullable = false)
    private UUID priorYearReturnId;
    
    /**
     * Reference to generated Form 27-NOL-CB.
     * References forms table (not defined in this service).
     */
    @Column(name = "carryback_form_id")
    private UUID carrybackFormId;
    
    /**
     * Current status of the refund claim.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "refund_status", nullable = false, length = 20)
    @Builder.Default
    private RefundStatus refundStatus = RefundStatus.CLAIMED;
    
    /**
     * Date when refund was received by taxpayer.
     * Null until refund_status = PAID.
     */
    @Column(name = "refund_date")
    private LocalDate refundDate;
    
    /**
     * Date when carryback claim was filed.
     */
    @Column(name = "filed_date", nullable = false)
    private LocalDate filedDate;
    
    /**
     * Audit trail: When this carryback record was created (immutable).
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    /**
     * Last modification timestamp (e.g., status change).
     */
    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
