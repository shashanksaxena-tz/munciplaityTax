# Quickstart Guide: Withholding Reconciliation System

**Feature**: Complete Withholding Reconciliation System  
**Phase**: Phase 1 (Design & Contracts)  
**Date**: 2024-11-28

---

## Overview

This quickstart guide helps developers:
- Set up local development environment
- File a W-1 withholding return via API
- Upload W-2s and perform year-end reconciliation
- Query cumulative totals
- Run integration tests
- Understand database migrations

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
cd /path/to/munitax---dublin-municipality-tax-calculator

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

# Verify withholding tables exist (from Flyway migrations)
\dt dublin.*

# Expected tables:
# dublin.w1_filings
# dublin.cumulative_withholding_totals
# dublin.withholding_reconciliations
# dublin.ignored_w2s
# dublin.withholding_payments
# dublin.withholding_audit_log
```

### 1.3 Build Backend Services

```bash
# Build tax-engine-service (contains withholding logic)
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
# Register test business user
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test-business@acmecorp.com",
    "password": "Test1234!",
    "role": "BUSINESS",
    "tenantCode": "dublin"
  }'

# Login to get JWT token
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

## 2. API Examples

### 2.1 File a W-1 Return (Quarterly)

```bash
# File Q1 2024 W-1 return
curl -X POST http://localhost:8080/api/v1/w1-filings \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "businessId": "550e8400-e29b-41d4-a716-446655440000",
    "taxYear": 2024,
    "filingFrequency": "QUARTERLY",
    "period": "Q1",
    "periodStartDate": "2024-01-01",
    "periodEndDate": "2024-03-31",
    "grossWages": 125000.00,
    "taxableWages": 125000.00,
    "adjustments": 0.00,
    "employeeCount": 15
  }' | jq

# Expected response:
# {
#   "id": "650e8400-e29b-41d4-a716-446655440000",
#   "businessId": "550e8400-e29b-41d4-a716-446655440000",
#   "taxYear": 2024,
#   "period": "Q1",
#   "dueDate": "2024-04-30",
#   "filingDate": "2024-04-25T14:30:00Z",
#   "taxDue": 2812.50,
#   "status": "FILED",
#   "cumulativeTotals": {
#     "periodsFiled": 1,
#     "cumulativeWagesYtd": 125000.00,
#     "cumulativeTaxYtd": 2812.50,
#     "projectedAnnualWages": 500000.00,
#     "onTrackIndicator": true
#   }
# }
```

### 2.2 Query Cumulative Totals

```bash
# Get YTD cumulative totals for business
curl -X GET "http://localhost:8080/api/v1/cumulative-totals?businessId=550e8400-e29b-41d4-a716-446655440000&taxYear=2024" \
  -H "Authorization: Bearer $JWT_TOKEN" | jq

# Expected response:
# {
#   "businessId": "550e8400-e29b-41d4-a716-446655440000",
#   "taxYear": 2024,
#   "periodsFiled": 3,
#   "cumulativeWagesYtd": 375000.00,
#   "cumulativeTaxYtd": 8437.50,
#   "lastFilingDate": "2024-09-20T10:15:00Z",
#   "estimatedAnnualWages": 500000.00,
#   "projectedAnnualWages": 500000.00,
#   "onTrackIndicator": true,
#   "onTrackExplanation": "Wages on pace with estimate (75% of year, 75% of estimate)"
# }
```

### 2.3 File Amended W-1

```bash
# Amend Q1 2024 W-1 (discovered payroll error)
curl -X POST "http://localhost:8080/api/v1/w1-filings/650e8400-e29b-41d4-a716-446655440000/amend" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "grossWages": 135000.00,
    "taxableWages": 135000.00,
    "adjustments": 0.00,
    "employeeCount": 16,
    "amendmentReason": "Discovered payroll processing error - missed employee bonus payments"
  }' | jq

# Expected response:
# {
#   "amendedFiling": {
#     "id": "660e8400-e29b-41d4-a716-446655440001",
#     "isAmended": true,
#     "amendsFilingId": "650e8400-e29b-41d4-a716-446655440000",
#     "grossWages": 135000.00,
#     "taxDue": 3037.50
#   },
#   "cascadeUpdateCount": 3,
#   "cascadeUpdateSummary": "Recalculated cumulative totals for Q2, Q3, Q4 (3 periods)"
# }
```

### 2.4 Initiate Year-End Reconciliation

```bash
# Upload W-2 PDFs and initiate reconciliation
curl -X POST http://localhost:8080/api/v1/reconciliations \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -F "businessId=550e8400-e29b-41d4-a716-446655440000" \
  -F "taxYear=2024" \
  -F "w2Files=@/path/to/w2_employee1.pdf" \
  -F "w2Files=@/path/to/w2_employee2.pdf" \
  -F "w2Files=@/path/to/w2_employee3.pdf" | jq

# Expected response (async processing):
# {
#   "reconciliationId": "750e8400-e29b-41d4-a716-446655440000",
#   "status": "IN_PROGRESS",
#   "message": "Reconciliation initiated. Processing 3 W-2 PDFs...",
#   "estimatedCompletionTime": "2024-01-15T14:35:00Z"
# }

# Poll reconciliation status
curl -X GET "http://localhost:8080/api/v1/reconciliations/750e8400-e29b-41d4-a716-446655440000" \
  -H "Authorization: Bearer $JWT_TOKEN" | jq '.status'

# Wait for status = "DISCREPANCY" or "RECONCILED"
```

### 2.5 Get Reconciliation Report

```bash
# Retrieve detailed reconciliation report
curl -X GET "http://localhost:8080/api/v1/reconciliations/750e8400-e29b-41d4-a716-446655440000" \
  -H "Authorization: Bearer $JWT_TOKEN" | jq

# Expected response:
# {
#   "id": "750e8400-e29b-41d4-a716-446655440000",
#   "taxYear": 2024,
#   "w1Totals": {
#     "totalWages": 500000.00,
#     "totalTax": 11250.00,
#     "periodsFiled": 4
#   },
#   "w2Totals": {
#     "totalWages": 502500.00,
#     "totalTax": 11306.25,
#     "w2Count": 15
#   },
#   "variance": {
#     "wagesVariance": -2500.00,
#     "wagesVariancePercentage": -0.50,
#     "explanation": "W-2 totals $2,500 higher than W-1 filings (0.5% variance)"
#   },
#   "status": "DISCREPANCY",
#   "ignoredW2s": {
#     "count": 2
#   }
# }
```

### 2.6 Resolve Discrepancy

```bash
# Accept minor discrepancy with explanation
curl -X PATCH "http://localhost:8080/api/v1/reconciliations/750e8400-e29b-41d4-a716-446655440000/resolve" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "action": "ACCEPT",
    "resolutionNotes": "Variance due to mid-year employee relocation (different locality). Confirmed with payroll records."
  }' | jq

# Expected response: status = "RECONCILED"
```

### 2.7 Get Ignored W-2s Report

```bash
# View W-2s not included in reconciliation
curl -X GET "http://localhost:8080/api/v1/reconciliations/750e8400-e29b-41d4-a716-446655440000/ignored-w2s" \
  -H "Authorization: Bearer $JWT_TOKEN" | jq

# Expected response:
# {
#   "ignoredW2Count": 2,
#   "ignoredW2s": [
#     {
#       "employerEin": "98-7654321",
#       "employerName": "XYZ Inc",
#       "reason": "WRONG_EIN",
#       "reasonDescription": "Employer EIN does not match business profile"
#     },
#     {
#       "employerEin": "12-3456789",
#       "reason": "DUPLICATE",
#       "reasonDescription": "Employee SSN already exists in reconciliation"
#     }
#   ]
# }
```

### 2.8 Calculate Penalties

```bash
# Preview penalty calculation (late filing)
curl -X GET "http://localhost:8080/api/v1/w1-filings/650e8400-e29b-41d4-a716-446655440000/penalties?asOfDate=2024-05-15" \
  -H "Authorization: Bearer $JWT_TOKEN" | jq

# Expected response:
# {
#   "filingId": "650e8400-e29b-41d4-a716-446655440000",
#   "dueDate": "2024-04-30",
#   "filingDate": "2024-05-15",
#   "daysLate": 15,
#   "lateFilingPenalty": {
#     "amount": 140.63,
#     "rate": 0.05,
#     "monthsLate": 1,
#     "calculation": "$2,812.50 tax due × 5% = $140.63 (1 month late)"
#   },
#   "totalPenalties": 140.63
# }
```

---

## 3. Database Queries

### 3.1 View All W-1 Filings for Business

```sql
-- Connect to PostgreSQL
psql -h localhost -U postgres -d munitax

-- Set schema
SET search_path TO dublin;

-- Query W-1 filings with cumulative totals
SELECT 
    f.period,
    f.period_end_date,
    f.due_date,
    f.filing_date,
    f.gross_wages,
    f.tax_due,
    f.is_amended,
    f.status,
    c.cumulative_wages_ytd,
    c.cumulative_tax_ytd,
    c.on_track_indicator
FROM w1_filings f
LEFT JOIN cumulative_withholding_totals c 
    ON c.business_id = f.business_id 
    AND c.tax_year = f.tax_year
WHERE f.business_id = '550e8400-e29b-41d4-a716-446655440000'
    AND f.tax_year = 2024
ORDER BY f.period_end_date;
```

### 3.2 View Reconciliation with W-2 Details

```sql
-- Query reconciliation report
SELECT 
    r.tax_year,
    r.w1_total_wages,
    r.w2_total_wages,
    r.variance_wages,
    r.variance_percentage,
    r.status,
    r.w2_count,
    r.ignored_w2_count,
    r.resolution_notes
FROM withholding_reconciliations r
WHERE r.business_id = '550e8400-e29b-41d4-a716-446655440000'
    AND r.tax_year = 2024;

-- View ignored W-2s
SELECT 
    employer_ein,
    employer_name,
    ignored_reason,
    uploaded_at,
    resolution_action
FROM ignored_w2s
WHERE reconciliation_id = '750e8400-e29b-41d4-a716-446655440000';
```

### 3.3 Audit Trail for W-1 Filing

```sql
-- View all audit log entries for a W-1 filing
SELECT 
    action,
    description,
    actor_role,
    created_at,
    old_value::json,
    new_value::json
FROM withholding_audit_log
WHERE entity_type = 'W1_FILING'
    AND entity_id = '650e8400-e29b-41d4-a716-446655440000'
ORDER BY created_at DESC;
```

---

## 4. Running Tests

### 4.1 Unit Tests

```bash
# Run unit tests for W1FilingService
cd backend/tax-engine-service
mvn test -Dtest=W1FilingServiceTest

# Run cumulative calculation tests
mvn test -Dtest=CumulativeCalculationServiceTest

# Run penalty calculation tests
mvn test -Dtest=PenaltyCalculationServiceTest

# Run all withholding tests
mvn test -Dtest="*Withholding*"
```

### 4.2 Integration Tests

```bash
# Run integration tests with TestContainers (PostgreSQL + Redis)
mvn test -Dtest=WithholdingReconciliationIntegrationTest

# Expected test scenarios:
# 1. File 4 quarterly W-1s → Verify cumulative totals
# 2. Amend Q1 W-1 → Verify cascade update of Q2-Q4
# 3. Upload W-2s → Verify reconciliation calculation
# 4. Resolve discrepancy → Verify status change to RECONCILED

# Run all integration tests
mvn verify -Pintegration-tests
```

### 4.3 Frontend Tests

```bash
# Run component tests (Vitest + React Testing Library)
cd ../../
npm test -- --run withholding

# Run E2E tests (Playwright)
npx playwright test specs/withholding-reconciliation.spec.ts

# Expected E2E workflow:
# 1. Login as business user
# 2. File Q1 W-1 return
# 3. Verify cumulative totals displayed
# 4. Upload 3 W-2 PDFs
# 5. Verify reconciliation report
# 6. Resolve discrepancy
# 7. Verify status = RECONCILED
```

---

## 5. Database Migrations

### 5.1 Flyway Migration Files

```text
backend/tax-engine-service/src/main/resources/db/migration/

V1.20__create_w1_filings_table.sql
V1.21__create_cumulative_withholding_totals_table.sql
V1.22__create_withholding_reconciliations_table.sql
V1.23__create_ignored_w2s_table.sql
V1.24__create_withholding_payments_table.sql
V1.25__create_withholding_audit_log_table.sql
V1.26__add_withholding_indexes.sql
V1.27__add_withholding_constraints.sql
```

### 5.2 Apply Migrations

```bash
# Migrations run automatically on service startup via Flyway
# To manually apply migrations:

cd backend/tax-engine-service
mvn flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:5432/munitax \
                   -Dflyway.user=postgres \
                   -Dflyway.password=postgres \
                   -Dflyway.schemas=dublin

# Verify migration history
mvn flyway:info

# Expected output:
# | Version | Description                                  | State   |
# |---------|----------------------------------------------|---------|
# | 1.20    | create w1 filings table                      | Success |
# | 1.21    | create cumulative withholding totals table   | Success |
# | 1.22    | create withholding reconciliations table     | Success |
# | 1.23    | create ignored w2s table                     | Success |
# | 1.24    | create withholding payments table            | Success |
# | 1.25    | create withholding audit log table           | Success |
# | 1.26    | add withholding indexes                      | Success |
# | 1.27    | add withholding constraints                  | Success |
```

### 5.3 Rollback Migrations (Development Only)

```bash
# Flyway undo migrations (requires Flyway Teams edition)
# For development, manually rollback via SQL:

psql -h localhost -U postgres -d munitax

-- Set schema
SET search_path TO dublin;

-- Drop tables in reverse order
DROP TABLE IF EXISTS withholding_audit_log CASCADE;
DROP TABLE IF EXISTS withholding_payments CASCADE;
DROP TABLE IF EXISTS ignored_w2s CASCADE;
DROP TABLE IF EXISTS withholding_reconciliations CASCADE;
DROP TABLE IF EXISTS cumulative_withholding_totals CASCADE;
DROP TABLE IF EXISTS w1_filings CASCADE;

-- Verify tables dropped
\dt

-- Re-run migrations
\q
mvn flyway:migrate
```

---

## 6. Event-Driven Architecture

### 6.1 Redis Pub/Sub (Development)

```bash
# Monitor Redis events
redis-cli

# Subscribe to W-1 filed events
SUBSCRIBE withholding:w1-filed

# In another terminal, file a W-1 return
curl -X POST http://localhost:8080/api/v1/w1-filings ...

# Expected Redis message:
# {
#   "eventType": "W1_FILED",
#   "businessId": "550e8400-e29b-41d4-a716-446655440000",
#   "filingId": "650e8400-e29b-41d4-a716-446655440000",
#   "grossWages": 125000.00,
#   "taxDue": 2812.50
# }
```

### 6.2 Kafka (Production)

```bash
# For production Kafka setup, see deployment guide

# Monitor Kafka topics
kafka-console-consumer --bootstrap-server localhost:9092 \
                       --topic withholding.w1-filed \
                       --from-beginning

# Expected Kafka message (same payload as Redis)
```

---

## 7. Performance Testing

### 7.1 Load Test: Cumulative Totals Query

```bash
# Install Apache Bench (ab)
brew install apache2  # macOS

# Load test cumulative totals endpoint (100 concurrent requests, 1000 total)
ab -n 1000 -c 100 \
   -H "Authorization: Bearer $JWT_TOKEN" \
   "http://localhost:8080/api/v1/cumulative-totals?businessId=550e8400-e29b-41d4-a716-446655440000&taxYear=2024"

# Expected results (from research R2 benchmark):
# Requests per second: 1250 [#/sec]
# Time per request: 80ms [avg]
# 95th percentile: <120ms
```

### 7.2 Load Test: W-1 Filing Submission

```bash
# Load test W-1 filing endpoint (simulate 50 businesses filing simultaneously)
ab -n 50 -c 50 \
   -H "Content-Type: application/json" \
   -H "Authorization: Bearer $JWT_TOKEN" \
   -p w1_filing_payload.json \
   "http://localhost:8080/api/v1/w1-filings"

# w1_filing_payload.json:
# {
#   "businessId": "550e8400-e29b-41d4-a716-446655440000",
#   "taxYear": 2024,
#   "filingFrequency": "QUARTERLY",
#   "period": "Q1",
#   "periodStartDate": "2024-01-01",
#   "periodEndDate": "2024-03-31",
#   "grossWages": 125000.00,
#   "taxableWages": 125000.00
# }

# Expected results:
# Requests per second: 25 [#/sec]
# Time per request: <2 seconds [avg] (FR-001 success criteria)
```

---

## 8. Troubleshooting

### 8.1 Service Not Starting

```bash
# Check service logs
docker-compose logs tax-engine-service

# Common issues:
# - Database connection failure: Verify PostgreSQL running (docker ps)
# - Eureka registration timeout: Verify discovery-service running
# - Port conflict: Check port 8083 not in use (lsof -i :8083)

# Restart service
docker-compose restart tax-engine-service
```

### 8.2 JWT Token Expired

```bash
# JWT tokens expire after 24 hours
# Re-login to get new token
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test-business@acmecorp.com",
    "password": "Test1234!"
  }' | jq -r '.token'

export JWT_TOKEN="<new_token>"
```

### 8.3 Cumulative Totals Not Updating

```bash
# Check Redis Pub/Sub connection
docker-compose logs tax-engine-service | grep "Redis"

# Manually trigger cumulative totals recalculation (self-healing job)
psql -h localhost -U postgres -d munitax

SET search_path TO dublin;

-- Force recalculation by updating updated_at timestamp
UPDATE cumulative_withholding_totals
SET updated_at = NOW() - INTERVAL '2 days'
WHERE business_id = '550e8400-e29b-41d4-a716-446655440000';

-- Wait for scheduled job (runs daily at 2 AM)
-- Or manually trigger via API endpoint (if implemented):
curl -X POST "http://localhost:8080/api/v1/admin/recalculate-cumulative-totals?businessId=550e8400-e29b-41d4-a716-446655440000" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

### 8.4 W-2 Extraction Failing

```bash
# Check extraction-service logs
docker-compose logs extraction-service | grep "W-2"

# Common issues:
# - Gemini API key missing: Verify GEMINI_API_KEY env var set
# - PDF corrupted: Re-upload W-2 PDF
# - Unsupported format: Verify PDF is valid (not scanned image without OCR)

# Test extraction service directly
curl -X POST http://localhost:8084/api/v1/extraction/w2 \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -F "file=@/path/to/w2.pdf" | jq

# Expected response:
# {
#   "formType": "W-2",
#   "employerEin": "12-3456789",
#   "localWages": 50000.00,
#   "localWithheld": 1125.00,
#   "confidenceScore": 0.95
# }
```

---

## 9. Next Steps

1. ✅ **Development Environment**: Local setup complete
2. ✅ **API Testing**: Filed W-1, queried totals, uploaded W-2s
3. ✅ **Database**: Migrations applied, data verified
4. ✅ **Tests**: Unit + integration tests passing
5. ⏳ **Frontend**: Implement React components (see UI_IMPLEMENTATION_PLAN.md)
6. ⏳ **Production Deployment**: Docker Compose → Kubernetes migration

---

## 10. Additional Resources

- **API Documentation**: OpenAPI specs in `/specs/1-withholding-reconciliation/contracts/`
- **Data Model**: See `/specs/1-withholding-reconciliation/data-model.md`
- **Research Decisions**: See `/specs/1-withholding-reconciliation/research.md`
- **Implementation Plan**: See `/specs/1-withholding-reconciliation/plan.md`
- **Constitution**: See `/.specify/memory/constitution.md`

---

## 11. Support

For questions or issues:
- GitHub Issues: https://github.com/munitax/munitax/issues
- Slack: #withholding-reconciliation
- Email: engineering@munitax.com

**Quickstart Complete!** You're ready to develop and test the withholding reconciliation system.
