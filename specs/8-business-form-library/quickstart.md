# Quickstart Guide: Business Form Library

**Feature**: Comprehensive Municipal Tax Form Generation System  
**Phase**: Phase 1 (Design & Contracts)  
**Date**: 2024-11-28

---

## Overview

This quickstart guide helps developers:
- Set up local development environment for PDF generation
- Generate Form 27-EXT (Extension Request) via API
- Generate quarterly estimated tax vouchers (Form 27-ES)
- Assemble complete filing package with multiple forms
- Test PDF generation with watermarks and validation
- Run integration tests for form generation workflows

**Prerequisites**:
- Docker Desktop (for PostgreSQL 16, Redis 7, MinIO S3)
- Java 21 JDK
- Maven 3.9+
- Node.js 20+ (for frontend development)
- curl or Postman (for API testing)
- PDF viewer (Adobe Acrobat, Preview, or Chrome)

---

## 1. Environment Setup

### 1.1 Start Infrastructure

```bash
# Navigate to project root
cd /path/to/munitax---dublin-municipality-tax-calculator

# Start all containers (includes MinIO for S3-compatible storage)
docker-compose up -d

# Verify all services running
docker-compose ps

# Expected output:
# discovery-service    HEALTHY   8761
# gateway-service      HEALTHY   8080
# auth-service         HEALTHY   8081
# pdf-service          HEALTHY   8082
# tax-engine-service   HEALTHY   8083
# postgresql           HEALTHY   5432
# redis                HEALTHY   6379
# minio                HEALTHY   9000
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

# Verify form library tables exist (from Flyway migrations)
\dt dublin.*

# Expected tables:
# dublin.form_templates
# dublin.generated_forms
# dublin.filing_packages
# dublin.form_generation_audit
# dublin.extension_requests
# dublin.estimated_tax_vouchers
```

### 1.3 MinIO S3 Storage Setup

```bash
# Access MinIO console at http://localhost:9001
# Default credentials: minioadmin / minioadmin

# Create required buckets via MinIO Console or CLI:
# - munitax-templates (for blank form templates)
# - munitax-forms (for generated forms)

# Or use mc CLI:
mc alias set local http://localhost:9000 minioadmin minioadmin
mc mb local/munitax-templates
mc mb local/munitax-forms
```

### 1.4 Upload Form Templates

```bash
# Download official municipal form templates (or use test templates)
# Place in: /tmp/form-templates/

# Upload Form 27-EXT template
mc cp /tmp/form-templates/27-EXT-2024.pdf local/munitax-templates/dublin/27-EXT-2024.pdf

# Upload Form 27-ES template
mc cp /tmp/form-templates/27-ES-2024.pdf local/munitax-templates/dublin/27-ES-2024.pdf

# Upload Form 27-NOL template
mc cp /tmp/form-templates/27-NOL-2024.pdf local/munitax-templates/dublin/27-NOL-2024.pdf
```

### 1.5 Seed Form Template Metadata

```bash
# Connect to PostgreSQL
psql -h localhost -U postgres -d munitax

# Insert Form 27-EXT template
INSERT INTO dublin.form_templates (
    id, tenant_id, form_code, form_name, template_file_path, 
    revision_date, applicable_years, field_mappings, page_count, category, created_by
) VALUES (
    gen_random_uuid(),
    (SELECT id FROM tenants WHERE code = 'DUBLIN'),
    '27-EXT',
    'Extension Request',
    's3://munitax-templates/dublin/27-EXT-2024.pdf',
    '2024-01-01',
    ARRAY[2024, 2025],
    '{
        "form_fields": {
            "business_name": {
                "pdf_field_name": "topmostSubform[0].Page1[0].BusinessName[0]",
                "data_source": "business.legal_name",
                "required": true
            },
            "fein": {
                "pdf_field_name": "topmostSubform[0].Page1[0].FEIN[0]",
                "data_source": "business.ein",
                "required": true
            },
            "tax_year": {
                "pdf_field_name": "topmostSubform[0].Page1[0].TaxYear[0]",
                "data_source": "tax_return.tax_year",
                "required": true
            },
            "estimated_tax": {
                "pdf_field_name": "topmostSubform[0].Page1[0].Line3_EstimatedTax[0]",
                "data_source": "extension_request.estimated_tax_liability",
                "format": "currency",
                "required": true
            }
        }
    }',
    2,
    'EXTENSION',
    (SELECT id FROM users WHERE email = 'admin@dublin.gov')
);
```

### 1.6 Build Backend Services

```bash
# Build pdf-service (contains PDF generation logic)
cd backend/pdf-service
mvn clean install -DskipTests

# Build tax-engine-service (contains form validation logic)
cd ../tax-engine-service
mvn clean install -DskipTests

# Restart services to pick up new migrations
docker-compose restart pdf-service tax-engine-service

# Check logs for startup success
docker-compose logs -f pdf-service

# Expected log entry:
# "Started PdfServiceApplication in 10.123 seconds"
```

### 1.7 Obtain JWT Token

```bash
# Register test business user
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "cpa@acmeconstruction.com",
    "password": "SecurePass123!",
    "role": "BUSINESS_FILER",
    "businessName": "Acme Construction LLC",
    "ein": "12-3456789"
  }'

# Login to get JWT token
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "cpa@acmeconstruction.com",
    "password": "SecurePass123!"
  }'

# Response:
# {
#   "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
#   "businessId": "uuid-business-123",
#   "tenantId": "uuid-tenant-dublin"
# }

# Store token for subsequent requests
export JWT_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

## 2. Generate Form 27-EXT (Extension Request)

### 2.1 Create Extension Request Data

```bash
# Create extension request for 2024 tax year
curl -X POST http://localhost:8080/api/v1/tax-engine/extension-requests \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "returnId": "return-uuid-123",
    "taxYear": 2024,
    "estimatedTaxLiability": 25000.00,
    "priorPayments": 5000.00,
    "extensionPayment": 20000.00,
    "extensionReason": "AWAITING_K1S",
    "originalDueDate": "2024-04-15",
    "extendedDueDate": "2024-10-15"
  }'

# Response:
# {
#   "id": "extension-uuid-456",
#   "returnId": "return-uuid-123",
#   "estimatedTaxLiability": 25000.00,
#   "balanceDue": 20000.00,
#   "granted": false,
#   "createdAt": "2024-04-10T10:00:00Z"
# }

export EXTENSION_ID="extension-uuid-456"
```

### 2.2 Generate Form 27-EXT PDF

```bash
# Generate draft Form 27-EXT with watermark
curl -X POST http://localhost:8080/api/v1/pdf-service/forms/generate \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "formCode": "27-EXT",
    "returnId": "return-uuid-123",
    "taxYear": 2024,
    "isDraft": true,
    "addWatermark": true
  }'

# Response:
# {
#   "generatedFormId": "form-uuid-789",
#   "formCode": "27-EXT",
#   "version": 1,
#   "status": "DRAFT",
#   "pdfFilePath": "s3://munitax-forms/dublin/2024/return-123/27-EXT-v1.pdf",
#   "downloadUrl": "http://localhost:8082/api/v1/forms/download/form-uuid-789",
#   "pageCount": 2,
#   "fileSizeBytes": 87654,
#   "generatedAt": "2024-04-10T10:05:00Z"
# }

export FORM_ID="form-uuid-789"
```

### 2.3 Download and View PDF

```bash
# Download generated PDF
curl -X GET "http://localhost:8080/api/v1/pdf-service/forms/download/$FORM_ID" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -o /tmp/27-EXT-draft.pdf

# Open in PDF viewer
open /tmp/27-EXT-draft.pdf  # macOS
# xdg-open /tmp/27-EXT-draft.pdf  # Linux

# Expected: PDF with:
# - Business name: "Acme Construction LLC"
# - FEIN: "12-3456789"
# - Tax year: "2024"
# - Estimated tax: "$25,000.00"
# - Prior payments: "$5,000.00"
# - Extension payment: "$20,000.00"
# - Balance due: "$20,000.00"
# - Diagonal watermark: "DRAFT - NOT FOR FILING"
```

### 2.4 Remove Watermark and Finalize

```bash
# Remove watermark and mark as final
curl -X POST "http://localhost:8080/api/v1/pdf-service/forms/$FORM_ID/finalize" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "removeWatermark": true,
    "flattenFields": false,
    "reason": "Reviewed and approved for submission"
  }'

# Response:
# {
#   "generatedFormId": "form-uuid-789",
#   "status": "FINAL",
#   "isWatermarked": false,
#   "downloadUrl": "http://localhost:8082/api/v1/forms/download/form-uuid-789",
#   "finalizedAt": "2024-04-10T11:00:00Z"
# }

# Download final version
curl -X GET "http://localhost:8080/api/v1/pdf-service/forms/download/$FORM_ID" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -o /tmp/27-EXT-final.pdf

# Expected: PDF without watermark, ready for submission
```

---

## 3. Generate Quarterly Estimated Tax Vouchers (Form 27-ES)

### 3.1 Create Estimated Tax Plan

```bash
# Create annual estimated tax plan with 4 quarterly vouchers
curl -X POST http://localhost:8080/api/v1/tax-engine/estimated-tax/plans \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "returnId": "return-uuid-123",
    "taxYear": 2024,
    "annualEstimatedTax": 20000.00,
    "paymentMethod": "EQUAL_QUARTERS",
    "quarters": [
      {
        "quarter": 1,
        "paymentAmount": 5000.00,
        "dueDate": "2024-04-15"
      },
      {
        "quarter": 2,
        "paymentAmount": 5000.00,
        "dueDate": "2024-06-15"
      },
      {
        "quarter": 3,
        "paymentAmount": 5000.00,
        "dueDate": "2024-09-15"
      },
      {
        "quarter": 4,
        "paymentAmount": 5000.00,
        "dueDate": "2025-01-15"
      }
    ]
  }'

# Response:
# {
#   "planId": "plan-uuid-111",
#   "voucherIds": [
#     "voucher-uuid-Q1",
#     "voucher-uuid-Q2",
#     "voucher-uuid-Q3",
#     "voucher-uuid-Q4"
#   ],
#   "totalAnnualAmount": 20000.00,
#   "createdAt": "2024-01-15T10:00:00Z"
# }
```

### 3.2 Generate All 4 Vouchers

```bash
# Generate all 4 vouchers in batch
curl -X POST http://localhost:8080/api/v1/pdf-service/forms/batch-generate \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "formRequests": [
      {"formCode": "27-ES", "quarter": 1, "returnId": "return-uuid-123", "taxYear": 2024},
      {"formCode": "27-ES", "quarter": 2, "returnId": "return-uuid-123", "taxYear": 2024},
      {"formCode": "27-ES", "quarter": 3, "returnId": "return-uuid-123", "taxYear": 2024},
      {"formCode": "27-ES", "quarter": 4, "returnId": "return-uuid-123", "taxYear": 2024}
    ]
  }'

# Response:
# {
#   "batchId": "batch-uuid-222",
#   "totalForms": 4,
#   "generatedForms": [
#     {
#       "formId": "form-uuid-Q1",
#       "formCode": "27-ES",
#       "quarter": 1,
#       "downloadUrl": "http://localhost:8082/api/v1/forms/download/form-uuid-Q1"
#     },
#     {
#       "formId": "form-uuid-Q2",
#       "formCode": "27-ES",
#       "quarter": 2,
#       "downloadUrl": "http://localhost:8082/api/v1/forms/download/form-uuid-Q2"
#     },
#     {
#       "formId": "form-uuid-Q3",
#       "formCode": "27-ES",
#       "quarter": 3,
#       "downloadUrl": "http://localhost:8082/api/v1/forms/download/form-uuid-Q3"
#     },
#     {
#       "formId": "form-uuid-Q4",
#       "formCode": "27-ES",
#       "quarter": 4,
#       "downloadUrl": "http://localhost:8082/api/v1/forms/download/form-uuid-Q4"
#     }
#   ],
#   "completedAt": "2024-01-15T10:02:00Z"
# }
```

### 3.3 Download Q1 Voucher

```bash
# Download Q1 voucher
curl -X GET "http://localhost:8080/api/v1/pdf-service/forms/download/form-uuid-Q1" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -o /tmp/27-ES-Q1-2024.pdf

# Expected: PDF with:
# - Business name and FEIN
# - Quarter: "1st Quarter 2024"
# - Due date: "April 15, 2024"
# - Payment amount: "$5,000.00"
# - Detachable payment stub with perforation line
# - Payment instructions (mail with check or pay online)
```

---

## 4. Generate Complete Filing Package

### 4.1 Prepare All Required Forms

```bash
# Ensure all forms are generated and finalized:
# - Form 27 (main return)
# - Form 27-Y (apportionment schedule)
# - Form 27-X (book-tax adjustments)
# - Form 27-NOL (NOL schedule, if applicable)
# - Form 27-W1 annual reconciliation

# List all generated forms for return
curl -X GET "http://localhost:8080/api/v1/pdf-service/forms?returnId=return-uuid-123&status=FINAL" \
  -H "Authorization: Bearer $JWT_TOKEN"

# Response:
# {
#   "forms": [
#     {"formId": "form-27-uuid", "formCode": "27", "formName": "Main Business Return", "pageCount": 3},
#     {"formId": "form-27Y-uuid", "formCode": "27-Y", "formName": "Apportionment Schedule", "pageCount": 2},
#     {"formId": "form-27X-uuid", "formCode": "27-X", "formName": "Book-Tax Adjustments", "pageCount": 1},
#     {"formId": "form-27NOL-uuid", "formCode": "27-NOL", "formName": "NOL Schedule", "pageCount": 1}
#   ],
#   "totalForms": 4,
#   "totalPages": 7
# }
```

### 4.2 Assemble Filing Package

```bash
# Create complete filing package with all forms
curl -X POST http://localhost:8080/api/v1/pdf-service/packages/assemble \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "returnId": "return-uuid-123",
    "taxYear": 2024,
    "packageType": "ORIGINAL",
    "includeFormIds": [
      "form-27-uuid",
      "form-27Y-uuid",
      "form-27X-uuid",
      "form-27NOL-uuid"
    ],
    "includeCoverPage": true,
    "includeTableOfContents": true,
    "addBookmarks": true,
    "optimizeFileSize": true,
    "targetFileSizeMB": 10
  }'

# Response:
# {
#   "packageId": "package-uuid-333",
#   "returnId": "return-uuid-123",
#   "totalPages": 9,
#   "fileSizeBytes": 1248576,
#   "packagePdfPath": "s3://munitax-forms/dublin/2024/return-123/filing-package.pdf",
#   "downloadUrl": "http://localhost:8082/api/v1/packages/download/package-uuid-333",
#   "tableOfContents": {
#     "entries": [
#       {"formName": "Form 27 - Main Return", "startPage": 1, "endPage": 3},
#       {"formName": "Form 27-Y - Apportionment", "startPage": 4, "endPage": 5},
#       {"formName": "Form 27-X - Adjustments", "startPage": 6, "endPage": 6},
#       {"formName": "Form 27-NOL - NOL Schedule", "startPage": 7, "endPage": 7}
#     ]
#   },
#   "createdAt": "2024-04-10T12:00:00Z"
# }

export PACKAGE_ID="package-uuid-333"
```

### 4.3 Download and Verify Filing Package

```bash
# Download complete filing package
curl -X GET "http://localhost:8080/api/v1/pdf-service/packages/download/$PACKAGE_ID" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -o /tmp/filing-package-2024.pdf

# Open in PDF viewer
open /tmp/filing-package-2024.pdf

# Expected:
# - Page 1: Cover page with business info and filing summary
# - Page 2: Table of contents with clickable links
# - Pages 3-5: Form 27 (main return)
# - Pages 6-7: Form 27-Y (apportionment schedule)
# - Page 8: Form 27-X (book-tax adjustments)
# - Page 9: Form 27-NOL (NOL schedule)
# - PDF bookmarks in sidebar for easy navigation
# - Footer on each page: "Page X of 9"
# - File size: <2 MB (optimized)
```

### 4.4 Validate Package Before Submission

```bash
# Run cross-form validation
curl -X POST "http://localhost:8080/api/v1/pdf-service/packages/$PACKAGE_ID/validate" \
  -H "Authorization: Bearer $JWT_TOKEN"

# Response:
# {
#   "packageId": "package-uuid-333",
#   "isValid": true,
#   "validationResults": [
#     {"rule": "NOL_CONSISTENCY", "status": "PASS", "message": "NOL on Form 27 ($80,000) matches Form 27-NOL total"},
#     {"rule": "APPORTIONMENT_PERCENTAGE", "status": "PASS", "message": "Apportionment % on Form 27 (65%) matches Form 27-Y calculation"},
#     {"rule": "FILE_SIZE_LIMIT", "status": "PASS", "message": "File size (1.2 MB) is within 10 MB limit"},
#     {"rule": "PDF_FORMAT", "status": "PASS", "message": "PDF/A-2 format validated"},
#     {"rule": "REQUIRED_FORMS", "status": "PASS", "message": "All required forms present"}
#   ],
#   "warnings": [],
#   "errors": [],
#   "readyForSubmission": true
# }
```

---

## 5. View Form History and Audit Trail

### 5.1 View Form Generation History

```bash
# Get history for a specific form
curl -X GET "http://localhost:8080/api/v1/pdf-service/forms/$FORM_ID/history" \
  -H "Authorization: Bearer $JWT_TOKEN"

# Response:
# {
#   "formId": "form-uuid-789",
#   "formCode": "27-EXT",
#   "versions": [
#     {
#       "version": 1,
#       "status": "DRAFT",
#       "generatedAt": "2024-04-10T10:05:00Z",
#       "generatedBy": "cpa@acmeconstruction.com",
#       "isWatermarked": true,
#       "supersededBy": "form-uuid-789-v2"
#     },
#     {
#       "version": 2,
#       "status": "FINAL",
#       "generatedAt": "2024-04-10T11:00:00Z",
#       "generatedBy": "cpa@acmeconstruction.com",
#       "isWatermarked": false,
#       "isCurrent": true
#     }
#   ]
# }
```

### 5.2 View Audit Trail

```bash
# Get audit trail for form
curl -X GET "http://localhost:8080/api/v1/pdf-service/audit/forms/$FORM_ID" \
  -H "Authorization: Bearer $JWT_TOKEN"

# Response:
# {
#   "formId": "form-uuid-789",
#   "auditEntries": [
#     {
#       "timestamp": "2024-04-10T10:05:00Z",
#       "action": "GENERATED",
#       "actor": "cpa@acmeconstruction.com",
#       "actorRole": "PREPARER",
#       "details": {"version": 1, "isDraft": true}
#     },
#     {
#       "timestamp": "2024-04-10T10:30:00Z",
#       "action": "DOWNLOADED",
#       "actor": "cpa@acmeconstruction.com",
#       "actorRole": "PREPARER",
#       "ipAddress": "192.168.1.100"
#     },
#     {
#       "timestamp": "2024-04-10T11:00:00Z",
#       "action": "WATERMARK_REMOVED",
#       "actor": "cpa@acmeconstruction.com",
#       "actorRole": "PREPARER",
#       "reason": "Reviewed and approved for submission"
#     },
#     {
#       "timestamp": "2024-04-10T11:00:00Z",
#       "action": "STATUS_CHANGED",
#       "actor": "cpa@acmeconstruction.com",
#       "oldValue": {"status": "DRAFT"},
#       "newValue": {"status": "FINAL"}
#     }
#   ]
# }
```

---

## 6. Integration Tests

### 6.1 Run PDF Service Tests

```bash
# Navigate to pdf-service
cd backend/pdf-service

# Run all integration tests
mvn test -Dtest=FormGenerationIntegrationTest

# Expected output:
# [INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0

# Run specific test: Generate Form 27-EXT
mvn test -Dtest=FormGenerationIntegrationTest#testGenerateForm27EXT

# Run specific test: Assemble filing package
mvn test -Dtest=FilingPackageIntegrationTest#testAssembleFilingPackage
```

### 6.2 Test Form Validation

```bash
# Run validation tests
mvn test -Dtest=FormValidationServiceTest

# Expected tests:
# - testRequiredFieldsMissing: Form generation fails if required data missing
# - testCrossFormConsistency: NOL on Form 27 matches Form 27-NOL
# - testApportionmentConsistency: Apportionment % on Form 27 matches Form 27-Y
# - testFileSizeLimit: Package generation fails if exceeds 20MB
# - testPDFFormatValidation: Generated PDFs meet PDF/A-2 standard
```

### 6.3 Test Template Management

```bash
# Run template tests
mvn test -Dtest=FormTemplateServiceTest

# Expected tests:
# - testFindTemplateByFormCodeAndYear: Retrieves correct template for 2024
# - testTemplateFieldMapping: Maps database fields to PDF form fields
# - testTemplateVersioning: Uses correct template when multiple versions exist
# - testTemplateCaching: Template cached after first load
```

---

## 7. Frontend Testing

### 7.1 Start Frontend Development Server

```bash
# Navigate to project root
cd /path/to/munitax---dublin-municipality-tax-calculator

# Install dependencies
npm install

# Start Vite dev server
npm run dev

# Expected output:
# Local:   http://localhost:5173/
# Network: use --host to expose
```

### 7.2 Test Form Generation UI

```bash
# Open browser to: http://localhost:5173/forms

# Test User Story 1 (US-1): Generate Form 27-EXT
# 1. Login as business user: cpa@acmeconstruction.com
# 2. Navigate to "Forms" tab
# 3. Click "Request Extension" button
# 4. Fill form:
#    - Estimated tax liability: $25,000
#    - Prior payments: $5,000
#    - Extension payment: $20,000
#    - Reason: "Awaiting K-1s"
# 5. Click "Generate Draft" button
# 6. Verify draft PDF appears with watermark
# 7. Click "Review and Finalize" button
# 8. Click "Remove Watermark" checkbox
# 9. Click "Finalize Form" button
# 10. Verify final PDF appears without watermark
# 11. Click "Download" button
# 12. Verify PDF downloads to local machine

# Test User Story 2 (US-2): Generate Estimated Tax Vouchers
# 1. Navigate to "Estimated Tax" tab
# 2. Enter annual estimated tax: $20,000
# 3. Select payment method: "Equal Quarters"
# 4. Click "Generate Vouchers" button
# 5. Verify 4 vouchers appear (Q1-Q4) with $5,000 each
# 6. Click "Download All" button
# 7. Verify ZIP file downloads with 4 PDFs

# Test User Story 5 (US-5): Assemble Filing Package
# 1. Navigate to "Filing Package" tab
# 2. Select forms to include:
#    - Form 27 (main return)
#    - Form 27-Y (apportionment)
#    - Form 27-X (adjustments)
#    - Form 27-NOL (NOL schedule)
# 3. Check "Include cover page"
# 4. Check "Include table of contents"
# 5. Check "Add bookmarks"
# 6. Click "Assemble Package" button
# 7. Verify package assembles (progress bar 0-100%)
# 8. Verify package preview appears
# 9. Click "Download Package" button
# 10. Verify complete PDF downloads with all forms
```

---

## 8. Troubleshooting

### 8.1 PDF Generation Fails

**Symptom**: API returns 500 error when generating form

**Possible Causes**:
1. Form template not found in S3
2. Missing field mappings in form_templates table
3. Required data missing from database
4. PDFBox library error

**Solutions**:
```bash
# Check pdf-service logs
docker-compose logs -f pdf-service

# Verify template exists in S3
mc ls local/munitax-templates/dublin/

# Verify template metadata in database
psql -h localhost -U postgres -d munitax
SELECT form_code, template_file_path, applicable_years 
FROM dublin.form_templates 
WHERE form_code = '27-EXT';

# Verify required data exists
SELECT * FROM dublin.extension_requests WHERE return_id = 'return-uuid-123';
```

### 8.2 Watermark Not Appearing

**Symptom**: Draft PDFs generated without watermark

**Solution**:
```bash
# Check isDraft and addWatermark flags in request
curl -X POST http://localhost:8080/api/v1/pdf-service/forms/generate \
  -d '{"formCode": "27-EXT", "isDraft": true, "addWatermark": true}' \
  ...

# Verify watermark service is running
docker-compose logs -f pdf-service | grep -i watermark

# Check PDFBox watermark configuration
# Edit: backend/pdf-service/src/main/resources/application.yml
# pdf.watermark.enabled: true
# pdf.watermark.text: "DRAFT - NOT FOR FILING"
```

### 8.3 Filing Package Too Large

**Symptom**: Package generation fails with "File size exceeds limit" error

**Solutions**:
```bash
# Check individual form sizes
curl -X GET "http://localhost:8080/api/v1/pdf-service/forms?returnId=return-uuid-123" \
  -H "Authorization: Bearer $JWT_TOKEN"

# Enable aggressive compression
curl -X POST http://localhost:8080/api/v1/pdf-service/packages/assemble \
  -d '{
    "optimizeFileSize": true,
    "compressionLevel": "MAXIMUM",
    "reduceImageQuality": true,
    "targetFileSizeMB": 10
  }' \
  ...

# Split into multiple packages if needed
curl -X POST http://localhost:8080/api/v1/pdf-service/packages/assemble \
  -d '{
    "packageType": "ORIGINAL_PART1",
    "includeFormIds": ["form-27-uuid", "form-27Y-uuid"]
  }' \
  ...
```

### 8.4 Database Migration Fails

**Symptom**: Flyway migration error on startup

**Solution**:
```bash
# Check migration status
psql -h localhost -U postgres -d munitax
SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC;

# Manually run pending migrations
cd backend/pdf-service
mvn flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:5432/munitax

# Repair failed migration
mvn flyway:repair

# Restart service
docker-compose restart pdf-service
```

---

## 9. Next Steps

After completing this quickstart:

1. **Read Specifications**: Review `/specs/8-business-form-library/spec.md` for complete functional requirements
2. **Review Data Model**: Study `/specs/8-business-form-library/data-model.md` for database schema details
3. **Explore Contracts**: Check `/specs/8-business-form-library/contracts/` for API endpoint specifications
4. **Run All Tests**: Execute full test suite with `mvn test` in pdf-service and tax-engine-service
5. **Generate Additional Forms**: Try generating Form 27-NOL, Form 27-W1, Form 27-Y
6. **Implement Custom Templates**: Upload your own form templates and configure field mappings
7. **Test E-filing Workflow**: Prepare forms for electronic submission (PDF/A format, digital signatures)

---

## 10. Helpful Commands Reference

```bash
# View all generated forms for a business
curl -X GET "http://localhost:8080/api/v1/pdf-service/forms?businessId=$BUSINESS_ID" \
  -H "Authorization: Bearer $JWT_TOKEN"

# Delete a draft form
curl -X DELETE "http://localhost:8080/api/v1/pdf-service/forms/$FORM_ID" \
  -H "Authorization: Bearer $JWT_TOKEN"

# Regenerate a form (creates new version)
curl -X POST "http://localhost:8080/api/v1/pdf-service/forms/$FORM_ID/regenerate" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{"reason": "Updated estimated tax amount"}'

# Download filing package
curl -X GET "http://localhost:8080/api/v1/pdf-service/packages/download/$PACKAGE_ID" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -o filing-package.pdf

# Submit filing package
curl -X POST "http://localhost:8080/api/v1/pdf-service/packages/$PACKAGE_ID/submit" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{"submissionMethod": "ELECTRONIC"}'

# View form generation metrics
curl -X GET "http://localhost:8080/api/v1/pdf-service/metrics/generation-times" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

---

**END OF QUICKSTART**
