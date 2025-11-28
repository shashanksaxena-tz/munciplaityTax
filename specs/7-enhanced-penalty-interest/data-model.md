# Data Model: Enhanced Penalty & Interest Calculation

**Feature**: Enhanced Penalty & Interest Calculation  
**Phase**: Phase 1 (Design & Contracts)  
**Date**: 2024-11-28

---

## Overview

This document defines the database schema for the enhanced penalty and interest calculation system. All entities are stored in the tax-engine-service database with tenant-scoped access per Constitution II (Multi-Tenant Data Isolation).

**Database**: PostgreSQL 16  
**Schema**: Tenant-scoped (`dublin.*`, `columbus.*`)  
**Migration Tool**: Flyway  
**ORM**: Spring Data JPA with Hibernate

---

## Entity Relationship Diagram

```
┌─────────────────┐         ┌──────────────────────────┐
│   TaxReturn     │────────<│   Penalty                │
│                 │         │                          │
│ - id (UUID)     │         │ - id (UUID)              │
│ - tenant_id     │         │ - return_id (FK)         │
│ - taxpayer_id   │         │ - penalty_type           │
│ - tax_year      │         │ - assessment_date        │
│ - tax_due       │         │ - tax_due_date           │
│ - filing_date   │         │ - actual_date            │
└─────────────────┘         │ - months_late            │
        │                   │ - unpaid_tax_amount      │
        │                   │ - penalty_rate           │
        │                   │ - penalty_amount         │
        │                   │ - maximum_penalty        │
        │                   │ - is_abated              │
        │                   │ - abatement_reason       │
        │                   │ - abatement_date         │
        │                   └──────────────────────────┘
        │                            │
        │                            │ references
        │                            ↓
        │                   ┌──────────────────────────┐
        │                   │ PenaltyAbatement         │
        │                   │                          │
        │                   │ - id (UUID)              │
        │                   │ - return_id (FK)         │
        │                   │ - penalty_id (FK)        │
        │                   │ - request_date           │
        │                   │ - abatement_type         │
        │                   │ - requested_amount       │
        │                   │ - reason                 │
        │                   │ - explanation            │
        │                   │ - status                 │
        │                   │ - reviewed_by            │
        │                   │ - approved_amount        │
        │                   │ - form_generated         │
        │                   └──────────────────────────┘
        │
        ├───────────────────>┌──────────────────────────┐
        │                   │ EstimatedTaxPenalty      │
        │                   │                          │
        │                   │ - id (UUID)              │
        │                   │ - return_id (FK)         │
        │                   │ - tax_year               │
        │                   │ - annual_tax_liability   │
        │                   │ - prior_year_tax         │
        │                   │ - agi                    │
        │                   │ - safe_harbor_1_met      │
        │                   │ - safe_harbor_2_met      │
        │                   │ - calculation_method     │
        │                   │ - total_penalty          │
        │                   └──────────────────────────┘
        │                            │
        │                            │ contains
        │                            ↓
        │                   ┌──────────────────────────┐
        │                   │ QuarterlyUnderpayment    │
        │                   │                          │
        │                   │ - id (UUID)              │
        │                   │ - estimated_penalty_id   │
        │                   │ - quarter                │
        │                   │ - due_date               │
        │                   │ - required_payment       │
        │                   │ - actual_payment         │
        │                   │ - underpayment           │
        │                   │ - quarters_unpaid        │
        │                   │ - penalty_rate           │
        │                   │ - penalty_amount         │
        │                   └──────────────────────────┘
        │
        ├───────────────────>┌──────────────────────────┐
        │                   │ Interest                 │
        │                   │                          │
        │                   │ - id (UUID)              │
        │                   │ - return_id (FK)         │
        │                   │ - tax_due_date           │
        │                   │ - unpaid_tax_amount      │
        │                   │ - annual_interest_rate   │
        │                   │ - compounding_frequency  │
        │                   │ - start_date             │
        │                   │ - end_date               │
        │                   │ - total_days             │
        │                   │ - total_interest         │
        │                   └──────────────────────────┘
        │                            │
        │                            │ contains
        │                            ↓
        │                   ┌──────────────────────────┐
        │                   │ QuarterlyInterest        │
        │                   │                          │
        │                   │ - id (UUID)              │
        │                   │ - interest_id (FK)       │
        │                   │ - quarter                │
        │                   │ - start_date             │
        │                   │ - end_date               │
        │                   │ - days                   │
        │                   │ - beginning_balance      │
        │                   │ - interest_accrued       │
        │                   │ - ending_balance         │
        │                   └──────────────────────────┘
        │
        └───────────────────>┌──────────────────────────┐
                            │ PaymentAllocation        │
                            │                          │
                            │ - id (UUID)              │
                            │ - return_id (FK)         │
                            │ - payment_date           │
                            │ - payment_amount         │
                            │ - applied_to_tax         │
                            │ - applied_to_penalties   │
                            │ - applied_to_interest    │
                            │ - remaining_tax_balance  │
                            │ - remaining_penalty_bal  │
                            │ - remaining_interest_bal │
                            │ - allocation_order       │
                            └──────────────────────────┘
```

---

## 1. Penalty Entity

**Purpose**: Stores penalties assessed on tax returns (late filing, late payment, etc.).

**Functional Requirements**: FR-001 (Late Filing), FR-007 (Late Payment), FR-012 (Combined Cap)

### 1.1 Fields

| Field Name | Type | Constraints | Description |
|------------|------|-------------|-------------|
| `id` | UUID | PRIMARY KEY | Unique identifier |
| `tenant_id` | UUID | NOT NULL, FK → tenants(id) | Multi-tenant isolation (Constitution II) |
| `return_id` | UUID | NOT NULL, FK → tax_returns(id) | Associated tax return |
| `penalty_type` | VARCHAR(50) | NOT NULL, ENUM | `LATE_FILING`, `LATE_PAYMENT`, `ESTIMATED_UNDERPAYMENT`, `OTHER` |
| `assessment_date` | DATE | NOT NULL | When penalty was calculated |
| `tax_due_date` | DATE | NOT NULL | Original due date |
| `actual_date` | DATE | NOT NULL | Actual filing or payment date |
| `months_late` | INTEGER | NOT NULL, CHECK (months_late >= 0) | Calculated months late (rounded up) |
| `unpaid_tax_amount` | DECIMAL(15,2) | NOT NULL, CHECK (unpaid_tax_amount >= 0) | Tax balance subject to penalty |
| `penalty_rate` | DECIMAL(5,4) | NOT NULL, CHECK (penalty_rate >= 0) | Rate per month (0.05 for filing, 0.01 for payment) |
| `penalty_amount` | DECIMAL(15,2) | NOT NULL, CHECK (penalty_amount >= 0) | Calculated penalty |
| `maximum_penalty` | DECIMAL(15,2) | NOT NULL | Cap on penalty (25% of tax) |
| `is_abated` | BOOLEAN | DEFAULT FALSE | Whether penalty was abated |
| `abatement_reason` | TEXT | NULL | Reason for abatement |
| `abatement_date` | DATE | NULL | When abatement approved |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Audit trail (Constitution III) |
| `created_by` | UUID | NOT NULL, FK → users(id) | User/system who assessed penalty |
| `updated_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Last modification timestamp |

### 1.2 Indexes

```sql
CREATE INDEX idx_penalty_return ON penalties(return_id);
CREATE INDEX idx_penalty_tenant_year ON penalties(tenant_id, assessment_date);
CREATE INDEX idx_penalty_type ON penalties(penalty_type);
CREATE INDEX idx_penalty_abated ON penalties(is_abated) WHERE is_abated = TRUE;
CREATE INDEX idx_penalty_due_date ON penalties(tax_due_date);
```

### 1.3 Constraints

```sql
-- Penalty amount cannot exceed maximum penalty
ALTER TABLE penalties ADD CONSTRAINT check_penalty_cap
    CHECK (penalty_amount <= maximum_penalty);

-- Maximum penalty is 25% of unpaid tax
ALTER TABLE penalties ADD CONSTRAINT check_maximum_penalty
    CHECK (maximum_penalty = unpaid_tax_amount * 0.25::DECIMAL(5,4));

-- Actual date must be after or equal to due date
ALTER TABLE penalties ADD CONSTRAINT check_late_date
    CHECK (actual_date >= tax_due_date);

-- If abated, must have reason and date
ALTER TABLE penalties ADD CONSTRAINT check_abatement_fields
    CHECK ((is_abated = FALSE) OR 
           (is_abated = TRUE AND abatement_reason IS NOT NULL AND abatement_date IS NOT NULL));

-- Months late calculation is performed in application logic to handle varying month lengths.
-- This constraint ensures months_late is not negative and does not exceed 120 months (10 years),
-- which is the maximum period allowed for late filing penalties per business rules.
ALTER TABLE penalties ADD CONSTRAINT check_months_late_reasonable
    CHECK (months_late >= 0 AND months_late <= 120);  -- Max 10 years (business rule)
```

### 1.4 Java Entity

```java
@Entity
@Table(name = "penalties")
public class Penalty {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_id", nullable = false)
    private TaxReturn taxReturn;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "penalty_type", nullable = false, length = 50)
    private PenaltyType penaltyType;
    
    @Column(name = "assessment_date", nullable = false)
    private LocalDate assessmentDate;
    
    @Column(name = "tax_due_date", nullable = false)
    private LocalDate taxDueDate;
    
    @Column(name = "actual_date", nullable = false)
    private LocalDate actualDate;
    
    @Column(name = "months_late", nullable = false)
    private Integer monthsLate;
    
    @Column(name = "unpaid_tax_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal unpaidTaxAmount;
    
    @Column(name = "penalty_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal penaltyRate;
    
    @Column(name = "penalty_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal penaltyAmount;
    
    @Column(name = "maximum_penalty", nullable = false, precision = 15, scale = 2)
    private BigDecimal maximumPenalty;
    
    @Column(name = "is_abated", nullable = false)
    private Boolean isAbated = false;
    
    @Column(name = "abatement_reason", columnDefinition = "TEXT")
    private String abatementReason;
    
    @Column(name = "abatement_date")
    private LocalDate abatementDate;
    
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

public enum PenaltyType {
    LATE_FILING,              // 5% per month, max 25%
    LATE_PAYMENT,             // 1% per month, max 25%
    ESTIMATED_UNDERPAYMENT,   // Quarterly underpayment
    OTHER                     // Other penalty types
}
```

---

## 2. EstimatedTaxPenalty Entity

**Purpose**: Stores quarterly estimated tax underpayment penalty calculations.

**Functional Requirements**: FR-015 (Safe Harbor), FR-020 (Quarterly Calculation)

### 2.1 Fields

| Field Name | Type | Constraints | Description |
|------------|------|-------------|-------------|
| `id` | UUID | PRIMARY KEY | Unique identifier |
| `tenant_id` | UUID | NOT NULL, FK → tenants(id) | Multi-tenant isolation |
| `return_id` | UUID | NOT NULL, FK → tax_returns(id) | Associated tax return |
| `tax_year` | INTEGER | NOT NULL | Tax year |
| `annual_tax_liability` | DECIMAL(15,2) | NOT NULL | Total tax for year |
| `prior_year_tax_liability` | DECIMAL(15,2) | NOT NULL | Prior year tax (for safe harbor) |
| `agi` | DECIMAL(15,2) | NOT NULL | Adjusted gross income |
| `safe_harbor_1_met` | BOOLEAN | DEFAULT FALSE | Paid 90% of current year |
| `safe_harbor_2_met` | BOOLEAN | DEFAULT FALSE | Paid 100%/110% of prior year |
| `calculation_method` | VARCHAR(20) | NOT NULL | `STANDARD`, `ANNUALIZED_INCOME` |
| `total_penalty` | DECIMAL(15,2) | NOT NULL, DEFAULT 0 | Sum of all quarterly penalties |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Audit trail |
| `created_by` | UUID | NOT NULL, FK → users(id) | User/system who calculated |
| `updated_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Last update |

### 2.2 Indexes

```sql
CREATE UNIQUE INDEX unique_estimated_penalty_return ON estimated_tax_penalties(return_id);
CREATE INDEX idx_estimated_penalty_tenant_year ON estimated_tax_penalties(tenant_id, tax_year);
CREATE INDEX idx_estimated_penalty_safe_harbor ON estimated_tax_penalties(safe_harbor_1_met, safe_harbor_2_met);
```

### 2.3 Constraints

```sql
-- Total penalty must equal sum of quarterly penalties
ALTER TABLE estimated_tax_penalties ADD CONSTRAINT check_total_penalty_calculation
    CHECK (total_penalty >= 0);

-- If either safe harbor met, total penalty should be 0
ALTER TABLE estimated_tax_penalties ADD CONSTRAINT check_safe_harbor_no_penalty
    CHECK (NOT (safe_harbor_1_met = TRUE OR safe_harbor_2_met = TRUE) OR total_penalty = 0);

-- Prior year tax must be non-negative
ALTER TABLE estimated_tax_penalties ADD CONSTRAINT check_prior_year_tax
    CHECK (prior_year_tax_liability >= 0);
```

### 2.4 Java Entity

```java
@Entity
@Table(name = "estimated_tax_penalties")
public class EstimatedTaxPenalty {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_id", nullable = false)
    private TaxReturn taxReturn;
    
    @Column(name = "tax_year", nullable = false)
    private Integer taxYear;
    
    @Column(name = "annual_tax_liability", nullable = false, precision = 15, scale = 2)
    private BigDecimal annualTaxLiability;
    
    @Column(name = "prior_year_tax_liability", nullable = false, precision = 15, scale = 2)
    private BigDecimal priorYearTaxLiability;
    
    @Column(name = "agi", nullable = false, precision = 15, scale = 2)
    private BigDecimal agi;
    
    @Column(name = "safe_harbor_1_met", nullable = false)
    private Boolean safeHarbor1Met = false;
    
    @Column(name = "safe_harbor_2_met", nullable = false)
    private Boolean safeHarbor2Met = false;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_method", nullable = false, length = 20)
    private CalculationMethod calculationMethod;
    
    @Column(name = "total_penalty", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalPenalty = BigDecimal.ZERO;
    
    @OneToMany(mappedBy = "estimatedTaxPenalty", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuarterlyUnderpayment> quarters = new ArrayList<>();
    
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

public enum CalculationMethod {
    STANDARD,           // 25% per quarter
    ANNUALIZED_INCOME   // Based on income earned each quarter
}
```

---

## 3. QuarterlyUnderpayment Entity

**Purpose**: Stores underpayment details for each quarter.

**Functional Requirements**: FR-022 (Quarterly Calculation), FR-024 (Overpayment Application)

### 3.1 Fields

| Field Name | Type | Constraints | Description |
|------------|------|-------------|-------------|
| `id` | UUID | PRIMARY KEY | Unique identifier |
| `tenant_id` | UUID | NOT NULL, FK → tenants(id) | Multi-tenant isolation |
| `estimated_penalty_id` | UUID | NOT NULL, FK → estimated_tax_penalties(id) | Parent penalty |
| `quarter` | VARCHAR(2) | NOT NULL, CHECK (quarter IN ('Q1','Q2','Q3','Q4')) | Quarter identifier |
| `due_date` | DATE | NOT NULL | Quarter due date |
| `required_payment` | DECIMAL(15,2) | NOT NULL | Required payment for quarter |
| `actual_payment` | DECIMAL(15,2) | NOT NULL | Actual payment made |
| `underpayment` | DECIMAL(15,2) | NOT NULL | Required - Actual (negative if overpaid) |
| `quarters_unpaid` | INTEGER | NOT NULL, CHECK (quarters_unpaid >= 0) | Quarters from due date to filing |
| `penalty_rate` | DECIMAL(5,4) | NOT NULL | Annual rate / 4 |
| `penalty_amount` | DECIMAL(15,2) | NOT NULL, CHECK (penalty_amount >= 0) | Calculated penalty |

### 3.2 Indexes

```sql
CREATE INDEX idx_quarterly_underpayment_penalty ON quarterly_underpayments(estimated_penalty_id);
CREATE INDEX idx_quarterly_underpayment_quarter ON quarterly_underpayments(quarter);
CREATE INDEX idx_quarterly_underpayment_due_date ON quarterly_underpayments(due_date);
```

### 3.3 Constraints

```sql
-- Underpayment must equal required - actual
-- Note: Underpayment is calculated in application layer with proper rounding
ALTER TABLE quarterly_underpayments ADD CONSTRAINT check_underpayment_calculation
    CHECK (underpayment = (required_payment - actual_payment));

-- Penalty amount must match formula (if underpayment > 0)
ALTER TABLE quarterly_underpayments ADD CONSTRAINT check_penalty_calculation
    CHECK (underpayment <= 0 OR 
           ABS(penalty_amount - (underpayment * penalty_rate * quarters_unpaid)) < 0.01);

-- Only one record per quarter per penalty
CREATE UNIQUE INDEX unique_quarterly_underpayment ON quarterly_underpayments(estimated_penalty_id, quarter);
```

### 3.4 Java Entity

```java
@Entity
@Table(name = "quarterly_underpayments")
public class QuarterlyUnderpayment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estimated_penalty_id", nullable = false)
    private EstimatedTaxPenalty estimatedTaxPenalty;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "quarter", nullable = false, length = 2)
    private Quarter quarter;
    
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;
    
    @Column(name = "required_payment", nullable = false, precision = 15, scale = 2)
    private BigDecimal requiredPayment;
    
    @Column(name = "actual_payment", nullable = false, precision = 15, scale = 2)
    private BigDecimal actualPayment;
    
    @Column(name = "underpayment", nullable = false, precision = 15, scale = 2)
    private BigDecimal underpayment;
    
    @Column(name = "quarters_unpaid", nullable = false)
    private Integer quartersUnpaid;
    
    @Column(name = "penalty_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal penaltyRate;
    
    @Column(name = "penalty_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal penaltyAmount;
    
    // Getters, setters omitted
}

public enum Quarter {
    Q1,  // Due April 15
    Q2,  // Due June 15
    Q3,  // Due September 15
    Q4   // Due January 15 (next year)
}
```

---

## 4. Interest Entity

**Purpose**: Stores interest calculations on unpaid tax.

**Functional Requirements**: FR-027 (Interest Rate), FR-028 (Daily Interest), FR-029 (Quarterly Compounding)

### 4.1 Fields

| Field Name | Type | Constraints | Description |
|------------|------|-------------|-------------|
| `id` | UUID | PRIMARY KEY | Unique identifier |
| `tenant_id` | UUID | NOT NULL, FK → tenants(id) | Multi-tenant isolation |
| `return_id` | UUID | NOT NULL, FK → tax_returns(id) | Associated tax return |
| `tax_due_date` | DATE | NOT NULL | When tax was due |
| `unpaid_tax_amount` | DECIMAL(15,2) | NOT NULL | Original tax balance |
| `annual_interest_rate` | DECIMAL(5,4) | NOT NULL | Retrieved from rule engine |
| `compounding_frequency` | VARCHAR(20) | NOT NULL, DEFAULT 'QUARTERLY' | `QUARTERLY` (standard) |
| `start_date` | DATE | NOT NULL | Interest starts accruing |
| `end_date` | DATE | NOT NULL | Interest stops accruing |
| `total_days` | INTEGER | NOT NULL | Days interest accrued |
| `total_interest` | DECIMAL(15,2) | NOT NULL | Sum of all interest |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Audit trail |
| `created_by` | UUID | NOT NULL, FK → users(id) | User/system who calculated |
| `updated_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Last update |

### 4.2 Indexes

```sql
CREATE INDEX idx_interest_return ON interests(return_id);
CREATE INDEX idx_interest_tenant ON interests(tenant_id);
CREATE INDEX idx_interest_start_date ON interests(start_date);
CREATE INDEX idx_interest_end_date ON interests(end_date);
```

### 4.3 Constraints

```sql
-- End date must be after or equal to start date
ALTER TABLE interests ADD CONSTRAINT check_interest_dates
    CHECK (end_date >= start_date);

-- Total days must match date difference
ALTER TABLE interests ADD CONSTRAINT check_total_days_calculation
    CHECK (total_days = (end_date - start_date));

-- Total interest must be sum of quarterly interest
ALTER TABLE interests ADD CONSTRAINT check_total_interest_non_negative
    CHECK (total_interest >= 0);
```

### 4.4 Java Entity

```java
@Entity
@Table(name = "interests")
public class Interest {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_id", nullable = false)
    private TaxReturn taxReturn;
    
    @Column(name = "tax_due_date", nullable = false)
    private LocalDate taxDueDate;
    
    @Column(name = "unpaid_tax_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal unpaidTaxAmount;
    
    @Column(name = "annual_interest_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal annualInterestRate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "compounding_frequency", nullable = false, length = 20)
    private CompoundingFrequency compoundingFrequency = CompoundingFrequency.QUARTERLY;
    
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;
    
    @Column(name = "total_days", nullable = false)
    private Integer totalDays;
    
    @Column(name = "total_interest", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalInterest;
    
    @OneToMany(mappedBy = "interest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuarterlyInterest> quarterlyInterests = new ArrayList<>();
    
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

public enum CompoundingFrequency {
    QUARTERLY
}
```

---

## 5. QuarterlyInterest Entity

**Purpose**: Stores interest calculation details for each quarter.

**Functional Requirements**: FR-029 (Quarterly Compounding), FR-031 (Interest Breakdown)

### 5.1 Fields

| Field Name | Type | Constraints | Description |
|------------|------|-------------|-------------|
| `id` | UUID | PRIMARY KEY | Unique identifier |
| `tenant_id` | UUID | NOT NULL, FK → tenants(id) | Multi-tenant isolation |
| `interest_id` | UUID | NOT NULL, FK → interests(id) | Parent interest |
| `quarter` | VARCHAR(10) | NOT NULL | "Q1 2024", "Q2 2024", etc. |
| `start_date` | DATE | NOT NULL | First day of quarter |
| `end_date` | DATE | NOT NULL | Last day of quarter |
| `days` | INTEGER | NOT NULL | Days in quarter (90-92) |
| `beginning_balance` | DECIMAL(15,2) | NOT NULL | Principal at start of quarter |
| `interest_accrued` | DECIMAL(15,2) | NOT NULL | Balance × Rate × Days/365 |
| `ending_balance` | DECIMAL(15,2) | NOT NULL | Beginning + Interest (compounded) |

### 5.2 Indexes

```sql
CREATE INDEX idx_quarterly_interest_parent ON quarterly_interests(interest_id);
CREATE INDEX idx_quarterly_interest_quarter ON quarterly_interests(quarter);
CREATE INDEX idx_quarterly_interest_dates ON quarterly_interests(start_date, end_date);
```

### 5.3 Constraints

```sql
-- Ending balance must equal beginning + interest
ALTER TABLE quarterly_interests ADD CONSTRAINT check_ending_balance_calculation
    CHECK (ABS(ending_balance - (beginning_balance + interest_accrued)) < 0.01);

-- Days must match date difference
ALTER TABLE quarterly_interests ADD CONSTRAINT check_days_calculation
    CHECK (days = (end_date - start_date + 1));

-- End date must be after start date
ALTER TABLE quarterly_interests ADD CONSTRAINT check_quarter_dates
    CHECK (end_date >= start_date);
```

### 5.4 Java Entity

```java
@Entity
@Table(name = "quarterly_interests")
public class QuarterlyInterest {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interest_id", nullable = false)
    private Interest interest;
    
    @Column(name = "quarter", nullable = false, length = 10)
    private String quarter;
    
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;
    
    @Column(name = "days", nullable = false)
    private Integer days;
    
    @Column(name = "beginning_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal beginningBalance;
    
    @Column(name = "interest_accrued", nullable = false, precision = 15, scale = 2)
    private BigDecimal interestAccrued;
    
    @Column(name = "ending_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal endingBalance;
    
    // Getters, setters omitted
}
```

---

## 6. PenaltyAbatement Entity

**Purpose**: Stores penalty abatement requests and decisions.

**Functional Requirements**: FR-033 (Abatement Request), FR-036 (First-Time Abatement), FR-038 (Status Tracking)

### 6.1 Fields

| Field Name | Type | Constraints | Description |
|------------|------|-------------|-------------|
| `id` | UUID | PRIMARY KEY | Unique identifier |
| `tenant_id` | UUID | NOT NULL, FK → tenants(id) | Multi-tenant isolation |
| `return_id` | UUID | NOT NULL, FK → tax_returns(id) | Associated tax return |
| `penalty_id` | UUID | NULL, FK → penalties(id) | Specific penalty (if applicable) |
| `request_date` | DATE | NOT NULL | When request submitted |
| `abatement_type` | VARCHAR(50) | NOT NULL | `LATE_FILING`, `LATE_PAYMENT`, `ESTIMATED`, `ALL` |
| `requested_amount` | DECIMAL(15,2) | NOT NULL | Penalty amount to abate |
| `reason` | VARCHAR(50) | NOT NULL | Reason code |
| `explanation` | TEXT | NULL | User-provided narrative |
| `supporting_documents` | JSONB | NULL | File references (hospital records, etc.) |
| `status` | VARCHAR(20) | NOT NULL | `PENDING`, `APPROVED`, `PARTIAL`, `DENIED`, `WITHDRAWN` |
| `reviewed_by` | UUID | NULL, FK → users(id) | Auditor who reviewed |
| `review_date` | DATE | NULL | When decision made |
| `approved_amount` | DECIMAL(15,2) | NULL | Amount actually abated |
| `denial_reason` | TEXT | NULL | Explanation if denied |
| `form_generated` | VARCHAR(500) | NULL | Form 27-PA PDF path |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Audit trail |
| `updated_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Last update |

### 6.2 Indexes

```sql
CREATE INDEX idx_abatement_return ON penalty_abatements(return_id);
CREATE INDEX idx_abatement_penalty ON penalty_abatements(penalty_id);
CREATE INDEX idx_abatement_status ON penalty_abatements(status);
CREATE INDEX idx_abatement_reason ON penalty_abatements(reason);
CREATE INDEX idx_abatement_reviewed_by ON penalty_abatements(reviewed_by);
```

### 6.3 Constraints

```sql
-- If status is approved/partial, must have approved amount
ALTER TABLE penalty_abatements ADD CONSTRAINT check_approved_amount
    CHECK ((status NOT IN ('APPROVED', 'PARTIAL')) OR 
           (approved_amount IS NOT NULL AND approved_amount > 0));

-- If status is denied, must have denial reason
ALTER TABLE penalty_abatements ADD CONSTRAINT check_denial_reason
    CHECK (status != 'DENIED' OR denial_reason IS NOT NULL);

-- If status is not pending, must have reviewer and review date
ALTER TABLE penalty_abatements ADD CONSTRAINT check_review_fields
    CHECK (status = 'PENDING' OR 
           (reviewed_by IS NOT NULL AND review_date IS NOT NULL));

-- Approved amount cannot exceed requested amount
ALTER TABLE penalty_abatements ADD CONSTRAINT check_approved_vs_requested
    CHECK (approved_amount IS NULL OR approved_amount <= requested_amount);
```

### 6.4 Java Entity

```java
@Entity
@Table(name = "penalty_abatements")
public class PenaltyAbatement {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_id", nullable = false)
    private TaxReturn taxReturn;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "penalty_id")
    private Penalty penalty;
    
    @Column(name = "request_date", nullable = false)
    private LocalDate requestDate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "abatement_type", nullable = false, length = 50)
    private AbatementType abatementType;
    
    @Column(name = "requested_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal requestedAmount;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 50)
    private AbatementReason reason;
    
    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;
    
    @Type(JsonBinaryType.class)
    @Column(name = "supporting_documents", columnDefinition = "jsonb")
    private String supportingDocuments;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AbatementStatus status = AbatementStatus.PENDING;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;
    
    @Column(name = "review_date")
    private LocalDate reviewDate;
    
    @Column(name = "approved_amount", precision = 15, scale = 2)
    private BigDecimal approvedAmount;
    
    @Column(name = "denial_reason", columnDefinition = "TEXT")
    private String denialReason;
    
    @Column(name = "form_generated", length = 500)
    private String formGenerated;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    // Getters, setters omitted
}

public enum AbatementType {
    LATE_FILING,
    LATE_PAYMENT,
    ESTIMATED,
    ALL
}

public enum AbatementReason {
    DEATH,              // Death in immediate family
    ILLNESS,            // Serious illness or incapacitation
    DISASTER,           // Natural disaster
    MISSING_RECORDS,    // Unable to obtain records
    ERRONEOUS_ADVICE,   // Erroneous advice from tax authority
    FIRST_TIME,         // First-time penalty abatement
    OTHER               // Other reasonable cause
}

public enum AbatementStatus {
    PENDING,    // Submitted, awaiting review
    APPROVED,   // Full abatement approved
    PARTIAL,    // Partial abatement approved
    DENIED,     // Request denied
    WITHDRAWN   // Taxpayer withdrew request
}
```

---

## 7. PaymentAllocation Entity

**Purpose**: Tracks how payments are allocated to tax, penalties, and interest.

**Functional Requirements**: FR-040 (Payment Order), FR-041 (Payment Tracking), FR-042 (Recalculation)

### 7.1 Fields

| Field Name | Type | Constraints | Description |
|------------|------|-------------|-------------|
| `id` | UUID | PRIMARY KEY | Unique identifier |
| `tenant_id` | UUID | NOT NULL, FK → tenants(id) | Multi-tenant isolation |
| `return_id` | UUID | NOT NULL, FK → tax_returns(id) | Associated tax return |
| `payment_date` | DATE | NOT NULL | Payment date |
| `payment_amount` | DECIMAL(15,2) | NOT NULL, CHECK (payment_amount > 0) | Total payment received |
| `applied_to_tax` | DECIMAL(15,2) | NOT NULL, CHECK (applied_to_tax >= 0) | Amount applied to principal |
| `applied_to_penalties` | DECIMAL(15,2) | NOT NULL, CHECK (applied_to_penalties >= 0) | Amount applied to penalties |
| `applied_to_interest` | DECIMAL(15,2) | NOT NULL, CHECK (applied_to_interest >= 0) | Amount applied to interest |
| `remaining_tax_balance` | DECIMAL(15,2) | NOT NULL, CHECK (remaining_tax_balance >= 0) | After this payment |
| `remaining_penalty_balance` | DECIMAL(15,2) | NOT NULL, CHECK (remaining_penalty_balance >= 0) | After this payment |
| `remaining_interest_balance` | DECIMAL(15,2) | NOT NULL, CHECK (remaining_interest_balance >= 0) | After this payment |
| `allocation_order` | VARCHAR(20) | NOT NULL, DEFAULT 'TAX_FIRST' | `TAX_FIRST` (standard IRS) |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Audit trail |
| `created_by` | UUID | NOT NULL, FK → users(id) | User/system who processed payment |

### 7.2 Indexes

```sql
CREATE INDEX idx_payment_allocation_return ON payment_allocations(return_id);
CREATE INDEX idx_payment_allocation_date ON payment_allocations(payment_date);
CREATE INDEX idx_payment_allocation_tenant ON payment_allocations(tenant_id);
```

### 7.3 Constraints

```sql
-- Payment amount must equal sum of allocations
ALTER TABLE payment_allocations ADD CONSTRAINT check_payment_allocation_sum
    CHECK (ABS(payment_amount - (applied_to_tax + applied_to_penalties + applied_to_interest)) < 0.01);

-- All allocation amounts must be non-negative
ALTER TABLE payment_allocations ADD CONSTRAINT check_allocation_non_negative
    CHECK (applied_to_tax >= 0 AND applied_to_penalties >= 0 AND applied_to_interest >= 0);
```

### 7.4 Java Entity

```java
@Entity
@Table(name = "payment_allocations")
public class PaymentAllocation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_id", nullable = false)
    private TaxReturn taxReturn;
    
    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;
    
    @Column(name = "payment_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal paymentAmount;
    
    @Column(name = "applied_to_tax", nullable = false, precision = 15, scale = 2)
    private BigDecimal appliedToTax;
    
    @Column(name = "applied_to_penalties", nullable = false, precision = 15, scale = 2)
    private BigDecimal appliedToPenalties;
    
    @Column(name = "applied_to_interest", nullable = false, precision = 15, scale = 2)
    private BigDecimal appliedToInterest;
    
    @Column(name = "remaining_tax_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal remainingTaxBalance;
    
    @Column(name = "remaining_penalty_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal remainingPenaltyBalance;
    
    @Column(name = "remaining_interest_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal remainingInterestBalance;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "allocation_order", nullable = false, length = 20)
    private AllocationOrder allocationOrder = AllocationOrder.TAX_FIRST;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @Column(name = "created_by", nullable = false)
    private UUID createdBy;
    
    // Getters, setters omitted
}

public enum AllocationOrder {
    TAX_FIRST   // Standard IRS ordering: Tax → Penalties → Interest
}
```

---

## 8. PenaltyAuditLog Entity

**Purpose**: Immutable audit trail for all penalty and interest actions (Constitution III).

**Functional Requirements**: Constitution III (Audit Trail Immutability), FR-045 (Audit Log)

### 8.1 Fields

| Field Name | Type | Constraints | Description |
|------------|------|-------------|-------------|
| `id` | UUID | PRIMARY KEY | Unique identifier |
| `tenant_id` | UUID | NOT NULL | Multi-tenant isolation |
| `entity_type` | VARCHAR(50) | NOT NULL | `PENALTY`, `INTEREST`, `ESTIMATED_TAX`, `ABATEMENT`, `PAYMENT_ALLOCATION` |
| `entity_id` | UUID | NOT NULL | ID of affected entity |
| `action` | VARCHAR(50) | NOT NULL | `ASSESSED`, `CALCULATED`, `ABATED`, `PAYMENT_APPLIED`, `RECALCULATED` |
| `actor_id` | UUID | NOT NULL, FK → users(id) | User who performed action |
| `actor_role` | VARCHAR(20) | NOT NULL | `TAXPAYER`, `AUDITOR`, `SYSTEM` |
| `description` | TEXT | NOT NULL | Human-readable description |
| `old_value` | JSONB | NULL | Previous state (if update) |
| `new_value` | JSONB | NULL | New state (if create/update) |
| `ip_address` | VARCHAR(45) | NULL | Actor's IP address |
| `user_agent` | VARCHAR(255) | NULL | Browser user agent |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Immutable timestamp |

### 8.2 Indexes

```sql
CREATE INDEX idx_penalty_audit_entity ON penalty_audit_logs(entity_type, entity_id);
CREATE INDEX idx_penalty_audit_actor ON penalty_audit_logs(actor_id);
CREATE INDEX idx_penalty_audit_created_at ON penalty_audit_logs(created_at);
CREATE INDEX idx_penalty_audit_tenant ON penalty_audit_logs(tenant_id);
```

### 8.3 Java Entity

```java
@Entity
@Table(name = "penalty_audit_logs")
@Immutable  // Hibernate: Never update, only insert
public class PenaltyAuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 50)
    private PenaltyAuditEntityType entityType;
    
    @Column(name = "entity_id", nullable = false)
    private UUID entityId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 50)
    private PenaltyAuditAction action;
    
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

public enum PenaltyAuditEntityType {
    PENALTY,
    INTEREST,
    ESTIMATED_TAX,
    ABATEMENT,
    PAYMENT_ALLOCATION
}

public enum PenaltyAuditAction {
    ASSESSED,
    CALCULATED,
    ABATED,
    PAYMENT_APPLIED,
    RECALCULATED
}

public enum ActorRole {
    TAXPAYER,   // Taxpayer or their representative
    AUDITOR,    // Municipality auditor
    SYSTEM      // Automated action
}
```

---

## Database Migration Plan

### Flyway Migration Files

```text
V1.30__create_penalties_table.sql
V1.31__create_estimated_tax_penalties_table.sql
V1.32__create_quarterly_underpayments_table.sql
V1.33__create_interests_table.sql
V1.34__create_quarterly_interests_table.sql
V1.35__create_penalty_abatements_table.sql
V1.36__create_payment_allocations_table.sql
V1.37__create_penalty_audit_logs_table.sql
V1.38__add_penalty_indexes.sql
V1.39__add_penalty_constraints.sql
```

### Rollback Strategy

All migrations reversible via `V1.3X__rollback.sql` scripts:

```sql
-- V1.39__rollback.sql
ALTER TABLE penalties DROP CONSTRAINT check_penalty_cap;
-- ... drop all constraints

-- V1.38__rollback.sql
DROP INDEX idx_penalty_return;
-- ... drop all indexes

-- V1.37__rollback.sql
DROP TABLE penalty_audit_logs;
-- ... drop all tables in reverse order
```

---

## Performance Considerations

### Query Optimization

1. **Penalty Calculation** (FR-001, FR-007):
   ```sql
   -- Calculate all penalties for a return
   SELECT * FROM penalties
   WHERE return_id = ? AND tenant_id = ?;
   ```
   **Performance**: <100ms with idx_penalty_return

2. **Safe Harbor Check** (FR-015):
   ```sql
   -- Check if estimated tax penalty applies
   SELECT safe_harbor_1_met, safe_harbor_2_met
   FROM estimated_tax_penalties
   WHERE return_id = ?;
   ```
   **Performance**: <50ms with unique index

3. **Interest Calculation** (FR-029):
   ```sql
   -- Get quarterly interest breakdown
   SELECT * FROM quarterly_interests
   WHERE interest_id = ?
   ORDER BY start_date;
   ```
   **Performance**: <100ms with idx_quarterly_interest_parent

4. **Payment History** (FR-043):
   ```sql
   -- Get payment allocation history
   SELECT * FROM payment_allocations
   WHERE return_id = ? AND tenant_id = ?
   ORDER BY payment_date DESC;
   ```
   **Performance**: <100ms with idx_payment_allocation_return

### Cache Strategy

- **Redis Cache**: Penalty and interest calculations TTL = 15 minutes
- **Event-Driven Update**: PaymentReceivedEvent invalidates cache, recalculates balances
- **Deterministic Calculations**: All calculations reproducible from stored data

---

## Data Retention

Per Constitution III (Audit Trail Immutability):

| Entity | Retention Period | Deletion Strategy |
|--------|------------------|-------------------|
| Penalty | 7 years (IRS requirement) | Soft delete after 7 years |
| EstimatedTaxPenalty | 7 years | Soft delete |
| Interest | 7 years | Soft delete |
| PenaltyAbatement | 7 years | Soft delete |
| PaymentAllocation | 7 years | Soft delete |
| PenaltyAuditLog | **Permanent** (never delete) | Archive to cold storage after 10 years |

---

## Next Steps

1. ✅ Data model complete
2. ⏳ Generate API contracts (OpenAPI 3.0) → `/contracts/`
3. ⏳ Create quickstart.md (developer guide)
4. ⏳ Create research.md (edge cases and decisions)
5. ⏳ Generate tasks.md (implementation breakdown)
