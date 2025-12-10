package com.munitax.submission.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * W3Reconciliation Entity - Represents year-end reconciliation of withholding (Form W-3).
 * 
 * This entity tracks the annual reconciliation between:
 * - Total W-1 filings (monthly/quarterly withholding returns) submitted throughout the year
 * - Total W-2 wages reported to employees
 * 
 * Functional Requirements:
 * - Store year-end totals from all W-1 filings
 * - Link to all W-1 filings for the year
 * - Track reconciliation status (BALANCED/UNBALANCED)
 * - Identify discrepancies between W-1 and W-2 totals
 * - Calculate penalties for late or missing filings
 */
@Entity
@Table(name = "w3_reconciliations", schema = "dublin")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class W3Reconciliation {
    
    /**
     * Unique identifier for this W-3 reconciliation.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /**
     * Multi-tenant isolation - tenant owning this reconciliation.
     */
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;
    
    /**
     * Business ID (FEIN holder) filing this W-3.
     */
    @Column(name = "business_id", nullable = false)
    private String businessId;
    
    /**
     * Tax year for this reconciliation (e.g., 2024).
     */
    @Column(name = "tax_year", nullable = false)
    private Integer taxYear;
    
    /**
     * Total tax from all W-1 filings for the year.
     * Sum of all W-1 tax_due amounts.
     */
    @Column(name = "total_w1_tax", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalW1Tax;
    
    /**
     * Total tax from W-2 forms reported to employees.
     * Sum of Box 19 (Local Income Tax) from all employee W-2s.
     */
    @Column(name = "total_w2_tax", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalW2Tax;
    
    /**
     * Discrepancy amount = totalW1Tax - totalW2Tax.
     * Positive: More tax remitted than reported on W-2s (overpayment)
     * Negative: Less tax remitted than reported on W-2s (underpayment)
     */
    @Column(name = "discrepancy", nullable = false, precision = 15, scale = 2)
    private BigDecimal discrepancy;
    
    /**
     * Reconciliation status:
     * - BALANCED: discrepancy within tolerance (< $1.00)
     * - UNBALANCED: discrepancy exceeds tolerance
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status; // BALANCED, UNBALANCED
    
    /**
     * Count of W-1 filings included in this reconciliation.
     */
    @Column(name = "w1_filing_count")
    private Integer w1FilingCount;
    
    /**
     * Count of W-2 forms included in this reconciliation.
     */
    @Column(name = "w2_form_count")
    private Integer w2FormCount;
    
    /**
     * Total number of employees reported on W-2s.
     */
    @Column(name = "total_employees")
    private Integer totalEmployees;
    
    /**
     * Late filing penalty if W-3 filed after deadline.
     * Deadline is typically January 31st of the following year.
     */
    @Column(name = "late_filing_penalty", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal lateFilingPenalty = BigDecimal.ZERO;
    
    /**
     * Penalty for missing W-1 filings during the year.
     */
    @Column(name = "missing_filing_penalty", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal missingFilingPenalty = BigDecimal.ZERO;
    
    /**
     * Total penalties = lateFilingPenalty + missingFilingPenalty.
     */
    @Column(name = "total_penalties", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalPenalties = BigDecimal.ZERO;
    
    /**
     * Due date for W-3 filing (typically January 31st of following year).
     */
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;
    
    /**
     * Actual filing date when W-3 was submitted.
     */
    @Column(name = "filing_date")
    private Instant filingDate;
    
    /**
     * Whether this W-3 has been submitted.
     */
    @Column(name = "is_submitted", nullable = false)
    @Builder.Default
    private Boolean isSubmitted = false;
    
    /**
     * Submission confirmation number.
     */
    @Column(name = "confirmation_number", length = 50)
    private String confirmationNumber;
    
    /**
     * Detailed notes about discrepancies or issues found.
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    /**
     * When this W-3 reconciliation record was created.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    /**
     * User who created this W-3 reconciliation.
     */
    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;
    
    /**
     * Last modification timestamp.
     */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    /**
     * IDs of all W-1 filings included in this reconciliation.
     * Stored as a JSON array or comma-separated list.
     */
    @ElementCollection
    @CollectionTable(name = "w3_w1_filings", joinColumns = @JoinColumn(name = "w3_reconciliation_id"))
    @Column(name = "w1_filing_id")
    private List<String> w1FilingIds;
    
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
