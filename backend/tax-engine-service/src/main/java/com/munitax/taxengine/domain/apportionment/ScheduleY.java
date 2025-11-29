package com.munitax.taxengine.domain.apportionment;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Schedule Y entity representing multi-state apportionment schedule (Form 27-Y).
 * Contains factor calculations, sourcing elections, and final apportionment percentage.
 *
 * @see ApportionmentFormula
 * @see SourcingMethodElection
 * @see ThrowbackElection
 */
@Entity
@Table(name = "schedule_y", indexes = {
    @Index(name = "idx_schedule_y_return_id", columnList = "return_id"),
    @Index(name = "idx_schedule_y_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_schedule_y_tax_year", columnList = "tax_year"),
    @Index(name = "idx_schedule_y_status", columnList = "status")
})
@Data
@NoArgsConstructor
public class ScheduleY {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "schedule_y_id", updatable = false, nullable = false)
    private UUID scheduleYId;

    @Column(name = "return_id", nullable = false)
    private UUID returnId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "tax_year", nullable = false)
    private Integer taxYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "apportionment_formula", nullable = false, length = 50)
    private ApportionmentFormula apportionmentFormula;

    @Type(JsonType.class)
    @Column(name = "formula_weights", columnDefinition = "jsonb")
    private Map<String, Integer> formulaWeights;

    @Column(name = "property_factor_percentage", precision = 5, scale = 2)
    private BigDecimal propertyFactorPercentage;

    @Column(name = "payroll_factor_percentage", precision = 5, scale = 2)
    private BigDecimal payrollFactorPercentage;

    @Column(name = "sales_factor_percentage", precision = 5, scale = 2)
    private BigDecimal salesFactorPercentage;

    @Column(name = "final_apportionment_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal finalApportionmentPercentage;

    @Enumerated(EnumType.STRING)
    @Column(name = "sourcing_method_election", nullable = false, length = 30)
    private SourcingMethodElection sourcingMethodElection;

    @Enumerated(EnumType.STRING)
    @Column(name = "throwback_election", length = 20)
    private ThrowbackElection throwbackElection;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_sourcing_method", length = 30)
    private ServiceSourcingMethod serviceSourcingMethod;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "DRAFT";

    @Column(name = "amends_schedule_y_id")
    private UUID amendsScheduleYId;

    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "last_modified_date", nullable = false)
    private LocalDateTime lastModifiedDate;

    @Column(name = "last_modified_by")
    private UUID lastModifiedBy;

    // Relationships
    @OneToOne(mappedBy = "scheduleY", cascade = CascadeType.ALL, orphanRemoval = true)
    private PropertyFactor propertyFactor;

    @OneToOne(mappedBy = "scheduleY", cascade = CascadeType.ALL, orphanRemoval = true)
    private PayrollFactor payrollFactor;

    @OneToOne(mappedBy = "scheduleY", cascade = CascadeType.ALL, orphanRemoval = true)
    private SalesFactor salesFactor;

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        lastModifiedDate = LocalDateTime.now();
        if (status == null) {
            status = "DRAFT";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastModifiedDate = LocalDateTime.now();
    }

    /**
     * Calculate final apportionment percentage based on formula and factor percentages.
     * Formula: (sum of weighted factors) / (sum of weights)
     */
    public void calculateApportionment() {
        if (apportionmentFormula == null) {
            throw new IllegalStateException("Apportionment formula must be set");
        }

        BigDecimal weightedSum = BigDecimal.ZERO;
        int totalWeight = 0;

        if (apportionmentFormula == ApportionmentFormula.CUSTOM) {
            if (formulaWeights == null || formulaWeights.isEmpty()) {
                throw new IllegalStateException("Custom formula requires formula weights");
            }
            if (propertyFactorPercentage != null && formulaWeights.containsKey("property")) {
                weightedSum = weightedSum.add(propertyFactorPercentage.multiply(BigDecimal.valueOf(formulaWeights.get("property"))));
                totalWeight += formulaWeights.get("property");
            }
            if (payrollFactorPercentage != null && formulaWeights.containsKey("payroll")) {
                weightedSum = weightedSum.add(payrollFactorPercentage.multiply(BigDecimal.valueOf(formulaWeights.get("payroll"))));
                totalWeight += formulaWeights.get("payroll");
            }
            if (salesFactorPercentage != null && formulaWeights.containsKey("sales")) {
                weightedSum = weightedSum.add(salesFactorPercentage.multiply(BigDecimal.valueOf(formulaWeights.get("sales"))));
                totalWeight += formulaWeights.get("sales");
            }
        } else {
            // Use standard formula weights
            if (propertyFactorPercentage != null && apportionmentFormula.getPropertyWeight() > 0) {
                weightedSum = weightedSum.add(propertyFactorPercentage.multiply(BigDecimal.valueOf(apportionmentFormula.getPropertyWeight())));
                totalWeight += apportionmentFormula.getPropertyWeight();
            }
            if (payrollFactorPercentage != null && apportionmentFormula.getPayrollWeight() > 0) {
                weightedSum = weightedSum.add(payrollFactorPercentage.multiply(BigDecimal.valueOf(apportionmentFormula.getPayrollWeight())));
                totalWeight += apportionmentFormula.getPayrollWeight();
            }
            if (salesFactorPercentage != null && apportionmentFormula.getSalesWeight() > 0) {
                weightedSum = weightedSum.add(salesFactorPercentage.multiply(BigDecimal.valueOf(apportionmentFormula.getSalesWeight())));
                totalWeight += apportionmentFormula.getSalesWeight();
            }
        }

        if (totalWeight == 0) {
            throw new IllegalStateException("No factors available for apportionment calculation");
        }

        this.finalApportionmentPercentage = weightedSum.divide(BigDecimal.valueOf(totalWeight), 2, BigDecimal.ROUND_HALF_UP);
    }
}
