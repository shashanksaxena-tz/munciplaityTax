# Spec 1: Complete Withholding Reconciliation System

**Priority:** HIGH  
**Feature Branch:** `1-withholding-reconciliation`  
**Spec Document:** `specs/1-withholding-reconciliation/spec.md`

## Overview

Build comprehensive withholding reconciliation logic essential for business filers - W-1 to W-2/W-3 reconciliation engine with cumulative tracking, year-end validation, and discrepancy detection.

## Implementation Status

**Current:** 0% - Not yet implemented  
**Required:** Full implementation of W-1 filing system and reconciliation logic

## Core Requirements (FR-001 to FR-020)

### W-1 Filing History & Cumulative Tracking
- [ ] Implement W1Filing entity with all required fields (Filing ID, Business Profile, Period, Filing Date, Wages, Tax Due, Payment Status, etc.)
- [ ] Build cumulative year-to-date calculation system (auto-update on each W-1 filing)
- [ ] Support amended W-1 filings with cascade updates to subsequent periods
- [ ] Implement filing frequency validation (daily, semi-monthly, monthly, quarterly)

### Year-End Reconciliation
- [ ] Create WithholdingReconciliation entity and reconciliation engine
- [ ] Build W-2/W-3 data aggregation from AI extraction service
- [ ] Implement variance detection ($100 or 1% threshold)
- [ ] Generate reconciliation report with detailed breakdown
- [ ] Support reconciliation status tracking (Not Started → In Progress → Reconciled)

### Validation & Compliance
- [ ] Validate W-1 wage amounts against filing frequency patterns
- [ ] Implement late-filing penalty calculation (5% per month, max 25%)
- [ ] Calculate underpayment penalties (90% safe harbor rule)
- [ ] Prevent next year filing if prior year reconciliation incomplete
- [ ] Support employee count validation (±20% tolerance)

### Payment Integration
- [ ] Integrate with payment gateway for W-1 payment tracking
- [ ] Track payment status per period (paid, partially paid, unpaid)
- [ ] Calculate aging of unpaid liabilities

## User Stories (5 Priority P1-P3)

1. **US-1 (P1):** Quarterly Filer Submits W-1 with Cumulative Validation
2. **US-2 (P1):** Year-End W-2/W-3 Reconciliation with Discrepancy Detection
3. **US-3 (P2):** Monthly Filer with Mid-Year Correction
4. **US-4 (P2):** Daily Filer with High-Volume Reconciliation
5. **US-5 (P3):** Semi-Monthly Filer with Employee Count Validation

## Success Criteria

- Business owners can view cumulative withholding totals within 2 seconds
- Year-end reconciliation completes in under 10 seconds (up to 52 W-1 filings)
- 95% of businesses with <1% variance complete reconciliation without amendments
- System correctly calculates late-filing penalties matching municipal schedule
- Amended W-1 filings automatically update all downstream calculations

## Dependencies

- AI Extraction Service (Gemini) for W-2 data extraction
- Payment Gateway integration for W-1 payments
- Business Profile Management (existing)
- PDF Generation Service for W-3 reconciliation forms
- Session Storage for filing history

## Edge Cases Documented

- Late W-1 filing with penalty calculation
- Zero-wage period (seasonal business)
- Negative adjustment (bonus reversal)
- Multiple EIN numbers
- Partial-year business
- Bankruptcy/dissolution mid-year
- Household employer (SSN-based filing)

## Technical Notes

- 20 Functional Requirements defined (FR-001 to FR-020)
- Key entities: W1Filing, WithholdingReconciliation, CumulativeWithholdingTotals, WithholdingPayment
- Must integrate with existing payment system and AI extraction
- Performance requirement: Sub-2-second cumulative calculations

## Related Specs

- Depends on: Payment Gateway, AI Extraction
- Feeds into: Spec 7 (Penalty & Interest), Spec 12 (Ledger System)
