# Spec 6: Net Operating Loss (NOL) Carryforward & Carryback System

**Priority:** HIGH  
**Feature Branch:** `6-nol-carryforward-tracker`  
**Spec Document:** `specs/6-nol-carryforward-tracker/spec.md`

## Overview

Implement comprehensive Net Operating Loss (NOL) tracking system that manages multi-year loss carryforwards, 20-year expiration schedules, CARES Act carryback provisions, and 80% taxable income limitation.

## Implementation Status

**Current:** Basic NOL calculation exists (~20%)  
**Required:** Full multi-year tracking, expiration management, carryback support

## Core Requirements (FR-001 to FR-051)

### Multi-Year NOL Tracking (FR-001 to FR-006)
- [ ] Create NOL record for each year with net operating loss
- [ ] Track NOL usage across years with remaining balance
- [ ] Calculate NOL balance available for current year
- [ ] Display NOL schedule showing: Year of Origin, Original Amount, Previously Used, Expired, Available, Used This Year, Remaining
- [ ] Automatically carry forward NOL balance to next tax year
- [ ] Retrieve prior year NOL data from database (no manual re-entry)

### 80% Taxable Income Limitation (FR-007 to FR-012)
- [ ] Determine applicable NOL limitation rule: Post-2017 (80%) vs Pre-2018 (100%)
- [ ] Calculate maximum NOL deduction: Min(Available NOL, 80% × Taxable income before NOL)
- [ ] Apply NOL deduction to taxable income
- [ ] Validate NOL deduction does not exceed 80% limit (post-2017)
- [ ] Calculate remaining NOL after current year
- [ ] Display calculation breakdown with all steps

### CARES Act Carryback (FR-013 to FR-020)
- [ ] Support NOL carryback election for 2018-2020 losses
- [ ] Allow user to elect carryback or waive (optional)
- [ ] Retrieve prior 5 years of tax returns when carryback elected
- [ ] Calculate carryback using FIFO ordering (oldest year first)
- [ ] Calculate refund amount for each carryback year
- [ ] Generate Form 27-NOL-CB (Carryback Application)
- [ ] Update NOL schedule with carryback amounts and refunds
- [ ] Support state-specific carryback rules (some states allow 2-year permanently)

### NOL Expiration Management (FR-021 to FR-026)
- [ ] Assign expiration date based on rules:
  - Pre-2018: 20-year carryforward
  - Post-2017 federal: Indefinite carryforward
  - State: Check state-specific rules
- [ ] Apply FIFO ordering by default (use oldest NOLs first)
- [ ] Calculate expired NOLs (current year > expiration year)
- [ ] Alert user of expiring NOLs (within 2 years)
- [ ] Allow manual NOL utilization ordering override with justification
- [ ] Prevent use of expired NOLs

### NOL by Entity Type (FR-027 to FR-031)
- [ ] Track entity type for each NOL: C-Corp, S-Corp, Partnership, Sole Prop
- [ ] For C-Corps: Track NOL at entity level (not passed to shareholders)
- [ ] For S-Corps: Calculate each shareholder's share (ownership % × loss)
- [ ] For Partnerships: Allocate NOL per partnership agreement (may not be pro-rata)
- [ ] Validate S-Corp/Partnership NOL usage against shareholder/partner basis

### Multi-State NOL Apportionment (FR-032 to FR-035)
- [ ] Calculate state NOL separately from federal NOL
- [ ] Ohio NOL = Federal NOL × Ohio apportionment %
- [ ] Handle state-specific NOL rules (limitation %, carryforward period, carryback)
- [ ] Reconcile federal vs state NOL differences (state addbacks/deductions)
- [ ] Display separate NOL schedules for federal and state

### NOL Forms & Reporting (FR-036 to FR-039)
- [ ] Generate Form 27-NOL (NOL Schedule) with carryforward table
- [ ] Generate Form 27-NOL-CB (Carryback Application) when elected
- [ ] Generate NOL expiration report (NOLs expiring in next 3 years)
- [ ] Include NOL detail in tax return PDF with footnote

### Amended Return NOL Recalculation (FR-040 to FR-043)
- [ ] Recalculate NOL when amended return filed
- [ ] Identify cascading effects on subsequent years
- [ ] Generate amended NOL schedule with changes
- [ ] Offer to prepare amended returns for subsequent years

### Validation & Audit Trail (FR-044 to FR-047)
- [ ] Validate NOL deduction ≤ Available balance and ≤ limitation %
- [ ] Reconcile NOL balance across years
- [ ] Create audit log for all NOL transactions
- [ ] Flag discrepancies (balance mismatch, over-utilization, expired NOL usage)

## User Stories (6 Priority P1-P3)

1. **US-1 (P1):** Track NOL Carryforward Across Multiple Years
2. **US-2 (P1):** Apply 80% Taxable Income Limitation (Post-TCJA)
3. **US-3 (P2):** CARES Act NOL Carryback (2018-2020 Losses)
4. **US-4 (P2):** NOL Expiration Tracking with Alerts
5. **US-5 (P2):** NOL by Entity Type & Apportionment
6. **US-6 (P3):** Amended Return NOL Recalculation

## Key Entities

### NOL (Net Operating Loss)
- nolId, businessId, taxYear, jurisdiction (FEDERAL/STATE_OHIO/MUNICIPALITY)
- entityType, originalNOLAmount, currentNOLBalance
- usedAmount, expiredAmount, expirationDate, carryforwardYears
- isCarriedBack, carrybackAmount, carrybackRefund
- apportionmentPercentage (for multi-state)

### NOLUsage
- nolUsageId, nolId, returnId, usageYear
- taxableIncomeBeforeNOL, nolLimitationPercentage, maximumNOLDeduction
- actualNOLDeduction, taxableIncomeAfterNOL, taxSavings
- orderingMethod (FIFO/MANUAL), overrideReason

### NOLCarryback
- carrybackId, nolId, carrybackYear
- priorYearTaxableIncome, nolApplied, priorYearTaxRate
- refundAmount, refundStatus, refundDate

### NOLSchedule
- scheduleId, returnId, taxYear
- totalBeginningBalance, newNOLGenerated, totalAvailableNOL
- nolDeduction, expiredNOL, totalEndingBalance
- limitationPercentage, taxableIncomeBeforeNOL, taxableIncomeAfterNOL
- nolVintages[] (array of NOLUsage records)

### NOLExpirationAlert
- alertId, businessId, nolId, taxYear, nolBalance, expirationDate
- yearsUntilExpiration, severityLevel (CRITICAL/WARNING/INFO)
- alertMessage, dismissed

### NOLAmendment
- amendmentId, nolId, originalReturnId, amendedReturnId
- originalNOL, amendedNOL, nolChange, reasonForAmendment
- affectedYears[], estimatedRefund, cascadingAmendments[]

## Success Criteria

- 100% of NOL carryforwards automatically tracked across years (no manual entry)
- Zero NOLs expire unused due to lack of tracking alerts
- 100% of post-2017 NOL deductions comply with 80% limitation
- Businesses with 2018-2020 losses claim average $10K-$50K refunds via carryback
- NOL schedules pass audit with zero adjustments
- NOL schedule preparation takes 10 minutes (vs 1-2 hours manual)

## Edge Cases Documented

- NOL exceeds 20-year carryforward (expires)
- Carryback to zero-income year
- Negative NOL balance (data entry error)
- Mid-year entity type change
- State addback creates negative state NOL
- Multiple NOL vintages with different expiration dates
- Carryback refund exceeds tax paid
- Amended return eliminates NOL

## Technical Implementation

### NOLService.java
- [ ] createNOL(), trackUsage(), calculateAvailable()
- [ ] applyCarryback(), checkExpiration(), generateSchedule()

### NOLController.java
- [ ] GET /api/nol/{businessId}/{taxYear}
- [ ] POST /api/nol/carryback/{nolId}
- [ ] GET /api/nol/expiration-alerts/{businessId}

### NOLScheduleView.tsx
- [ ] Display NOL vintage table
- [ ] Show current year calculation
- [ ] Expiration alerts with warnings
- [ ] Carryback election interface

## Dependencies

- Rule Engine (Spec 4) - NOL limitation %, carryforward period, carryback rules
- Schedule X (Spec 2) - Book-to-tax differences affect NOL calculation
- Enhanced Discrepancy Detection (Spec 3) - Validate NOL against available balance
- Schedule Y (Spec 5) - Multi-state apportionment for state NOL
- Business Form Library (Spec 8) - Form 27-NOL and Form 27-NOL-CB generation
- Historical Return Data - Retrieve prior year returns for carryback

## Related Specs

- Feeds into: Spec 11 (Consolidated Returns - consolidated NOL tracking)
- Used by: Spec 2 (Schedule X adjustments affect NOL)
- Integrates with: Spec 5 (Apportionment affects state NOL)
