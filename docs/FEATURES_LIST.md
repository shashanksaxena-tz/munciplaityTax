# MuniTax Features List

## Overview

This document provides a comprehensive list of features in the MuniTax system, organized by functional area with implementation status.

---

## Feature Status Legend

| Status | Description |
|--------|-------------|
| ✅ **IMPLEMENTED** | Feature is complete and working |
| ⚠️ **PARTIAL/LIMITED** | Feature exists but has significant limitations or gaps |
| 🚧 **IN PROGRESS** | Feature is partially implemented |
| ⏳ **PLANNED** | Feature is planned but not started |
| ❌ **NOT PLANNED** | Feature is not in current roadmap |

---

## 1. Tax Filing Features

### Individual Tax Filing

| Feature | Status | Description |
|---------|--------|-------------|
| W-2 Form Processing | ✅ | Process multiple W-2 forms with employer information |
| W-2 Qualifying Wages Rules | ✅ | 4 configurable rules (Highest, Box 1, Box 5, Box 18) |
| 1099-NEC Processing | ✅ | Non-employee compensation income |
| 1099-MISC Processing | ✅ | Miscellaneous income |
| W-2G Processing | ✅ | Gambling winnings |
| Schedule C | ✅ | Business profit/loss for sole proprietors |
| Schedule E | ✅ | Rental and partnership income |
| Schedule F | ✅ | Farm income |
| Federal 1040 Reconciliation | ✅ | Cross-reference with federal return |
| Local 1040/1040EZ | ✅ | Dublin municipal tax forms |
| Schedule Y Credits | ✅ | Credits for taxes paid to other cities |
| Discrepancy Detection | ✅ | Automatic validation with 15+ rules |
| Whole-Dollar Rounding | ✅ | Optional rounding configuration |
| Multiple W-2 Support | ✅ | Handle multiple employers |

### Business Tax Filing

| Feature | Status | Description |
|---------|--------|-------------|
| Schedule X (Reconciliation) | ⚠️ | Partially Complete - UI has 31 fields, backend supports 29 fields (originally 6 fields, 22% complete) |
| Schedule Y (Allocation) | ✅ | 3-factor apportionment formula |
| NOL Carryforward | ✅ | Net Operating Loss tracking with 50% cap |
| W-1 Withholding Filing | ✅ | Employer withholding returns |
| W-3 Reconciliation | ⚠️ | Backend only with mock implementation - fetchW1FilingsForYear() returns zero amounts |
| Form 27 (Net Profits) | ✅ | Business net profits return |
| Multi-State Apportionment | ⏳ | Property/Payroll/Sales allocation |
| JEDD Zone Support | ⏳ | Joint Economic Development Districts |
| Consolidated Returns | ⏳ | Affiliated group filing |
| Estimated Tax | 🚧 | Quarterly estimated payments |

### Filing Frequencies

| Frequency | Status | Description |
|-----------|--------|-------------|
| Annual | ✅ | Once per year filing |
| Quarterly | ✅ | Q1, Q2, Q3, Q4 periods |
| Monthly | ✅ | M01-M12 periods |
| Semi-Monthly | ✅ | SM01-SM24 periods |
| Daily | ✅ | Daily deposit filers |

---

## 2. Document Processing Features

### AI Document Extraction

| Feature | Status | Description |
|---------|--------|-------------|
| PDF Document Upload | ✅ | Upload PDF tax documents |
| Image Document Upload | ✅ | Upload scanned images |
| Gemini AI Integration | ✅ | Google Generative AI extraction |
| Real-time Progress Updates | ✅ | Streaming extraction progress |
| Confidence Scoring | ✅ | Per-field confidence levels |
| Multi-Form Detection | ✅ | Detect multiple forms in document |
| Profile Extraction | ✅ | Extract taxpayer name/SSN/address |
| Form Type Detection | ✅ | Automatic form classification |

### Supported Forms

| Form | Status | Description |
|------|--------|-------------|
| W-2 | ✅ | Wage and Tax Statement |
| W-2G | ✅ | Gambling Winnings |
| 1099-NEC | ✅ | Non-Employee Compensation |
| 1099-MISC | ✅ | Miscellaneous Income |
| Schedule C | ✅ | Profit or Loss from Business |
| Schedule E | ✅ | Rental/Partnership Income |
| Schedule F | ✅ | Farm Income |
| Federal 1040 | ✅ | U.S. Individual Tax Return |
| Dublin 1040 | ✅ | Municipal Tax Return |
| Form 1120 | ✅ | Corporate Tax Return |
| Form 1065 | ✅ | Partnership Return |
| Form W-1 | ✅ | Withholding Return |

---

## 3. Validation & Discrepancy Features

### Validation Rules

| Rule | Status | Description |
|------|--------|-------------|
| FR-001: W-2 Box 18 vs Box 1 | ✅ | Wage comparison within 20% |
| FR-002: Withholding Rate Check | ✅ | Rate between 0-3.0% |
| FR-003: Duplicate W-2 Detection | ✅ | Same EIN and wage detection |
| FR-004: Employer Locality Check | ✅ | Dublin jurisdiction validation |
| FR-006: Schedule C Estimated Tax | ✅ | High income warning |
| FR-007: Rental Property Count | ✅ | Property data completeness |
| FR-008: Rental Property Location | ✅ | Dublin jurisdiction check |
| FR-009: Passive Loss Limitation | ✅ | AGI threshold check |
| FR-014: Credit Limit Check | ✅ | Credits vs liability validation |
| FR-017: Federal AGI Reconciliation | ✅ | Federal vs local income |
| FR-019: Federal Wages vs W-2s | ✅ | 1040 Line 1 reconciliation |

### Discrepancy Severity Levels

| Level | Status | Description |
|-------|--------|-------------|
| HIGH | ✅ | Blocks filing, requires resolution |
| MEDIUM | ✅ | Should review, may file anyway |
| LOW | ✅ | Informational, no action required |

---

## 4. Auditor Workflow Features

### Queue Management

| Feature | Status | Description |
|---------|--------|-------------|
| Submission Queue | ✅ | Prioritized list of pending returns |
| Status Filtering | ✅ | Filter by PENDING, IN_REVIEW, etc. |
| Priority Filtering | ✅ | Filter by HIGH, MEDIUM, LOW |
| Risk Score Display | ✅ | Show calculated risk score |
| Days in Queue | ✅ | Track queue aging |
| Pagination | ✅ | Page through large queues |
| Sorting | ✅ | Sort by various fields |

### Review Features

| Feature | Status | Description |
|---------|--------|-------------|
| Auditor Assignment | ✅ | Assign returns to auditors |
| Reassignment | ✅ | Transfer to different auditor |
| Split-Screen Review | ❌ | NOT STARTED - No code exists for PDF + extracted data side by side |
| Audit Report View | ✅ | Risk assessment and flags |
| Taxpayer History | ❌ | NOT STARTED - No code exists for prior year comparison |
| Document Requests | ⚠️ | BACKEND ONLY - Backend model and API exist, but no UI component |
| Document Tracking | ✅ | Track request status |

### Decision Actions

| Feature | Status | Description |
|---------|--------|-------------|
| Approve Return | ✅ | E-signature approval |
| Reject Return | ✅ | Detailed rejection reason |
| Request Documentation | ✅ | Request specific documents |
| Priority Override | ✅ | Change return priority |
| Bulk Actions | ⏳ | Approve/reject multiple |

### Audit Trail

| Feature | Status | Description |
|---------|--------|-------------|
| Immutable Logging | ✅ | Append-only audit entries |
| Digital Signatures | ✅ | SHA-256 signature hashing |
| IP Address Tracking | ✅ | Log user IP addresses |
| User Agent Logging | ✅ | Log browser/client info |
| Event Types | ✅ | 15+ tracked event types |
| 7-Year Retention | ✅ | IRS compliance retention |

### Risk Scoring

| Feature | Status | Description |
|---------|--------|-------------|
| Automated Risk Score | ✅ | 0-100 risk calculation |
| Year-over-Year Variance | 🚧 | Compare to prior years |
| Industry Benchmarks | ⏳ | Compare to similar businesses |
| Pattern Analysis | 🚧 | Detect unusual patterns |
| Anomaly Detection | 🚧 | Statistical outlier detection |

---

## 5. Rule Engine Features

### Rule Configuration

| Feature | Status | Description |
|---------|--------|-------------|
| Dynamic Rule Loading | ❌ | NOT WORKING - Rules stored in database but not applied in calculations |
| Temporal Effective Dating | ✅ | Start/end date ranges |
| Multi-Tenant Rules | ✅ | Per-municipality configuration |
| Rule Versioning | ✅ | Track rule changes |
| Rule Categories | ✅ | 8 categories supported |
| Rule Value Types | ✅ | Number, Percentage, Formula, etc. |

### Rule Categories

| Category | Status | Description |
|----------|--------|-------------|
| TAX_RATES | ✅ | Municipal tax rates |
| INCOME_INCLUSION | ✅ | What counts as taxable |
| DEDUCTIONS | ✅ | Allowed deductions |
| PENALTIES | ✅ | Penalty calculations |
| FILING | ✅ | Filing requirements |
| ALLOCATION | ✅ | Apportionment formulas |
| WITHHOLDING | ✅ | Employer withholding |
| VALIDATION | ✅ | Data quality rules |

### Rule Approval Workflow

| Feature | Status | Description |
|---------|--------|-------------|
| Pending Status | ✅ | New rules start pending |
| Approval Process | ✅ | Manager approval required |
| Rejection Process | ✅ | With rejection reason |
| Change Logging | ✅ | All changes tracked |
| Rollback | 🚧 | Revert to prior version |

---

## 6. Payment Features

### Payment Processing

| Feature | Status | Description |
|---------|--------|-------------|
| Mock Payment Provider | ✅ | Development/testing payments |
| Credit Card Payments | ⏳ | Real payment gateway |
| ACH Payments | ⏳ | Bank transfer |
| Payment Confirmation | ✅ | Confirmation numbers |
| Receipt Generation | ❌ | NOT STARTED - No code exists for payment receipts |

### Ledger Management

| Feature | Status | Description |
|---------|--------|-------------|
| Double-Entry Ledger | ✅ | Debit/credit accounting |
| Journal Entries | ✅ | Transaction recording |
| Account Balances | ✅ | Running balance tracking |
| Trial Balance | ✅ | Balance verification |
| Account Statements | ✅ | Taxpayer statements |
| Reconciliation | ✅ | Two-way reconciliation |

### Penalty & Interest

| Feature | Status | Description |
|---------|--------|-------------|
| Late Filing Penalty | ✅ | 5% per month, max 25% |
| Underpayment Penalty | ✅ | 15% of underpayment |
| Interest Calculation | ✅ | 7% annual rate |
| Safe Harbor Check | ✅ | 90% rule validation |
| Minimum Penalty | ✅ | $50 minimum |
| Penalty Abatement | ⏳ | First-time forgiveness |

---

## 7. PDF Generation Features

### Form Generation

| Feature | Status | Description |
|---------|--------|-------------|
| Dublin 1040 PDF | ✅ | Individual tax return |
| Tax Summary Report | ✅ | Calculation breakdown |
| Form Library | ⏳ | All municipal forms |
| Filing Package | ⏳ | Complete submission packet |

### PDF Features

| Feature | Status | Description |
|---------|--------|-------------|
| Apache PDFBox | ✅ | PDF generation engine |
| Form Field Population | ✅ | Auto-fill calculated data |
| Signature Section | ✅ | E-signature area |
| Amendment Marking | ✅ | Amendment status display |
| Download Endpoint | ✅ | Direct PDF download |

---

## 8. Session Management Features

### Session Storage

| Feature | Status | Description |
|---------|--------|-------------|
| PostgreSQL Persistence | ✅ | Durable session storage |
| Session States | ✅ | DRAFT, IN_PROGRESS, etc. |
| JSON Data Storage | ✅ | Flexible form storage |
| Session Type | ✅ | INDIVIDUAL, BUSINESS |
| Automatic Timestamps | ✅ | Created, modified, submitted |

### Session Operations

| Feature | Status | Description |
|---------|--------|-------------|
| Create Session | ✅ | Start new return |
| Update Session | ✅ | Save progress |
| Delete Session | ✅ | Remove draft |
| Query Sessions | ✅ | List user sessions |
| Auto-Save | ⏳ | Periodic saving |
| Version History | ⏳ | Track changes |

---

## 9. Multi-Tenancy Features

### Tenant Management

| Feature | Status | Description |
|---------|--------|-------------|
| Schema-per-Tenant | ✅ | Database isolation |
| Tenant Configuration | ✅ | Per-tenant settings |
| Tenant Switching | 🚧 | Admin capability |
| Tenant Branding | ⏳ | Custom logos/colors |
| Tenant-Specific Rules | ✅ | Custom tax rules |

### Address Validation

| Feature | Status | Description |
|---------|--------|-------------|
| Dublin ZIP Validation | ✅ | 43016, 43017, 43065 |
| Format Validation | ✅ | Street, city, state, ZIP |
| Verification Status | ✅ | VERIFIED, UNVERIFIED, etc. |
| Ohio Cities List | ✅ | Comprehensive city list |

---

## 10. Infrastructure Features

### Service Discovery

| Feature | Status | Description |
|---------|--------|-------------|
| Eureka Registry | ✅ | Service registration |
| Health Monitoring | ✅ | Service health checks |
| Load Balancing | ✅ | Client-side load balancing |
| Self-Preservation | ✅ | Network partition handling |

### Observability

| Feature | Status | Description |
|---------|--------|-------------|
| Distributed Tracing | ✅ | Zipkin integration |
| Request Correlation | ✅ | Trace ID propagation |
| Actuator Endpoints | ✅ | Health and metrics |
| Structured Logging | ✅ | JSON log format |

### Deployment

| Feature | Status | Description |
|---------|--------|-------------|
| Docker Containerization | ✅ | All services containerized |
| Docker Compose | ✅ | Single-command deployment |
| Environment Configuration | ✅ | Environment variables |
| Volume Persistence | ✅ | Data persistence |

---

## Feature Roadmap Summary

### Implemented (Phase 1-3)
- Individual Tax Calculator
- Business Tax Calculator
- Gemini AI Extraction
- Session Management
- PDF Generation
- Address Validation
- Basic Authentication
- Microservices Infrastructure
- Rule Engine (Basic)
- Auditor Workflow
- Ledger System

### In Progress
- Schedule X Expansion (UI has 31 fields, originally 6 fields)
- W-3 Reconciliation (Backend only, mock implementation)
- Advanced Risk Scoring
- Dynamic Rule Loading (Rules not applied in calculations)

### Planned (Phase 4+)
- Multi-State Apportionment
- JEDD Zone Support
- Consolidated Returns
- Payment Gateway Integration
- Mobile Application
- Advanced Analytics
- Bulk Actions
- Auto-Save

### Not Started
- Split-Screen Auditor Review
- Taxpayer History (Prior Year Comparison)
- Receipt Generation

---

## Known Limitations

This section documents incomplete features and their current status:

### Schedule X (Reconciliation)
- **Status**: ⚠️ Partially Complete
- **Current State**: UI has 31 fields defined, backend supports 29 fields
- **Original State**: 6 fields (22% complete)
- **Limitation**: While the UI and data models support comprehensive M-1 reconciliation, the original implementation only covered 6 basic fields

### Dynamic Rule Loading
- **Status**: ❌ NOT WORKING
- **Current State**: Rules are stored in database with full infrastructure (temporal dating, versioning, approval workflow)
- **Limitation**: Rules are not being applied during tax calculations - calculations use hardcoded values instead of database rules
- **Impact**: Changes to tax rules in the admin UI do not affect calculated tax amounts

### W-3 Reconciliation
- **Status**: ⚠️ BACKEND ONLY (Mock Implementation)
- **Current State**: Backend models, services, and APIs exist
- **Limitation**: `fetchW1FilingsForYear()` method returns zero amounts (mock implementation)
- **Impact**: All reconciliations show incorrect discrepancies; integration with tax-engine-service required
- **Code Location**: `W3ReconciliationService.java` lines 345-379

### Document Requests
- **Status**: ⚠️ BACKEND ONLY
- **Current State**: Backend model (`DocumentRequest.java`) and repository exist
- **Limitation**: No UI component to create, view, or manage document requests
- **Impact**: Feature is accessible only via direct API calls

### Split-Screen Review
- **Status**: ❌ NOT STARTED
- **Current State**: No code exists
- **Limitation**: Auditors cannot view PDF and extracted data side-by-side

### Taxpayer History
- **Status**: ❌ NOT STARTED
- **Current State**: No code exists
- **Limitation**: No prior year comparison available for auditors

### Receipt Generation
- **Status**: ❌ NOT STARTED
- **Current State**: No code exists
- **Limitation**: No payment receipt generation or download capability

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.1 | 2025-12-10 | Updated feature statuses to reflect actual implementation; added Known Limitations section |
| 1.0 | 2025-12-01 | Initial features documentation |

---

**Document Owner:** Product Team  
**Last Updated:** December 10, 2025
