# Quickstart Guide: Enhanced Penalty & Interest Calculation

**Feature**: Enhanced Penalty & Interest Calculation  
**Phase**: Phase 1 (Design & Contracts)  
**Date**: 2024-11-28

---

## Overview

This quickstart guide helps developers:
- Calculate late filing and late payment penalties
- Evaluate estimated tax safe harbor rules
- Calculate quarterly compounding interest
- Request penalty abatement
- Track payment allocations
- Run integration tests

**Prerequisites**:
- Docker Desktop (for PostgreSQL 16, Redis 7)
- Java 21 JDK
- Maven 3.9+
- Node.js 20+ (for frontend development)
- curl or Postman (for API testing)

---

## 1. Environment Setup

### 1.1 Start Infrastructure

```bash
# Navigate to project root
cd /home/runner/work/munciplaityTax/munciplaityTax

# Start all containers (PostgreSQL, Redis, Eureka, Gateway, Services)
docker-compose up -d

# Verify all services running
docker-compose ps

# Expected output:
# discovery-service    HEALTHY   8761
# gateway-service      HEALTHY   8080
# auth-service         HEALTHY   8081
# tax-engine-service   HEALTHY   8083
# extraction-service   HEALTHY   8084
# pdf-service          HEALTHY   8085
# postgresql           HEALTHY   5432
# redis                HEALTHY   6379
```

### 1.2 Database Setup

```bash
# Connect to PostgreSQL (password: postgres)
psql -h localhost -U postgres -d munitax

# Verify tenant schemas exist
\dn

# Expected output:
# dublin
# columbus

# Verify penalty tables exist (from Flyway migrations V1.30-V1.39)
\dt dublin.*

# Expected tables:
# dublin.penalties
# dublin.estimated_tax_penalties
# dublin.quarterly_underpayments
# dublin.interests
# dublin.quarterly_interests
# dublin.penalty_abatements
# dublin.payment_allocations
# dublin.penalty_audit_logs
```

### 1.3 Build Backend Services

```bash
# Build tax-engine-service (contains penalty/interest logic)
cd backend/tax-engine-service
mvn clean install -DskipTests

# Restart service to pick up new migrations
docker-compose restart tax-engine-service

# Check logs for startup success
docker-compose logs -f tax-engine-service

# Expected log entry:
# "Started TaxEngineServiceApplication in 12.345 seconds"
```

### 1.4 Obtain JWT Token

```bash
# Register test individual taxpayer
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.taxpayer@example.com",
    "password": "SecurePass123!",
    "firstName": "John",
    "lastName": "Taxpayer",
    "userType": "INDIVIDUAL",
    "tenantId": "dublin"
  }'

# Login to obtain JWT token
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.taxpayer@example.com",
    "password": "SecurePass123!"
  }'

# Expected response:
# {
#   "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
#   "expiresIn": 86400,
#   "tenantId": "dublin",
#   "userId": "550e8400-e29b-41d4-a716-446655440000"
# }

# Save token to environment variable
export JWT_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

## 2. API Usage Examples

### 2.1 Calculate Late Filing Penalty

Calculate penalty for tax return filed 3 months late:

```bash
curl -X POST http://localhost:8080/api/v1/penalties/calculate/late-filing \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "returnId": "550e8400-e29b-41d4-a716-446655440001",
    "taxDueDate": "2024-04-15",
    "actualFilingDate": "2024-07-15",
    "unpaidTaxAmount": 10000.00
  }'

# Expected response:
# {
#   "penaltyId": "650e8400-e29b-41d4-a716-446655440002",
#   "penaltyType": "LATE_FILING",
#   "monthsLate": 3,
#   "penaltyRate": 0.05,
#   "penaltyAmount": 1500.00,
#   "maximumPenalty": 2500.00,
#   "calculation": {
#     "formula": "$10,000 × 5% × 3 months",
#     "breakdown": [
#       { "month": 1, "rate": "5%", "amount": 500.00 },
#       { "month": 2, "rate": "5%", "amount": 500.00 },
#       { "month": 3, "rate": "5%", "amount": 500.00 }
#     ]
#   },
#   "message": "Filed 3 months late → 5% × 3 = 15% penalty on $10,000 unpaid tax = $1,500"
# }
```

### 2.2 Calculate Late Payment Penalty

Calculate penalty for tax paid 4 months late:

```bash
curl -X POST http://localhost:8080/api/v1/penalties/calculate/late-payment \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "returnId": "550e8400-e29b-41d4-a716-446655440001",
    "taxDueDate": "2024-04-15",
    "paymentDate": "2024-08-15",
    "unpaidTaxAmount": 5000.00
  }'

# Expected response:
# {
#   "penaltyId": "750e8400-e29b-41d4-a716-446655440003",
#   "penaltyType": "LATE_PAYMENT",
#   "monthsLate": 4,
#   "penaltyRate": 0.01,
#   "penaltyAmount": 200.00,
#   "maximumPenalty": 1250.00,
#   "calculation": {
#     "formula": "$5,000 × 1% × 4 months",
#     "breakdown": [
#       { "month": 1, "rate": "1%", "amount": 50.00 },
#       { "month": 2, "rate": "1%", "amount": 50.00 },
#       { "month": 3, "rate": "1%", "amount": 50.00 },
#       { "month": 4, "rate": "1%", "amount": 50.00 }
#     ]
#   },
#   "message": "Paid 4 months late → 1% × 4 = 4% penalty on $5,000 = $200"
# }
```

### 2.3 Calculate Combined Penalty (with Cap)

Calculate combined late filing and late payment penalty:

```bash
curl -X POST http://localhost:8080/api/v1/penalties/calculate/combined \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "returnId": "550e8400-e29b-41d4-a716-446655440001",
    "taxDueDate": "2024-04-15",
    "actualFilingDate": "2024-06-15",
    "paymentDate": "2024-06-15",
    "unpaidTaxAmount": 10000.00
  }'

# Expected response:
# {
#   "penalties": [
#     {
#       "penaltyId": "850e8400-e29b-41d4-a716-446655440004",
#       "penaltyType": "LATE_FILING",
#       "monthsLate": 2,
#       "penaltyRate": 0.05,
#       "penaltyAmount": 1000.00,
#       "cappedAmount": 1000.00
#     },
#     {
#       "penaltyId": "950e8400-e29b-41d4-a716-446655440005",
#       "penaltyType": "LATE_PAYMENT",
#       "monthsLate": 2,
#       "penaltyRate": 0.01,
#       "penaltyAmount": 200.00,
#       "cappedAmount": 0.00,
#       "note": "Absorbed by late filing penalty (combined cap = 5%/month)"
#     }
#   ],
#   "totalPenalty": 1000.00,
#   "combinedCapApplied": true,
#   "calculation": {
#     "withoutCap": {
#       "lateFiling": 1000.00,
#       "latePayment": 200.00,
#       "total": 1200.00
#     },
#     "withCap": {
#       "combined": 1000.00,
#       "cappedAt": "5% per month (not 6%)"
#     }
#   },
#   "message": "Combined penalty capped at 5% × 2 = 10% = $1,000 (not 12%)"
# }
```

### 2.4 Evaluate Estimated Tax Safe Harbor

Check if estimated tax safe harbor rules are met:

```bash
curl -X POST http://localhost:8080/api/v1/penalties/estimated-tax/safe-harbor \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "returnId": "550e8400-e29b-41d4-a716-446655440001",
    "taxYear": 2024,
    "annualTaxLiability": 20000.00,
    "priorYearTaxLiability": 15000.00,
    "agi": 80000.00,
    "estimatedPayments": {
      "q1": 4000.00,
      "q2": 4000.00,
      "q3": 4000.00,
      "q4": 4000.00
    }
  }'

# Expected response:
# {
#   "estimatedPenaltyId": "a50e8400-e29b-41d4-a716-446655440006",
#   "safeHarbor1": {
#     "met": false,
#     "required": 18000.00,
#     "paid": 16000.00,
#     "calculation": "90% × $20,000 current year tax = $18,000"
#   },
#   "safeHarbor2": {
#     "met": true,
#     "required": 15000.00,
#     "paid": 16000.00,
#     "calculation": "100% × $15,000 prior year tax = $15,000 (AGI < $150K)"
#   },
#   "penaltyApplies": false,
#   "totalPenalty": 0.00,
#   "message": "✓ Safe Harbor Met: Paid 100% of prior year tax ($15,000) → No underpayment penalty"
# }
```

### 2.5 Calculate Underpayment Penalty (Failed Safe Harbor)

Calculate quarterly underpayment penalty when safe harbor failed:

```bash
curl -X POST http://localhost:8080/api/v1/penalties/estimated-tax/underpayment \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "returnId": "550e8400-e29b-41d4-a716-446655440001",
    "taxYear": 2024,
    "annualTaxLiability": 20000.00,
    "priorYearTaxLiability": 12000.00,
    "agi": 180000.00,
    "estimatedPayments": {
      "q1": 2000.00,
      "q2": 3000.00,
      "q3": 5000.00,
      "q4": 6000.00
    },
    "filingDate": "2025-04-15",
    "underpaymentPenaltyRate": 0.05
  }'

# Expected response:
# {
#   "estimatedPenaltyId": "b50e8400-e29b-41d4-a716-446655440007",
#   "safeHarbor1Met": false,
#   "safeHarbor2Met": false,
#   "quarters": [
#     {
#       "quarter": "Q1",
#       "dueDate": "2024-04-15",
#       "requiredPayment": 5000.00,
#       "actualPayment": 2000.00,
#       "underpayment": 3000.00,
#       "quartersUnpaid": 4,
#       "penaltyAmount": 150.00,
#       "calculation": "$3,000 × 1.25% × 4 quarters"
#     },
#     {
#       "quarter": "Q2",
#       "dueDate": "2024-06-15",
#       "requiredPayment": 5000.00,
#       "actualPayment": 3000.00,
#       "underpayment": 2000.00,
#       "quartersUnpaid": 3,
#       "penaltyAmount": 75.00,
#       "calculation": "$2,000 × 1.25% × 3 quarters"
#     },
#     {
#       "quarter": "Q3",
#       "dueDate": "2024-09-15",
#       "requiredPayment": 5000.00,
#       "actualPayment": 5000.00,
#       "underpayment": 0.00,
#       "quartersUnpaid": 0,
#       "penaltyAmount": 0.00
#     },
#     {
#       "quarter": "Q4",
#       "dueDate": "2025-01-15",
#       "requiredPayment": 5000.00,
#       "actualPayment": 6000.00,
#       "underpayment": -1000.00,
#       "overpaymentAppliedTo": "Q1",
#       "penaltyReduction": -25.00
#     }
#   ],
#   "totalPenalty": 200.00,
#   "message": "Failed both safe harbors → Underpayment penalty applies"
# }
```

### 2.6 Calculate Compound Interest

Calculate quarterly compounding interest on unpaid tax:

```bash
curl -X POST http://localhost:8080/api/v1/interest/calculate \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "returnId": "550e8400-e29b-41d4-a716-446655440001",
    "taxDueDate": "2024-04-15",
    "unpaidTaxAmount": 10000.00,
    "startDate": "2024-04-15",
    "endDate": "2025-04-15",
    "annualInterestRate": 0.06
  }'

# Expected response:
# {
#   "interestId": "c50e8400-e29b-41d4-a716-446655440008",
#   "totalDays": 365,
#   "totalInterest": 614.00,
#   "quarters": [
#     {
#       "quarter": "Q2 2024",
#       "startDate": "2024-04-15",
#       "endDate": "2024-06-30",
#       "days": 77,
#       "beginningBalance": 10000.00,
#       "interestAccrued": 150.00,
#       "endingBalance": 10150.00,
#       "calculation": "$10,000 × 6% × 77/365"
#     },
#     {
#       "quarter": "Q3 2024",
#       "startDate": "2024-07-01",
#       "endDate": "2024-09-30",
#       "days": 92,
#       "beginningBalance": 10150.00,
#       "interestAccrued": 153.00,
#       "endingBalance": 10303.00,
#       "calculation": "$10,150 × 6% × 92/365 (compounded)"
#     },
#     {
#       "quarter": "Q4 2024",
#       "startDate": "2024-10-01",
#       "endDate": "2024-12-31",
#       "days": 92,
#       "beginningBalance": 10303.00,
#       "interestAccrued": 155.00,
#       "endingBalance": 10458.00,
#       "calculation": "$10,303 × 6% × 92/365 (compounded)"
#     },
#     {
#       "quarter": "Q1 2025",
#       "startDate": "2025-01-01",
#       "endDate": "2025-04-15",
#       "days": 104,
#       "beginningBalance": 10458.00,
#       "interestAccrued": 156.00,
#       "endingBalance": 10614.00,
#       "calculation": "$10,458 × 6% × 104/365 (compounded)"
#     }
#   ],
#   "message": "Compound quarterly interest: $614 (vs $600 simple interest)"
# }
```

### 2.7 Request Penalty Abatement

Request abatement for reasonable cause:

```bash
curl -X POST http://localhost:8080/api/v1/penalties/abatement/request \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "returnId": "550e8400-e29b-41d4-a716-446655440001",
    "penaltyId": "650e8400-e29b-41d4-a716-446655440002",
    "abatementType": "LATE_FILING",
    "requestedAmount": 1500.00,
    "reason": "ILLNESS",
    "explanation": "Hospitalized for COVID-19 from March 15 to May 20, 2024. Unable to access records or prepare tax return during this period.",
    "supportingDocuments": [
      {
        "fileName": "hospital-admission-records.pdf",
        "fileUrl": "s3://munitax-documents/abatements/hospital-records-2024.pdf"
      }
    ]
  }'

# Expected response:
# {
#   "abatementId": "d50e8400-e29b-41d4-a716-446655440009",
#   "status": "PENDING",
#   "requestDate": "2024-11-28",
#   "formGenerated": "s3://munitax-documents/forms/form-27-pa-d50e8400.pdf",
#   "message": "Penalty abatement request submitted. Form 27-PA generated. Status: PENDING review by auditor.",
#   "nextSteps": [
#     "Auditor will review within 30 days",
#     "You will be notified of decision via email",
#     "If approved, penalty will be removed from your account"
#   ]
# }
```

### 2.8 Apply Payment and Track Allocation

Apply payment to tax return with penalties and interest:

```bash
curl -X POST http://localhost:8080/api/v1/payments/apply \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "returnId": "550e8400-e29b-41d4-a716-446655440001",
    "paymentAmount": 8000.00,
    "paymentDate": "2024-11-28",
    "paymentMethod": "ACH"
  }'

# Expected response:
# {
#   "allocationId": "e50e8400-e29b-41d4-a716-446655440010",
#   "paymentAmount": 8000.00,
#   "allocation": {
#     "appliedToTax": 5000.00,
#     "appliedToPenalties": 2000.00,
#     "appliedToInterest": 1000.00
#   },
#   "remainingBalances": {
#     "tax": 0.00,
#     "penalties": 500.00,
#     "interest": 200.00,
#     "total": 700.00
#   },
#   "allocationOrder": "TAX_FIRST",
#   "allocationBreakdown": [
#     { "step": 1, "to": "Tax Principal", "amount": 5000.00, "balance": 5000.00 },
#     { "step": 2, "to": "Late Filing Penalty", "amount": 1500.00, "balance": 3500.00 },
#     { "step": 3, "to": "Late Payment Penalty", "amount": 500.00, "balance": 3000.00 },
#     { "step": 4, "to": "Interest", "amount": 1000.00, "balance": 2000.00 }
#   ],
#   "message": "Payment applied per IRS standard order: Tax → Penalties → Interest. Remaining balance: $700"
# }
```

---

## 3. Frontend Usage

### 3.1 Start Frontend Development Server

```bash
cd /home/runner/work/munciplaityTax/munciplaityTax

# Install dependencies (if not already done)
npm install

# Start Vite development server
npm run dev

# Expected output:
# VITE v5.0.0  ready in 500 ms
# ➜  Local:   http://localhost:5173/
# ➜  Network: use --host to expose
```

### 3.2 Navigate to Penalty Summary Page

1. Open browser to http://localhost:5173
2. Login with test credentials (john.taxpayer@example.com / SecurePass123!)
3. Navigate to **Tax Return** → **2024 Individual Return**
4. Click **View Penalties & Interest** tab

**Expected UI Components**:
- Penalty Summary Card (Late Filing, Late Payment, Estimated)
- Interest Calculation Breakdown (Quarterly)
- Payment History Table
- Request Abatement Button
- Penalty Calculator Tool

### 3.3 Use Penalty Calculator

1. Click **Calculate Penalties** button
2. Enter filing date (e.g., 2024-07-15)
3. Enter payment date (e.g., 2024-08-15)
4. System displays:
   - Late filing penalty: $1,500
   - Late payment penalty: $200
   - Combined cap applied: $1,000 saved
   - Total penalties: $1,700
   - Interest: $614 (compounded quarterly)
   - **Total Due**: $12,314

---

## 4. Integration Tests

### 4.1 Run Unit Tests

```bash
cd backend/tax-engine-service

# Run all penalty and interest tests
mvn test -Dtest=PenaltyCalculationServiceTest

# Expected output:
# [INFO] Tests run: 25, Failures: 0, Errors: 0, Skipped: 0
#
# Test results:
# ✓ testLateFilingPenalty_ThreeMonthsLate
# ✓ testLateFilingPenalty_MaxCap
# ✓ testLatePaymentPenalty_FourMonthsLate
# ✓ testCombinedPenaltyCap_BothPenaltiesApply
# ✓ testSafeHarbor1_NinetyPercentCurrentYear
# ✓ testSafeHarbor2_HundredPercentPriorYear
# ✓ testSafeHarbor2_HighIncomeThreshold
# ✓ testUnderpaymentPenalty_QuarterlyCalculation
# ✓ testUnderpaymentPenalty_OverpaymentApplication
# ✓ testInterestCalculation_QuarterlyCompounding
# ... (15 more tests)
```

### 4.2 Run Integration Tests

```bash
# Run API integration tests
mvn test -Dtest=PenaltyApiIntegrationTest

# Expected output:
# [INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
#
# Test results:
# ✓ testCalculateLateFilingPenalty_Api
# ✓ testCalculateLatePaymentPenalty_Api
# ✓ testEvaluateSafeHarbor_Api
# ✓ testCalculateInterest_Api
# ✓ testRequestAbatement_Api
# ✓ testApplyPaymentAllocation_Api
# ... (9 more tests)
```

### 4.3 Run End-to-End Tests

```bash
cd /home/runner/work/munciplaityTax/munciplaityTax

# Run Playwright E2E tests
npm run test:e2e -- specs/7-enhanced-penalty-interest

# Expected output:
# ✓ [E2E] Calculate Late Filing Penalty (2.5s)
# ✓ [E2E] Calculate Late Payment Penalty (2.3s)
# ✓ [E2E] Evaluate Safe Harbor Rules (3.1s)
# ✓ [E2E] Request Penalty Abatement (4.2s)
# ✓ [E2E] Apply Payment with Allocation (3.8s)
#
# 5 passed (15.9s)
```

---

## 5. Database Queries (Verification)

### 5.1 Verify Penalty Records

```sql
-- Connect to PostgreSQL
psql -h localhost -U postgres -d munitax

-- Check penalties for a return
SELECT 
    penalty_type,
    months_late,
    penalty_rate,
    penalty_amount,
    is_abated
FROM dublin.penalties
WHERE return_id = '550e8400-e29b-41d4-a716-446655440001'
ORDER BY assessment_date DESC;

-- Expected output:
-- penalty_type    | months_late | penalty_rate | penalty_amount | is_abated
-- ----------------+-------------+--------------+----------------+-----------
-- LATE_FILING     |           3 |       0.0500 |        1500.00 | f
-- LATE_PAYMENT    |           4 |       0.0100 |         200.00 | f
```

### 5.2 Verify Interest Calculation

```sql
-- Check interest calculation
SELECT 
    i.total_days,
    i.annual_interest_rate,
    i.total_interest,
    COUNT(qi.id) as quarters
FROM dublin.interests i
LEFT JOIN dublin.quarterly_interests qi ON qi.interest_id = i.id
WHERE i.return_id = '550e8400-e29b-41d4-a716-446655440001'
GROUP BY i.id;

-- Expected output:
-- total_days | annual_interest_rate | total_interest | quarters
-- -----------+----------------------+----------------+----------
--        365 |               0.0600 |         614.00 |        4
```

### 5.3 Verify Payment Allocation

```sql
-- Check payment allocation
SELECT 
    payment_date,
    payment_amount,
    applied_to_tax,
    applied_to_penalties,
    applied_to_interest,
    remaining_tax_balance + remaining_penalty_balance + remaining_interest_balance as total_remaining
FROM dublin.payment_allocations
WHERE return_id = '550e8400-e29b-41d4-a716-446655440001'
ORDER BY payment_date DESC;

-- Expected output:
-- payment_date | payment_amount | applied_to_tax | applied_to_penalties | applied_to_interest | total_remaining
-- -------------+----------------+----------------+----------------------+---------------------+-----------------
-- 2024-11-28   |        8000.00 |        5000.00 |              2000.00 |             1000.00 |          700.00
```

### 5.4 Verify Audit Trail

```sql
-- Check audit trail
SELECT 
    entity_type,
    action,
    actor_role,
    description,
    created_at
FROM dublin.penalty_audit_logs
WHERE entity_id IN (
    SELECT id FROM dublin.penalties 
    WHERE return_id = '550e8400-e29b-41d4-a716-446655440001'
)
ORDER BY created_at DESC
LIMIT 10;

-- Expected output:
-- entity_type | action    | actor_role | description                           | created_at
-- ------------+-----------+------------+---------------------------------------+-------------------------
-- PENALTY     | ASSESSED  | SYSTEM     | Late filing penalty assessed: $1,500  | 2024-11-28 10:15:23.456
-- PENALTY     | ASSESSED  | SYSTEM     | Late payment penalty assessed: $200   | 2024-11-28 10:15:24.789
-- INTEREST    | CALCULATED| SYSTEM     | Quarterly compound interest: $614     | 2024-11-28 10:15:25.123
-- PAYMENT_... | PAYMENT_..| TAXPAYER   | Payment applied: $8,000               | 2024-11-28 11:30:45.678
```

---

## 6. Troubleshooting

### 6.1 Penalty Not Calculated

**Symptom**: API returns no penalty despite late filing.

**Causes**:
1. Extension deadline used instead of original due date
2. Tax paid on time (no late filing penalty if tax paid by due date)
3. First-time filer grace period (business registered <90 days)

**Solution**:
```bash
# Check return extension status
curl -X GET http://localhost:8080/api/v1/returns/550e8400-e29b-41d4-a716-446655440001 \
  -H "Authorization: Bearer $JWT_TOKEN" | jq '.extensionGranted, .extendedDueDate'

# Verify tax payment date
curl -X GET http://localhost:8080/api/v1/payments/return/550e8400-e29b-41d4-a716-446655440001 \
  -H "Authorization: Bearer $JWT_TOKEN" | jq '.payments[0].paymentDate'
```

### 6.2 Safe Harbor Not Applied

**Symptom**: Underpayment penalty assessed despite meeting 100% prior year rule.

**Causes**:
1. Prior year tax liability not retrieved from database
2. AGI threshold misapplied (110% for high earners)
3. Estimated payments credited to wrong quarters

**Solution**:
```bash
# Verify prior year tax liability
curl -X GET http://localhost:8080/api/v1/returns?taxYear=2023&taxpayerId=550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer $JWT_TOKEN" | jq '.totalTaxLiability'

# Check AGI threshold
curl -X GET http://localhost:8080/api/v1/penalties/estimated-tax/safe-harbor/550e8400-e29b-41d4-a716-446655440001 \
  -H "Authorization: Bearer $JWT_TOKEN" | jq '.safeHarbor2.required, .safeHarbor2.agiThreshold'
```

### 6.3 Interest Not Compounding

**Symptom**: Interest calculation shows simple interest instead of compound.

**Causes**:
1. Quarterly interest records not created
2. Ending balance not carried to next quarter beginning balance
3. Compounding frequency set to SIMPLE instead of QUARTERLY

**Solution**:
```sql
-- Check quarterly interest records
SELECT 
    quarter,
    beginning_balance,
    interest_accrued,
    ending_balance
FROM dublin.quarterly_interests
WHERE interest_id = 'c50e8400-e29b-41d4-a716-446655440008'
ORDER BY start_date;

-- Verify compounding
-- Each quarter's beginning balance should equal previous quarter's ending balance
-- If not, recalculate:
DELETE FROM dublin.quarterly_interests WHERE interest_id = 'c50e8400-e29b-41d4-a716-446655440008';

-- Re-trigger interest calculation API
```

### 6.4 Abatement Request Not Processing

**Symptom**: Penalty abatement status stuck at PENDING.

**Causes**:
1. No auditor assigned to review queue
2. Supporting documents not uploaded
3. First-time abatement eligibility check failed (prior penalties exist)

**Solution**:
```bash
# Check abatement status
curl -X GET http://localhost:8080/api/v1/penalties/abatement/d50e8400-e29b-41d4-a716-446655440009 \
  -H "Authorization: Bearer $JWT_TOKEN" | jq '.status, .reviewedBy, .supportingDocuments'

# Manually assign to auditor (admin only)
curl -X PATCH http://localhost:8080/api/v1/penalties/abatement/d50e8400-e29b-41d4-a716-446655440009/assign \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{ "auditorId": "auditor-uuid-here" }'
```

---

## 7. Performance Benchmarks

Expected API response times (p95):

| Endpoint | Response Time | Notes |
|----------|---------------|-------|
| Calculate Late Filing Penalty | <200ms | Simple calculation |
| Calculate Late Payment Penalty | <200ms | Simple calculation |
| Calculate Combined Penalty | <300ms | Two penalties + cap logic |
| Evaluate Safe Harbor | <500ms | Requires prior year tax lookup |
| Calculate Underpayment Penalty | <800ms | Quarterly loop + overpayment allocation |
| Calculate Interest | <1000ms | Quarterly compounding (4 quarters) |
| Request Abatement | <2000ms | PDF generation (Form 27-PA) |
| Apply Payment Allocation | <300ms | Update multiple balances |

---

## 8. Next Steps

1. **Production Deployment**: Follow deployment guide in `/docs/deployment.md`
2. **Load Testing**: Run JMeter tests with 1000 concurrent users
3. **Security Audit**: Verify encryption of sensitive penalty data (SSN, EIN)
4. **Compliance Check**: Review calculations with tax attorney for Ohio R.C. 718.27 compliance
5. **User Training**: Provide training materials for municipal auditors on penalty abatement workflow

---

## Support

For issues or questions:
- **Documentation**: `/specs/7-enhanced-penalty-interest/`
- **API Reference**: OpenAPI spec at `/specs/7-enhanced-penalty-interest/contracts/`
- **Slack Channel**: #munitax-penalties
- **GitHub Issues**: Tag with `spec-7` label
