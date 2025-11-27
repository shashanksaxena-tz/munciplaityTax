# Data Model: Withholding Reconciliation System

**Feature**: Complete Withholding Reconciliation System  
**Phase**: Phase 1 (Design & Contracts)  
**Date**: 2024-11-28

---

## Overview

This document defines the database schema for the withholding reconciliation system. All entities are stored in the tax-engine-service database with tenant-scoped access per Constitution II (Multi-Tenant Data Isolation).

**Database**: PostgreSQL 16  
**Schema**: Tenant-scoped (`dublin.*`, `columbus.*`)  
**Migration Tool**: Flyway  
**ORM**: Spring Data JPA with Hibernate

---

## Entity Relationship Diagram

```
┌─────────────────┐         ┌──────────────────────────┐
│   Business      │────────<│   W1Filing               │
│                 │         │                          │
│ - id (UUID)     │         │ - id (UUID)              │
│ - ein (String)  │         │ - business_id (FK)       │
│ - name          │         │ - period                 │
│ - filing_freq   │         │ - period_end_date        │
└─────────────────┘         │ - due_date               │
                            │ - filing_date            │
                            │ - wages                  │
                            │ - tax_due                │
                            │ - is_amended             │
                            │ - amends_filing_id (FK)  │
                            └──────────────────────────┘
                                       │
                                       │ triggers
                                       ↓
                            ┌──────────────────────────┐
┌─────────────────┐         │ CumulativeWithholding    │
│   Business      │────────<│ Totals                   │
│                 │         │                          │
│                 │         │ - id (UUID)              │
│                 │         │ - business_id (FK)       │
│                 │         │ - tax_year               │
│                 │         │ - periods_filed          │
│                 │         │ - cumulative_wages_ytd   │
│                 │         │ - cumulative_tax_ytd     │
│                 │         │ - projected_annual_wages │
│                 │         │ - on_track_indicator     │
└─────────────────┘         └──────────────────────────┘
        │
        │
        ↓
┌──────────────────────────┐
│ WithholdingReconciliation │
│                           │         ┌──────────────────────────┐
│ - id (UUID)               │────────<│   IgnoredW2              │
│ - business_id (FK)        │         │                          │
│ - tax_year                │         │ - id (UUID)              │
│ - w1_total_wages          │         │ - reconciliation_id (FK) │
│ - w2_total_wages          │         │ - employer_ein           │
│ - w1_total_tax            │         │ - employer_name          │
│ - w2_total_tax            │         │ - ignored_reason         │
│ - variance_wages          │         │ - uploaded_file_path     │
│ - variance_tax            │         │ - metadata (jsonb)       │
│ - status                  │         │ - uploaded_at            │
│ - resolution_notes        │         └──────────────────────────┘
└──────────────────────────┘
        │
        ↓
┌──────────────────────────┐
│ WithholdingPayment        │
│                           │
│ - id (UUID)               │
│ - w1_filing_id (FK)       │
│ - payment_date            │
│ - payment_amount          │
│ - payment_method          │
│ - transaction_id          │
│ - status                  │
└──────────────────────────┘
```

---

## 1. W1Filing Entity

**Purpose**: Stores individual W-1 withholding return filings (quarterly, monthly, semi-monthly, or daily).

**Functional Requirements**: FR-001 (W-1 Filing), FR-003 (Amended W-1), FR-013 (Filing Frequency)

### 1.1 Fields

| Field Name | Type | Constraints | Description |
|------------|------|-------------|-------------|
| `id` | UUID | PRIMARY KEY | Unique identifier |
| `tenant_id` | UUID | NOT NULL, FK → tenants(id) | Multi-tenant isolation (Constitution II) |
| `business_id` | UUID | NOT NULL, FK → businesses(id) | Business filing the W-1 |
| `tax_year` | INTEGER | NOT NULL, CHECK (tax_year >= 2020) | Tax year (e.g., 2024) |
| `filing_frequency` | VARCHAR(20) | NOT NULL, ENUM | `DAILY`, `SEMI_MONTHLY`, `MONTHLY`, `QUARTERLY` |
| `period` | VARCHAR(10) | NOT NULL | Period identifier: `Q1`, `Q2`, `M01`, `M02`, `D20240115` |
| `period_start_date` | DATE | NOT NULL | First day of filing period |
| `period_end_date` | DATE | NOT NULL | Last day of filing period |
| `due_date` | DATE | NOT NULL | Calculated due date (stored immutably, research R5) |
| `filing_date` | TIMESTAMP | NOT NULL | Actual submission timestamp |
| `gross_wages` | DECIMAL(15,2) | NOT NULL, CHECK (gross_wages >= 0) | Total gross wages subject to tax |
| `taxable_wages` | DECIMAL(15,2) | NOT NULL, CHECK (taxable_wages >= 0) | Wages after deductions |
| `tax_rate` | DECIMAL(5,4) | NOT NULL, CHECK (tax_rate > 0) | Municipality tax rate (e.g., 0.0225 = 2.25%) |
| `tax_due` | DECIMAL(15,2) | NOT NULL, CHECK (tax_due >= 0) | Calculated tax due (taxable_wages × tax_rate) |
| `adjustments` | DECIMAL(15,2) | DEFAULT 0 | Manual adjustments (prior overpayments, credits) |
| `total_amount_due` | DECIMAL(15,2) | NOT NULL | tax_due + adjustments |
| `is_amended` | BOOLEAN | DEFAULT FALSE | Is this an amended W-1? |
| `amends_filing_id` | UUID | NULL, FK → w1_filings(id) | Original filing if amended |
| `amendment_reason` | TEXT | NULL | Why amended (required if is_amended = true) |
| `employee_count` | INTEGER | NULL, CHECK (employee_count >= 0) | Number of employees paid (optional) |
| `status` | VARCHAR(20) | NOT NULL, DEFAULT 'FILED' | `FILED`, `PAID`, `OVERDUE`, `AMENDED` |
| `late_filing_penalty` | DECIMAL(15,2) | DEFAULT 0 | Calculated penalty (research R4) |
| `underpayment_penalty` | DECIMAL(15,2) | DEFAULT 0 | Calculated penalty (FR-012) |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Audit trail (Constitution III) |
| `created_by` | UUID | NOT NULL, FK → users(id) | User who filed |
| `updated_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Last modification timestamp |

### 1.2 Indexes

```sql
CREATE INDEX idx_w1_business_year ON w1_filings(business_id, tax_year);
CREATE INDEX idx_w1_tenant_year ON w1_filings(tenant_id, tax_year);
CREATE INDEX idx_w1_due_date ON w1_filings(due_date);
CREATE INDEX idx_w1_filing_date ON w1_filings(filing_date);
CREATE INDEX idx_w1_status ON w1_filings(status);
CREATE INDEX idx_w1_amended ON w1_filings(is_amended) WHERE is_amended = TRUE;
```

### 1.3 Constraints

```sql
-- Amended filing must reference original
ALTER TABLE w1_filings ADD CONSTRAINT check_amended_filing
    CHECK ((is_amended = FALSE AND amends_filing_id IS NULL) OR
           (is_amended = TRUE AND amends_filing_id IS NOT NULL));

-- Period end must be after period start
ALTER TABLE w1_filings ADD CONSTRAINT check_period_dates
    CHECK (period_end_date >= period_start_date);

-- Due date must be after period end (for monthly/quarterly)
ALTER TABLE w1_filings ADD CONSTRAINT check_due_date
    CHECK (due_date >= period_end_date);

-- Unique filing per business + period (cannot file Q1 2024 twice)
CREATE UNIQUE INDEX unique_w1_filing ON w1_filings(business_id, tax_year, period, is_amended)
    WHERE is_amended = FALSE;
```

### 1.4 Java Entity

```java
@Entity
@Table(name = "w1_filings")
public class W1Filing {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;
    
    @Column(name = "tax_year", nullable = false)
    private Integer taxYear;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "filing_frequency", nullable = false, length = 20)
    private FilingFrequency filingFrequency;
    
    @Column(name = "period", nullable = false, length = 10)
    private String period;
    
    @Column(name = "period_start_date", nullable = false)
    private LocalDate periodStartDate;
    
    @Column(name = "period_end_date", nullable = false)
    private LocalDate periodEndDate;
    
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;
    
    @Column(name = "filing_date", nullable = false)
    private LocalDateTime filingDate;
    
    @Column(name = "gross_wages", nullable = false, precision = 15, scale = 2)
    private BigDecimal grossWages;
    
    @Column(name = "taxable_wages", nullable = false, precision = 15, scale = 2)
    private BigDecimal taxableWages;
    
    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal taxRate;
    
    @Column(name = "tax_due", nullable = false, precision = 15, scale = 2)
    private BigDecimal taxDue;
    
    @Column(name = "adjustments", precision = 15, scale = 2)
    private BigDecimal adjustments = BigDecimal.ZERO;
    
    @Column(name = "total_amount_due", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmountDue;
    
    @Column(name = "is_amended", nullable = false)
    private Boolean isAmended = false;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "amends_filing_id")
    private W1Filing amendsFilingId;
    
    @Column(name = "amendment_reason", columnDefinition = "TEXT")
    private String amendmentReason;
    
    @Column(name = "employee_count")
    private Integer employeeCount;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private W1FilingStatus status = W1FilingStatus.FILED;
    
    @Column(name = "late_filing_penalty", precision = 15, scale = 2)
    private BigDecimal lateFilingPenalty = BigDecimal.ZERO;
    
    @Column(name = "underpayment_penalty", precision = 15, scale = 2)
    private BigDecimal underpaymentPenalty = BigDecimal.ZERO;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @Column(name = "created_by", nullable = false)
    private UUID createdBy;
    
    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    // Getters, setters, equals, hashCode omitted for brevity
}

public enum FilingFrequency {
    DAILY,
    SEMI_MONTHLY,
    MONTHLY,
    QUARTERLY
}

public enum W1FilingStatus {
    FILED,      // Submitted, awaiting payment
    PAID,       // Payment received
    OVERDUE,    // Past due date, unpaid
    AMENDED     // Superseded by amended filing
}
```

---

## 2. CumulativeWithholdingTotals Entity

**Purpose**: Cached year-to-date cumulative totals for business withholding filings (research R2: Option B - Cached table).

**Functional Requirements**: FR-002 (Cumulative Totals), FR-004 (Annual Projection), FR-005 (On-Track Indicator)

### 2.1 Fields

| Field Name | Type | Constraints | Description |
|------------|------|-------------|-------------|
| `id` | UUID | PRIMARY KEY | Unique identifier |
| `tenant_id` | UUID | NOT NULL, FK → tenants(id) | Multi-tenant isolation |
| `business_id` | UUID | NOT NULL, FK → businesses(id) | Business profile |
| `tax_year` | INTEGER | NOT NULL | Tax year (e.g., 2024) |
| `periods_filed` | INTEGER | NOT NULL, DEFAULT 0 | Count of W-1 filings for year |
| `cumulative_wages_ytd` | DECIMAL(15,2) | NOT NULL, DEFAULT 0 | Sum of gross wages from all W-1s |
| `cumulative_tax_ytd` | DECIMAL(15,2) | NOT NULL, DEFAULT 0 | Sum of tax due from all W-1s |
| `cumulative_adjustments_ytd` | DECIMAL(15,2) | NOT NULL, DEFAULT 0 | Sum of adjustments |
| `last_filing_date` | TIMESTAMP | NULL | Most recent W-1 filing date |
| `estimated_annual_wages` | DECIMAL(15,2) | NULL | From business registration (FR-004) |
| `projected_annual_wages` | DECIMAL(15,2) | NULL | Based on run rate (FR-004) |
| `on_track_indicator` | BOOLEAN | DEFAULT TRUE | Within 15% of estimated wages (FR-005) |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Record creation timestamp |
| `updated_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Last update (event-driven from W1FiledEvent) |

### 2.2 Indexes

```sql
CREATE UNIQUE INDEX unique_cumulative_business_year ON cumulative_withholding_totals(business_id, tax_year);
CREATE INDEX idx_cumulative_tenant_year ON cumulative_withholding_totals(tenant_id, tax_year);
CREATE INDEX idx_cumulative_updated_at ON cumulative_withholding_totals(updated_at);
CREATE INDEX idx_cumulative_on_track ON cumulative_withholding_totals(on_track_indicator) WHERE on_track_indicator = FALSE;
```

### 2.3 Constraints

```sql
-- Cannot have negative cumulative totals
ALTER TABLE cumulative_withholding_totals ADD CONSTRAINT check_non_negative_wages
    CHECK (cumulative_wages_ytd >= 0);

ALTER TABLE cumulative_withholding_totals ADD CONSTRAINT check_non_negative_tax
    CHECK (cumulative_tax_ytd >= 0);

-- Periods filed must be >= 0
ALTER TABLE cumulative_withholding_totals ADD CONSTRAINT check_periods_filed
    CHECK (periods_filed >= 0);
```

### 2.4 Java Entity

```java
@Entity
@Table(name = "cumulative_withholding_totals")
public class CumulativeWithholdingTotals {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;
    
    @Column(name = "tax_year", nullable = false)
    private Integer taxYear;
    
    @Column(name = "periods_filed", nullable = false)
    private Integer periodsFiled = 0;
    
    @Column(name = "cumulative_wages_ytd", nullable = false, precision = 15, scale = 2)
    private BigDecimal cumulativeWagesYtd = BigDecimal.ZERO;
    
    @Column(name = "cumulative_tax_ytd", nullable = false, precision = 15, scale = 2)
    private BigDecimal cumulativeTaxYtd = BigDecimal.ZERO;
    
    @Column(name = "cumulative_adjustments_ytd", nullable = false, precision = 15, scale = 2)
    private BigDecimal cumulativeAdjustmentsYtd = BigDecimal.ZERO;
    
    @Column(name = "last_filing_date")
    private LocalDateTime lastFilingDate;
    
    @Column(name = "estimated_annual_wages", precision = 15, scale = 2)
    private BigDecimal estimatedAnnualWages;
    
    @Column(name = "projected_annual_wages", precision = 15, scale = 2)
    private BigDecimal projectedAnnualWages;
    
    @Column(name = "on_track_indicator")
    private Boolean onTrackIndicator = true;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    // Getters, setters omitted
}
```

---

## 3. WithholdingReconciliation Entity

**Purpose**: Stores year-end reconciliation comparing W-1 cumulative totals to W-2 totals.

**Functional Requirements**: FR-006 (Reconciliation), FR-007 (Reconciliation Report), FR-008 (Discrepancy Resolution), FR-009 (Year-End Requirement)

### 3.1 Fields

| Field Name | Type | Constraints | Description |
|------------|------|-------------|-------------|
| `id` | UUID | PRIMARY KEY | Unique identifier |
| `tenant_id` | UUID | NOT NULL, FK → tenants(id) | Multi-tenant isolation |
| `business_id` | UUID | NOT NULL, FK → businesses(id) | Business being reconciled |
| `tax_year` | INTEGER | NOT NULL | Reconciliation year (e.g., 2024) |
| `w1_total_wages` | DECIMAL(15,2) | NOT NULL | Sum from all W-1 filings |
| `w2_total_wages` | DECIMAL(15,2) | NOT NULL | Sum from all W-2s (Box 18) |
| `w1_total_tax` | DECIMAL(15,2) | NOT NULL | Sum from all W-1 filings |
| `w2_total_tax` | DECIMAL(15,2) | NOT NULL | Sum from all W-2s (Box 19) |
| `variance_wages` | DECIMAL(15,2) | NOT NULL | w1_total_wages - w2_total_wages |
| `variance_tax` | DECIMAL(15,2) | NOT NULL | w1_total_tax - w2_total_tax |
| `variance_percentage` | DECIMAL(5,2) | NOT NULL | (variance_wages / w2_total_wages) × 100 |
| `status` | VARCHAR(20) | NOT NULL | `NOT_STARTED`, `IN_PROGRESS`, `DISCREPANCY`, `RECONCILED` |
| `reconciliation_date` | TIMESTAMP | NULL | When reconciliation completed |
| `resolution_notes` | TEXT | NULL | Explanation if discrepancy accepted (FR-008) |
| `w2_count` | INTEGER | NOT NULL | Number of W-2s uploaded |
| `w2_employee_count` | INTEGER | NOT NULL | Distinct employees from W-2s |
| `ignored_w2_count` | INTEGER | DEFAULT 0 | W-2s not matched to business EIN (research R1) |
| `approved_by` | UUID | NULL, FK → users(id) | Auditor who approved discrepancy |
| `approved_at` | TIMESTAMP | NULL | Approval timestamp |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Audit trail |
| `created_by` | UUID | NOT NULL, FK → users(id) | User who initiated reconciliation |
| `updated_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Last update timestamp |

### 3.2 Indexes

```sql
CREATE UNIQUE INDEX unique_reconciliation_business_year ON withholding_reconciliations(business_id, tax_year);
CREATE INDEX idx_reconciliation_tenant_year ON withholding_reconciliations(tenant_id, tax_year);
CREATE INDEX idx_reconciliation_status ON withholding_reconciliations(status);
CREATE INDEX idx_reconciliation_discrepancy ON withholding_reconciliations(status) WHERE status = 'DISCREPANCY';
```

### 3.3 Constraints

```sql
-- Variance must equal w1 - w2
ALTER TABLE withholding_reconciliations ADD CONSTRAINT check_variance_wages
    CHECK (ABS(variance_wages - (w1_total_wages - w2_total_wages)) < 0.01);

ALTER TABLE withholding_reconciliations ADD CONSTRAINT check_variance_tax
    CHECK (ABS(variance_tax - (w1_total_tax - w2_total_tax)) < 0.01);

-- If discrepancy resolved, must have resolution notes
ALTER TABLE withholding_reconciliations ADD CONSTRAINT check_resolution_notes
    CHECK ((status != 'RECONCILED' OR variance_wages = 0) OR 
           (status = 'RECONCILED' AND resolution_notes IS NOT NULL));
```

### 3.4 Java Entity

```java
@Entity
@Table(name = "withholding_reconciliations")
public class WithholdingReconciliation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;
    
    @Column(name = "tax_year", nullable = false)
    private Integer taxYear;
    
    @Column(name = "w1_total_wages", nullable = false, precision = 15, scale = 2)
    private BigDecimal w1TotalWages;
    
    @Column(name = "w2_total_wages", nullable = false, precision = 15, scale = 2)
    private BigDecimal w2TotalWages;
    
    @Column(name = "w1_total_tax", nullable = false, precision = 15, scale = 2)
    private BigDecimal w1TotalTax;
    
    @Column(name = "w2_total_tax", nullable = false, precision = 15, scale = 2)
    private BigDecimal w2TotalTax;
    
    @Column(name = "variance_wages", nullable = false, precision = 15, scale = 2)
    private BigDecimal varianceWages;
    
    @Column(name = "variance_tax", nullable = false, precision = 15, scale = 2)
    private BigDecimal varianceTax;
    
    @Column(name = "variance_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal variancePercentage;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReconciliationStatus status = ReconciliationStatus.NOT_STARTED;
    
    @Column(name = "reconciliation_date")
    private LocalDateTime reconciliationDate;
    
    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;
    
    @Column(name = "w2_count", nullable = false)
    private Integer w2Count;
    
    @Column(name = "w2_employee_count", nullable = false)
    private Integer w2EmployeeCount;
    
    @Column(name = "ignored_w2_count")
    private Integer ignoredW2Count = 0;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;
    
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @Column(name = "created_by", nullable = false)
    private UUID createdBy;
    
    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    // Getters, setters omitted
}

public enum ReconciliationStatus {
    NOT_STARTED,   // No reconciliation initiated
    IN_PROGRESS,   // W-2s being uploaded
    DISCREPANCY,   // Variance detected, requires resolution
    RECONCILED     // Completed successfully
}
```

---

## 4. IgnoredW2 Entity

**Purpose**: Tracks W-2 PDFs uploaded but not included in reconciliation (research R1: Hybrid approach).

**Functional Requirements**: Constitution IV (AI Transparency), FR-015 (Reconciliation Dashboard showing ignored items)

### 4.1 Fields

| Field Name | Type | Constraints | Description |
|------------|------|-------------|-------------|
| `id` | UUID | PRIMARY KEY | Unique identifier |
| `tenant_id` | UUID | NOT NULL, FK → tenants(id) | Multi-tenant isolation |
| `reconciliation_id` | UUID | NOT NULL, FK → withholding_reconciliations(id) | Parent reconciliation |
| `employer_ein` | VARCHAR(20) | NULL | EIN from W-2 Box b (may be NULL if extraction failed) |
| `employer_name` | VARCHAR(255) | NULL | Employer name from W-2 Box c |
| `employee_ssn_last4` | VARCHAR(4) | NULL | Last 4 digits of SSN (for duplicate detection) |
| `ignored_reason` | VARCHAR(50) | NOT NULL | `WRONG_EIN`, `DUPLICATE`, `EXTRACTION_ERROR`, `INCOMPLETE_DATA` |
| `uploaded_file_path` | VARCHAR(500) | NOT NULL | S3/local path to original PDF |
| `metadata` | JSONB | NULL | Flexible storage: { confidenceScore, pageNumber, localWages, errors } |
| `uploaded_at` | TIMESTAMP | NOT NULL | Upload timestamp |
| `reviewed_by` | UUID | NULL, FK → users(id) | User who reviewed (if manually resolved) |
| `reviewed_at` | TIMESTAMP | NULL | Review timestamp |
| `resolution_action` | VARCHAR(50) | NULL | `REUPLOADED`, `EIN_OVERRIDDEN`, `DELETED`, `KEPT_DUPLICATE` |

### 4.2 Indexes

```sql
CREATE INDEX idx_ignored_w2_reconciliation ON ignored_w2s(reconciliation_id);
CREATE INDEX idx_ignored_w2_reason ON ignored_w2s(ignored_reason);
CREATE INDEX idx_ignored_w2_uploaded_at ON ignored_w2s(uploaded_at);
CREATE INDEX idx_ignored_w2_metadata ON ignored_w2s USING gin(metadata);  -- JSONB index
```

### 4.3 Java Entity

```java
@Entity
@Table(name = "ignored_w2s")
public class IgnoredW2 {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reconciliation_id", nullable = false)
    private WithholdingReconciliation reconciliation;
    
    @Column(name = "employer_ein", length = 20)
    private String employerEin;
    
    @Column(name = "employer_name")
    private String employerName;
    
    @Column(name = "employee_ssn_last4", length = 4)
    private String employeeSsnLast4;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "ignored_reason", nullable = false, length = 50)
    private IgnoredW2Reason ignoredReason;
    
    @Column(name = "uploaded_file_path", nullable = false, length = 500)
    private String uploadedFilePath;
    
    @Type(JsonBinaryType.class)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;  // JSON string: { "confidenceScore": 0.95, "pageNumber": 1, "localWages": 50000 }
    
    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;
    
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_action", length = 50)
    private ResolutionAction resolutionAction;
    
    // Getters, setters omitted
}

public enum IgnoredW2Reason {
    WRONG_EIN,          // Employer EIN ≠ business EIN
    DUPLICATE,          // Same employee SSN appears twice
    EXTRACTION_ERROR,   // Gemini failed to extract data
    INCOMPLETE_DATA     // W-2 missing required fields (Box 18, 19)
}

public enum ResolutionAction {
    REUPLOADED,         // User uploaded corrected PDF
    EIN_OVERRIDDEN,     // User manually linked to reconciliation
    DELETED,            // User removed duplicate
    KEPT_DUPLICATE      // User confirmed both W-2s valid (job change)
}
```

---

## 5. WithholdingPayment Entity

**Purpose**: Tracks payments made against W-1 filings (integrates with existing payment gateway).

**Functional Requirements**: FR-020 (Payment Tracking), FR-012 (Underpayment Penalty)

### 5.1 Fields

| Field Name | Type | Constraints | Description |
|------------|------|-------------|-------------|
| `id` | UUID | PRIMARY KEY | Unique identifier |
| `tenant_id` | UUID | NOT NULL, FK → tenants(id) | Multi-tenant isolation |
| `w1_filing_id` | UUID | NOT NULL, FK → w1_filings(id) | Associated W-1 filing |
| `payment_date` | TIMESTAMP | NOT NULL | Payment timestamp |
| `payment_amount` | DECIMAL(15,2) | NOT NULL, CHECK (payment_amount > 0) | Amount paid |
| `payment_method` | VARCHAR(50) | NOT NULL | `ACH`, `CHECK`, `CREDIT_CARD`, `WIRE_TRANSFER` |
| `transaction_id` | VARCHAR(100) | NOT NULL | Payment gateway transaction ID |
| `confirmation_number` | VARCHAR(50) | NOT NULL | User-facing confirmation number |
| `status` | VARCHAR(20) | NOT NULL | `PENDING`, `COMPLETED`, `FAILED`, `REFUNDED` |
| `failure_reason` | TEXT | NULL | If status = FAILED, reason from gateway |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Audit trail |

### 5.2 Indexes

```sql
CREATE INDEX idx_payment_w1_filing ON withholding_payments(w1_filing_id);
CREATE INDEX idx_payment_transaction_id ON withholding_payments(transaction_id);
CREATE INDEX idx_payment_date ON withholding_payments(payment_date);
CREATE INDEX idx_payment_status ON withholding_payments(status);
```

### 5.3 Java Entity

```java
@Entity
@Table(name = "withholding_payments")
public class WithholdingPayment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "w1_filing_id", nullable = false)
    private W1Filing w1Filing;
    
    @Column(name = "payment_date", nullable = false)
    private LocalDateTime paymentDate;
    
    @Column(name = "payment_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal paymentAmount;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 50)
    private PaymentMethod paymentMethod;
    
    @Column(name = "transaction_id", nullable = false, length = 100)
    private String transactionId;
    
    @Column(name = "confirmation_number", nullable = false, length = 50)
    private String confirmationNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;
    
    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    // Getters, setters omitted
}

public enum PaymentMethod {
    ACH,
    CHECK,
    CREDIT_CARD,
    WIRE_TRANSFER
}

public enum PaymentStatus {
    PENDING,
    COMPLETED,
    FAILED,
    REFUNDED
}
```

---

## 6. WithholdingAuditLog Entity

**Purpose**: Immutable audit trail for all withholding actions (Constitution III).

**Functional Requirements**: Constitution III (Audit Trail Immutability), FR-010 (Filing History)

### 6.1 Fields

| Field Name | Type | Constraints | Description |
|------------|------|-------------|-------------|
| `id` | UUID | PRIMARY KEY | Unique identifier |
| `tenant_id` | UUID | NOT NULL | Multi-tenant isolation |
| `entity_type` | VARCHAR(50) | NOT NULL | `W1_FILING`, `RECONCILIATION`, `CUMULATIVE_TOTALS`, `PAYMENT` |
| `entity_id` | UUID | NOT NULL | ID of affected entity |
| `action` | VARCHAR(50) | NOT NULL | `FILED`, `AMENDED`, `RECONCILED`, `PAYMENT_RECEIVED`, `CUMULATIVE_UPDATED` |
| `actor_id` | UUID | NOT NULL, FK → users(id) | User who performed action |
| `actor_role` | VARCHAR(20) | NOT NULL | `BUSINESS`, `AUDITOR`, `SYSTEM` |
| `description` | TEXT | NOT NULL | Human-readable description |
| `old_value` | JSONB | NULL | Previous state (if update) |
| `new_value` | JSONB | NULL | New state (if create/update) |
| `ip_address` | VARCHAR(45) | NULL | Actor's IP address (IPv6 support) |
| `user_agent` | VARCHAR(255) | NULL | Browser user agent |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Immutable timestamp |

### 6.2 Indexes

```sql
CREATE INDEX idx_audit_entity ON withholding_audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_actor ON withholding_audit_log(actor_id);
CREATE INDEX idx_audit_created_at ON withholding_audit_log(created_at);
CREATE INDEX idx_audit_tenant ON withholding_audit_log(tenant_id);
```

### 6.3 Java Entity

```java
@Entity
@Table(name = "withholding_audit_log")
@Immutable  // Hibernate: Never update, only insert
public class WithholdingAuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 50)
    private AuditEntityType entityType;
    
    @Column(name = "entity_id", nullable = false)
    private UUID entityId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 50)
    private AuditAction action;
    
    @Column(name = "actor_id", nullable = false)
    private UUID actorId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "actor_role", nullable = false, length = 20)
    private ActorRole actorRole;
    
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;
    
    @Type(JsonBinaryType.class)
    @Column(name = "old_value", columnDefinition = "jsonb")
    private String oldValue;
    
    @Type(JsonBinaryType.class)
    @Column(name = "new_value", columnDefinition = "jsonb")
    private String newValue;
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(name = "user_agent")
    private String userAgent;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    // Only getters, no setters (immutable)
}

public enum AuditEntityType {
    W1_FILING,
    RECONCILIATION,
    CUMULATIVE_TOTALS,
    PAYMENT
}

public enum AuditAction {
    FILED,
    AMENDED,
    RECONCILED,
    PAYMENT_RECEIVED,
    CUMULATIVE_UPDATED,
    DISCREPANCY_RESOLVED
}

public enum ActorRole {
    BUSINESS,   // Business owner/accountant
    AUDITOR,    // Municipality auditor
    SYSTEM      // Automated action (event-driven update)
}
```

---

## Database Migration Plan

### Flyway Migration Files

```text
V1.20__create_w1_filings_table.sql
V1.21__create_cumulative_withholding_totals_table.sql
V1.22__create_withholding_reconciliations_table.sql
V1.23__create_ignored_w2s_table.sql
V1.24__create_withholding_payments_table.sql
V1.25__create_withholding_audit_log_table.sql
V1.26__add_withholding_indexes.sql
V1.27__add_withholding_constraints.sql
```

### Rollback Strategy

All migrations reversible via `V1.2X__rollback.sql` scripts (Flyway undo migrations):

```sql
-- V1.27__rollback.sql
ALTER TABLE withholding_reconciliations DROP CONSTRAINT check_variance_wages;
-- ... drop all constraints

-- V1.26__rollback.sql
DROP INDEX idx_w1_business_year;
-- ... drop all indexes

-- V1.25__rollback.sql
DROP TABLE withholding_audit_log;
-- ... drop all tables in reverse order
```

---

## Performance Considerations

### Query Optimization

1. **Dashboard Query** (FR-002):
   ```sql
   -- Cached cumulative totals: O(1) query
   SELECT * FROM cumulative_withholding_totals
   WHERE business_id = ? AND tax_year = ?;
   ```
   **Performance**: <80ms (research R2 benchmark)

2. **Reconciliation Dashboard** (FR-015):
   ```sql
   -- Single JOIN to get reconciliation + ignored W-2 count
   SELECT r.*, COUNT(i.id) AS ignored_w2_count
   FROM withholding_reconciliations r
   LEFT JOIN ignored_w2s i ON r.id = i.reconciliation_id
   WHERE r.business_id = ? AND r.tax_year = ?
   GROUP BY r.id;
   ```
   **Performance**: <100ms with proper indexes

3. **Filing History** (FR-010):
   ```sql
   -- Paginated query with limit
   SELECT * FROM w1_filings
   WHERE business_id = ? AND tax_year = ?
   ORDER BY period_end_date DESC
   LIMIT 20 OFFSET ?;
   ```
   **Performance**: <50ms with idx_w1_business_year

### Cache Strategy

- **Redis Cache**: CumulativeTotals TTL = 5 minutes (fallback to database)
- **Event-Driven Update**: W1FiledEvent invalidates Redis cache, updates database
- **Self-Healing**: Daily job recalculates stale cumulatives (research R2)

---

## Data Retention

Per Constitution III (Audit Trail Immutability):

| Entity | Retention Period | Deletion Strategy |
|--------|------------------|-------------------|
| W1Filing | 7 years (IRS requirement) | Soft delete after 7 years (is_deleted flag) |
| WithholdingReconciliation | 7 years | Soft delete |
| CumulativeWithholdingTotals | 7 years | Soft delete |
| WithholdingAuditLog | **Permanent** (never delete) | Archive to cold storage after 10 years |
| IgnoredW2 | 7 years | Hard delete after 7 years |
| WithholdingPayment | 7 years | Soft delete |

---

## Next Steps

1. ✅ Data model complete
2. ⏳ Generate API contracts (OpenAPI 3.0) → `/contracts/`
3. ⏳ Create quickstart.md (developer guide)
4. ⏳ Re-evaluate Constitution Check post-design
