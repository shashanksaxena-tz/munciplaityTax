package com.munitax.taxengine.domain.withholding;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * IgnoredW2 Entity - Tracks W-2 PDFs uploaded but not included in reconciliation.
 * 
 * Purpose:
 * AI Transparency (Constitution Principle IV) - Users must see which W-2s were
 * excluded from reconciliation and why. Research R1: Hybrid approach with JSON metadata.
 * 
 * Functional Requirements:
 * - FR-014: Aggregate W-2 data from AI extraction service
 * - FR-015: Display reconciliation dashboard showing ignored items
 * - Constitution IV: Provide "Ignored Items Report" with confidence scores and reasons
 * 
 * Common Ignored Reasons:
 * - WRONG_EIN: Employer EIN from W-2 Box b does not match business profile EIN
 * - DUPLICATE: Same employee SSN appears multiple times (job change, correction)
 * - EXTRACTION_ERROR: Gemini AI failed to extract required fields (Box 18, 19)
 * - INCOMPLETE_DATA: W-2 missing required fields even if extraction succeeded
 * 
 * Resolution Actions:
 * - REUPLOADED: User uploaded corrected PDF
 * - EIN_OVERRIDDEN: User manually linked W-2 to reconciliation (subsidiary case)
 * - DELETED: User removed duplicate W-2
 * - KEPT_DUPLICATE: User confirmed both W-2s valid (mid-year job change)
 * 
 * @see WithholdingReconciliation
 */
@Entity
@Table(name = "ignored_w2s", schema = "dublin")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IgnoredW2 {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /**
     * Multi-tenant isolation.
     */
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    /**
     * Parent reconciliation this ignored W-2 belongs to.
     */
    @Column(name = "reconciliation_id", nullable = false)
    private UUID reconciliationId;
    
    /**
     * Employer EIN from W-2 Box b (may be NULL if extraction failed).
     * If not NULL and != business profile EIN → reason = WRONG_EIN.
     */
    @Column(name = "employer_ein", length = 20)
    private String employerEin;
    
    /**
     * Employer name from W-2 Box c (may be NULL if extraction failed).
     */
    @Column(name = "employer_name")
    private String employerName;
    
    /**
     * Last 4 digits of employee SSN for duplicate detection.
     * Used to identify if same employee has multiple W-2s.
     * Full SSN not stored per security requirements.
     */
    @Column(name = "employee_ssn_last4", length = 4)
    private String employeeSsnLast4;
    
    /**
     * Why this W-2 was not included in reconciliation.
     */
    @Column(name = "ignored_reason", nullable = false, length = 50)
    private String ignoredReason; // WRONG_EIN, DUPLICATE, EXTRACTION_ERROR, INCOMPLETE_DATA
    
    /**
     * Path to original uploaded PDF (S3 bucket or local filesystem).
     * Example: "s3://munitax-w2s/2024/550e8400-e29b-41d4-a716-446655440000/w2_employee1.pdf"
     */
    @Column(name = "uploaded_file_path", nullable = false, length = 500)
    private String uploadedFilePath;
    
    /**
     * Flexible JSON metadata storage.
     * Structure: {
     *   "confidenceScore": 0.95,
     *   "pageNumber": 1,
     *   "localWages": 50000.00,
     *   "localWithheld": 1125.00,
     *   "extractionErrors": ["Box 18 missing", "Box 19 illegible"]
     * }
     * 
     * Type: JSONB in PostgreSQL for efficient querying.
     */
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;
    
    /**
     * When W-2 PDF was uploaded.
     */
    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;
    
    /**
     * User who reviewed this ignored W-2 (if manually resolved).
     * Null if not yet reviewed.
     */
    @Column(name = "reviewed_by")
    private UUID reviewedBy;
    
    /**
     * When user reviewed and resolved this ignored W-2.
     */
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
    
    /**
     * How user resolved this ignored W-2.
     * Null if not yet resolved.
     */
    @Column(name = "resolution_action", length = 50)
    private String resolutionAction; // REUPLOADED, EIN_OVERRIDDEN, DELETED, KEPT_DUPLICATE
}
