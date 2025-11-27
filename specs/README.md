# MuniTax System Specifications

**Project:** Dublin Municipality Tax Calculator  
**Documentation Type:** Technical Specifications  
**Status:** Complete (12 of 12 specifications)  
**Last Updated:** 2025-11-27

---

## Overview

This directory contains comprehensive technical specifications for the MuniTax system improvements. Each specification follows the SpecKit methodology and includes user scenarios, functional requirements, key entities, success criteria, and edge cases. These specifications are implementation-ready and designed to guide development teams through the enhancement of the municipal tax filing system.

**Current System Status:** MVP demonstrating core concepts but requiring significant expansion for production readiness. Most features are 0-40% complete with hardcoded rules and limited field coverage.

**Target State:** Production-ready system supporting complete tax workflows for business filers, individual filers, and municipality auditors with comprehensive validation, flexible rule engine, and full audit trails.

---

## Specification Index

### Priority: CRITICAL (P1)

These specifications address fundamental gaps that prevent the system from being production-ready. Implement first.

| # | Specification | Status | FRs | Entities | Priority |
|---|---------------|--------|-----|----------|----------|
| 1 | [Withholding Reconciliation](1-withholding-reconciliation/spec.md) | Complete | 20 | 4 | CRITICAL |
| 2 | [Expand Schedule X to 27 Fields](2-expand-schedule-x/spec.md) | Complete | 43 | 3 | CRITICAL |
| 3 | [Enhanced Discrepancy Detection](3-enhanced-discrepancy-detection/spec.md) | Complete | 28 | 2 | CRITICAL |
| 9 | [Auditor Workflow](9-auditor-workflow/spec.md) | Complete | 59 | 5 | CRITICAL |

**Rationale:** These four features are essential for basic system operation:
- **Spec 1:** Business withholding currently 40% complete, missing W-1 to W-2/W-3 reconciliation
- **Spec 2:** Schedule X has only 6 fields vs 25+ required for complete M-1 reconciliation
- **Spec 3:** Only 2 validation rules vs 10+ needed to catch common errors
- **Spec 9:** Auditor workflow completely missing (0% complete), blocks municipality adoption

---

### Priority: HIGH (P2)

These specifications add significant value and enable production use at scale. Implement after critical features.

| # | Specification | Status | FRs | Entities | Priority |
|---|---------------|--------|-----|----------|----------|
| 4 | [Rule Configuration UI](4-rule-configuration-ui/spec.md) | Complete | 37 | 2 | HIGH |
| 5 | [Schedule Y Sourcing Rules](5-schedule-y-sourcing/spec.md) | Complete | 50 | 6 | HIGH |
| 6 | [NOL Carryforward Tracker](6-nol-carryforward-tracker/spec.md) | Complete | 47 | 6 | HIGH |
| 7 | [Enhanced Penalty/Interest](7-enhanced-penalty-interest/spec.md) | Complete | 47 | 6 | HIGH |
| 8 | [Business Form Library](8-business-form-library/spec.md) | Complete | 42 | 3 | HIGH |

**Rationale:** These features are necessary for production deployment but can be phased:
- **Spec 4:** Rule engine currently 0% (hardcoded), prevents multi-tenant scaling
- **Spec 5:** Multi-state sourcing needed for businesses operating across state lines
- **Spec 6:** NOL tracking required for multi-year tax optimization
- **Spec 7:** Current penalty logic oversimplified, misses quarterly estimated tax
- **Spec 8:** Form generation manual, needs automation for filer convenience

---

### Priority: MEDIUM (P3)

These specifications support advanced use cases and specialized scenarios. Implement after core features stable.

| # | Specification | Status | FRs | Entities | Priority |
|---|---------------|--------|-----|----------|----------|
| 10 | [JEDD Zone Support](10-jedd-zone-support/spec.md) | Complete | 35 | 5 | MEDIUM |
| 11 | [Consolidated Returns](11-consolidated-returns/spec.md) | Complete | 44 | 8 | MEDIUM |
| 12 | [Double-Entry Ledger System](12-double-entry-ledger/spec.md) | Complete | 55 | 10 | MEDIUM |

**Rationale:** These features serve specific use cases:
- **Spec 10:** JEDD zones are Ohio-specific for joint economic development districts
- **Spec 11:** Consolidated filing for corporate groups (affects <10% of filers)
- **Spec 12:** Mock payment provider and ledger system for testing and reconciliation

---

## Quick Statistics

**Total Specifications:** 12  
**Total Functional Requirements:** 507  
**Total User Stories:** 69  
**Total Key Entities:** 60

**Breakdown by Priority:**
- CRITICAL: 150 FRs, 27 user stories, 14 entities
- HIGH: 223 FRs, 27 user stories, 23 entities  
- MEDIUM: 134 FRs, 15 user stories, 23 entities

**Feature Completion Status (Current System):**
- Withholding: 40% complete → Spec 1 brings to 100%
- Schedule X: 15% complete (6 fields) → Spec 2 expands to 27 fields
- Discrepancy Detection: 20% complete (2 rules) → Spec 3 adds 10+ rules
- Rule Engine: 0% complete (hardcoded) → Spec 4 enables dynamic rules
- Auditor Workflow: 0% complete → Spec 9 implements full workflow
- Business Rule Engine: 15% complete → Spec 4 completes
- Multi-state Sourcing: 0% complete → Spec 5 implements
- NOL Tracking: 0% complete → Spec 6 implements
- Penalty/Interest: 30% complete (basic only) → Spec 7 enhances
- Form Library: 0% complete (manual) → Spec 8 automates
- JEDD Support: 0% complete → Spec 10 implements
- Consolidated Returns: 0% complete → Spec 11 implements
- Ledger System: 0% complete → Spec 12 implements

---

## Implementation Roadmap

### Phase 1: Foundation (Specs 1-3, 4) - 3-4 months
**Goal:** Make system production-ready for basic municipal tax filing

**Specifications:**
1. Withholding Reconciliation (Spec 1) - 4 weeks
2. Expand Schedule X (Spec 2) - 3 weeks
   - **CRITICAL:** Update AI extraction service (Gemini) for all 27 fields (FR-039 to FR-043)
3. Enhanced Discrepancy Detection (Spec 3) - 3 weeks
4. Rule Configuration UI (Spec 4) - 4 weeks

**Deliverables:**
- Complete withholding W-1 to W-2/W-3 reconciliation
- Schedule X expanded from 6 to 27 fields with AI sync
- 10+ discrepancy validation rules
- Dynamic rule engine replacing hardcoded logic
- System ready for pilot deployment (100-200 businesses)

**Dependencies:** None (can start immediately)

---

### Phase 2: Scale & Audit (Specs 5-9) - 4-5 months
**Goal:** Enable multi-state operations, multi-year tracking, and municipality audit workflows

**Specifications:**
5. Schedule Y Sourcing Rules (Spec 5) - 5 weeks
6. NOL Carryforward Tracker (Spec 6) - 4 weeks
7. Enhanced Penalty/Interest (Spec 7) - 4 weeks
8. Business Form Library (Spec 8) - 4 weeks
9. Auditor Workflow (Spec 9) - 6 weeks

**Deliverables:**
- Multi-state apportionment with Joyce/Finnigan elections
- Multi-year NOL tracking with carryback/carryforward
- Quarterly estimated tax penalties
- Automated PDF form generation
- Complete auditor review and approval workflow
- System ready for full production (1,000+ businesses)

**Dependencies:**
- Spec 5 requires Spec 4 (rule engine for sourcing elections)
- Spec 6 requires Spec 2 (Schedule X for NOL calculation)
- Spec 7 requires Spec 4 (rule engine for penalty rates)
- Spec 8 requires Specs 1-7 (all forms pull data from these features)
- Spec 9 requires Specs 1-8 (audits all features)

---

### Phase 3: Advanced Features (Specs 10-12) - 3-4 months
**Goal:** Support specialized use cases and advanced accounting

**Specifications:**
10. JEDD Zone Support (Spec 10) - 4 weeks
11. Consolidated Returns (Spec 11) - 5 weeks
12. Double-Entry Ledger System (Spec 12) - 5 weeks

**Deliverables:**
- JEDD zone income allocation and blended tax rates
- Consolidated filing for affiliated corporate groups
- Mock payment gateway for testing
- Double-entry ledger with two-way reconciliation
- Complete financial reporting for municipalities
- System supports all Ohio municipal tax scenarios

**Dependencies:**
- Spec 10 requires Spec 5 (JEDD zones use apportionment logic)
- Spec 11 requires Specs 2, 5, 6 (consolidated returns aggregate Schedule X, Schedule Y, NOLs)
- Spec 12 requires Spec 7 (ledger records penalties and interest)

---

## Key Dependencies Across Specifications

### Database & Infrastructure
- **PostgreSQL 16:** All specs require jsonb storage for flexible data (rules, factors, allocations)
- **Redis 7:** Caching for rule evaluation (Spec 4), AI extraction results (Spec 2)
- **Gemini AI:** Extraction service MUST be updated for Schedule X expansion (Spec 2: FR-039 to FR-043)

### Shared Entities
- **Business:** Core entity used by all specs (filer information)
- **TaxReturn:** Referenced by Specs 1, 2, 3, 6, 7, 8, 9, 10, 11
- **TaxRule:** Created in Spec 4, used by Specs 5, 6, 7, 10, 11
- **DiscrepancyReport:** Created in Spec 3, consumed by Spec 9 (auditor workflow)
- **GeneratedForm:** Created in Spec 8, attached to returns in all specs

### Cross-Specification Workflows
1. **Filing Workflow:** Spec 2 (Schedule X) → Spec 3 (Discrepancies) → Spec 8 (Forms) → Spec 9 (Audit)
2. **Withholding Workflow:** Spec 1 (W-1 reconciliation) → Spec 3 (Discrepancies) → Spec 9 (Audit)
3. **Multi-Year Workflow:** Spec 6 (NOL carryforward) → Spec 7 (Interest on NOL adjustments) → Spec 11 (Consolidated NOL)
4. **Payment Workflow:** Spec 7 (Penalties) → Spec 12 (Ledger entries) → Spec 9 (Audit trail)

---

## Technical Architecture Notes

### Backend Services (Spring Boot 3.2.3, Java 21)
Each specification impacts one or more microservices:

- **tax-engine-service:** Specs 1, 2, 3, 5, 6, 7, 10, 11 (core tax calculations)
- **extraction-service:** Spec 2 (AI extraction for Schedule X fields - CRITICAL UPDATE)
- **pdf-service:** Spec 8 (form generation)
- **submission-service:** Specs 3, 9 (validation and audit queue)
- **tenant-service:** Spec 4 (rule configuration and multi-tenant rules)
- **auth-service:** Spec 9 (auditor roles and permissions)
- **gateway-service:** All specs (API routing)

### Frontend Components (React 18, TypeScript)
New components needed per specification:

- **Spec 1:** `WithholdingReconciliationWizard.tsx`, `W1FilingHistory.tsx`
- **Spec 2:** Update `BusinessRegistration.tsx` (27 fields), `ExtractionSummary.tsx` (AI sync status)
- **Spec 3:** `DiscrepancyView.tsx` (enhanced with severity levels and resolution workflow)
- **Spec 4:** `RuleConfigurationScreen.tsx` (already exists, enhance for temporal rules)
- **Spec 5:** `ScheduleYWizard.tsx`, `ApportionmentFactors.tsx`
- **Spec 6:** `NOLCarryforwardTracker.tsx`, `NOLSchedule.tsx`
- **Spec 7:** `PenaltyInterestCalculator.tsx`, `EstimatedTaxWizard.tsx`
- **Spec 8:** `FormLibrary.tsx`, `FormPreview.tsx`, `FilingPackageAssembler.tsx`
- **Spec 9:** `AuditorDashboard.tsx`, `SubmissionReview.tsx`, `AuditTrail.tsx` (already started, complete)
- **Spec 10:** `JEDDZoneSelector.tsx`, `JEDDAllocationSchedule.tsx`
- **Spec 11:** `ConsolidatedGroupManager.tsx`, `IntercompanyEliminations.tsx`
- **Spec 12:** `AccountStatement.tsx`, `PaymentGateway.tsx` (enhance for mock provider), `ReconciliationReport.tsx`

### Database Schema Changes
Each specification requires new tables:

- **Spec 1:** `w1_filings`, `withholding_reconciliations`, `withholding_payments`
- **Spec 2:** Alter `business_schedule_x_details` (add 21 columns), `ai_extraction_mappings`
- **Spec 3:** `discrepancy_reports`, `discrepancy_items`, `discrepancy_resolutions`
- **Spec 4:** `tax_rules`, `rule_change_logs`
- **Spec 5:** `schedule_y`, `property_factors`, `payroll_factors`, `sales_factors`, `sale_transactions`, `nexus_tracking`
- **Spec 6:** `nols`, `nol_usages`, `nol_carrybacks`, `nol_schedules`, `nol_expiration_alerts`, `nol_amendments`
- **Spec 7:** `penalties`, `estimated_tax_penalties`, `quarterly_underpayments`, `interest`, `quarterly_interest`, `penalty_abatements`, `payment_allocations`
- **Spec 8:** `form_templates`, `generated_forms`, `filing_packages`
- **Spec 9:** `audit_queue`, `audit_actions`, `document_requests`, `audit_reports`, `audit_trails`
- **Spec 10:** `jedd_zones`, `jedd_business_profiles`, `jedd_tax_allocations`, `jedd_withholdings`, `jedd_revenue_reports`
- **Spec 11:** `affiliated_groups`, `group_members`, `intercompany_transactions`, `consolidated_income`, `tax_allocations`, `entity_tax_allocations`, `consolidated_nols`
- **Spec 12:** `mock_payment_provider`, `payment_transactions`, `chart_of_accounts`, `journal_entries`, `journal_entry_lines`, `account_balances`, `filer_account_statements`, `statement_transactions`, `reconciliation_reports`, `discrepancies`

**Total New Tables:** 68  
**Total Altered Tables:** 1 (business_schedule_x_details)

---

## Common Patterns Across Specifications

### 1. Rule-Based Validation (Specs 3, 4, 5, 7)
All validation rules stored in `tax_rules` table (Spec 4) and evaluated dynamically:
- Discrepancy rules (Spec 3): "W-2 Box 18 must be within 3% of Box 1"
- Sourcing rules (Spec 5): "Services sourced to customer location if revenue < $5M"
- Penalty rules (Spec 7): "Late filing penalty = 5% per month, max 25%"

**Implementation:** Rule engine evaluates rules per tenant, tax year, entity type. Rules versioned for temporal changes.

---

### 2. Multi-Year Tracking (Specs 1, 6, 7, 11)
Many features require tracking across tax years:
- Withholding YTD totals (Spec 1): Accumulate Q1-Q4 W-1 filings for year-end reconciliation
- NOL carryforward (Spec 6): Track origination year, usage, expiration (up to 20 years)
- Penalty interest (Spec 7): Compound quarterly over multiple years
- Consolidated NOL (Spec 11): Allocate upon deconsolidation years later

**Implementation:** Use `originationYear` and `currentYear` fields to track lifecycle. Cascade updates when prior years amended.

---

### 3. AI Extraction Synchronization (Spec 2)
**CRITICAL:** When adding new fields to forms, AI extraction must be updated:

**Example - Schedule X Expansion:**
1. Add 21 new fields to `business_schedule_x_details` table
2. Update AI extraction prompt in `extraction-service` to extract new fields:
   - Depreciation differences
   - Meals & entertainment (50% limit)
   - Related-party expenses
   - Political contributions
   - Officer life insurance
   - Capital losses
   - Federal tax refunds
   - Section 179 excess
   - Bonus depreciation
   - Bad debt reserves
   - Charitable contribution excess
   - Domestic production activities deduction
   - Stock-based compensation
   - Inventory valuation changes
   - State/municipal tax add-backs
   - Interest income deductions
   - Dividend received deductions
   - IRC Section 199A deductions
   - Foreign tax credit deductions
   - Prior year overpayment credits
   - Other deductions
3. Create `ai_extraction_mappings` table linking PDF field names to database columns
4. Test AI extraction accuracy (target: 95%+ for structured fields)
5. Implement manual correction UI for AI errors

**Same pattern applies to any new form/schedule:**
- Schedule Y (Spec 5): Extract property, payroll, sales factors from multi-state returns
- W-1 (Spec 1): Extract withholding amounts, payment dates
- Schedule NOL (Spec 6): Extract NOL origination year, amounts, expiration dates

---

### 4. Audit Trail (Specs 3, 7, 9, 11, 12)
All critical operations require immutable audit logs:
- Discrepancy resolution (Spec 3): Who resolved, when, how
- Penalty abatement (Spec 7): Who approved, reason, amount waived
- Audit actions (Spec 9): All auditor actions logged with e-signature
- Consolidated elections (Spec 11): Election date, member changes, termination
- Ledger entries (Spec 12): Who created, posted, reversed journal entries

**Implementation:** Append-only tables with `createdBy`, `createdAt`, `action`, `oldValue`, `newValue`, `reason`. Retention: 7 years minimum per IRS regulations.

---

### 5. Multi-Tenant Configuration (Specs 4, 5, 7, 10)
Each municipality has unique rules:
- Tax rates (Spec 4): Dublin 2.5%, Columbus 2.0%
- Withholding rates (Spec 1): Dublin 2.5%, Westerville 2.25%
- Penalty rates (Spec 7): Some municipalities waive first offense
- JEDD zones (Spec 10): Dublin-Columbus JEDD 60/40 split

**Implementation:** All rules have `tenantId` foreign key to `tenants` table. Default rules (state-level) have `tenantId` = NULL, municipalities override with custom rules.

---

## Testing Strategy

### Unit Tests
Each functional requirement (FR) should have corresponding unit tests:
- **Example (Spec 3, FR-007):** Test W-2 Box 18 vs Box 1 variance detection
  - Input: Box 1 = $50,000, Box 18 = $48,000 (4% variance)
  - Expected: HIGH severity discrepancy ("Box 18 ($48K) differs from Box 1 ($50K) by 4%. Tolerance: 3%.")
  - Input: Box 1 = $50,000, Box 18 = $49,500 (1% variance)
  - Expected: No discrepancy (within 3% tolerance)

**Target:** 100% coverage of functional requirements (507 FRs = 507+ unit tests)

---

### Integration Tests
Test workflows across specifications:
- **Withholding Flow:** File W-1 (Spec 1) → AI extract (Spec 2) → Validate (Spec 3) → Generate W-1 form (Spec 8) → Audit review (Spec 9)
- **NOL Flow:** Calculate NOL (Spec 6) → Carry forward to next year → Apply to income (Spec 2) → Assess interest (Spec 7)
- **JEDD Flow:** Detect JEDD zone (Spec 10) → Allocate income (Spec 5) → Calculate blended rate → Distribute payments (Spec 12)

**Target:** 25+ integration tests covering cross-spec workflows

---

### User Acceptance Tests
Each user story should have UAT scenario:
- **Example (Spec 9, US-1):** Auditor reviews submission
  - Setup: Business files Q1 return with $10K tax, 2 discrepancies (HIGH, MEDIUM)
  - Action: Auditor logs in, sees submission in queue, clicks Review, sees split-screen with return + discrepancies
  - Expected: Auditor can approve, reject, or request documentation
  - Validation: Audit action logged with timestamp and user ID

**Target:** 69 user stories = 69 UAT scenarios

---

## Success Metrics

### Development Metrics
- **Specification Coverage:** 12 of 12 complete ✓
- **Functional Requirements:** 507 defined ✓
- **Implementation Status:** 0 of 507 implemented (next phase)

### System Metrics (Post-Implementation)
- **Withholding Reconciliation:** 100% of W-1 filings reconcile to W-2/W-3 (vs current 60%)
- **Schedule X Completeness:** 27 fields captured (vs current 6 fields)
- **Discrepancy Detection:** 95%+ of errors caught before submission (vs current 20%)
- **Rule Flexibility:** 100% of tax rules configurable (vs current 0% hardcoded)
- **Auditor Efficiency:** 75% reduction in manual review time (split-screen, automated checks)
- **Form Automation:** 100% of forms auto-generated (vs current 0% manual)
- **Payment Accuracy:** 100% of ledger entries balanced (debits = credits)

### User Satisfaction Metrics
- **Filer NPS:** Target 8+ (Net Promoter Score)
- **Auditor Adoption:** 90%+ of municipalities use auditor workflow
- **Error Rate:** <5% of returns require correction after submission
- **Time to File:** 50% reduction (from 2 hours to 1 hour average per return)

---

## Migration Strategy

### Data Migration
For municipalities with existing filers:
1. **Historical Data:** Import prior year returns (Spec 6 NOL requires 20 years history)
2. **Withholding History:** Import W-1 filings from prior quarters (Spec 1 YTD reconciliation)
3. **Payment History:** Import payment ledger (Spec 12 account statements)
4. **Rule Migration:** Convert hardcoded rules to `tax_rules` table (Spec 4)

### Incremental Rollout
1. **Pilot:** 1 municipality, 100 businesses (Phase 1 specs only)
2. **Expansion:** 5 municipalities, 500 businesses (Phase 1 + Phase 2 specs)
3. **Production:** All municipalities, 5,000+ businesses (all specs)

---

## Risk Management

### High-Risk Areas
1. **AI Extraction Accuracy (Spec 2):** If Gemini service extracts wrong values, Schedule X will be incorrect
   - **Mitigation:** Human review workflow, confidence scores, manual correction UI, regression testing on 1,000+ sample PDFs
2. **Rule Engine Performance (Spec 4):** Evaluating 100+ rules per return may be slow
   - **Mitigation:** Redis caching, rule pre-compilation, parallel evaluation
3. **Ledger Balancing (Spec 12):** Any bug in double-entry logic will cause unbalanced books
   - **Mitigation:** 100% unit test coverage of journal entries, daily trial balance validation, automated alerts
4. **Multi-Year NOL Tracking (Spec 6):** Errors in NOL allocation affect future years
   - **Mitigation:** Immutable NOL records, amendment workflow, yearly reconciliation reports

### Dependencies on External Systems
- **Gemini AI (Spec 2):** Google's API availability and accuracy
- **Payment Gateway (Spec 12):** Mock provider sufficient for testing, real gateway (Stripe/Square) for production
- **GIS/Geocoding (Spec 10):** For JEDD zone detection (use OpenStreetMap or Google Maps API)

---

## Glossary

**Common Tax Terms Across Specifications:**

- **Apportionment (Spec 5):** Allocation of multi-state income to Ohio based on property, payroll, sales factors
- **Book-Tax Difference (Spec 2):** Difference between financial accounting income and taxable income (Schedule X reconciles)
- **Carryforward/Carryback (Spec 6):** Applying NOL from one year to offset income in another year
- **Discrepancy (Spec 3):** Error or inconsistency in tax return requiring correction
- **JEDD (Spec 10):** Joint Economic Development District (multi-municipality tax zone)
- **M-1 Reconciliation (Spec 2):** Federal Form 1120 Schedule M-1, reconciles book income to taxable income (Schedule X is municipal equivalent)
- **NOL (Spec 6):** Net Operating Loss (when deductions exceed income, can carry forward up to 20 years)
- **Safe Harbor (Spec 7):** Minimum payment threshold to avoid estimated tax penalty (90% of current year or 100% of prior year)
- **Withholding (Spec 1):** Employer withholds municipal tax from employee wages, remits to municipality

---

## Contribution Guidelines

**For Developers Implementing These Specifications:**

1. **Read Full Specification:** Don't skip edge cases and assumptions
2. **Follow SpecKit Structure:** User stories → Functional requirements → Entities → Success criteria
3. **Test Every FR:** Each functional requirement should have unit test(s)
4. **Document Deviations:** If implementation differs from spec, document why in code comments
5. **Update Spec:** If spec has errors or omissions, update spec first, then implement
6. **Cross-Reference:** When implementing Spec 5, check dependencies on Specs 2 and 4

**For Product Owners Reviewing Specifications:**

1. **Validate User Stories:** Are these the right scenarios? Are priorities correct?
2. **Check Completeness:** Are all requirements captured? Any missing edge cases?
3. **Verify Success Criteria:** Are metrics measurable? Are targets realistic?
4. **Approve Scope:** Review Out of Scope section, ensure nothing critical is excluded

---

## Contact & Support

**For Questions About Specifications:**
- Review individual spec files in this directory
- Check edge cases section for complex scenarios
- Refer to dependency diagrams for cross-spec relationships

**For Technical Implementation Support:**
- Backend: Spring Boot 3.2.3 (Java 21) microservices
- Frontend: React 18 + TypeScript + Tailwind CSS
- Database: PostgreSQL 16 + Redis 7
- AI: Gemini for document extraction

**Specification Maintenance:**
- These specs are living documents
- Update as requirements evolve
- Version control all changes
- Maintain backward compatibility where possible

---

## Appendix: Specification Template

All specifications follow this structure (from `.specify/templates/spec-template.md`):

1. **Overview:** Feature description, current state, target users
2. **User Scenarios:** 5-7 user stories with P1-P3 priorities, Given/When/Then acceptance criteria
3. **Functional Requirements:** FR-### numbered requirements (testable, unambiguous)
4. **Key Entities:** Data models with attributes
5. **Success Criteria:** Measurable, technology-agnostic outcomes
6. **Assumptions:** What we're assuming is true
7. **Dependencies:** Other specs or systems required
8. **Out of Scope:** What we're explicitly NOT doing
9. **Edge Cases:** Complex scenarios with specific handling

**Why This Structure?**
- **User-Centric:** Start with user value (what they need and why)
- **Testable:** Every FR can be verified with pass/fail test
- **Complete:** Edge cases prevent surprises during implementation
- **Traceable:** FRs link to user stories link to success criteria

---

## Version History

**v1.0 (2025-11-27):** Initial specification library complete
- 12 specifications covering all identified gaps
- 507 functional requirements
- 69 user stories
- 60 key entities
- Ready for implementation planning

---

**END OF README**
