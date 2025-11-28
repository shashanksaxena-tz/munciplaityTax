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
 * NOL (Net Operating Loss) Entity - Represents a net operating loss from a specific tax year.
 * 
 * Functional Requirements:
 * - FR-001: Record NOL generated each year with origin, entity type, jurisdiction, expiration
 * - FR-002: Track NOL usage across years
 * - FR-003: Calculate NOL balance available for current year
 * - FR-021: Assign expiration date to each NOL based on rules
 * 
 * Constitution Compliance:
 * - Principle II: Multi-tenant data isolation via tenant_id
 * - Principle III: Immutable audit trail (created_at, created_by never updated)
 * 
 * @see NOLUsage
 * @see NOLCarryback
 * @see EntityType
 * @see Jurisdiction
 */
@Entity
@Table(name = "nols", schema = "dublin", indexes = {
    @Index(name = "idx_nol_business_year", columnList = "business_id, tax_year"),
    @Index(name = "idx_nol_jurisdiction", columnList = "jurisdiction, municipality_code"),
    @Index(name = "idx_nol_expiration", columnList = "expiration_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NOL {
    
    /**
     * Unique identifier for this NOL record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /**
     * Multi-tenant isolation - tenant owning this NOL.
     * MUST be set on all operations (Constitution Principle II).
     */
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    /**
     * Business profile ID that generated this NOL.
     * References businesses table (not defined in this service).
     */
    @Column(name = "business_id", nullable = false)
    private UUID businessId;
    
    /**
     * Tax year when the loss originated (e.g., 2020, 2021).
     * CHECK constraint: tax_year >= 2000
     */
    @Column(name = "tax_year", nullable = false)
    private Integer taxYear;
    
    /**
     * Tax jurisdiction for this NOL.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "jurisdiction", nullable = false, length = 20)
    private Jurisdiction jurisdiction;
    
    /**
     * Municipality code if jurisdiction is MUNICIPALITY.
     * Null for FEDERAL or STATE_OHIO jurisdictions.
     */
    @Column(name = "municipality_code", length = 10)
    private String municipalityCode;
    
    /**
     * Entity type that generated this NOL.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 20)
    private EntityType entityType;
    
    /**
     * Original NOL amount when loss was first created.
     * Immutable once set.
     * CHECK constraint: original_nol_amount >= 0
     */
    @Column(name = "original_nol_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal originalNOLAmount;
    
    /**
     * Current NOL balance available for future use.
     * Updated as NOL is used each year.
     * CHECK constraint: current_nol_balance >= 0
     * CHECK constraint: current_nol_balance <= original_nol_amount
     */
    @Column(name = "current_nol_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal currentNOLBalance;
    
    /**
     * Total NOL amount used across all years.
     * Sum of all NOLUsage records for this NOL.
     * CHECK constraint: used_amount >= 0
     */
    @Column(name = "used_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal usedAmount = BigDecimal.ZERO;
    
    /**
     * Amount of NOL that expired unused.
     * Non-zero only if NOL reached expiration date with remaining balance.
     * CHECK constraint: expired_amount >= 0
     */
    @Column(name = "expired_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal expiredAmount = BigDecimal.ZERO;
    
    /**
     * Date when this NOL expires (null if indefinite carryforward).
     * Pre-2018 NOLs: tax_year + 20 years
     * Post-2017 NOLs (federal): null (indefinite)
     * State NOLs: depends on state rules
     */
    @Column(name = "expiration_date")
    private LocalDate expirationDate;
    
    /**
     * Number of years NOL can be carried forward.
     * 20 for pre-TCJA NOLs, null for indefinite post-TCJA NOLs.
     */
    @Column(name = "carryforward_years")
    private Integer carryforwardYears;
    
    /**
     * Whether this NOL was carried back to prior years.
     */
    @Column(name = "is_carried_back", nullable = false)
    @Builder.Default
    private Boolean isCarriedBack = false;
    
    /**
     * Amount of NOL used in carryback to prior years.
     * Sum of all NOLCarryback records for this NOL.
     * CHECK constraint: carryback_amount >= 0
     */
    @Column(name = "carryback_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal carrybackAmount = BigDecimal.ZERO;
    
    /**
     * Total tax refund received from carryback.
     * Sum of refund amounts from all NOLCarryback records.
     * CHECK constraint: carryback_refund >= 0
     */
    @Column(name = "carryback_refund", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal carrybackRefund = BigDecimal.ZERO;
    
    /**
     * Ohio apportionment percentage if multi-state business.
     * Used to calculate Ohio NOL = Federal NOL × apportionment_percentage.
     * Range: 0.00 to 100.00 (percentage)
     * Null for federal NOLs or single-state businesses.
     */
    @Column(name = "apportionment_percentage", precision = 5, scale = 2)
    private BigDecimal apportionmentPercentage;
    
    /**
     * Audit trail: When this NOL was created (immutable).
     * Constitution Principle III: Never updated.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    /**
     * Audit trail: User who created this NOL (immutable).
     * References users table via user_id.
     */
    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;
    
    /**
     * Last modification timestamp (e.g., when balance was updated).
     */
    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    /**
     * Helper method to check if NOL has expired based on expiration date.
     * 
     * @return true if NOL is expired, false otherwise
     */
    public boolean isExpired() {
        if (expirationDate == null) {
            return false; // Indefinite carryforward
        }
        return LocalDate.now().isAfter(expirationDate);
    }
    
    /**
     * Helper method to check if NOL has remaining balance available.
     * 
     * @return true if NOL has balance > 0 and not expired, false otherwise
     */
    public boolean hasRemainingBalance() {
        return currentNOLBalance.compareTo(BigDecimal.ZERO) > 0 && !isExpired();
    }
}
