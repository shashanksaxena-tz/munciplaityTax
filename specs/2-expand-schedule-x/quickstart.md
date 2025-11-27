# Quickstart Guide: Comprehensive Business Schedule X Reconciliation

**Feature**: Comprehensive Business Schedule X Reconciliation (25+ Fields)  
**Phase**: Phase 1 (Design & Contracts)  
**Date**: 2025-11-27

---

## Overview

This quickstart guide helps developers:
- Set up local development environment for Schedule X expansion
- Update existing BusinessScheduleXDetails from 6 fields to 27 fields
- Test AI extraction from Form 1120 Schedule M-1 and Form 4562
- Use auto-calculation helpers (meals, 5% Rule, charitable contributions)
- Query multi-year comparison data
- Generate Form 27 PDF with expanded Schedule X

**Prerequisites**:
- Docker Desktop (for PostgreSQL 16, Redis 7)
- Java 21 JDK
- Maven 3.9+
- Node.js 20+ (for frontend development)
- curl or Postman (for API testing)
- Sample tax documents: Form 1120 Schedule M-1, Form 4562 (depreciation schedule)

---

## 1. Environment Setup

### 1.1 Start Infrastructure

```bash
# Navigate to project root
cd /home/runner/work/munciplaityTax/munciplaityTax

# Start all containers
docker-compose up -d

# Verify services running
docker-compose ps

# Expected output:
# tax-engine-service   HEALTHY   8083
# extraction-service   HEALTHY   8084
# pdf-service          HEALTHY   8085
# postgresql           HEALTHY   5432
# redis                HEALTHY   6379
```

### 1.2 Build Backend Services

```bash
# Build tax-engine-service (contains expanded Schedule X logic)
cd backend/tax-engine-service
mvn clean install -DskipTests

# Build extraction-service (updated for 27-field extraction)
cd ../extraction-service
mvn clean install -DskipTests

# Build pdf-service (updated Form 27 PDF generation)
cd ../pdf-service
mvn clean install -DskipTests

# Restart services
cd ../..
docker-compose restart tax-engine-service extraction-service pdf-service
```

### 1.3 Verify Database Schema

```bash
# Connect to PostgreSQL
psql -h localhost -U postgres -d munitax

# Set schema
SET search_path TO dublin;

# Verify business_tax_return table exists (no migration needed - JSONB expansion)
\d business_tax_return

# Expected: schedule_x_details column is type 'jsonb'
# Column        | Type          
# --------------+---------------
# id            | uuid          
# business_id   | uuid          
# tax_year      | integer       
# schedule_x_details | jsonb    <- Expands from 6 to 27 fields (no schema change)
```

### 1.4 Obtain JWT Token

```bash
# Login as test business user
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test-business@acmecorp.com",
    "password": "Test1234!"
  }' | jq -r '.token'

# Save token to environment variable
export JWT_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

## 2. API Examples: Schedule X Operations

### 2.1 Get Schedule X (6-Field Old Format)

```bash
# Retrieve existing Schedule X (old format with 6 fields)
curl -X GET "http://localhost:8080/api/net-profits/550e8400-e29b-41d4-a716-446655440000/schedule-x" \
  -H "Authorization: Bearer $JWT_TOKEN" | jq

# Expected response (OLD FORMAT - pre-expansion):
# {
#   "fedTaxableIncome": 500000,
#   "incomeAndStateTaxes": 10000,
#   "interestIncome": 5000,
#   "dividends": 3000,
#   "capitalGains": 2000,
#   "other": 0,
#   "adjustedMunicipalIncome": 500000
# }
```

### 2.2 Update Schedule X (New 27-Field Format)

```bash
# Update Schedule X with new fields (depreciation, meals & entertainment)
curl -X PUT "http://localhost:8080/api/net-profits/550e8400-e29b-41d4-a716-446655440000/schedule-x" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "fedTaxableIncome": 500000,
    "addBacks": {
      "depreciationAdjustment": 50000,
      "amortizationAdjustment": 0,
      "incomeAndStateTaxes": 10000,
      "guaranteedPayments": 0,
      "mealsAndEntertainment": 15000,
      "relatedPartyExcess": 0,
      "penaltiesAndFines": 0,
      "politicalContributions": 0,
      "officerLifeInsurance": 0,
      "capitalLossExcess": 0,
      "federalTaxRefunds": 0,
      "expensesOnIntangibleIncome": 0,
      "section179Excess": 0,
      "bonusDepreciation": 0,
      "badDebtReserveIncrease": 0,
      "charitableContributionExcess": 0,
      "domesticProductionActivities": 0,
      "stockCompensationAdjustment": 0,
      "inventoryMethodChange": 0,
      "otherAddBacks": 0
    },
    "deductions": {
      "interestIncome": 0,
      "dividends": 0,
      "capitalGains": 0,
      "section179Recapture": 0,
      "municipalBondInterest": 0,
      "depletionDifference": 0,
      "otherDeductions": 0
    }
  }' | jq

# Expected response (NEW FORMAT with calculated fields):
# {
#   "fedTaxableIncome": 500000,
#   "addBacks": { ... },
#   "deductions": { ... },
#   "calculatedFields": {
#     "totalAddBacks": 75000,
#     "totalDeductions": 0,
#     "adjustedMunicipalIncome": 575000
#   },
#   "metadata": {
#     "lastModified": "2024-11-27T20:15:00Z",
#     "autoCalculatedFields": [],
#     "manualOverrides": [],
#     "attachedDocuments": []
#   }
# }
```

### 2.3 Auto-Calculate Meals & Entertainment

```bash
# Use auto-calculation helper for meals (50% federal → 100% municipal add-back)
curl -X POST "http://localhost:8080/api/schedule-x/auto-calculate" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "field": "mealsAndEntertainment",
    "inputs": {
      "federalMealsDeduction": 15000
    }
  }' | jq

# Expected response:
# {
#   "calculatedValue": 30000,
#   "explanation": "Federal allows 50% deduction for business meals ($15,000). Municipal allows 0% deduction. Add back full expense: $15,000 × 2 = $30,000.",
#   "metadata": {
#     "formula": "federalMealsDeduction × 2"
#   }
# }
```

### 2.4 Auto-Calculate 5% Rule (Intangible Income Expenses)

```bash
# Calculate 5% Rule expense add-back for intangible income
curl -X POST "http://localhost:8080/api/schedule-x/auto-calculate" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "field": "expensesOnIntangibleIncome",
    "inputs": {
      "interestIncome": 20000,
      "dividendIncome": 15000,
      "capitalGains": 0
    }
  }' | jq

# Expected response:
# {
#   "calculatedValue": 1750,
#   "explanation": "5% Rule: Add back expenses incurred to earn non-taxable intangible income. Total intangible income: $20,000 (interest) + $15,000 (dividends) = $35,000. Add-back: $35,000 × 5% = $1,750.",
#   "metadata": {
#     "formula": "(interestIncome + dividendIncome + capitalGains) × 0.05",
#     "allowManualOverride": true
#   }
# }
```

### 2.5 Auto-Calculate Charitable Contribution 10% Limit

```bash
# Calculate charitable contribution deduction with 10% limit and carryforward
curl -X POST "http://localhost:8080/api/schedule-x/auto-calculate" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "field": "charitableContributionExcess",
    "inputs": {
      "businessId": "550e8400-e29b-41d4-a716-446655440000",
      "taxYear": 2024,
      "contributionsPaid": 80000,
      "taxableIncomeBeforeContributions": 600000
    }
  }' | jq

# Expected response:
# {
#   "calculatedValue": 20000,
#   "explanation": "10% limit on $600,000 taxable income = $60,000 maximum deduction. Contributions paid: $80,000. Prior year carryforward: $0. Total available: $80,000. Deduct $60,000 this year, carry forward $20,000 to 2025.",
#   "metadata": {
#     "currentYearDeduction": 60000,
#     "carryforward": 20000,
#     "priorYearCarryforward": 0
#   }
# }
```

### 2.6 Import Schedule X from Federal Return (AI Extraction)

```bash
# Upload Form 1120 PDF and extract Schedule X fields
curl -X POST "http://localhost:8080/api/schedule-x/import-from-federal" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -F "returnId=550e8400-e29b-41d4-a716-446655440000" \
  -F "federalFormPdf=@/path/to/form_1120_schedule_m1.pdf" \
  -F "depreciationSchedulePdf=@/path/to/form_4562.pdf" | jq

# Expected response (async processing):
# {
#   "extractionId": "650e8400-e29b-41d4-a716-446655440001",
#   "status": "IN_PROGRESS",
#   "message": "AI extraction initiated. Processing Form 1120 Schedule M-1 and Form 4562...",
#   "estimatedCompletionTime": "2024-11-27T20:20:00Z"
# }

# Poll extraction status
sleep 10
curl -X GET "http://localhost:8080/api/extraction/schedule-x/650e8400-e29b-41d4-a716-446655440001" \
  -H "Authorization: Bearer $JWT_TOKEN" | jq

# Expected response (completed):
# {
#   "extractionId": "650e8400-e29b-41d4-a716-446655440001",
#   "status": "COMPLETED",
#   "extractedAt": "2024-11-27T20:19:45Z",
#   "fields": {
#     "depreciationAdjustment": {
#       "value": 50000,
#       "confidence": 0.95,
#       "boundingBox": {
#         "page": 2,
#         "vertices": [
#           {"x": 245, "y": 189},
#           {"x": 298, "y": 189},
#           {"x": 298, "y": 205},
#           {"x": 245, "y": 205}
#         ]
#       },
#       "sourceDocument": "Form 4562, Part II Line 17 Column (h)"
#     },
#     "mealsAndEntertainment": {
#       "value": 30000,
#       "confidence": 0.88,
#       "boundingBox": { ... },
#       "sourceDocument": "Form 1120, Line 20 (Other Deductions - Detail)"
#     },
#     "incomeAndStateTaxes": {
#       "value": 10000,
#       "confidence": 0.92,
#       "boundingBox": { ... },
#       "sourceDocument": "Form 1120, Line 17 (Taxes and Licenses)"
#     }
#   },
#   "averageConfidence": 0.92,
#   "fieldsExtracted": 27
# }
```

### 2.7 Multi-Year Comparison (3 Years)

```bash
# Retrieve Schedule X for 2024, 2023, 2022 (side-by-side comparison)
curl -X GET "http://localhost:8080/api/schedule-x/multi-year-comparison?businessId=550e8400-e29b-41d4-a716-446655440000&years=2024,2023,2022" \
  -H "Authorization: Bearer $JWT_TOKEN" | jq

# Expected response:
# {
#   "years": [2024, 2023, 2022],
#   "data": [
#     {
#       "taxYear": 2024,
#       "fedTaxableIncome": 575000,
#       "calculatedFields": {
#         "totalAddBacks": 75000,
#         "totalDeductions": 0,
#         "adjustedMunicipalIncome": 575000
#       },
#       "addBacks": {
#         "depreciationAdjustment": 50000,
#         "mealsAndEntertainment": 15000,
#         "incomeAndStateTaxes": 10000
#       }
#     },
#     {
#       "taxYear": 2023,
#       "fedTaxableIncome": 550000,
#       "calculatedFields": {
#         "totalAddBacks": 60000,
#         "totalDeductions": 0,
#         "adjustedMunicipalIncome": 550000
#       },
#       "addBacks": {
#         "depreciationAdjustment": 45000,
#         "mealsAndEntertainment": 10000,
#         "incomeAndStateTaxes": 5000
#       }
#     },
#     {
#       "taxYear": 2022,
#       "fedTaxableIncome": 525000,
#       "calculatedFields": {
#         "totalAddBacks": 55000,
#         "totalDeductions": 0,
#         "adjustedMunicipalIncome": 525000
#       },
#       "addBacks": {
#         "depreciationAdjustment": 40000,
#         "mealsAndEntertainment": 10000,
#         "incomeAndStateTaxes": 5000
#       }
#     }
#   ],
#   "responseTime": 178
# }
```

### 2.8 Attach Supporting Documentation

```bash
# Upload depreciation schedule to support depreciationAdjustment field
curl -X POST "http://localhost:8080/api/schedule-x/550e8400-e29b-41d4-a716-446655440000/attachments" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -F "file=@/path/to/depreciation_schedule.xlsx" \
  -F "fieldName=depreciationAdjustment" | jq

# Expected response:
# {
#   "attachmentId": "750e8400-e29b-41d4-a716-446655440002",
#   "fileName": "depreciation_schedule.xlsx",
#   "fileUrl": "s3://munitax-docs/dublin/return-550e8400/depreciation_schedule.xlsx",
#   "fieldName": "depreciationAdjustment",
#   "uploadedAt": "2024-11-27T20:25:00Z",
#   "uploadedBy": "user-uuid-456"
# }
```

---

## 3. Database Queries

### 3.1 View Schedule X Details (JSONB Query)

```sql
-- Connect to PostgreSQL
psql -h localhost -U postgres -d munitax

-- Set schema
SET search_path TO dublin;

-- Query Schedule X for business
SELECT 
    tax_year,
    schedule_x_details->>'fedTaxableIncome' as federal_income,
    schedule_x_details->'calculatedFields'->>'totalAddBacks' as total_addbacks,
    schedule_x_details->'calculatedFields'->>'totalDeductions' as total_deductions,
    schedule_x_details->'calculatedFields'->>'adjustedMunicipalIncome' as adjusted_income
FROM business_tax_return
WHERE business_id = '550e8400-e29b-41d4-a716-446655440000'
  AND tax_year = 2024;

-- Expected output:
-- tax_year | federal_income | total_addbacks | total_deductions | adjusted_income
-- ---------+----------------+----------------+------------------+-----------------
--   2024   |    500000      |    75000       |        0         |     575000
```

### 3.2 Find All Returns with Depreciation Adjustments > $50K

```sql
-- Query returns with significant depreciation adjustments
SELECT 
    b.name as business_name,
    r.tax_year,
    (r.schedule_x_details->'addBacks'->>'depreciationAdjustment')::numeric as depreciation_adj
FROM business_tax_return r
JOIN businesses b ON b.id = r.business_id
WHERE (r.schedule_x_details->'addBacks'->>'depreciationAdjustment')::numeric > 50000
ORDER BY depreciation_adj DESC
LIMIT 10;
```

### 3.3 Find Returns with >20% Variance (Flag for Review)

```sql
-- Query returns where adjusted municipal income differs from federal by >20%
SELECT 
    b.name,
    r.tax_year,
    (r.schedule_x_details->>'fedTaxableIncome')::numeric as fed_income,
    (r.schedule_x_details->'calculatedFields'->>'adjustedMunicipalIncome')::numeric as muni_income,
    ROUND(
        (ABS((r.schedule_x_details->'calculatedFields'->>'adjustedMunicipalIncome')::numeric - 
             (r.schedule_x_details->>'fedTaxableIncome')::numeric) / 
         NULLIF((r.schedule_x_details->>'fedTaxableIncome')::numeric, 0)) * 100, 
        2
    ) as variance_pct
FROM business_tax_return r
JOIN businesses b ON b.id = r.business_id
WHERE ABS(
    (r.schedule_x_details->'calculatedFields'->>'adjustedMunicipalIncome')::numeric - 
    (r.schedule_x_details->>'fedTaxableIncome')::numeric
) / NULLIF((r.schedule_x_details->>'fedTaxableIncome')::numeric, 0) > 0.20
ORDER BY variance_pct DESC;
```

---

## 4. Running Tests

### 4.1 Unit Tests

```bash
# Run unit tests for Schedule X calculation service
cd backend/tax-engine-service
mvn test -Dtest=ScheduleXCalculationServiceTest

# Test cases:
# - testCalculateTotalAddBacks (sum of 20 fields)
# - testCalculateTotalDeductions (sum of 7 fields)
# - testCalculateAdjustedMunicipalIncome (federal + addbacks - deductions)
# - testBackwardCompatibility (old 6-field format still calculates correctly)
# - testAutoCalculationMeals (50% → 100% conversion)
# - testAutoCalculation5PercentRule (intangible income × 0.05)
```

### 4.2 Integration Tests

```bash
# Run integration tests (all 5 user stories)
mvn test -Dtest=ScheduleXIntegrationTest

# Test scenarios:
# 1. US-1: C-Corp with depreciation, meals, state taxes → Adjusted $575K
# 2. US-2: Partnership with guaranteed payments, intangible income, 5% Rule → Adjusted $316.75K
# 3. US-3: S-Corp with related-party rent adjustment → Adjusted $402.5K
# 4. US-4: C-Corp with charitable contributions 10% limit → No add-back
# 5. US-5: Manufacturing with DPAD → Add-back $25K

# Run AI extraction tests
mvn test -Dtest=ScheduleXExtractionIntegrationTest

# Test cases:
# - Extract 27 fields from sample Form 1120 Schedule M-1
# - Verify confidence scores >= 0.85 (average)
# - Verify bounding box coordinates returned
# - Verify extraction accuracy >= 90% (25/27 fields correct)
```

### 4.3 Frontend Tests

```bash
# Run component tests (Vitest)
cd ../../
npm test -- --run schedule-x

# Test components:
# - ScheduleXAccordion (collapsible add-backs/deductions sections)
# - ScheduleXFieldInput (currency formatting, help tooltips)
# - ScheduleXAutoCalcButton (auto-calculation triggers)
# - ScheduleXConfidenceScore (clickable badges with PDF viewer)
# - ScheduleXMultiYearComparison (3-year side-by-side table)

# Run E2E tests (Playwright)
npx playwright test specs/schedule-x-expansion.spec.ts

# E2E workflow:
# 1. Login as business user
# 2. Upload Form 1120 Schedule M-1 PDF
# 3. Verify AI extraction of 27 fields
# 4. Override 2 extracted values (test manual override)
# 5. Use auto-calculation for meals & entertainment
# 6. Save Schedule X
# 7. Verify calculated totals (Total Add-Backs, Adjusted Municipal Income)
# 8. Generate Form 27 PDF
# 9. Verify PDF contains all 27 Schedule X fields (3 pages)
```

---

## 5. AI Extraction Testing

### 5.1 Test with Sample Form 1120 Schedule M-1

```bash
# Test extraction service directly
cd backend/extraction-service

# Start extraction service (if not running)
mvn spring-boot:run

# Upload sample Form 1120 Schedule M-1 PDF
curl -X POST http://localhost:8084/api/extraction/schedule-x \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -F "file=@src/test/resources/test-pdfs/form-1120-schedule-m1-sample-1.pdf" \
  -F "entityType=C-Corp" | jq

# Expected response:
# {
#   "formType": "Form 1120 Schedule M-1",
#   "extractedFields": 27,
#   "averageConfidence": 0.92,
#   "fields": {
#     "depreciationAdjustment": { "value": 50000, "confidence": 0.95 },
#     "mealsAndEntertainment": { "value": 30000, "confidence": 0.88 },
#     // ... 25 more fields
#   },
#   "extractionTime": 8200
# }
```

### 5.2 Test Bounding Box Coordinates

```bash
# Verify bounding boxes returned in extraction result
curl -X POST http://localhost:8084/api/extraction/schedule-x \
  -F "file=@form_1120_schedule_m1.pdf" | jq '.fields.depreciationAdjustment.boundingBox'

# Expected output:
# {
#   "page": 2,
#   "vertices": [
#     {"x": 245, "y": 189},
#     {"x": 298, "y": 189},
#     {"x": 298, "y": 205},
#     {"x": 245, "y": 205}
#   ]
# }

# Test PDF viewer integration (frontend)
# 1. Click on confidence badge (e.g., "95% confident")
# 2. PDF viewer modal opens, highlights bounding box region
# 3. Verify highlighted region matches extracted field location
```

---

## 6. Form 27 PDF Generation

### 6.1 Generate PDF with Expanded Schedule X

```bash
# Generate Form 27 PDF with all 27 Schedule X fields
curl -X POST "http://localhost:8080/api/pdf/form-27/550e8400-e29b-41d4-a716-446655440000" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  --output form_27_2024.pdf

# Expected output:
# PDF file saved: form_27_2024.pdf
# File size: ~45 KB (3 pages)

# Open PDF to verify layout
open form_27_2024.pdf  # macOS
xdg-open form_27_2024.pdf  # Linux
start form_27_2024.pdf  # Windows

# Verify PDF structure:
# - Page 1: Form 27 summary (Federal $500K, Add-backs $75K, Adjusted $575K)
# - Page 2: Schedule X add-backs detail (fields 1-13)
# - Page 3: Schedule X add-backs continued (14-20) + deductions (21-27)
# - Font size: 10pt (readable)
# - Totals: Bold 11pt
```

### 6.2 Test Multi-Page PDF Layout

```bash
# Generate PDF with all 27 fields populated (stress test)
curl -X POST "http://localhost:8080/api/pdf/form-27/test-return-all-fields" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  --output form_27_all_fields.pdf

# Verify all fields present:
# - Page 2: 13 add-back fields (depreciationAdjustment through expensesOnIntangibleIncome)
# - Page 3: 7 add-back fields (section179Excess through otherAddBacks) + 7 deduction fields
# - Page 3: TOTAL ADD-BACKS, TOTAL DEDUCTIONS, ADJUSTED MUNICIPAL INCOME (bold)

# Measure PDF generation time
time curl -X POST "http://localhost:8080/api/pdf/form-27/550e8400-e29b-41d4-a716-446655440000" \
  --output /dev/null

# Expected: <3 seconds (including 27-field Schedule X rendering)
```

---

## 7. Performance Testing

### 7.1 Multi-Year Comparison Load Test

```bash
# Load test multi-year comparison endpoint (100 concurrent requests)
ab -n 100 -c 10 \
   -H "Authorization: Bearer $JWT_TOKEN" \
   "http://localhost:8080/api/schedule-x/multi-year-comparison?businessId=550e8400-e29b-41d4-a716-446655440000&years=2024,2023,2022"

# Expected results (from Research R4 benchmark):
# Requests per second: 55 [#/sec]
# Time per request: 180ms [avg]
# 95th percentile: <210ms
# Success criteria: <2 seconds (FR-038) ✓
```

### 7.2 AI Extraction Performance

```bash
# Measure AI extraction time for 27 fields
time curl -X POST http://localhost:8084/api/extraction/schedule-x \
  -F "file=@form_1120_schedule_m1.pdf" \
  --output /dev/null

# Expected results (from Research R1 benchmark):
# Extraction time: 8.2 seconds (avg)
# Success criteria: <10 seconds (FR-039, Success Criteria) ✓
```

### 7.3 Auto-Calculation Performance

```bash
# Benchmark frontend auto-calculations (TypeScript)
# Open browser console at http://localhost:3000

# Test meals 50%→100% calculation
console.time('autoCalcMeals');
for (let i = 0; i < 1000; i++) {
  calculateMealsAddBack(30000);
}
console.timeEnd('autoCalcMeals');
// Expected: <100ms for 1000 calculations (<0.1ms per calc)

# Test 5% Rule calculation
console.time('autoCalc5PercentRule');
for (let i = 0; i < 1000; i++) {
  calculate5PercentRule(20000, 15000, 0);
}
console.timeEnd('autoCalc5PercentRule');
// Expected: <100ms for 1000 calculations
```

---

## 8. Troubleshooting

### 8.1 Old Format Not Converting

**Problem**: Loading old 6-field Schedule X returns error "Cannot parse addBacks object"

**Solution**:
```bash
# Check conversion logic in BusinessScheduleXService.java
# Verify detection logic:
if (json.has("interestIncome") && !json.has("deductions")) {
    // OLD FORMAT: should trigger conversion
}

# Test conversion manually
psql -h localhost -U postgres -d munitax
SET search_path TO dublin;

-- View old format
SELECT schedule_x_details FROM business_tax_return WHERE id = '...';

-- Trigger conversion by updating return (conversion happens on save)
-- Load return in UI, save without changes → auto-converts to new format
```

### 8.2 AI Extraction Low Confidence (<0.85)

**Problem**: Extraction confidence scores < 0.85 for multiple fields

**Possible Causes**:
1. PDF is scanned image (not native PDF with text layer)
2. Handwritten entries on form
3. Form has non-standard layout (state-specific variant)

**Solutions**:
```bash
# Re-upload PDF with OCR pre-processing
# If scanned PDF, use OCR tool first:
tesseract form_1120_scanned.pdf form_1120_ocr pdf

# Upload OCR'd PDF
curl -X POST http://localhost:8080/api/schedule-x/import-from-federal \
  -F "federalFormPdf=@form_1120_ocr.pdf"

# If handwritten entries, manually override:
# 1. View extracted fields in UI
# 2. Click confidence badge (e.g., "65% confident")
# 3. Compare highlighted PDF region with entered value
# 4. Override incorrect values
```

### 8.3 Multi-Year Comparison Slow (>2 seconds)

**Problem**: Multi-year comparison query taking >2 seconds

**Diagnosis**:
```sql
-- Check query execution plan
EXPLAIN ANALYZE
SELECT tax_year, schedule_x_details 
FROM business_tax_return 
WHERE business_id = '550e8400-e29b-41d4-a716-446655440000'
  AND tax_year IN (2024, 2023, 2022);

-- If missing index, create:
CREATE INDEX idx_business_tax_year 
    ON business_tax_return(business_id, tax_year);

-- If JSONB query slow, create GIN index:
CREATE INDEX idx_schedule_x_details 
    ON business_tax_return USING gin(schedule_x_details);
```

### 8.4 Form 27 PDF Missing Schedule X Fields

**Problem**: Generated PDF missing some Schedule X fields or showing truncated values

**Diagnosis**:
```bash
# Check PDF generation logs
docker-compose logs pdf-service | grep "Schedule X"

# Verify all fields passed to PDF generator
curl -X GET "http://localhost:8080/api/net-profits/550e8400-e29b-41d4-a716-446655440000/schedule-x" \
  -H "Authorization: Bearer $JWT_TOKEN" | jq '.addBacks'

# Test PDF generation with sample data
mvn test -Dtest=Form27GeneratorTest

# If font issues (text too large), adjust font size in Form27Generator.java:
new Font(Font.FontFamily.HELVETICA, 9) // Reduce from 10pt to 9pt
```

---

## 9. Sample Data for Testing

### 9.1 User Story 1: C-Corp with Depreciation, Meals, State Taxes

```json
{
  "fedTaxableIncome": 500000,
  "addBacks": {
    "depreciationAdjustment": 50000,
    "incomeAndStateTaxes": 10000,
    "mealsAndEntertainment": 15000
  },
  "deductions": {},
  "expectedAdjustedIncome": 575000
}
```

### 9.2 User Story 2: Partnership with Guaranteed Payments, Intangible Income, 5% Rule

```json
{
  "fedTaxableIncome": 300000,
  "addBacks": {
    "guaranteedPayments": 50000,
    "expensesOnIntangibleIncome": 1750
  },
  "deductions": {
    "interestIncome": 20000,
    "dividends": 15000
  },
  "expectedAdjustedIncome": 316750
}
```

### 9.3 User Story 3: S-Corp with Related-Party Transactions

```json
{
  "fedTaxableIncome": 400000,
  "addBacks": {
    "relatedPartyExcess": 2500
  },
  "deductions": {},
  "expectedAdjustedIncome": 402500
}
```

---

## 10. Next Steps

1. ✅ **Development Environment**: Local setup complete
2. ✅ **API Testing**: Updated Schedule X, tested auto-calculations, AI extraction
3. ✅ **Database**: JSONB expansion tested, backward compatibility verified
4. ✅ **Frontend**: Components integrated (ScheduleXAccordion, auto-calc buttons)
5. ⏳ **Production Deployment**: Update production tax-engine-service, extraction-service, pdf-service
6. ⏳ **CPA Training**: Schedule training sessions for new Schedule X UI (27 fields)

---

## 11. Additional Resources

- **API Documentation**: OpenAPI specs in `/specs/2-expand-schedule-x/contracts/`
- **Data Model**: See `/specs/2-expand-schedule-x/data-model.md`
- **Research Decisions**: See `/specs/2-expand-schedule-x/research.md`
- **Implementation Plan**: See `/specs/2-expand-schedule-x/plan.md`
- **Constitution**: See `/.specify/memory/constitution.md`

---

## 12. Support

For questions or issues:
- GitHub Issues: https://github.com/munitax/munitax/issues
- Slack: #schedule-x-expansion
- Email: engineering@munitax.com

**Quickstart Complete!** You're ready to develop and test the expanded Schedule X reconciliation system (27 fields).
