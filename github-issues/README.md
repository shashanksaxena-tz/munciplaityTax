# GitHub Issues Summary for 12 Specifications

This directory contains detailed issue content for all 12 specifications. Each issue is structured with:

## Issue Structure

- **Title**: Spec number and name
- **Priority**: HIGH or MEDIUM
- **Feature Branch**: Suggested branch name
- **Spec Document**: Link to full specification
- **Overview**: Brief description
- **Implementation Status**: Current progress percentage
- **Core Requirements**: All functional requirements (FR-XXX)
- **User Stories**: 5-10 user stories with priorities
- **Key Entities**: Data models and structures
- **Success Criteria**: Measurable outcomes
- **Edge Cases**: Special scenarios documented
- **Dependencies**: Related specs and external systems
- **Related Specs**: Cross-references

## All 12 Specifications

### 1. Withholding Reconciliation System
- **File**: `spec-01-withholding-reconciliation.md`
- **Status**: 0% - Not yet implemented
- **Priority**: HIGH
- **Key Features**: W-1 filing history, cumulative tracking, year-end reconciliation

### 2. Expand Schedule X (Book-Tax Reconciliation)
- **File**: `spec-02-expand-schedule-x.md`
- **Status**: ~65% - Backend complete, frontend UI in progress
- **Priority**: HIGH
- **Key Features**: 25+ reconciliation fields, auto-calculations, M-1 adjustments

### 3. Enhanced Discrepancy Detection
- **File**: `spec-03-enhanced-discrepancy-detection.md`
- **Status**: ~30% - Basic detection exists
- **Priority**: HIGH
- **Key Features**: 10+ validation rules, W-2 validation, cross-year checks

### 4. Rule Configuration UI
- **File**: `spec-04-rule-configuration-ui.md`
- **Status**: 0% - All rules hardcoded
- **Priority**: HIGH
- **Key Features**: Dynamic rules, versioning, temporal rules, multi-tenant

### 5. Multi-State Income Sourcing (Schedule Y)
- **File**: `spec-05-schedule-y-sourcing.md`
- **Status**: 0% - Single-jurisdiction only
- **Priority**: HIGH
- **Key Features**: Apportionment, Joyce/Finnigan, throwback, market-based sourcing

### 6. NOL Carryforward Tracker
- **File**: `spec-06-nol-carryforward-tracker.md`
- **Status**: ~20% - Basic NOL calculation
- **Priority**: HIGH
- **Key Features**: Multi-year tracking, 80% limitation, CARES Act carryback, expiration management

### 7. Enhanced Penalty & Interest
- **File**: `spec-07-enhanced-penalty-interest.md`
- **Status**: ~20% - Basic penalty calculation
- **Priority**: HIGH
- **Key Features**: Late filing/payment penalties, underpayment penalties, compound interest, abatement

### 8. Business Form Library
- **File**: `spec-08-business-form-library.md`
- **Status**: ~10% - Basic Form 27 only
- **Priority**: HIGH
- **Key Features**: 9+ form types, PDF generation, filing packages, extensions, vouchers

### 9. Auditor Workflow
- **File**: `spec-09-auditor-workflow.md`
- **Status**: 0% - Filer-only system
- **Priority**: MEDIUM
- **Key Features**: Submission queue, review interface, approval/rejection, automated audits

### 10. JEDD Zone Support
- **File**: `spec-10-jedd-zone-support.md`
- **Status**: 0% - No JEDD support
- **Priority**: MEDIUM
- **Key Features**: JEDD zone identification, multi-jurisdiction allocation, blended withholding

### 11. Consolidated Returns
- **File**: `spec-11-consolidated-returns.md`
- **Status**: 0% - Separate filing only
- **Priority**: MEDIUM
- **Key Features**: Affiliated groups, intercompany eliminations, combined apportionment, consolidated NOL

### 12. Double-Entry Ledger
- **File**: `spec-12-double-entry-ledger.md`
- **Status**: 0% - Simple transaction tracking
- **Priority**: MEDIUM
- **Key Features**: Chart of accounts, journal entries, trial balance, reconciliation, mock payment provider

## Implementation Priority

### Critical Path (HIGH Priority)
These specs are essential for MVP and full tax filing functionality:
1. **Spec 2**: Expand Schedule X (65% done - complete UI)
2. **Spec 3**: Enhanced Discrepancy Detection (30% done - add 10+ rules)
3. **Spec 1**: Withholding Reconciliation (0% - essential for businesses)
4. **Spec 4**: Rule Configuration UI (0% - stops hardcoding)
5. **Spec 5**: Schedule Y Sourcing (0% - multi-state businesses)
6. **Spec 6**: NOL Tracker (20% - multi-year tracking)
7. **Spec 7**: Penalty & Interest (20% - full calculation engine)
8. **Spec 8**: Form Library (10% - all forms needed)

### Enhanced Features (MEDIUM Priority)
These specs add advanced functionality and scalability:
9. **Spec 9**: Auditor Workflow (0% - municipality review)
10. **Spec 10**: JEDD Zones (0% - special economic districts)
11. **Spec 11**: Consolidated Returns (0% - affiliated groups)
12. **Spec 12**: Double-Entry Ledger (0% - financial tracking)

## Creating GitHub Issues

Since GitHub CLI is not authenticated, you have two options:

### Option 1: Manual Creation (Recommended)
1. Go to: https://github.com/shashanksaxena-tz/munciplaityTax/issues/new
2. Copy the title from each spec file
3. Copy the content from the spec file as the issue body
4. Add label: `feature`, and appropriate `priority:high` or `priority:medium`
5. Add the spec number to the title (e.g., "Spec 1: Withholding Reconciliation System")

### Option 2: Use the gh-create-issues.sh Script
See `gh-create-issues.sh` for a bash script that can create all issues if you authenticate:
```bash
gh auth login
./github-issues/gh-create-issues.sh
```

## Next Steps

1. **Review and prioritize**: Determine which specs to implement first based on business needs
2. **Create issues**: Use manual or automated approach to create GitHub issues
3. **Plan sprints**: Break down HIGH priority specs into 2-week sprints
4. **Start implementation**: Begin with Spec 2 (already 65% complete) to build momentum

## Estimated Implementation Effort

- **Spec 2 (Schedule X)**: ~9 hours remaining for MVP
- **Spec 1 (Withholding)**: ~40 hours (full implementation)
- **Spec 3 (Discrepancy)**: ~30 hours (10+ validation rules)
- **Spec 4 (Rule Config)**: ~60 hours (complex UI + backend)
- **Spec 5 (Schedule Y)**: ~50 hours (multi-state sourcing)
- **Spec 6 (NOL)**: ~35 hours (multi-year tracking)
- **Spec 7 (Penalty)**: ~25 hours (full penalty engine)
- **Spec 8 (Forms)**: ~45 hours (9+ form types)
- **Spec 9 (Auditor)**: ~40 hours (full workflow)
- **Spec 10 (JEDD)**: ~30 hours (zone management)
- **Spec 11 (Consolidated)**: ~50 hours (complex eliminations)
- **Spec 12 (Ledger)**: ~55 hours (double-entry system)

**Total Estimated Effort**: ~470 hours for all 12 specs
**MVP (Specs 1-8)**: ~294 hours
