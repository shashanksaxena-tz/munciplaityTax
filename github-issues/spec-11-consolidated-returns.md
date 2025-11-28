# Spec 11: Affiliated Group Consolidated Tax Return Filing

**Priority:** MEDIUM  
**Feature Branch:** `11-consolidated-returns`  
**Spec Document:** `specs/11-consolidated-returns/spec.md`

## Overview

Implement consolidated return filing for affiliated business groups (parent corporation with 80%+ owned subsidiaries) that elect to file a single combined municipal tax return instead of separate returns for each entity. Includes intercompany transaction eliminations, combined apportionment calculation, consolidated NOL tracking, and proper allocation of tax liability among group members.

## Implementation Status

**Current:** 0% - Each entity must file separately  
**Required:** Full consolidated return system with intercompany eliminations

## Core Requirements (FR-001 to FR-044)

### Affiliated Group Management (FR-001 to FR-005)
- [ ] Support creation of affiliated group with:
  - Group name, Group ID, Filing entity (parent FEIN)
  - Member entities (parent + subsidiaries), Ownership structure
  - Election date, Election term (typically 5 years binding)
- [ ] Validate consolidated filing eligibility:
  - Parent owns 80%+ of each subsidiary (by vote and value)
  - All entities use same tax year and accounting method
  - All entities must consent to consolidation
- [ ] Display ownership structure as tree diagram
- [ ] Lock consolidation election (cannot change mid-year)
- [ ] Support adding/removing entities mid-year with pro-rating

### Intercompany Transaction Elimination (FR-006 to FR-012)
- [ ] Identify intercompany transactions requiring elimination:
  - Sales of goods, services, interest, royalties, rent, dividends, loans
- [ ] Require entities to report intercompany transactions with counterparty
- [ ] Validate matching intercompany transactions (sales match purchases)
- [ ] Eliminate intercompany transactions from consolidated income:
  - Remove revenue from selling entity, expense from buying entity
  - Net impact should be zero for at-cost transactions
- [ ] Generate elimination schedule showing transaction type, parties, amounts
- [ ] Eliminate intercompany dividends (100% eliminated)
- [ ] Handle deferred intercompany profit (inventory not yet sold to third party)

### Consolidated Income Calculation (FR-013 to FR-016)
- [ ] Calculate consolidated income:
  - Sum of all entities' standalone incomes
  - Minus intercompany eliminations
  - Equals consolidated taxable income
- [ ] Handle consolidated losses (losses offset profits automatically)
- [ ] Apply consolidated adjustments (Schedule X at group level)
- [ ] Calculate consolidated NOL if group has net loss

### Combined Apportionment (FR-017 to FR-021)
- [ ] Calculate combined apportionment factors:
  - Property: Sum of all OH property / Sum of all total property
  - Payroll: Sum of all OH payroll / Sum of all total payroll
  - Sales: Sum of all OH sales / Sum of all total sales
- [ ] Handle sourcing elections at consolidated level (Joyce/Finnigan, throwback)
- [ ] Apply Ohio apportionment formula to combined factors
- [ ] Calculate consolidated Ohio taxable income: Consolidated × Apportionment %
- [ ] Generate consolidated Schedule Y with combined factors

### Tax Liability Allocation (FR-022 to FR-027)
- [ ] Calculate consolidated tax liability on consolidated Ohio taxable income
- [ ] Support allocation methods:
  - Pro-rata by standalone taxable income
  - Pro-rata by apportioned income
  - Pro-rata by Ohio-source income only
  - Custom allocation (manual percentages)
- [ ] Allocate consolidated tax liability to entities per chosen method
- [ ] Allocate penalties and interest using same method
- [ ] Generate internal allocation report for intercompany billing
- [ ] Support intercompany tax settlement tracking

### Consolidated Forms & Reporting (FR-028 to FR-032)
- [ ] Generate consolidated Form 27 with:
  - Filing entity (parent FEIN and name), designation "Consolidated Return"
  - Schedule of all member entities with FEINs
  - Consolidated income calculation, eliminations schedule
  - Consolidated Ohio taxable income, consolidated tax liability
- [ ] Generate consolidated Schedule Y (combined factors)
- [ ] Generate consolidated Schedule X (group-level adjustments)
- [ ] Attach entity-level schedules showing standalone income
- [ ] Generate intercompany elimination schedule

### Consolidated NOL Tracking (FR-033 to FR-037)
- [ ] Create consolidated NOL when group has net loss
- [ ] Allocate consolidated NOL to loss-generating entities pro-rata by losses
- [ ] Track consolidated NOL separately from standalone NOLs
- [ ] Apply consolidated NOL in future years to consolidated income (80% limit)
- [ ] Handle NOL upon deconsolidation:
  - Allocate remaining NOL to entities per allocation schedule
  - Convert consolidated NOL to standalone NOLs
  - Ensure all NOL is allocated (no orphaned NOL)

### Deconsolidation (FR-038 to FR-041)
- [ ] Support deconsolidation when:
  - Group elects to terminate (after binding period)
  - Entity sold or ownership drops below 80%
  - Group fails eligibility requirements
- [ ] Allocate consolidated NOL upon deconsolidation
- [ ] Calculate final consolidated return for year of deconsolidation
- [ ] Convert entities back to standalone filing for following years

### Validation & Audit Trail (FR-042 to FR-044)
- [ ] Validate consolidated return:
  - All member entities reported income
  - Intercompany transactions reconciled
  - Eliminations net to zero (or explainable)
  - Apportionment factors sum correctly
  - Tax allocation equals total consolidated tax
- [ ] Create audit trail for consolidation: election, eliminations, allocations
- [ ] Flag potential errors: unmatched intercompany, ownership <80%, mismatched tax years

## User Stories (5 Priority P1-P3)

1. **US-1 (P1):** Elect Consolidated Filing for Affiliated Group
2. **US-2 (P1):** Eliminate Intercompany Transactions
3. **US-3 (P2):** Calculate Consolidated Apportionment
4. **US-4 (P2):** Allocate Consolidated Tax Liability Among Group Members
5. **US-5 (P3):** Track Consolidated NOL Separately from Standalone NOLs

## Key Entities

### AffiliatedGroup
- groupId, groupName, filingEntity (parent company)
- filingFEIN, electionDate, electionTerm (years)
- memberEntities[] (array of GroupMember), status

### GroupMember
- entityId, entityFEIN, entityName
- role (PARENT/SUBSIDIARY), ownershipPercentage
- joinDate, leaveDate

### IntercompanyTransaction
- transactionId, groupId, taxYear
- transactionType (SALES_GOODS/SERVICES/INTEREST/ROYALTIES/RENT/DIVIDENDS)
- sellerEntityId, buyerEntityId, amount
- eliminationAmount, deferredProfit, matched

### ConsolidatedIncome
- consolidatedIncomeId, groupId, returnId, taxYear
- standaloneIncomes (JSON), totalStandaloneIncome
- intercompanyEliminations, consolidatedTaxableIncome
- ohioApportionmentPercentage, ohioTaxableIncome
- consolidatedTaxLiability

### TaxAllocation
- allocationId, groupId, returnId, taxYear
- allocationMethod (STANDALONE_INCOME/APPORTIONED_INCOME/OHIO_SOURCE/CUSTOM)
- entityAllocations[] (array of EntityTaxAllocation)
- totalConsolidatedTax

### EntityTaxAllocation
- entityId, allocationBasis, allocationPercentage
- allocatedTax, allocatedPenalties, allocatedInterest
- totalAllocated

### ConsolidatedNOL
- consolidatedNOLId, groupId, taxYear
- totalConsolidatedNOL, entityAllocations (JSON)
- remainingBalance, deconsolidationDate

## Success Criteria

- Consolidated groups file 1 return instead of N returns (75% reduction for 4-entity group)
- Losses offset profits automatically (vs separate filing where losses may go unused)
- 100% of intercompany eliminations reconcile (zero unmatched transactions)
- Consolidated returns with full elimination schedules pass audit with zero adjustments
- NOL properly allocated when group terminates (no "lost" NOL)

## Edge Cases Documented

- Mid-year acquisition (entity joins group mid-year, pro-rate income)
- Mid-year sale (entity leaves group mid-year, final consolidated return)
- Ownership fluctuates around 80% (average determines eligibility)
- Intercompany markup (profit deferral required)
- Circular intercompany transactions
- Negative apportionment factor (treat as zero)
- Consolidated NOL exceeds individual NOLs
- Triple-weighted sales factor applied to group

## Technical Implementation

### Backend Services
- [ ] AffiliatedGroupService.java
- [ ] IntercompanyTransactionService.java
- [ ] ConsolidatedIncomeService.java
- [ ] ConsolidatedApportionmentService.java
- [ ] TaxAllocationService.java

### Controllers
- [ ] ConsolidatedReturnController.java
  - POST /api/consolidated/group/create
  - POST /api/consolidated/intercompany-transaction
  - GET /api/consolidated/income/{groupId}/{taxYear}
  - POST /api/consolidated/allocate/{returnId}

### Frontend Components
- [ ] AffiliatedGroupBuilder.tsx
- [ ] OwnershipTreeView.tsx
- [ ] IntercompanyTransactionForm.tsx
- [ ] EliminationScheduleView.tsx
- [ ] ConsolidatedIncomeView.tsx
- [ ] TaxAllocationView.tsx

## Dependencies

- Schedule Y Sourcing (Spec 5) - Combined apportionment calculation
- NOL Carryforward Tracker (Spec 6) - Consolidated NOL tracking and allocation
- Business Form Library (Spec 8) - Consolidated Form 27 generation with entity schedule
- Rule Engine (Spec 4) - Consolidation eligibility rules, allocation methods

## Out of Scope

- Federal consolidated returns (IRS Form 1120 consolidation)
- State consolidated returns (Ohio IT-1120)
- International consolidation (foreign subsidiaries)
- Chain ownership (Parent → Sub1 → Sub2 indirect ownership)
- Brother-sister controlled groups

## Related Specs

- Uses: Spec 5 (Apportionment for combined factors)
- Integrates with: Spec 6 (NOL tracking for consolidated NOL)
- Complex feature requiring: Spec 2 (Schedule X), Spec 8 (Forms)
