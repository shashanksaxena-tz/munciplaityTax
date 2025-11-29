package com.munitax.taxengine.domain.nol;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * NOLAmendment Entity - Represents amended return that changes NOL.
 * 
 * Functional Requirements:
 * - FR-040: Recalculate NOL when amended return filed
 * - FR-041: Identify cascading effects of amended NOL
 * - FR-042: Generate amended NOL schedule
 * - FR-043: Offer to prepare amended returns for subsequent years
 * 
 * Constitution Compliance:
 * - Principle II: Multi-tenant data isolation via tenant_id
 * - Principle III: Immutable audit trail
 * 
 * @see NOL
 */
@Entity
@Table(name = "nol_amendments", schema = "dublin", indexes = {
    @Index(name = "idx_nol_amendment_nol", columnList = "nol_id"),
    @Index(name = "idx_nol_amendment_original", columnList = "original_return_id"),
    @Index(name = "idx_nol_amendment_amended", columnList = "amended_return_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NOLAmendment {
    
    /**
     * Unique identifier for this amendment record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /**
     * Multi-tenant isolation - tenant owning this amendment.
     */
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    /**
     * Reference to the NOL affected by this amendment.
     */
    @Column(name = "nol_id", nullable = false)
    private UUID nolId;
    
    /**
     * Reference to original tax return.
     * References tax_returns table (not defined in this service).
     */
    @Column(name = "original_return_id", nullable = false)
    private UUID originalReturnId;
    
    /**
     * Reference to amended tax return.
     * References tax_returns table (not defined in this service).
     */
    @Column(name = "amended_return_id", nullable = false)
    private UUID amendedReturnId;
    
    /**
     * Date when amendment was filed.
     */
    @Column(name = "amendment_date", nullable = false)
    private LocalDate amendmentDate;
    
    /**
     * Original NOL amount before amendment.
     * CHECK constraint: original_nol >= 0
     */
    @Column(name = "original_nol", nullable = false, precision = 15, scale = 2)
    private BigDecimal originalNOL;
    
    /**
     * Amended NOL amount after amendment.
     * CHECK constraint: amended_nol >= 0
     */
    @Column(name = "amended_nol", nullable = false, precision = 15, scale = 2)
    private BigDecimal amendedNOL;
    
    /**
     * Difference between amended and original NOL.
     * = amended_nol - original_nol
     * Can be positive (NOL increased) or negative (NOL decreased/eliminated)
     */
    @Column(name = "nol_change", nullable = false, precision = 15, scale = 2)
    private BigDecimal nolChange;
    
    /**
     * User explanation for why amendment was filed.
     * Example: "Discovered unreported business expense of $100K"
     */
    @Column(name = "reason_for_amendment", nullable = false, columnDefinition = "TEXT")
    private String reasonForAmendment;
    
    /**
     * Comma-separated list of subsequent tax years that used this NOL.
     * Example: "2023,2024,2025"
     * Used to identify which years may need amended returns.
     */
    @Column(name = "affected_years", length = 100)
    private String affectedYears;
    
    /**
     * Estimated total refund if subsequent years are also amended.
     * = sum of (nol_change × tax_rate) for all affected years
     * CHECK constraint: estimated_refund >= 0
     */
    @Column(name = "estimated_refund", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal estimatedRefund = BigDecimal.ZERO;
    
    /**
     * Comma-separated list of amended return IDs filed for subsequent years.
     * Example: "uuid1,uuid2,uuid3"
     * Empty if no cascading amendments filed yet.
     */
    @Column(name = "cascading_amendments", columnDefinition = "TEXT")
    private String cascadingAmendments;
    
    /**
     * Audit trail: When this amendment record was created (immutable).
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}
