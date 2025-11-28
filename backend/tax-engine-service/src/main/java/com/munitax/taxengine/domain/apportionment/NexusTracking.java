package com.munitax.taxengine.domain.apportionment;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Nexus Tracking entity for tracking nexus status in each state/municipality.
 * Used for throwback rule determination.
 */
@Entity
@Table(name = "nexus_tracking", 
    uniqueConstraints = @UniqueConstraint(
        name = "uk_nexus_tracking_business_state_year", 
        columnNames = {"business_id", "state", "municipality", "tax_year"}
    ),
    indexes = {
        @Index(name = "idx_nexus_tracking_business_id", columnList = "business_id"),
        @Index(name = "idx_nexus_tracking_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_nexus_tracking_state", columnList = "state"),
        @Index(name = "idx_nexus_tracking_has_nexus", columnList = "has_nexus"),
        @Index(name = "idx_nexus_tracking_tax_year", columnList = "tax_year")
    }
)
@Data
@NoArgsConstructor
public class NexusTracking {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "nexus_id", updatable = false, nullable = false)
    private UUID nexusId;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "tax_year", nullable = false)
    private Integer taxYear;

    @Column(name = "state", nullable = false, length = 2)
    private String state;

    @Column(name = "municipality", length = 100)
    private String municipality;

    @Column(name = "has_nexus", nullable = false)
    private Boolean hasNexus = false;

    @ElementCollection
    @CollectionTable(name = "nexus_reasons", joinColumns = @JoinColumn(name = "nexus_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "reason")
    private List<NexusReason> nexusReasons = new ArrayList<>();

    @Column(name = "sales_in_state", precision = 15, scale = 2)
    private BigDecimal salesInState = BigDecimal.ZERO;

    @Column(name = "property_in_state", precision = 15, scale = 2)
    private BigDecimal propertyInState = BigDecimal.ZERO;

    @Column(name = "payroll_in_state", precision = 15, scale = 2)
    private BigDecimal payrollInState = BigDecimal.ZERO;

    @Column(name = "employee_count_in_state")
    private Integer employeeCountInState = 0;

    @Column(name = "economic_nexus_threshold", precision = 15, scale = 2)
    private BigDecimal economicNexusThreshold;

    @Column(name = "transaction_count_in_state")
    private Integer transactionCountInState = 0;

    @Column(name = "nexus_established_date")
    private LocalDate nexusEstablishedDate;

    @Column(name = "nexus_terminated_date")
    private LocalDate nexusTerminatedDate;

    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "last_modified_date", nullable = false)
    private LocalDateTime lastModifiedDate;

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        lastModifiedDate = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastModifiedDate = LocalDateTime.now();
    }

    /**
     * Determine if nexus is established based on available data.
     */
    public void evaluateNexus() {
        this.nexusReasons.clear();

        // Physical presence nexus
        if (propertyInState != null && propertyInState.compareTo(BigDecimal.ZERO) > 0) {
            this.nexusReasons.add(NexusReason.PHYSICAL_PRESENCE);
        }

        // Employee presence nexus
        if (employeeCountInState != null && employeeCountInState > 0) {
            this.nexusReasons.add(NexusReason.EMPLOYEE_PRESENCE);
        }
        if (payrollInState != null && payrollInState.compareTo(BigDecimal.ZERO) > 0) {
            if (!this.nexusReasons.contains(NexusReason.EMPLOYEE_PRESENCE)) {
                this.nexusReasons.add(NexusReason.EMPLOYEE_PRESENCE);
            }
        }

        // Economic nexus (post-Wayfair)
        if (economicNexusThreshold != null && salesInState != null 
            && salesInState.compareTo(economicNexusThreshold) >= 0) {
            this.nexusReasons.add(NexusReason.ECONOMIC_NEXUS);
        }

        // Factor presence nexus
        if ((salesInState != null && salesInState.compareTo(BigDecimal.ZERO) > 0)
            || (propertyInState != null && propertyInState.compareTo(BigDecimal.ZERO) > 0)
            || (payrollInState != null && payrollInState.compareTo(BigDecimal.ZERO) > 0)) {
            this.nexusReasons.add(NexusReason.FACTOR_PRESENCE);
        }

        // Set has_nexus flag
        this.hasNexus = !this.nexusReasons.isEmpty();
    }

    /**
     * Add a nexus reason if not already present.
     */
    public void addNexusReason(NexusReason reason) {
        if (!this.nexusReasons.contains(reason)) {
            this.nexusReasons.add(reason);
            this.hasNexus = true;
        }
    }
}
