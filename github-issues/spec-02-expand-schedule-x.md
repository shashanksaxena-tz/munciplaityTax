# Spec 2: Comprehensive Business Schedule X Reconciliation (25+ Fields)

**Priority:** HIGH  
**Feature Branch:** `2-expand-schedule-x`  
**Spec Document:** `specs/2-expand-schedule-x/spec.md`

## Overview

Expand Schedule X from 6 basic fields to 25+ comprehensive book-to-tax reconciliation fields including depreciation (MACRS vs Book), amortization, officer compensation limits, related-party transactions, charitable contributions, meals & entertainment (50% rule), penalties/fines, and all standard M-1 adjustments.

## Implementation Status

**Current:** ~65% Complete (per IMPLEMENTATION_STATUS.md)  
**Completed:**
- ✅ Backend BusinessScheduleXDetails model with all 27 fields
- ✅ ScheduleXCalculationService, ScheduleXValidationService, ScheduleXAutoCalculationService
- ✅ Frontend services: scheduleXService.ts, autoCalculationService.ts
- ✅ React hooks: useScheduleX.ts, useScheduleXAutoCalc.ts
- ✅ Validation components and utilities

**Remaining Work (~35%):**

## Critical Tasks for MVP (US1-US2)

### Frontend UI Completion
- [ ] Complete ScheduleXAccordion.tsx with all 27 fields (currently has 3)
  - Add-backs section: 20 fields (FR-001 to FR-020)
  - Deductions section: 7 fields (FR-021 to FR-027)
- [ ] Integrate ScheduleXAccordion into NetProfitsWizard as Step 3
- [ ] Wire up auto-calculation buttons for meals, 5% Rule, charitable contributions

### Backend Verification
- [ ] Verify/implement auto-calculation methods in ScheduleXAutoCalculationService
  - Meals & Entertainment (50% → 100% conversion)
  - 5% Rule for intangible income expenses
  - Charitable contributions 10% limit
  - Related-party excess calculation
- [ ] Test AI extraction updates for Form 1120/1065 parsing

### Testing
- [ ] Integration tests for US1 (C-Corp M-1 reconciliation)
- [ ] Integration tests for US2 (Partnership with guaranteed payments)
- [ ] Validation tests for >20% variance detection (FR-034)

## Full Implementation Tasks (US3-US5)

### Entity-Specific Logic
- [ ] S-Corp related-party transaction validation (US3)
- [ ] Charitable contribution carryforward tracking (US4)
- [ ] DPAD (Domestic Production Activities) deduction handling (US5)
- [ ] Entity-specific income inclusion rules

### Advanced Features
- [ ] Multi-year comparison view (FR-038)
- [ ] Supporting documentation attachment per field (FR-036)
- [ ] Auto-calculation confidence tracking (FR-042, FR-043)
- [ ] Override history for audit trail (FR-037)

## Core Requirements (FR-001 to FR-043)

### Add-Back Fields (20 fields)
- Depreciation/Amortization adjustments
- Income taxes (state/local/foreign)
- Guaranteed payments, Meals & Entertainment
- Related-party excess, Penalties/Fines
- Section 179 excess, Bonus depreciation
- Bad debt reserve, Stock compensation
- And 11 more categories

### Deduction Fields (7 fields)
- Interest income, Dividends, Capital gains
- Municipal bond interest, Depletion difference
- Other deductions with descriptions

### Calculation & Validation
- Total add-backs and deductions auto-sum
- Adjusted Municipal Taxable Income = Federal + Add-backs - Deductions
- Validation flag if difference >20% from Federal (FR-034)

## User Stories (5 Priority P1-P3)

1. **US-1 (P1):** CPA Performs Complete M-1 Reconciliation for C-Corporation - 60% complete
2. **US-2 (P1):** Partnership Files Form 27 with Guaranteed Payments and Intangible Income - 50% complete
3. **US-3 (P2):** S-Corporation with Related-Party Transactions and Officer Compensation - 40% complete
4. **US-4 (P2):** Corporation with Charitable Contributions Exceeding 10% Limit - 35% complete
5. **US-5 (P3):** Service Business with Domestic Production Activities Deduction - 30% complete

## Success Criteria

- CPAs complete full Schedule X reconciliation in under 10 minutes (vs 45+ minutes manual)
- 90% of AI-extracted Schedule X fields require zero manual correction
- System handles 95% of common book-to-tax adjustments without manual formula entry
- Adjusted Municipal Income calculation matches CPA's workpaper within $100 (98% accuracy)
- Multi-year comparison view loads in under 2 seconds

## Estimated Remaining Effort

**Option A - MVP Focus (US1-US2):** ~9 hours
- Complete ScheduleXAccordion with US1-US2 fields (4-5 hours)
- NetProfitsWizard integration (1 hour)
- Backend auto-calc method verification (2 hours)
- Basic integration tests (2 hours)

**Option B - Full Implementation (US1-US5):** ~14 hours
- Complete ScheduleXAccordion with ALL 27 fields (6 hours)
- NetProfitsWizard integration (1 hour)
- Implement all auto-calc methods (3 hours)
- Comprehensive testing (4 hours)

## Dependencies

- AI Extraction Service (Gemini) - must parse Form 1120 Schedule M-1, Form 4562
- PDF Generation Service - expanded Schedule X layout
- BusinessTaxCalculator.java - uses expanded BusinessScheduleXDetails
- NetProfitsWizard UI - needs redesign for 27 fields (accordion recommended)
- Constants: DEFAULT_BUSINESS_RULES updates

## Technical Notes

- All 27 fields exist in backend model (BusinessScheduleXDetails)
- Frontend infrastructure (services, hooks, validation) complete as of current session
- Primary work is UI completion and integration
- AI extraction prompts need updates for additional fields

## Related Specs

- Feeds into: Spec 6 (NOL tracking - affects consolidated income)
- Integrates with: Spec 8 (Form Library - Form 27-X generation)
- Uses: Spec 4 (Rule Engine - for calculation rules)
