# Data Model: Comprehensive Business Schedule X Reconciliation (25+ Fields)

**Feature**: Comprehensive Business Schedule X Reconciliation (25+ Fields)  
**Phase**: Phase 1 (Design & Contracts)  
**Date**: 2025-11-27

---

## Overview

This document defines the expanded data model for Business Schedule X reconciliation, extending from 6 basic fields to 27 comprehensive book-to-tax reconciliation fields. The expanded model supports complete M-1 reconciliation for C-Corporations, Partnerships, and S-Corporations per IRS Form 1120 Schedule M-1 requirements.

**Database**: PostgreSQL 16  
**Schema**: Tenant-scoped (`dublin.*`, `columbus.*`)  
**Storage**: JSONB field in existing `business_tax_return` table (no SQL migration needed)  
**ORM**: Spring Data JPA with Hibernate  
**Backward Compatibility**: Runtime conversion from old 6-field format to new 27-field format (Research R2)

---

## Entity Relationship Overview

```
┌─────────────────────────┐
│   Business              │
│                         │
│ - id (UUID)             │
│ - ein (String)          │
│ - name                  │
│ - entity_type           │────┐
└─────────────────────────┘    │
                               │
                               ↓
                    ┌──────────────────────────────┐
                    │   NetProfitReturn            │
                    │                              │
                    │ - id (UUID)                  │
                    │ - business_id (FK)           │
                    │ - tax_year                   │
                    │ - schedule_x_details (JSONB) │────→ BusinessScheduleXDetails (expanded to 27 fields)
                    │ - status                     │
                    │ - created_at                 │
                    └──────────────────────────────┘
                               │
                               │ references
                               ↓
                    ┌──────────────────────────────┐
                    │   ScheduleXExtractionResult  │
                    │   (extraction-service)       │
                    │                              │
                    │ - return_id (FK)             │
                    │ - fields (Map<String,        │
                    │   ScheduleXFieldExtraction>) │
                    │ - extracted_at               │
                    └──────────────────────────────┘
```

---

## 1. BusinessScheduleXDetails (EXPANDED: 6 → 27 Fields)

**Purpose**: Comprehensive book-to-tax reconciliation data for Schedule X (municipal equivalent of IRS Form 1120 Schedule M-1).

**Storage**: JSONB field `schedule_x_details` in `business_tax_return` table (existing table, no migration needed).

**Functional Requirements**: FR-001 through FR-038 (all Schedule X fields and calculations)

### 1.1 Top-Level Structure

| Field Name | Type | Description |
|------------|------|-------------|
| `fedTaxableIncome` | number | Federal taxable income from Form 1120 Line 30 / Form 1065 Line 22 / Form 1120-S Line 23 |
| `addBacks` | AddBacks object | 20 fields that increase federal income for municipal purposes |
| `deductions` | Deductions object | 7 fields that decrease federal income for municipal purposes |
| `calculatedFields` | CalculatedFields object | Read-only computed values (totals, adjusted income) |
| `metadata` | Metadata object | Audit trail, auto-calculation flags, attached documents |

---

### 1.2 AddBacks Object (20 Fields)

**Purpose**: Adjustments that **increase** federal taxable income to arrive at municipal taxable income.

| Field Name | Type | FR # | Description | Example |
|------------|------|------|-------------|---------|
| `depreciationAdjustment` | number | FR-001 | Difference between book depreciation and MACRS tax depreciation | Book $80K - MACRS $130K = **-$50K** (negative add-back = book less than tax) |
| `amortizationAdjustment` | number | FR-002 | Difference between book and tax amortization for intangibles (goodwill, patents) | Book $5K - Tax $3K = **$2K** |
| `incomeAndStateTaxes` | number | FR-003 | State/local/foreign income taxes deducted on federal return (add back for municipal) | **$10,000** state taxes |
| `guaranteedPayments` | number | FR-004 | Form 1065 Line 10 guaranteed payments to partners (deductible federally, not municipally) | **$50,000** (partnerships only) |
| `mealsAndEntertainment` | number | FR-005 | 100% of meals/entertainment expenses (50% deductible federally, 0% municipally) | Federal deducted $15K (50% of $30K), add back **$30,000** |
| `relatedPartyExcess` | number | FR-006 | Payments to related parties above fair market value | Paid $10K rent, FMV $7.5K, add back **$2,500** |
| `penaltiesAndFines` | number | FR-007 | Government penalties/fines (already non-deductible federally, but verify) | EPA fine **$10,000** |
| `politicalContributions` | number | FR-008 | Political campaign contributions (already non-deductible federally, but verify) | Campaign donation **$5,000** |
| `officerLifeInsurance` | number | FR-009 | Life insurance premiums where corp is beneficiary (non-deductible) | **$3,000** premiums |
| `capitalLossExcess` | number | FR-010 | Capital losses exceeding capital gains (Form 1120 Line 8 carryforward) | Losses $15K, gains $10K, add back **$5,000** |
| `federalTaxRefunds` | number | FR-011 | Prior year federal income tax refunds included in income (rare) | **$1,000** refund |
| `expensesOnIntangibleIncome` | number | FR-012 | 5% Rule: expenses incurred to earn non-taxable intangible income | Interest $20K + Dividends $15K = $35K × 5% = **$1,750** |
| `section179Excess` | number | FR-013 | Section 179 expensing exceeding municipal limits | Federal $1M, municipal $500K, add back **$500,000** |
| `bonusDepreciation` | number | FR-014 | 100% bonus depreciation allowed federally but not municipally | Equipment $200K × 100% = **$200,000** |
| `badDebtReserveIncrease` | number | FR-015 | Increase in bad debt reserve (reserve method vs direct write-off) | Reserve increased **$8,000** |
| `charitableContributionExcess` | number | FR-016 | Contributions exceeding 10% limit (federal error, municipal follows 10% rule) | Contributions $80K, limit $60K (10% of $600K), add back **$20,000** |
| `domesticProductionActivities` | number | FR-017 | Section 199 DPAD deduction (pre-TCJA, no longer applies for most) | **$25,000** DPAD |
| `stockCompensationAdjustment` | number | FR-018 | Difference between book expense (ASC 718) and tax deduction (intrinsic value) | Book $50K, tax $40K, add back **$10,000** |
| `inventoryMethodChange` | number | FR-019 | Section 481(a) adjustment for inventory method change (LIFO → FIFO) | **$30,000** adjustment |
| `otherAddBacks` | number | FR-020 | Catch-all for adjustments not covered by specific fields | **$5,000** (requires description) |
| `otherAddBacksDescription` | string | FR-020 | **Required** if `otherAddBacks` > 0 | "Foreign currency translation adjustment" |

**Validation Rules**:
- All numeric fields must be `>= 0` (add-backs increase income, cannot be negative)
- `otherAddBacksDescription` is **required** if `otherAddBacks` > 0
- `guaranteedPayments` only applies to partnerships (entity_type = "PARTNERSHIP")

---

### 1.3 Deductions Object (7 Fields)

**Purpose**: Adjustments that **decrease** federal taxable income to arrive at municipal taxable income.

| Field Name | Type | FR # | Description | Example |
|------------|------|------|-------------|---------|
| `interestIncome` | number | FR-021 | Taxable interest income (non-taxable for municipal purposes) | **$5,000** interest |
| `dividends` | number | FR-022 | Qualified and ordinary dividends (non-taxable municipally) | **$3,000** dividends |
| `capitalGains` | number | FR-023 | Net capital gains (non-taxable municipally) | **$2,000** gains |
| `section179Recapture` | number | FR-024 | Recapture of Section 179 deduction (if asset sold before recovery period) | **$10,000** recapture |
| `municipalBondInterest` | number | FR-025 | Municipal bond interest taxable at different jurisdiction | Cross-jurisdiction bonds **$1,500** |
| `depletionDifference` | number | FR-026 | Percentage depletion (oil/gas) exceeding cost depletion | Percentage $50K, cost $40K, deduct **$10,000** |
| `otherDeductions` | number | FR-027 | Catch-all for deductions not covered by specific fields | **$2,000** (requires description) |
| `otherDeductionsDescription` | string | FR-027 | **Required** if `otherDeductions` > 0 | "IRC §108 insolvency exclusion" |

**Validation Rules**:
- All numeric fields must be `>= 0` (deductions decrease income, cannot be negative)
- `otherDeductionsDescription` is **required** if `otherDeductions` > 0

---

### 1.4 CalculatedFields Object (Read-Only)

**Purpose**: Computed values derived from `addBacks` and `deductions`. **Never manually entered** - always calculated by system.

| Field Name | Type | FR # | Formula | Example |
|------------|------|------|---------|---------|
| `totalAddBacks` | number | FR-028 | Sum of all 20 add-back fields | $50K + $10K + $15K = **$75,000** |
| `totalDeductions` | number | FR-029 | Sum of all 7 deduction fields | $5K + $3K + $2K = **$10,000** |
| `adjustedMunicipalIncome` | number | FR-030 | `fedTaxableIncome` + `totalAddBacks` - `totalDeductions` | $500K + $75K - $10K = **$565,000** |

**Calculation Service** (BusinessTaxCalculator.java):

```java
public void recalculateTotals(BusinessScheduleXDetails scheduleX) {
    CalculatedFields calc = new CalculatedFields();
    
    // Sum all add-backs
    AddBacks addBacks = scheduleX.getAddBacks();
    calc.setTotalAddBacks(
        addBacks.getDepreciationAdjustment() +
        addBacks.getAmortizationAdjustment() +
        addBacks.getIncomeAndStateTaxes() +
        // ... sum all 20 fields
    );
    
    // Sum all deductions
    Deductions deductions = scheduleX.getDeductions();
    calc.setTotalDeductions(
        deductions.getInterestIncome() +
        deductions.getDividends() +
        // ... sum all 7 fields
    );
    
    // Calculate adjusted income
    calc.setAdjustedMunicipalIncome(
        scheduleX.getFedTaxableIncome() +
        calc.getTotalAddBacks() -
        calc.getTotalDeductions()
    );
    
    scheduleX.setCalculatedFields(calc);
}
```

---

### 1.5 Metadata Object

**Purpose**: Audit trail, auto-calculation tracking, and document attachments.

| Field Name | Type | FR # | Description | Example |
|------------|------|------|-------------|---------|
| `lastModified` | timestamp | - | Last modification timestamp (ISO 8601) | `"2024-11-27T19:45:00Z"` |
| `autoCalculatedFields` | string[] | FR-037 | List of fields populated by auto-calculation helpers | `["mealsAndEntertainment", "expensesOnIntangibleIncome"]` |
| `manualOverrides` | string[] | FR-037 | List of fields where user overrode AI-extracted value | `["depreciationAdjustment"]` |
| `attachedDocuments` | AttachedDocument[] | FR-036 | Supporting documentation for adjustments | `[{ fileName: "depreciation_schedule.xlsx", fileUrl: "s3://...", fieldName: "depreciationAdjustment" }]` |

**AttachedDocument Schema**:
```typescript
interface AttachedDocument {
  fileName: string;        // "depreciation_schedule.xlsx"
  fileUrl: string;         // "s3://munitax-docs/tenant-123/return-456/depreciation.xlsx"
  fieldName: string;       // "depreciationAdjustment" (which Schedule X field this supports)
  uploadedAt: string;      // "2024-11-27T19:45:00Z"
  uploadedBy: string;      // User ID who uploaded
}
```

---

### 1.6 Complete JSON Example (User Story 1: C-Corp with Depreciation, Meals, State Taxes)

```json
{
  "fedTaxableIncome": 500000.00,
  "addBacks": {
    "depreciationAdjustment": 50000.00,
    "amortizationAdjustment": 0.00,
    "incomeAndStateTaxes": 10000.00,
    "guaranteedPayments": 0.00,
    "mealsAndEntertainment": 15000.00,
    "relatedPartyExcess": 0.00,
    "penaltiesAndFines": 0.00,
    "politicalContributions": 0.00,
    "officerLifeInsurance": 0.00,
    "capitalLossExcess": 0.00,
    "federalTaxRefunds": 0.00,
    "expensesOnIntangibleIncome": 0.00,
    "section179Excess": 0.00,
    "bonusDepreciation": 0.00,
    "badDebtReserveIncrease": 0.00,
    "charitableContributionExcess": 0.00,
    "domesticProductionActivities": 0.00,
    "stockCompensationAdjustment": 0.00,
    "inventoryMethodChange": 0.00,
    "otherAddBacks": 0.00,
    "otherAddBacksDescription": null
  },
  "deductions": {
    "interestIncome": 0.00,
    "dividends": 0.00,
    "capitalGains": 0.00,
    "section179Recapture": 0.00,
    "municipalBondInterest": 0.00,
    "depletionDifference": 0.00,
    "otherDeductions": 0.00,
    "otherDeductionsDescription": null
  },
  "calculatedFields": {
    "totalAddBacks": 75000.00,
    "totalDeductions": 0.00,
    "adjustedMunicipalIncome": 575000.00
  },
  "metadata": {
    "lastModified": "2024-11-27T19:45:00Z",
    "autoCalculatedFields": ["mealsAndEntertainment"],
    "manualOverrides": [],
    "attachedDocuments": [
      {
        "fileName": "form_4562_depreciation.pdf",
        "fileUrl": "s3://munitax-docs/dublin/return-123/form4562.pdf",
        "fieldName": "depreciationAdjustment",
        "uploadedAt": "2024-11-27T19:40:00Z",
        "uploadedBy": "user-uuid-456"
      }
    ]
  }
}
```

---

### 1.7 Backward Compatibility (Old 6-Field Format)

**Old Format** (pre-expansion):
```json
{
  "fedTaxableIncome": 500000,
  "incomeAndStateTaxes": 10000,
  "interestIncome": 5000,
  "dividends": 3000,
  "capitalGains": 2000,
  "other": 0
}
```

**Conversion Logic** (see Research R2):
- Detect old format: `if (json.has("interestIncome") && !json.has("deductions"))`
- Migrate fields to nested structure:
  - `incomeAndStateTaxes` → `addBacks.incomeAndStateTaxes`
  - `interestIncome` → `deductions.interestIncome`
  - `dividends` → `deductions.dividends`
  - `capitalGains` → `deductions.capitalGains`
  - `other` → `deductions.otherDeductions` (or `addBacks.otherAddBacks` if negative)
- Initialize all other fields to `0` or `null`
- Save in new format on next update

**Test Case**: Old format still calculates correctly:
```java
@Test
public void testOldFormatCompatibility() {
    // Load old format
    BusinessScheduleXDetails oldScheduleX = loadOldFormat();
    
    // Verify calculation matches
    assertEquals(500000, oldScheduleX.getFedTaxableIncome());
    assertEquals(10000, oldScheduleX.getAddBacks().getIncomeAndStateTaxes());
    assertEquals(10000, oldScheduleX.getCalculatedFields().getTotalAddBacks());
    assertEquals(10000, oldScheduleX.getCalculatedFields().getTotalDeductions());
    assertEquals(500000, oldScheduleX.getCalculatedFields().getAdjustedMunicipalIncome());
}
```

---

### 1.8 Java Entity (BusinessScheduleXDetails.java)

```java
/**
 * Expanded BusinessScheduleXDetails entity with 27 fields for complete M-1 reconciliation.
 * Stored as JSONB in business_tax_return.schedule_x_details field.
 * Supports backward compatibility with old 6-field format (runtime conversion).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusinessScheduleXDetails {
    
    private Double fedTaxableIncome; // Federal Form 1120 Line 30 / 1065 Line 22 / 1120-S Line 23
    
    private AddBacks addBacks;
    private Deductions deductions;
    private CalculatedFields calculatedFields; // Read-only, always recalculated
    private Metadata metadata;
    
    /**
     * Recalculate totalAddBacks, totalDeductions, adjustedMunicipalIncome.
     * Called after any field update.
     */
    public void recalculateTotals() {
        if (calculatedFields == null) {
            calculatedFields = new CalculatedFields();
        }
        
        // Sum all add-backs
        double totalAddBacks = 
            addBacks.getDepreciationAdjustment() +
            addBacks.getAmortizationAdjustment() +
            addBacks.getIncomeAndStateTaxes() +
            addBacks.getGuaranteedPayments() +
            addBacks.getMealsAndEntertainment() +
            addBacks.getRelatedPartyExcess() +
            addBacks.getPenaltiesAndFines() +
            addBacks.getPoliticalContributions() +
            addBacks.getOfficerLifeInsurance() +
            addBacks.getCapitalLossExcess() +
            addBacks.getFederalTaxRefunds() +
            addBacks.getExpensesOnIntangibleIncome() +
            addBacks.getSection179Excess() +
            addBacks.getBonusDepreciation() +
            addBacks.getBadDebtReserveIncrease() +
            addBacks.getCharitableContributionExcess() +
            addBacks.getDomesticProductionActivities() +
            addBacks.getStockCompensationAdjustment() +
            addBacks.getInventoryMethodChange() +
            addBacks.getOtherAddBacks();
        
        calculatedFields.setTotalAddBacks(totalAddBacks);
        
        // Sum all deductions
        double totalDeductions =
            deductions.getInterestIncome() +
            deductions.getDividends() +
            deductions.getCapitalGains() +
            deductions.getSection179Recapture() +
            deductions.getMunicipalBondInterest() +
            deductions.getDepletionDifference() +
            deductions.getOtherDeductions();
        
        calculatedFields.setTotalDeductions(totalDeductions);
        
        // Calculate adjusted municipal income
        double adjustedIncome = fedTaxableIncome + totalAddBacks - totalDeductions;
        calculatedFields.setAdjustedMunicipalIncome(adjustedIncome);
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddBacks {
        private Double depreciationAdjustment = 0.0;
        private Double amortizationAdjustment = 0.0;
        private Double incomeAndStateTaxes = 0.0;
        private Double guaranteedPayments = 0.0;
        private Double mealsAndEntertainment = 0.0;
        private Double relatedPartyExcess = 0.0;
        private Double penaltiesAndFines = 0.0;
        private Double politicalContributions = 0.0;
        private Double officerLifeInsurance = 0.0;
        private Double capitalLossExcess = 0.0;
        private Double federalTaxRefunds = 0.0;
        private Double expensesOnIntangibleIncome = 0.0;
        private Double section179Excess = 0.0;
        private Double bonusDepreciation = 0.0;
        private Double badDebtReserveIncrease = 0.0;
        private Double charitableContributionExcess = 0.0;
        private Double domesticProductionActivities = 0.0;
        private Double stockCompensationAdjustment = 0.0;
        private Double inventoryMethodChange = 0.0;
        private Double otherAddBacks = 0.0;
        private String otherAddBacksDescription;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Deductions {
        private Double interestIncome = 0.0;
        private Double dividends = 0.0;
        private Double capitalGains = 0.0;
        private Double section179Recapture = 0.0;
        private Double municipalBondInterest = 0.0;
        private Double depletionDifference = 0.0;
        private Double otherDeductions = 0.0;
        private String otherDeductionsDescription;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CalculatedFields {
        private Double totalAddBacks = 0.0;
        private Double totalDeductions = 0.0;
        private Double adjustedMunicipalIncome = 0.0;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Metadata {
        private String lastModified; // ISO 8601 timestamp
        private List<String> autoCalculatedFields = new ArrayList<>();
        private List<String> manualOverrides = new ArrayList<>();
        private List<AttachedDocument> attachedDocuments = new ArrayList<>();
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttachedDocument {
        private String fileName;
        private String fileUrl;
        private String fieldName;
        private String uploadedAt;
        private String uploadedBy;
    }
}
```

---

### 1.9 TypeScript Interface (Frontend)

**scheduleX.ts**:

```typescript
export interface BusinessScheduleXDetails {
  fedTaxableIncome: number;
  addBacks: AddBacks;
  deductions: Deductions;
  calculatedFields: CalculatedFields;
  metadata: Metadata;
}

export interface AddBacks {
  depreciationAdjustment: number;
  amortizationAdjustment: number;
  incomeAndStateTaxes: number;
  guaranteedPayments: number;
  mealsAndEntertainment: number;
  relatedPartyExcess: number;
  penaltiesAndFines: number;
  politicalContributions: number;
  officerLifeInsurance: number;
  capitalLossExcess: number;
  federalTaxRefunds: number;
  expensesOnIntangibleIncome: number;
  section179Excess: number;
  bonusDepreciation: number;
  badDebtReserveIncrease: number;
  charitableContributionExcess: number;
  domesticProductionActivities: number;
  stockCompensationAdjustment: number;
  inventoryMethodChange: number;
  otherAddBacks: number;
  otherAddBacksDescription?: string | null;
}

export interface Deductions {
  interestIncome: number;
  dividends: number;
  capitalGains: number;
  section179Recapture: number;
  municipalBondInterest: number;
  depletionDifference: number;
  otherDeductions: number;
  otherDeductionsDescription?: string | null;
}

export interface CalculatedFields {
  totalAddBacks: number;
  totalDeductions: number;
  adjustedMunicipalIncome: number;
}

export interface Metadata {
  lastModified: string; // ISO 8601
  autoCalculatedFields: string[];
  manualOverrides: string[];
  attachedDocuments: AttachedDocument[];
}

export interface AttachedDocument {
  fileName: string;
  fileUrl: string;
  fieldName: string;
  uploadedAt: string;
  uploadedBy: string;
}
```

---

## 2. ScheduleXExtractionResult (extraction-service)

**Purpose**: AI extraction results from Form 1120 Schedule M-1 and Form 4562 (depreciation schedules).

**Functional Requirements**: FR-039 (AI extraction), FR-040 (field identification), FR-041 (depreciation schedule parsing), FR-042 (confidence scores)

### 2.1 Fields

| Field Name | Type | Description |
|------------|------|-------------|
| `returnId` | UUID | Foreign key to `business_tax_return.id` |
| `extractedAt` | timestamp | Extraction timestamp |
| `fields` | Map<String, ScheduleXFieldExtraction> | Extracted field values with confidence scores and bounding boxes |
| `sourceDocuments` | SourceDocument[] | List of PDFs processed (Form 1120 Schedule M-1, Form 4562, etc.) |

### 2.2 ScheduleXFieldExtraction

**Purpose**: Individual field extraction result with confidence score and bounding box coordinates (Constitution IV compliance, Research R1).

| Field Name | Type | Description |
|------------|------|-------------|
| `value` | number | Extracted numeric value |
| `confidence` | number | AI confidence score (0.0 - 1.0) |
| `boundingBox` | BoundingBox | Coordinates of extracted text in source PDF |
| `sourceDocument` | string | Which PDF this field was extracted from (e.g., "Form 1120 Schedule M-1, Line 5a") |

### 2.3 BoundingBox

**Purpose**: PDF coordinates for highlighted regions when user clicks confidence badge (Research R1).

| Field Name | Type | Description |
|------------|------|-------------|
| `page` | number | PDF page number (1-indexed) |
| `vertices` | Vertex[] | 4 vertices defining rectangle (top-left, top-right, bottom-right, bottom-left) |

**Vertex**:
```typescript
interface Vertex {
  x: number; // Horizontal position (pixels from left edge)
  y: number; // Vertical position (pixels from top edge)
}
```

### 2.4 Complete JSON Example

```json
{
  "returnId": "550e8400-e29b-41d4-a716-446655440000",
  "extractedAt": "2024-11-27T20:00:00Z",
  "fields": {
    "depreciationAdjustment": {
      "value": 50000.00,
      "confidence": 0.95,
      "boundingBox": {
        "page": 2,
        "vertices": [
          {"x": 245, "y": 189},
          {"x": 298, "y": 189},
          {"x": 298, "y": 205},
          {"x": 245, "y": 205}
        ]
      },
      "sourceDocument": "Form 1120 Schedule M-1, Line 5a (Depreciation)"
    },
    "mealsAndEntertainment": {
      "value": 30000.00,
      "confidence": 0.88,
      "boundingBox": {
        "page": 1,
        "vertices": [
          {"x": 412, "y": 345},
          {"x": 465, "y": 345},
          {"x": 465, "y": 361},
          {"x": 412, "y": 361}
        ]
      },
      "sourceDocument": "Form 1120, Line 20 (Other Deductions - Detail)"
    },
    "incomeAndStateTaxes": {
      "value": 10000.00,
      "confidence": 0.92,
      "boundingBox": {
        "page": 1,
        "vertices": [
          {"x": 412, "y": 278},
          {"x": 465, "y": 278},
          {"x": 465, "y": 294},
          {"x": 412, "y": 294}
        ]
      },
      "sourceDocument": "Form 1120, Line 17 (Taxes and Licenses)"
    }
  },
  "sourceDocuments": [
    {
      "fileName": "form_1120_2024.pdf",
      "fileUrl": "s3://munitax-uploads/tenant-123/form1120.pdf",
      "pageCount": 5,
      "uploadedAt": "2024-11-27T19:55:00Z"
    },
    {
      "fileName": "form_4562_depreciation_2024.pdf",
      "fileUrl": "s3://munitax-uploads/tenant-123/form4562.pdf",
      "pageCount": 3,
      "uploadedAt": "2024-11-27T19:56:00Z"
    }
  ]
}
```

---

## 3. Field Definitions & Help Text (Constants)

**Purpose**: Centralized field definitions for UI help tooltips, validation rules, and auto-calculation rules.

**Location**: `scheduleXConstants.ts` (frontend), `BusinessScheduleXConstants.java` (backend)

### 3.1 SCHEDULE_X_FIELDS Constant

```typescript
export const SCHEDULE_X_FIELDS = {
  addBacks: {
    depreciationAdjustment: {
      label: "Depreciation Adjustment (Book vs Tax)",
      helpText: "Add back if book depreciation is less than MACRS tax depreciation. Subtract (enter negative) if book depreciation exceeds MACRS. Common for businesses with accelerated tax depreciation (5-year MACRS) vs straight-line book depreciation.",
      example: "Book depreciation $80,000, MACRS depreciation $130,000 → Add-back ($80K - $130K) = -$50,000 (negative means book < tax)",
      applicableEntityTypes: ["C-CORP", "S-CORP", "PARTNERSHIP"],
      autoCalcAvailable: false,
      validationRule: "Must match Form 4562 (Depreciation Schedule) calculation"
    },
    mealsAndEntertainment: {
      label: "Meals & Entertainment (100% Add-Back)",
      helpText: "Federal allows 50% deduction for business meals. Municipal allows 0% deduction. Add back the full federal meals expense (not just the 50% that was deducted).",
      example: "Total meals expense $30,000. Federal deducted $15,000 (50%). Municipal add-back = $30,000 (100%).",
      applicableEntityTypes: ["C-CORP", "S-CORP", "PARTNERSHIP"],
      autoCalcAvailable: true,
      autoCalcFormula: "federalMealsDeduction × 2 (to get back to 100%)",
      validationRule: "Cannot exceed total operating expenses"
    },
    // ... remaining 18 add-back fields
  },
  deductions: {
    interestIncome: {
      label: "Interest Income (Non-Taxable)",
      helpText: "Interest income included in federal taxable income but not subject to municipal tax. Includes interest from savings accounts, bonds, CDs, and other debt instruments.",
      example: "Business earned $5,000 interest from corporate bonds → Deduct $5,000 from municipal income",
      applicableEntityTypes: ["C-CORP", "S-CORP", "PARTNERSHIP"],
      autoCalcAvailable: false,
      validationRule: "Must not exceed total income reported on federal return"
    },
    // ... remaining 6 deduction fields
  }
};
```

---

## 4. Data Retention & Audit Trail

**Retention Period**: 7 years (IRS requirement per IRC § 6001)

| Data | Retention Policy |
|------|------------------|
| `business_tax_return.schedule_x_details` | 7 years (soft delete after 7 years, archive to cold storage) |
| `metadata.attachedDocuments` (supporting docs) | 7 years (S3 lifecycle policy to Glacier after 2 years, delete after 7) |
| Audit logs (schedule X updates) | Permanent (never delete, archive to cold storage after 10 years) |

**Audit Log Entry Example**:
```json
{
  "entityType": "SCHEDULE_X",
  "entityId": "550e8400-e29b-41d4-a716-446655440000",
  "action": "UPDATED",
  "actor": "user-uuid-456",
  "actorRole": "CPA",
  "description": "Updated Schedule X: Added depreciation adjustment $50,000, meals & entertainment $15,000",
  "oldValue": {"addBacks": {"depreciationAdjustment": 0, "mealsAndEntertainment": 0}},
  "newValue": {"addBacks": {"depreciationAdjustment": 50000, "mealsAndEntertainment": 15000}},
  "timestamp": "2024-11-27T19:45:00Z"
}
```

---

## 5. Validation Rules

### 5.1 Field-Level Validation

| Field | Validation Rule |
|-------|----------------|
| `fedTaxableIncome` | Must match Form 1120 Line 30 / Form 1065 Line 22 / Form 1120-S Line 23 (FR-033) |
| All `addBacks.*` numeric fields | Must be `>= 0` (add-backs increase income) |
| All `deductions.*` numeric fields | Must be `>= 0` (deductions decrease income) |
| `otherAddBacksDescription` | **Required** if `otherAddBacks` > 0 |
| `otherDeductionsDescription` | **Required** if `otherDeductions` > 0 |
| `guaranteedPayments` | Only applies if `entityType = "PARTNERSHIP"` (show warning if C-Corp/S-Corp enters value) |

### 5.2 Cross-Field Validation

| Validation | Rule |
|------------|------|
| Variance check (FR-034) | If `adjustedMunicipalIncome` differs from `fedTaxableIncome` by >20%, flag for review with warning message |
| Meals reasonability | If `mealsAndEntertainment` > 5% of total expenses, show warning (may indicate data entry error) |
| Depreciation reasonability | If `depreciationAdjustment` > 50% of `fedTaxableIncome`, show warning (highly unusual) |

---

## 6. Database Schema Changes

**NO SQL MIGRATIONS REQUIRED** ✅

BusinessScheduleXDetails is stored as JSONB field in existing `business_tax_return.schedule_x_details` column. JSONB allows dynamic field expansion without schema changes.

**Backward Compatibility**:
- Old 6-field format: `{ fedTaxableIncome, incomeAndStateTaxes, interestIncome, dividends, capitalGains, other }`
- New 27-field format: `{ fedTaxableIncome, addBacks{...}, deductions{...}, calculatedFields{...}, metadata{...} }`
- Runtime conversion detects old format and migrates on save (Research R2)

**Index Optimization** (optional, for performance):
```sql
-- GIN index on JSONB for faster queries on specific fields
CREATE INDEX idx_schedule_x_federal_income 
    ON business_tax_return USING gin ((schedule_x_details -> 'fedTaxableIncome'));

CREATE INDEX idx_schedule_x_adjusted_income 
    ON business_tax_return USING gin ((schedule_x_details -> 'calculatedFields' -> 'adjustedMunicipalIncome'));
```

---

## 7. Performance Considerations

### 7.1 JSONB Query Performance

**Query**: Retrieve Schedule X for single return
```sql
SELECT schedule_x_details FROM business_tax_return WHERE id = ?;
```
**Performance**: <50ms (indexed primary key lookup)

**Query**: Multi-year comparison (3 years)
```sql
SELECT tax_year, schedule_x_details 
FROM business_tax_return 
WHERE business_id = ? AND tax_year IN (2024, 2023, 2022);
```
**Performance**: 180ms avg, 210ms p95 (Research R4 benchmark)

### 7.2 Frontend Calculation Performance

**Simple Auto-Calculations** (TypeScript):
- Meals 50%→100%: <10ms
- 5% Rule: <10ms
- Related-party excess: <10ms

**Complex Auto-Calculations** (Backend API):
- Charitable 10% limit: 320ms (includes DB query for prior year carryforward)
- Depreciation adjustment: 450ms (Form 4562 parsing)

---

## Next Steps

1. ✅ Data model complete (27-field BusinessScheduleXDetails)
2. ✅ AI extraction result structure (ScheduleXExtractionResult with bounding boxes)
3. ✅ Field definitions and help text (SCHEDULE_X_FIELDS constant)
4. ⏳ Generate API contracts (OpenAPI 3.0) → `/contracts/`
5. ⏳ Create quickstart.md (developer guide)
6. ⏳ Re-evaluate Constitution Check post-design
