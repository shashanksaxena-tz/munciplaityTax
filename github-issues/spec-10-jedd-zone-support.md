# Spec 10: Joint Economic Development District (JEDD) Tax Allocation

**Priority:** MEDIUM  
**Feature Branch:** `10-jedd-zone-support`  
**Spec Document:** `specs/10-jedd-zone-support/spec.md`

## Overview

Implement Joint Economic Development District (JEDD) tax allocation system for businesses operating in special economic zones shared by multiple municipalities. JEDD agreements allow municipalities to collaborate by sharing tax revenue from designated zones according to contractual percentages.

## Implementation Status

**Current:** 0% - System treats each municipality independently  
**Required:** Full JEDD zone management and multi-jurisdiction allocation

## Core Requirements (FR-001 to FR-035)

### JEDD Zone Management (FR-001 to FR-005)
- [ ] Maintain database of JEDD zones with attributes:
  - Zone ID, name, participating municipalities (2-3 typically)
  - Revenue allocation percentages (must sum to 100%)
  - Geographic boundaries (polygon coordinates or address ranges)
  - Effective date, expiration date, JEDD agreement document (PDF)
- [ ] Support geocoding to determine if business address falls within JEDD zone
- [ ] Allow manual JEDD zone assignment if geocoding fails
- [ ] Validate JEDD configuration: percentages sum to 100%, no overlapping zones
- [ ] Display JEDD zone information on business profile

### Business Income Allocation (FR-006 to FR-012)
- [ ] Allocate business net profit to JEDD municipalities by configured percentages
- [ ] Apply each municipality's tax rate independently to allocated income
- [ ] Calculate total JEDD tax (sum of all municipality taxes)
- [ ] Generate separate tax return for each JEDD municipality (Form 27-JEDD)
- [ ] Display allocation breakdown with income and tax per municipality
- [ ] Support multi-location businesses:
  - Apportion income to locations by payroll, revenue, or property
  - Apply JEDD allocation to JEDD-location income only
- [ ] Handle JEDD zone changes mid-year with pro-rating

### Withholding for JEDD Employees (FR-013 to FR-019)
- [ ] Calculate blended JEDD withholding rate: Σ(Municipality % × Rate)
  - Example: (60% × 2.5%) + (40% × 2.0%) = 2.3%
- [ ] Withhold from employee wages at blended JEDD rate
- [ ] Allocate withheld tax to JEDD municipalities by percentages for W-1 reporting
- [ ] Generate separate W-1 withholding reports for each JEDD municipality
- [ ] Track year-to-date withholding by municipality for each employee
- [ ] Generate W-2 for JEDD employees with total JEDD wages and withholding
- [ ] Handle employees working multiple locations (JEDD and non-JEDD)

### Payment Processing & Distribution (FR-020 to FR-024)
- [ ] Accept single payment for total JEDD tax
- [ ] Distribute payment to municipality accounts by JEDD percentages
- [ ] Track payment status separately for each municipality
- [ ] Generate payment vouchers for each JEDD municipality
- [ ] Support electronic payment with automatic distribution

### JEDD Forms & Reporting (FR-025 to FR-028)
- [ ] Generate Form 27-JEDD for each participating municipality with:
  - Total business income, allocated income, tax rate, tax due
  - Withholding credits allocated to municipality, net tax due
- [ ] Generate JEDD allocation summary (master document)
- [ ] Generate W-1 withholding reports for each JEDD municipality
- [ ] Generate annual reconciliation report for JEDD businesses

### Municipality Revenue Reporting (FR-029 to FR-032)
- [ ] Generate JEDD revenue report for municipality finance officers with:
  - All businesses in municipality's JEDD zones
  - Tax collected, municipality's share, partner municipalities' shares
  - Payment status, summary totals
- [ ] Support filtering by: JEDD zone, Quarter, Tax year, Payment status
- [ ] Export JEDD report as PDF and Excel
- [ ] Highlight discrepancies (wrong municipality payment, allocation mismatch)

### Validation & Audit (FR-033 to FR-035)
- [ ] Validate JEDD allocations: percentages sum to 100%, allocated income sums to total
- [ ] Create audit trail for JEDD transactions: allocation calculations, tax calculations, payment distribution
- [ ] Flag potential errors: JEDD business filing with single municipality only, payment mismatch

## User Stories (5 Priority P1-P3)

1. **US-1 (P1):** Identify Business Location in JEDD Zone
2. **US-2 (P1):** Allocate Business Income Tax Across JEDD Municipalities
3. **US-3 (P1):** Withhold JEDD Tax from Employee Wages
4. **US-4 (P2):** Handle Multi-Location Business (JEDD + Non-JEDD)
5. **US-5 (P3):** JEDD Revenue Distribution Report for Municipalities

## Key Entities

### JEDDZone
- zoneId, zoneName ("Dublin-Columbus JEDD Zone 1")
- participatingMunicipalities[] (array of MunicipalityAllocation objects)
- geographicBoundaries (GeoJSON), addressRanges[]
- effectiveDate, expirationDate, agreementDocumentPath

### MunicipalityAllocation
- municipalityCode, municipalityName
- allocationPercentage (60.0, 40.0 - must sum to 100)
- taxRate

### JEDDBusinessProfile
- businessId, jeddZoneId, inJEDD, jeddStartDate, jeddEndDate
- multiLocation, allocationMethod (PAYROLL/REVENUE/PROPERTY)

### JEDDTaxAllocation
- allocationId, returnId, jeddZoneId, totalIncome
- allocations[] (array of MunicipalityTaxAllocation)
- totalJEDDTax

### MunicipalityTaxAllocation
- municipalityCode, allocationPercentage, allocatedIncome
- taxRate, taxDue, withholdingCredit, netTaxDue

### JEDDWithholding
- withholdingId, employeeId, jeddZoneId, payPeriod
- wagesEarned, blendedRate, totalWithholding
- allocations[], w2Box18, w2Box19

### JEDDRevenueReport
- reportId, municipalityCode, reportPeriod
- jeddZones[], businesses[], totalTaxCollected
- municipalityShare, partnerShares, paymentStatus
- generatedDate

## Success Criteria

- 100% of JEDD-zone businesses correctly identified and allocated
- Zero manual calculation required for JEDD splits (automatic allocation)
- Blended JEDD rates calculated correctly 100% of time
- Municipality revenue reports reconcile to 100% accuracy
- JEDD businesses file with all participating municipalities (zero missed filings)

## Edge Cases Documented

- Business on JEDD boundary (geocoding ambiguous)
- JEDD zone expires mid-year
- Municipality changes tax rate mid-year in JEDD
- Employee works multiple JEDD zones
- Partial payment allocated incorrectly
- JEDD business files with one municipality only
- Municipality leaves JEDD mid-year
- JEDD with 3+ municipalities

## Technical Implementation

### Backend Services
- [ ] JEDDZoneService.java
- [ ] JEDDAllocationService.java
- [ ] JEDDWithholdingService.java
- [ ] JEDDRevenueReportService.java

### Controllers
- [ ] JEDDController.java
  - GET /api/jedd/zone/lookup?address={address}
  - POST /api/jedd/allocate/{returnId}
  - GET /api/jedd/withholding/{employeeId}
  - GET /api/jedd/revenue-report/{municipalityCode}

### Frontend Components
- [ ] JEDDZoneIndicator.tsx
- [ ] JEDDAllocationBreakdown.tsx
- [ ] JEDDWithholdingCalculator.tsx
- [ ] JEDDRevenueReport.tsx

### GIS Integration
- [ ] Geocoding service for address lookup
- [ ] Polygon boundary checking for JEDD zones
- [ ] Address validation against zone boundaries

## Dependencies

- Rule Engine (Spec 4) - JEDD zone configurations, allocation percentages, tax rates
- Schedule Y Sourcing (Spec 5) - Multi-location apportionment logic for JEDD + non-JEDD
- Withholding Reconciliation (Spec 1) - W-1 forms generated separately for each JEDD municipality
- Business Form Library (Spec 8) - Form 27-JEDD generation
- Double-Entry Ledger (Spec 12) - Payment distribution to multiple municipality accounts

## Out of Scope

- JEDD vs TIF zones (Tax Increment Financing)
- Enterprise zones (different tax incentives)
- Inter-state JEDD (Ohio-Kentucky cross-state agreements)
- JEDD zone creation (administrative process)

## Related Specs

- Integrates with: Spec 5 (Apportionment for multi-location + JEDD)
- Uses: Spec 1 (Withholding for JEDD employees)
- Feeds into: Spec 12 (Ledger for payment distribution)
