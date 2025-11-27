package com.munitax.taxengine.domain.withholding;

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
 * W1Filing Entity - Represents a single withholding return filing (Form W-1).
 * 
 * Functional Requirements:
 * - FR-001: Maintains complete W-1 filing history for each business profile
 * - FR-003: Supports amended W-1 filings with reference to original
 * - FR-011: Tracks late-filing penalties
 * - FR-013: Supports multiple filing frequencies (daily, semi-monthly, monthly, quarterly)
 * 
 * Constitution Compliance:
 * - Principle II: Multi-tenant data isolation via tenant_id
 * - Principle III: Immutable audit trail (created_at, created_by never updated)
 * 
 * @see FilingFrequency
 * @see W1FilingStatus
 */
@Entity
@Table(name = "w1_filings", schema = "dublin")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class W1Filing {
    
    /**
     * Unique identifier for this W-1 filing.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /**
     * Multi-tenant isolation - tenant owning this filing.
     * MUST be set on all operations (Constitution Principle II).
     */
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    /**
     * Business profile ID (EIN holder) filing this W-1.
     * References businesses table (not defined in this service).
     */
    @Column(name = "business_id", nullable = false)
    private UUID businessId;
    
    /**
     * Tax year for this filing (e.g., 2024).
     * CHECK constraint: tax_year >= 2020
     */
    @Column(name = "tax_year", nullable = false)
    private Integer taxYear;
    
    /**
     * Filing frequency for this business.
     * Determines due date calculation (FR-013).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "filing_frequency", nullable = false, length = 20)
    private FilingFrequency filingFrequency;
    
    /**
     * Period identifier: Q1, Q2, Q3, Q4 for quarterly; M01-M12 for monthly;
     * D20240115 for daily; SM01-SM24 for semi-monthly.
     */
    @Column(name = "period", nullable = false, length = 10)
    private String period;
    
    /**
     * First day of the filing period.
     */
    @Column(name = "period_start_date", nullable = false)
    private LocalDate periodStartDate;
    
    /**
     * Last day of the filing period.
     */
    @Column(name = "period_end_date", nullable = false)
    private LocalDate periodEndDate;
    
    /**
     * Calculated due date for this filing.
     * Stored immutably to prevent recalculation bugs (Research R5).
     * Accounts for weekends and federal holidays.
     */
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;
    
    /**
     * Actual submission timestamp when W-1 was filed.
     * Used to calculate late-filing penalties if filing_date > due_date.
     */
    @Column(name = "filing_date", nullable = false)
    private LocalDateTime filingDate;
    
    /**
     * Total gross wages paid to all employees during period.
     * CHECK constraint: gross_wages >= 0
     */
    @Column(name = "gross_wages", nullable = false, precision = 15, scale = 2)
    private BigDecimal grossWages;
    
    /**
     * Taxable wages after deductions.
     * Typically equal to gross_wages for municipal withholding (no deductions).
     * CHECK constraint: taxable_wages >= 0
     */
    @Column(name = "taxable_wages", nullable = false, precision = 15, scale = 2)
    private BigDecimal taxableWages;
    
    /**
     * Municipality tax rate applied to taxable wages (e.g., 0.0225 = 2.25%).
     * CHECK constraint: tax_rate > 0
     */
    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal taxRate;
    
    /**
     * Calculated tax due = taxable_wages × tax_rate.
     * CHECK constraint: tax_due >= 0
     */
    @Column(name = "tax_due", nullable = false, precision = 15, scale = 2)
    private BigDecimal taxDue;
    
    /**
     * Manual adjustments (prior period overpayments, credits).
     * Can be negative. Default 0.
     */
    @Column(name = "adjustments", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal adjustments = BigDecimal.ZERO;
    
    /**
     * Total amount due = tax_due + adjustments + late_filing_penalty + underpayment_penalty.
     */
    @Column(name = "total_amount_due", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmountDue;
    
    /**
     * Is this an amended W-1 filing?
     * If true, amends_filing_id MUST reference the original filing.
     */
    @Column(name = "is_amended", nullable = false)
    @Builder.Default
    private Boolean isAmended = false;
    
    /**
     * Reference to original W-1 filing if this is an amendment.
     * Null if is_amended = false.
     * Enables audit trail: both original and amended filings preserved (FR-003, FR-017).
     */
    @Column(name = "amends_filing_id")
    private UUID amendsFilingId;
    
    /**
     * Required explanation when filing amended W-1 (FR-003).
     * Example: "Discovered payroll processing error - missed employee bonus payments"
     */
    @Column(name = "amendment_reason", columnDefinition = "TEXT")
    private String amendmentReason;
    
    /**
     * Number of employees paid during this period (optional).
     * Used for year-end employee count validation (FR-018).
     * CHECK constraint: employee_count >= 0
     */
    @Column(name = "employee_count")
    private Integer employeeCount;
    
    /**
     * Current status of this W-1 filing.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private W1FilingStatus status = W1FilingStatus.FILED;
    
    /**
     * Late-filing penalty calculated if filing_date > due_date.
     * FR-011: 5% per month late, maximum 25%, minimum $50 if tax due > $200.
     */
    @Column(name = "late_filing_penalty", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal lateFilingPenalty = BigDecimal.ZERO;
    
    /**
     * Underpayment penalty if cumulative payments < 90% of current year liability (FR-012).
     */
    @Column(name = "underpayment_penalty", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal underpaymentPenalty = BigDecimal.ZERO;
    
    /**
     * Audit trail: When this W-1 was created (immutable).
     * Constitution Principle III: Never updated.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    /**
     * Audit trail: User who filed this W-1 (immutable).
     * References users table via user_id.
     */
    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;
    
    /**
     * Last modification timestamp (e.g., status change FILED → PAID).
     */
    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
