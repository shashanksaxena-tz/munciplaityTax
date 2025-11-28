# Data Model: Business Form Library

**Feature**: Comprehensive Municipal Tax Form Generation System  
**Phase**: Phase 1 (Design & Contracts)  
**Date**: 2024-11-28

---

## Overview

This document defines the database schema for the business form library system. All entities are stored in the pdf-service and tax-engine-service databases with tenant-scoped access per Constitution II (Multi-Tenant Data Isolation).

**Database**: PostgreSQL 16  
**Schema**: Tenant-scoped (`dublin.*`, `columbus.*`)  
**Migration Tool**: Flyway  
**ORM**: Spring Data JPA with Hibernate  
**File Storage**: S3-compatible object storage with tenant-prefixed buckets

---

## Entity Relationship Diagram

```
┌─────────────────┐         ┌──────────────────────────┐
│   TaxReturn     │────────<│   GeneratedForm          │
│                 │         │                          │
│ - id (UUID)     │         │ - id (UUID)              │
│ - business_id   │         │ - return_id (FK)         │
│ - tax_year      │         │ - form_code              │
│ - status        │         │ - tax_year               │
└─────────────────┘         │ - version                │
                            │ - status                 │
        ↓                   │ - pdf_file_path          │
                            │ - xml_file_path          │
┌─────────────────┐         │ - is_watermarked         │
│  FormTemplate   │────────<│ - page_count             │
│                 │         │ - file_size_bytes        │
│ - id (UUID)     │         └──────────────────────────┘
│ - form_code     │                    │
│ - form_name     │                    │
│ - revision_date │                    │
│ - template_path │                    │ included in
│ - field_mappings│                    ↓
│ - validation    │         ┌──────────────────────────┐
└─────────────────┘         │   FilingPackage          │
                            │                          │
                            │ - id (UUID)              │
                            │ - return_id (FK)         │
                            │ - tax_year               │
                            │ - package_type           │
                            │ - included_forms[]       │
                            │ - package_pdf_path       │
                            │ - total_pages            │
                            │ - confirmation_number    │
                            └──────────────────────────┘
                                       │
                                       │
                                       ↓
                            ┌──────────────────────────┐
                            │   FormGenerationAudit    │
                            │                          │
                            │ - id (UUID)              │
                            │ - form_id (FK)           │
                            │ - action                 │
                            │ - actor_id               │
                            │ - timestamp              │
                            │ - old_value              │
                            │ - new_value              │
                            └──────────────────────────┘
```

---

## 1. FormTemplate Entity

**Purpose**: Stores metadata about official municipal tax form templates, including field mappings and validation rules.

**Functional Requirements**: FR-001, FR-002, FR-021 (Template Management)

### 1.1 Fields

| Field Name | Type | Constraints | Description |
|------------|------|-------------|-------------|
| `id` | UUID | PRIMARY KEY | Unique identifier |
| `tenant_id` | UUID | NOT NULL, FK → tenants(id) | Multi-tenant isolation (Constitution II) |
| `form_code` | VARCHAR(20) | NOT NULL | Form identifier: `27`, `27-EXT`, `27-ES`, `27-NOL`, `27-W1`, `27-Y`, `27-X`, `27-PA`, `27-AMD` |
| `form_name` | VARCHAR(100) | NOT NULL | Human-readable name: `"Extension Request"`, `"Estimated Tax Voucher"` |
| `description` | TEXT | NULL | Form description and purpose |
| `template_file_path` | VARCHAR(500) | NOT NULL | S3 path to blank PDF template |
| `revision_date` | DATE | NOT NULL | Form version date (e.g., Rev. 01/2024) |
| `applicable_years` | INTEGER[] | NOT NULL | Years this template is valid: `{2024, 2025}` |
| `field_mappings` | JSONB | NOT NULL | Map database fields to PDF form fields (research R6) |
| `validation_rules` | JSONB | NULL | Required fields, formats, cross-field validations |
| `page_count` | INTEGER | NOT NULL, CHECK (page_count > 0) | Number of pages in template |
| `supports_continuation` | BOOLEAN | DEFAULT FALSE | Can generate continuation sheets if data overflows |
| `has_payment_stub` | BOOLEAN | DEFAULT FALSE | Includes detachable payment voucher |
| `omb_number` | VARCHAR(50) | NULL | Office of Management and Budget control number |
| `instructions_url` | VARCHAR(500) | NULL | Link to form instructions |
| `category` | VARCHAR(50) | NOT NULL | `MAIN_RETURN`, `EXTENSION`, `ESTIMATED`, `SCHEDULE`, `WITHHOLDING`, `AMENDMENT` |
| `is_active` | BOOLEAN | DEFAULT TRUE | Is this template currently in use? |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Audit trail (Constitution III) |
| `created_by` | UUID | NOT NULL, FK → users(id) | User who uploaded template |
| `updated_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Last modification timestamp |

### 1.2 Field Mappings Structure (JSONB)

```json
{
  "form_fields": {
    "business_name": {
      "pdf_field_name": "topmostSubform[0].Page1[0].BusinessName[0]",
      "data_source": "business.legal_name",
      "max_length": 50,
      "required": true,
      "validation": "^[A-Za-z0-9\\s\\-\\.,']+$"
    },
    "fein": {
      "pdf_field_name": "topmostSubform[0].Page1[0].FEIN[0]",
      "data_source": "business.ein",
      "format": "XX-XXXXXXX",
      "required": true,
      "validation": "^\\d{2}-\\d{7}$"
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
  },
  "calculation_fields": {
    "balance_due": {
      "pdf_field_name": "topmostSubform[0].Page1[0].Line6_BalanceDue[0]",
      "formula": "estimated_tax - prior_payments - credits",
      "required": true
    }
  }
}
```

### 1.3 Indexes

```sql
CREATE INDEX idx_template_form_code ON form_templates(form_code);
CREATE INDEX idx_template_tenant ON form_templates(tenant_id);
CREATE INDEX idx_template_years ON form_templates USING GIN(applicable_years);
CREATE INDEX idx_template_active ON form_templates(is_active) WHERE is_active = TRUE;
CREATE INDEX idx_template_category ON form_templates(category);
```

### 1.4 Constraints

```sql
-- Form code must be valid municipal form
ALTER TABLE form_templates ADD CONSTRAINT check_form_code
    CHECK (form_code IN ('27', '27-EXT', '27-ES', '27-NOL', '27-W1', 
                         '27-Y', '27-X', '27-PA', '27-AMD'));

-- Applicable years must be sorted and non-empty
ALTER TABLE form_templates ADD CONSTRAINT check_applicable_years
    CHECK (array_length(applicable_years, 1) > 0);

-- Template path must be valid S3 URI
ALTER TABLE form_templates ADD CONSTRAINT check_template_path
    CHECK (template_file_path LIKE 's3://%' OR template_file_path LIKE 'file://%');
```

---

## 2. GeneratedForm Entity

**Purpose**: Tracks individual generated tax forms (PDFs) with versioning, status, and file paths.

**Functional Requirements**: FR-003 (Form Generation), FR-037 (Form History), FR-038 (Versioning)

### 2.1 Fields

| Field Name | Type | Constraints | Description |
|------------|------|-------------|-------------|
| `id` | UUID | PRIMARY KEY | Unique identifier |
| `tenant_id` | UUID | NOT NULL, FK → tenants(id) | Multi-tenant isolation |
| `return_id` | UUID | NOT NULL, FK → tax_returns(id) | Associated tax return |
| `template_id` | UUID | NOT NULL, FK → form_templates(id) | Template used for generation |
| `form_code` | VARCHAR(20) | NOT NULL | Form identifier (denormalized for queries) |
| `tax_year` | INTEGER | NOT NULL | Tax year |
| `version` | INTEGER | NOT NULL, DEFAULT 1 | Version number (increments with regeneration) |
| `supersedes_form_id` | UUID | NULL, FK → generated_forms(id) | Previous version if regenerated |
| `status` | VARCHAR(20) | NOT NULL, DEFAULT 'DRAFT' | `DRAFT`, `FINAL`, `SUBMITTED`, `AMENDED`, `SUPERSEDED` |
| `generated_date` | TIMESTAMP | NOT NULL, DEFAULT NOW() | When form was generated |
| `generated_by` | UUID | NOT NULL, FK → users(id) | User who generated form |
| `pdf_file_path` | VARCHAR(500) | NOT NULL | S3 path to generated PDF |
| `xml_file_path` | VARCHAR(500) | NULL | XML data file for e-filing (if applicable) |
| `is_watermarked` | BOOLEAN | DEFAULT TRUE | Whether PDF has DRAFT watermark |
| `is_fillable` | BOOLEAN | DEFAULT TRUE | PDF has editable fields vs flattened |
| `page_count` | INTEGER | NOT NULL, CHECK (page_count > 0) | Number of pages in generated PDF |
| `file_size_bytes` | BIGINT | NOT NULL, CHECK (file_size_bytes > 0) | PDF file size |
| `checksum` | VARCHAR(64) | NOT NULL | SHA-256 hash of PDF for integrity verification |
| `data_snapshot` | JSONB | NOT NULL | Complete data used for generation (data provenance - research R4) |
| `validation_errors` | JSONB | NULL | Validation errors if generation failed |
| `generation_duration_ms` | INTEGER | NULL | Time taken to generate PDF (performance tracking) |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Audit trail |
| `updated_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Last status change |

### 2.2 Data Snapshot Structure (JSONB)

```json
{
  "business": {
    "legal_name": "Acme Construction LLC",
    "ein": "12-3456789",
    "address": "123 Main St, Dublin, OH 43017"
  },
  "tax_return": {
    "tax_year": 2024,
    "taxable_income": 500000,
    "total_tax": 12500
  },
  "form_specific_data": {
    "estimated_tax_liability": 25000,
    "prior_payments": 5000,
    "extension_payment": 20000,
    "extension_reason": "AWAITING_K1S"
  },
  "generation_metadata": {
    "generated_at": "2024-04-10T15:30:00Z",
    "generated_by_user_id": "uuid-123",
    "template_id": "uuid-456",
    "template_revision": "2024-01"
  }
}
```

### 2.3 Indexes

```sql
CREATE INDEX idx_form_return ON generated_forms(return_id);
CREATE INDEX idx_form_tenant_year ON generated_forms(tenant_id, tax_year);
CREATE INDEX idx_form_code ON generated_forms(form_code);
CREATE INDEX idx_form_status ON generated_forms(status);
CREATE INDEX idx_form_version ON generated_forms(return_id, form_code, version DESC);
CREATE INDEX idx_form_supersedes ON generated_forms(supersedes_form_id) WHERE supersedes_form_id IS NOT NULL;
```

### 2.4 Constraints

```sql
-- Version must be positive
ALTER TABLE generated_forms ADD CONSTRAINT check_version
    CHECK (version > 0);

-- Superseded forms must have SUPERSEDED status
ALTER TABLE generated_forms ADD CONSTRAINT check_superseded_status
    CHECK ((supersedes_form_id IS NULL) OR 
           (status = 'SUPERSEDED'));

-- Final/Submitted forms cannot be watermarked
ALTER TABLE generated_forms ADD CONSTRAINT check_watermark_status
    CHECK ((status IN ('FINAL', 'SUBMITTED')) = (is_watermarked = FALSE));

-- PDF path must be valid S3 URI
ALTER TABLE generated_forms ADD CONSTRAINT check_pdf_path
    CHECK (pdf_file_path LIKE 's3://%');
```

---

## 3. FilingPackage Entity

**Purpose**: Represents a complete filing package containing multiple forms assembled into a single PDF.

**Functional Requirements**: FR-031 (Filing Package Assembly), FR-032 (PDF Bookmarks), FR-033 (File Size Optimization)

### 3.1 Fields

| Field Name | Type | Constraints | Description |
|------------|------|-------------|-------------|
| `id` | UUID | PRIMARY KEY | Unique identifier |
| `tenant_id` | UUID | NOT NULL, FK → tenants(id) | Multi-tenant isolation |
| `return_id` | UUID | NOT NULL, FK → tax_returns(id) | Associated tax return |
| `tax_year` | INTEGER | NOT NULL | Tax year |
| `package_type` | VARCHAR(20) | NOT NULL | `ORIGINAL`, `AMENDED`, `EXTENSION`, `QUARTERLY` |
| `created_date` | TIMESTAMP | NOT NULL, DEFAULT NOW() | When package was assembled |
| `created_by` | UUID | NOT NULL, FK → users(id) | User who created package |
| `included_forms` | UUID[] | NOT NULL | Array of generated_forms.id in order |
| `total_pages` | INTEGER | NOT NULL, CHECK (total_pages > 0) | Total page count across all forms |
| `package_pdf_path` | VARCHAR(500) | NOT NULL | S3 path to combined PDF |
| `file_size_bytes` | BIGINT | NOT NULL | Total file size |
| `table_of_contents` | JSONB | NOT NULL | TOC with form names and page numbers |
| `bookmarks` | JSONB | NOT NULL | PDF bookmark structure |
| `submission_date` | TIMESTAMP | NULL | When submitted to municipality |
| `confirmation_number` | VARCHAR(50) | NULL | Submission tracking number |
| `status` | VARCHAR(20) | NOT NULL, DEFAULT 'DRAFT' | `DRAFT`, `READY`, `SUBMITTED`, `ACCEPTED`, `REJECTED` |
| `validation_summary` | JSONB | NULL | Cross-form validation results |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Audit trail |
| `updated_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Last modification |

### 3.2 Table of Contents Structure (JSONB)

```json
{
  "toc_entries": [
    {
      "form_code": "27",
      "form_name": "Municipal Business Tax Return",
      "start_page": 1,
      "end_page": 3,
      "form_id": "uuid-123"
    },
    {
      "form_code": "27-Y",
      "form_name": "Apportionment Schedule",
      "start_page": 4,
      "end_page": 5,
      "form_id": "uuid-456"
    },
    {
      "form_code": "27-NOL",
      "form_name": "Net Operating Loss Schedule",
      "start_page": 6,
      "end_page": 6,
      "form_id": "uuid-789"
    }
  ],
  "supporting_documents": [
    {
      "document_name": "Federal Form 1120",
      "start_page": 7,
      "end_page": 21
    }
  ]
}
```

### 3.3 Bookmarks Structure (JSONB)

```json
{
  "bookmarks": [
    {
      "title": "Form 27 - Main Return",
      "page": 1,
      "level": 0
    },
    {
      "title": "Schedule Y - Apportionment",
      "page": 4,
      "level": 0
    },
    {
      "title": "Property Factor",
      "page": 4,
      "level": 1
    },
    {
      "title": "Payroll Factor",
      "page": 4,
      "level": 1
    },
    {
      "title": "Sales Factor",
      "page": 5,
      "level": 1
    }
  ]
}
```

### 3.4 Indexes

```sql
CREATE INDEX idx_package_return ON filing_packages(return_id);
CREATE INDEX idx_package_tenant_year ON filing_packages(tenant_id, tax_year);
CREATE INDEX idx_package_status ON filing_packages(status);
CREATE INDEX idx_package_submission ON filing_packages(submission_date) WHERE submission_date IS NOT NULL;
CREATE INDEX idx_package_forms ON filing_packages USING GIN(included_forms);
```

### 3.5 Constraints

```sql
-- Must include at least one form
ALTER TABLE filing_packages ADD CONSTRAINT check_forms_not_empty
    CHECK (array_length(included_forms, 1) > 0);

-- Submitted packages must have submission date
ALTER TABLE filing_packages ADD CONSTRAINT check_submission_date
    CHECK ((status != 'SUBMITTED') OR (submission_date IS NOT NULL));

-- File size must be reasonable (<20MB)
ALTER TABLE filing_packages ADD CONSTRAINT check_file_size
    CHECK (file_size_bytes <= 20971520);  -- 20MB in bytes
```

---

## 4. FormGenerationAudit Entity

**Purpose**: Immutable audit log tracking all form generation, regeneration, and status changes.

**Functional Requirements**: FR-008 (Audit Trail), FR-037 (Form History)

### 4.1 Fields

| Field Name | Type | Constraints | Description |
|------------|------|-------------|-------------|
| `id` | UUID | PRIMARY KEY | Unique identifier |
| `tenant_id` | UUID | NOT NULL, FK → tenants(id) | Multi-tenant isolation |
| `form_id` | UUID | NULL, FK → generated_forms(id) | Form being audited (NULL for failed generations) |
| `package_id` | UUID | NULL, FK → filing_packages(id) | Package being audited (if applicable) |
| `action` | VARCHAR(50) | NOT NULL | `GENERATED`, `REGENERATED`, `STATUS_CHANGED`, `DOWNLOADED`, `WATERMARK_REMOVED`, `SUBMITTED` |
| `actor_id` | UUID | NOT NULL, FK → users(id) | User who performed action |
| `actor_role` | VARCHAR(50) | NOT NULL | User role: `FILER`, `PREPARER`, `ADMIN`, `SYSTEM` |
| `timestamp` | TIMESTAMP | NOT NULL, DEFAULT NOW() | When action occurred |
| `old_value` | JSONB | NULL | Previous state (for STATUS_CHANGED) |
| `new_value` | JSONB | NOT NULL | New state or action details |
| `reason` | TEXT | NULL | User-provided reason for action |
| `ip_address` | INET | NULL | User's IP address |
| `user_agent` | TEXT | NULL | User's browser/client |

### 4.2 New Value Structure (JSONB)

```json
{
  "action_details": {
    "form_code": "27-EXT",
    "version": 2,
    "status_changed_from": "DRAFT",
    "status_changed_to": "FINAL",
    "watermark_removed": true,
    "reason": "Reviewed and approved for submission"
  },
  "generation_metrics": {
    "duration_ms": 1250,
    "page_count": 2,
    "file_size_bytes": 87654
  }
}
```

### 4.3 Indexes

```sql
CREATE INDEX idx_audit_form ON form_generation_audit(form_id);
CREATE INDEX idx_audit_package ON form_generation_audit(package_id);
CREATE INDEX idx_audit_tenant_time ON form_generation_audit(tenant_id, timestamp DESC);
CREATE INDEX idx_audit_actor ON form_generation_audit(actor_id);
CREATE INDEX idx_audit_action ON form_generation_audit(action);
```

### 4.4 Constraints

```sql
-- Must have either form_id or package_id
ALTER TABLE form_generation_audit ADD CONSTRAINT check_audit_target
    CHECK (form_id IS NOT NULL OR package_id IS NOT NULL);

-- Audit records are immutable (no updates, no deletes)
-- Implemented via database triggers and application-level enforcement
```

---

## 5. Additional Supporting Entities

### 5.1 ExtensionRequest Entity

**Purpose**: Stores data specific to Form 27-EXT extension requests.

| Field Name | Type | Description |
|------------|------|-------------|
| `id` | UUID | PRIMARY KEY |
| `return_id` | UUID | FK → tax_returns(id) |
| `estimated_tax_liability` | DECIMAL(15,2) | Estimated total tax for year |
| `prior_payments` | DECIMAL(15,2) | Payments already made |
| `extension_payment` | DECIMAL(15,2) | Amount being paid with extension |
| `extension_reason` | VARCHAR(100) | `MORE_TIME_NEEDED`, `AWAITING_K1S`, `OTHER` |
| `original_due_date` | DATE | April 15 |
| `extended_due_date` | DATE | October 15 |
| `granted` | BOOLEAN | Whether extension was granted |

### 5.2 EstimatedTaxVoucher Entity

**Purpose**: Stores data for quarterly estimated tax vouchers (Form 27-ES).

| Field Name | Type | Description |
|------------|------|-------------|
| `id` | UUID | PRIMARY KEY |
| `return_id` | UUID | FK → tax_returns(id) |
| `quarter` | INTEGER | 1, 2, 3, 4 |
| `due_date` | DATE | Apr 15, Jun 15, Sep 15, Jan 15 |
| `payment_amount` | DECIMAL(15,2) | Amount due for quarter |
| `payment_status` | VARCHAR(20) | `GENERATED`, `PAID`, `LATE`, `OVERPAID` |
| `payment_date` | DATE | Actual payment date |
| `transaction_id` | VARCHAR(100) | Payment gateway transaction ID |

### 5.3 NOLScheduleData Entity

**Purpose**: Stores data for Form 27-NOL Net Operating Loss schedules.

| Field Name | Type | Description |
|------------|------|-------------|
| `id` | UUID | PRIMARY KEY |
| `return_id` | UUID | FK → tax_returns(id) |
| `nol_vintages` | JSONB | Array of NOL carryforwards by year |
| `current_year_nol` | DECIMAL(15,2) | New NOL generated this year |
| `nol_deduction_claimed` | DECIMAL(15,2) | NOL used to offset income |
| `remaining_carryforward` | DECIMAL(15,2) | NOL available for future years |
| `eighty_percent_limit` | DECIMAL(15,2) | 80% of taxable income limit |

---

## 6. Flyway Migration Scripts

### 6.1 V1__create_form_templates.sql

```sql
CREATE TABLE form_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    form_code VARCHAR(20) NOT NULL,
    form_name VARCHAR(100) NOT NULL,
    description TEXT,
    template_file_path VARCHAR(500) NOT NULL,
    revision_date DATE NOT NULL,
    applicable_years INTEGER[] NOT NULL,
    field_mappings JSONB NOT NULL,
    validation_rules JSONB,
    page_count INTEGER NOT NULL CHECK (page_count > 0),
    supports_continuation BOOLEAN DEFAULT FALSE,
    has_payment_stub BOOLEAN DEFAULT FALSE,
    omb_number VARCHAR(50),
    instructions_url VARCHAR(500),
    category VARCHAR(50) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL REFERENCES users(id),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT check_form_code CHECK (form_code IN ('27', '27-EXT', '27-ES', '27-NOL', '27-W1', '27-Y', '27-X', '27-PA', '27-AMD')),
    CONSTRAINT check_applicable_years CHECK (array_length(applicable_years, 1) > 0)
);

CREATE INDEX idx_template_form_code ON form_templates(form_code);
CREATE INDEX idx_template_tenant ON form_templates(tenant_id);
CREATE INDEX idx_template_years ON form_templates USING GIN(applicable_years);
CREATE INDEX idx_template_active ON form_templates(is_active) WHERE is_active = TRUE;
```

### 6.2 V2__create_generated_forms.sql

```sql
CREATE TABLE generated_forms (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    return_id UUID NOT NULL REFERENCES tax_returns(id),
    template_id UUID NOT NULL REFERENCES form_templates(id),
    form_code VARCHAR(20) NOT NULL,
    tax_year INTEGER NOT NULL,
    version INTEGER NOT NULL DEFAULT 1 CHECK (version > 0),
    supersedes_form_id UUID REFERENCES generated_forms(id),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    generated_date TIMESTAMP NOT NULL DEFAULT NOW(),
    generated_by UUID NOT NULL REFERENCES users(id),
    pdf_file_path VARCHAR(500) NOT NULL,
    xml_file_path VARCHAR(500),
    is_watermarked BOOLEAN DEFAULT TRUE,
    is_fillable BOOLEAN DEFAULT TRUE,
    page_count INTEGER NOT NULL CHECK (page_count > 0),
    file_size_bytes BIGINT NOT NULL CHECK (file_size_bytes > 0),
    checksum VARCHAR(64) NOT NULL,
    data_snapshot JSONB NOT NULL,
    validation_errors JSONB,
    generation_duration_ms INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT check_pdf_path CHECK (pdf_file_path LIKE 's3://%')
);

CREATE INDEX idx_form_return ON generated_forms(return_id);
CREATE INDEX idx_form_tenant_year ON generated_forms(tenant_id, tax_year);
CREATE INDEX idx_form_status ON generated_forms(status);
CREATE INDEX idx_form_version ON generated_forms(return_id, form_code, version DESC);
```

### 6.3 V3__create_filing_packages.sql

```sql
CREATE TABLE filing_packages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    return_id UUID NOT NULL REFERENCES tax_returns(id),
    tax_year INTEGER NOT NULL,
    package_type VARCHAR(20) NOT NULL,
    created_date TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL REFERENCES users(id),
    included_forms UUID[] NOT NULL,
    total_pages INTEGER NOT NULL CHECK (total_pages > 0),
    package_pdf_path VARCHAR(500) NOT NULL,
    file_size_bytes BIGINT NOT NULL CHECK (file_size_bytes <= 20971520),
    table_of_contents JSONB NOT NULL,
    bookmarks JSONB NOT NULL,
    submission_date TIMESTAMP,
    confirmation_number VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    validation_summary JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT check_forms_not_empty CHECK (array_length(included_forms, 1) > 0)
);

CREATE INDEX idx_package_return ON filing_packages(return_id);
CREATE INDEX idx_package_status ON filing_packages(status);
CREATE INDEX idx_package_forms ON filing_packages USING GIN(included_forms);
```

### 6.4 V4__create_form_generation_audit.sql

```sql
CREATE TABLE form_generation_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    form_id UUID REFERENCES generated_forms(id),
    package_id UUID REFERENCES filing_packages(id),
    action VARCHAR(50) NOT NULL,
    actor_id UUID NOT NULL REFERENCES users(id),
    actor_role VARCHAR(50) NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT NOW(),
    old_value JSONB,
    new_value JSONB NOT NULL,
    reason TEXT,
    ip_address INET,
    user_agent TEXT,
    CONSTRAINT check_audit_target CHECK (form_id IS NOT NULL OR package_id IS NOT NULL)
);

CREATE INDEX idx_audit_form ON form_generation_audit(form_id);
CREATE INDEX idx_audit_tenant_time ON form_generation_audit(tenant_id, timestamp DESC);
CREATE INDEX idx_audit_action ON form_generation_audit(action);

-- Make audit log immutable
CREATE OR REPLACE FUNCTION prevent_audit_modification()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Audit records are immutable';
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER prevent_audit_update
    BEFORE UPDATE ON form_generation_audit
    FOR EACH ROW EXECUTE FUNCTION prevent_audit_modification();

CREATE TRIGGER prevent_audit_delete
    BEFORE DELETE ON form_generation_audit
    FOR EACH ROW EXECUTE FUNCTION prevent_audit_modification();
```

---

## 7. Data Access Patterns

### 7.1 Generate Form 27-EXT

```java
// Step 1: Find applicable template
FormTemplate template = formTemplateRepository
    .findByFormCodeAndTaxYear("27-EXT", 2024);

// Step 2: Fetch data from multiple sources
Business business = businessRepository.findById(businessId);
TaxReturn taxReturn = taxReturnRepository.findById(returnId);
ExtensionRequest extension = extensionRepository.findByReturnId(returnId);

// Step 3: Map data to PDF fields using field_mappings
Map<String, Object> formData = fieldMapper.mapData(
    template.getFieldMappings(),
    business, taxReturn, extension
);

// Step 4: Generate PDF
byte[] pdfBytes = pdfGenerator.fillTemplate(
    template.getTemplateFilePath(),
    formData,
    isDraft ? Watermark.DRAFT : Watermark.NONE
);

// Step 5: Save to S3
String s3Path = s3Service.upload(pdfBytes, buildS3Key(businessId, "27-EXT"));

// Step 6: Create GeneratedForm record
GeneratedForm form = new GeneratedForm();
form.setReturnId(returnId);
form.setFormCode("27-EXT");
form.setPdfFilePath(s3Path);
form.setDataSnapshot(buildDataSnapshot(business, taxReturn, extension));
generatedFormRepository.save(form);

// Step 7: Audit log
auditService.logFormGeneration(form, currentUser);
```

### 7.2 Assemble Filing Package

```java
// Step 1: Get all forms for return
List<GeneratedForm> forms = generatedFormRepository
    .findByReturnIdAndStatus(returnId, FormStatus.FINAL);

// Step 2: Validate cross-form consistency
ValidationResult validation = crossFormValidator.validate(forms);
if (!validation.isValid()) {
    throw new ValidationException(validation.getErrors());
}

// Step 3: Load PDF bytes for all forms
List<byte[]> pdfDocuments = forms.stream()
    .map(form -> s3Service.download(form.getPdfFilePath()))
    .collect(Collectors.toList());

// Step 4: Merge PDFs with bookmarks
byte[] mergedPdf = pdfMerger.merge(pdfDocuments, buildBookmarks(forms));

// Step 5: Add table of contents
byte[] finalPdf = pdfMerger.addTableOfContents(mergedPdf, buildTOC(forms));

// Step 6: Optimize file size
byte[] optimizedPdf = pdfOptimizer.compress(finalPdf, targetSize: 10MB);

// Step 7: Upload package
String packagePath = s3Service.upload(optimizedPdf, buildPackageKey(returnId));

// Step 8: Create FilingPackage record
FilingPackage package = new FilingPackage();
package.setReturnId(returnId);
package.setIncludedForms(forms.stream().map(GeneratedForm::getId).toList());
package.setPackagePdfPath(packagePath);
filingPackageRepository.save(package);
```

---

## 8. Performance Considerations

### 8.1 Caching Strategy

- **Template Metadata**: Cache FormTemplate records (field mappings, validation rules) in Redis for 24 hours
- **Data Snapshots**: Cache business/return data for 1 hour to avoid repeated database queries during multi-form generation
- **PDF Templates**: Cache blank PDF templates in memory (file system cache) to avoid S3 download overhead

### 8.2 Batch Operations

- **Filing Package Generation**: Use asynchronous job queue for packages >5 forms or >50 pages
- **Bulk Form Generation**: Generate multiple forms concurrently (e.g., all 4 estimated tax vouchers in parallel)

### 8.3 Storage Optimization

- **PDF Compression**: Use PDF/A-2 format with JPEG compression for images, reduce to 150 DPI for scanned documents
- **Archival Strategy**: Move forms older than 3 years to S3 Glacier for cost savings (7-year retention required)

---

## 9. Security & Compliance

### 9.1 Access Control

- **Row-Level Security**: All queries automatically filter by tenant_id via JPA @Filter
- **Form Access**: Users can only access forms for businesses they're authorized to view (enforced in application layer)

### 9.2 Data Retention

- **Audit Logs**: Retain form_generation_audit records for 7 years minimum (IRS requirement)
- **Generated Forms**: Retain PDF files for 7 years minimum
- **Superseded Forms**: Never delete superseded forms (keep all versions for audit trail)

### 9.3 PII Protection

- **Redaction**: Implement PII redaction for forms shared externally (SSNs, account numbers)
- **Encryption**: Store PDFs with S3 server-side encryption (SSE-S3 or SSE-KMS)

---

## 10. Testing Data

### 10.1 Sample FormTemplate

```sql
INSERT INTO form_templates (tenant_id, form_code, form_name, template_file_path, revision_date, applicable_years, field_mappings, page_count, category)
VALUES (
    'dublin-tenant-uuid',
    '27-EXT',
    'Extension Request',
    's3://munitax-templates/dublin/27-EXT-2024.pdf',
    '2024-01-01',
    ARRAY[2024, 2025],
    '{
      "form_fields": {
        "business_name": {"pdf_field_name": "BusinessName", "data_source": "business.legal_name", "required": true},
        "fein": {"pdf_field_name": "FEIN", "data_source": "business.ein", "required": true}
      }
    }',
    2,
    'EXTENSION'
);
```

### 10.2 Sample GeneratedForm

```sql
INSERT INTO generated_forms (tenant_id, return_id, template_id, form_code, tax_year, pdf_file_path, data_snapshot, page_count, file_size_bytes, checksum)
VALUES (
    'dublin-tenant-uuid',
    'return-uuid-123',
    'template-uuid-456',
    '27-EXT',
    2024,
    's3://munitax-forms/dublin/2024/return-123/27-EXT-v1.pdf',
    '{"business": {"legal_name": "Acme LLC", "ein": "12-3456789"}, "extension": {"estimated_tax": 25000}}',
    2,
    87654,
    'sha256-hash-here'
);
```

---

**END OF DATA MODEL**
