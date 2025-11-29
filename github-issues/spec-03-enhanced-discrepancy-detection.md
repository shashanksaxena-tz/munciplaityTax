# Spec 3: Enhanced Discrepancy Detection (10+ Validation Rules)

**Priority:** HIGH  
**Feature Branch:** `3-enhanced-discrepancy-detection`  
**Spec Document:** `specs/3-enhanced-discrepancy-detection/spec.md`

## Overview

Implement comprehensive discrepancy detection with 10+ validation rules including W-2 box consistency (Box 1 vs 18), Schedule C income validation, rental property counts, K-1 allocation checks, municipal credit limits, withholding rate validation, and cross-year carryforward verification.

## Implementation Status

**Current:** Basic discrepancy detection exists (~30%)  
**Required:** Expand to 10+ comprehensive validation rules

## Core Requirements (FR-001 to FR-050)

### W-2 Validation Rules (FR-001 to FR-005)
- [ ] Validate W-2 Box 18 (Local wages) within 20% of Box 1 (Federal wages)
- [ ] Validate withholding rate (Box 19/Box 18) between 0% and 3.0%
- [ ] Detect duplicate W-2s (Employer EIN + Employee SSN + Wage Amount)
- [ ] Validate W-2 employer address within Dublin municipal limits
- [ ] Support "Corrected W-2" vs "Duplicate" marking

### Schedule C/E/F Validation (FR-006 to FR-010)
- [ ] Calculate required estimated tax payments (90% current or 100% prior year)
- [ ] Validate Schedule E rental property count matches complete property data
- [ ] Validate rental property addresses against Dublin boundaries
- [ ] Check Schedule E passive loss limits ($150K AGI threshold)
- [ ] Validate Schedule C gross receipts match 1099-K forms

### K-1 Validation Rules (FR-011 to FR-013)
- [ ] Extract and validate K-1 Box 1, Box 4c, Box 12, Box 13 components
- [ ] Validate partner's profit share percentage matches partnership totals
- [ ] Flag K-1 income requiring municipal adjustment

### Municipal Credit Validation (FR-014 to FR-016)
- [ ] Validate municipal credits ≤ Dublin tax liability (cap at liability)
- [ ] Enforce credit order: other cities → withholding → estimates
- [ ] Validate credit percentages (lesser of taxes paid OR Dublin rate × income)

### Federal Form Reconciliation (FR-017 to FR-019)
- [ ] Compare Federal Form 1040 Line 11 (AGI) to local income sum
- [ ] Identify non-taxable federal components (interest, dividends, unemployment, etc.)
- [ ] Validate Federal Form 1040 Line 1 (Wages) matches W-2 Box 1 totals

### Cross-Year Validation (FR-020 to FR-022)
- [ ] Verify carryforward amounts against prior year returns (NOL, credits, losses)
- [ ] Query prior year return for safe harbor calculation (100% prior / 90% current)
- [ ] Track multi-year NOL utilization and remaining balances

### Validation Reporting (FR-023 to FR-028)
- [ ] Assign severity levels: HIGH (prevents filing), MEDIUM (warning), LOW (info)
- [ ] Display all discrepancies in DiscrepancyView component with details
- [ ] Allow user to "Accept" informational discrepancies with reason
- [ ] Generate validation report PDF showing all checks
- [ ] Block filing submission if HIGH severity discrepancies remain
- [ ] Save discrepancy analysis with return submission

## User Stories (10 Priority P1-P3)

1. **US-1 (P1):** W-2 Box Validation Catches Data Entry Error
2. **US-2 (P1):** Schedule C Income vs Estimated Tax Paid Mismatch
3. **US-3 (P2):** Schedule E Rental Property Count Validation
4. **US-4 (P2):** K-1 Income Allocation Check
5. **US-5 (P1):** Municipal Credit Limit Validation
6. **US-6 (P2):** Withholding Rate Validation
7. **US-7 (P3):** Cross-Year Carryforward Verification
8. **US-8 (P2):** Federal Form 1040 AGI vs Local Calculation
9. **US-9 (P2):** Duplicate W-2 Detection
10. **US-10 (P3):** Schedule E Passive Loss Limitation Check

## Key Entity Updates

### DiscrepancyReport (EXPANDED)
- **hasDiscrepancies**: boolean
- **issues**: array of DiscrepancyIssue objects with:
  - issueId, ruleId, category, field
  - sourceValue, formValue, difference, differencePercent
  - severity (HIGH/MEDIUM/LOW), message, recommendedAction
  - isAccepted, acceptanceNote, acceptedDate
- **summary**: totalIssues, counts by severity, blocksFiling flag

## Success Criteria

- System detects 95% of W-2 data entry errors
- Validation completes in under 3 seconds (up to 10 income forms)
- False positive rate under 10%
- Users resolve/accept all discrepancies in under 5 minutes
- HIGH severity discrepancies block filing 100% of time
- Validation report exports to PDF with all 10+ rule checks

## Edge Cases Documented

- Non-resident with partial-year Dublin employment
- Multiple state W-2s (each validated separately)
- Corrected W-2 after original filing
- Section 125 cafeteria plan (Box 1 < Box 18 is correct)
- Statutory employee (Schedule C instead of wages)
- Household employee (SSN instead of EIN)
- Railroad retirement (Form RRB-1099-R)
- Foreign earned income exclusion
- Alimony received (pre-2019 divorces)
- IRA distributions

## Technical Implementation

### IndividualTaxCalculator.java
- [ ] Expand analyzeDiscrepancies() method with FR-001 to FR-022
- [ ] Implement each validation rule with proper thresholds
- [ ] Generate DiscrepancyReport with structured issues

### DiscrepancyView.tsx
- [ ] Support expanded DiscrepancyIssue structure
- [ ] Show severity badges, acceptance controls
- [ ] Display recommended actions per issue
- [ ] Implement acceptance workflow with notes

### Validation Rule Engine Integration
- [ ] Load validation thresholds from Rule Engine (Spec 4)
- [ ] Support configurable thresholds (20% variance, $500 difference, etc.)

## Dependencies

- AI Extraction Service - Federal Form 1040, K-1, 1099-K extraction
- GIS Service - Address validation against Dublin boundaries
- Session Storage - Prior year return data for cross-year validation
- Rule Engine (Spec 4) - Validation thresholds and rules

## Related Specs

- Integrates with: Spec 4 (Rule Engine for validation rules)
- Feeds into: Spec 9 (Auditor Workflow uses discrepancy flags)
- Uses: Spec 6 (NOL tracking for carryforward validation)
