# Consolidated Returns for Affiliated Groups

**Feature Name:** Affiliated Group Consolidated Tax Return Filing  
**Priority:** MEDIUM  
**Status:** Specification  
**Created:** 2025-11-27

---

## Overview

Implement consolidated return filing for affiliated business groups (parent corporation with 80%+ owned subsidiaries) that elect to file a single combined municipal tax return instead of separate returns for each entity. This includes intercompany transaction eliminations, combined apportionment calculation, consolidated NOL tracking, and proper allocation of tax liability among group members. This feature is essential for corporate groups to minimize administrative burden and optimize tax efficiency.

**Current State:** No consolidated return support (0% complete). Each entity must file separately even if part of controlled group.

**Target Users:** CFOs of multi-entity corporations, tax managers of holding companies, CPAs preparing consolidated returns, parent companies with multiple subsidiaries.

---

## User Scenarios & Testing

### US-1: Elect Consolidated Filing for Affiliated Group (P1 - Critical)

**User Story:**  
As a parent company with 3 wholly-owned subsidiaries, I want to elect consolidated filing for our affiliated group, designate the parent as the filing entity, and have all subsidiaries included automatically, so that we file one return instead of four separate returns.

**Business Context:**  
Consolidated filing allowed when parent owns 80%+ of subsidiaries. Election typically binding for 5 years. Benefits: Offset profits with losses, single filing, reduced administrative burden. Requirements: All entities must use same tax year, same accounting method.

**Independent Test:**  
- ParentCo (FEIN 12-3456789) owns:
  - SubA (100% owned, FEIN 23-4567890)
  - SubB (100% owned, FEIN 34-5678901)
  - SubC (85% owned, FEIN 45-6789012)
- All use calendar year, accrual method
- Parent elects consolidated filing for 2024
- System creates affiliated group "ParentCo Consolidated Group"
- Single return filed under ParentCo FEIN with all 4 entities included

**Acceptance Criteria:**
- GIVEN parent company with qualifying subsidiaries (80%+ owned)
- WHEN parent elects consolidated filing
- THEN system MUST create affiliated group with:
  - Group name (default: Parent company name + "Consolidated Group")
  - Filing entity (parent company)
  - Group FEIN (parent's FEIN used for return)
  - Member entities (parent + subsidiaries)
  - Ownership percentages
  - Election date
  - Election term (default 5 years, binding)
- AND system MUST validate eligibility:
  - Parent owns 80%+ of each subsidiary
  - All entities use same tax year
  - All entities use same accounting method (cash vs accrual)
  - All entities consent to consolidation
- AND system MUST display group structure with ownership tree
- AND system MUST lock election (cannot undo mid-year)

---

### US-2: Eliminate Intercompany Transactions (P1 - Critical)

**User Story:**  
As a CFO preparing a consolidated return, I want the system to automatically eliminate intercompany transactions (ParentCo sold $1M inventory to SubA), so that the consolidated return reports only third-party revenue and expenses, avoiding double-counting.

**Business Context:**  
Intercompany transactions must be eliminated to avoid overstating income/expenses. Examples:
- ParentCo sells $1M inventory to SubA at cost → Eliminate $1M from ParentCo revenue and SubA COGS
- SubA pays $100K interest to ParentCo → Eliminate $100K from SubA interest expense and ParentCo interest income
- SubB provides $500K services to SubC → Eliminate $500K from both

Without eliminations, consolidated group shows inflated activity.

**Independent Test:**  
- ParentCo standalone income: $5M
- SubA standalone income: $2M
- Intercompany sales: ParentCo → SubA $1M (at cost, no markup)
- Consolidated income: $5M (ParentCo third-party) + $2M (SubA third-party) = $7M
- Eliminations: ParentCo revenue -$1M, SubA COGS -$1M (net zero effect on income)
- Consolidated return shows: $7M income, no intercompany sales

**Acceptance Criteria:**
- GIVEN affiliated group with intercompany transactions
- WHEN calculating consolidated income
- THEN system MUST identify intercompany transactions:
  - Intercompany sales of goods
  - Intercompany services
  - Intercompany interest income/expense
  - Intercompany royalties
  - Intercompany rent
  - Intercompany dividends
- AND system MUST eliminate matching transactions:
  - If ParentCo reports $1M intercompany revenue, SubA must report $1M intercompany expense (or COGS)
  - Eliminate both: Revenue -$1M, Expense -$1M
- AND system MUST validate eliminations net to zero (revenue elimination = expense elimination)
- AND system MUST display elimination schedule showing:
  - Transaction type (sales, interest, services, etc.)
  - Parties (e.g., "ParentCo → SubA")
  - Amount eliminated
  - Impact on consolidated income (should be zero for at-cost transactions)

---

### US-3: Calculate Consolidated Apportionment (P2 - High Value)

**User Story:**  
As a consolidated group with entities in multiple states, I want the system to calculate combined apportionment using the group's total property, payroll, and sales factors, so that we correctly allocate consolidated income to Ohio.

**Business Context:**  
Consolidated apportionment uses combined factors. If ParentCo has $10M sales ($2M Ohio) and SubA has $5M sales ($3M Ohio), consolidated sales factor = ($2M + $3M) / ($10M + $5M) = 33.33%. This determines what portion of consolidated income is taxable in Ohio.

**Independent Test:**  
- ParentCo: $10M total sales, $2M OH sales
- SubA: $5M total sales, $3M OH sales
- SubB: $8M total sales, $1M OH sales
- Consolidated sales factor: ($2M + $3M + $1M) / ($10M + $5M + $8M) = $6M / $23M = 26.09%
- If consolidated income is $10M, OH taxable income = $10M × 26.09% = $2.609M

**Acceptance Criteria:**
- GIVEN consolidated group with multi-state operations
- WHEN calculating Ohio apportionment
- THEN system MUST combine all entities' factors:
  - Property factor: (Sum of all OH property) / (Sum of all total property)
  - Payroll factor: (Sum of all OH payroll) / (Sum of all total payroll)
  - Sales factor: (Sum of all OH sales) / (Sum of all total sales)
- AND system MUST apply Ohio formula (4-factor double-weighted sales or custom)
- AND system MUST calculate consolidated Ohio apportionment percentage
- AND system MUST apply to consolidated income: OH income = Consolidated income × Apportionment %
- AND system MUST display combined Schedule Y showing each entity's contribution to factors

---

### US-4: Allocate Consolidated Tax Liability Among Group Members (P2 - High Value)

**User Story:**  
As a parent company, I want the system to allocate the consolidated tax liability among group members (ParentCo pays 60%, SubA pays 25%, SubB pays 15%) based on each entity's contribution to consolidated income, so that we can charge each entity for their share and track internal cost allocation.

**Business Context:**  
Parent pays consolidated tax but internally allocates to subsidiaries. Allocation methods:
- Pro-rata by standalone income (most common)
- Pro-rata by apportioned income
- Custom allocation (e.g., parent pays all)

This is internal accounting, not required by municipality, but essential for financial reporting.

**Independent Test:**  
- Consolidated OH taxable income: $3M, Tax @ 2.5% = $75K
- Entity standalone incomes: ParentCo $5M, SubA $2M, SubB $1M (total $8M)
- Allocation by standalone income:
  - ParentCo: ($5M / $8M) × $75K = $46,875 (62.5%)
  - SubA: ($2M / $8M) × $75K = $18,750 (25%)
  - SubB: ($1M / $8M) × $75K = $9,375 (12.5%)
- Parent collects $18,750 from SubA and $9,375 from SubB for reimbursement

**Acceptance Criteria:**
- GIVEN consolidated tax liability calculated
- WHEN allocating among group members
- THEN system MUST support allocation methods:
  - Pro-rata by standalone taxable income (before eliminations)
  - Pro-rata by apportioned income (after apportionment factor)
  - Pro-rata by OH-source income only
  - Custom percentages (manually entered)
- AND system MUST calculate allocated liability per entity
- AND system MUST display allocation schedule:
  - Entity name
  - Standalone income (or allocation basis)
  - Allocation percentage
  - Allocated tax liability
  - Allocated penalties/interest (if any)
- AND system MUST generate internal allocation report for accounting purposes

---

### US-5: Track Consolidated NOL Separately from Standalone NOLs (P3 - Future)

**User Story:**  
As a consolidated group that generated a $2M NOL in 2023 (ParentCo loss $3M offset by SubA profit $1M), I want the system to track the consolidated NOL separately from each entity's standalone NOL, so that we correctly apply the NOL in future years and handle deconsolidation if an entity leaves the group.

**Business Context:**  
Consolidated NOL is different from standalone NOLs. If group deconsolidates, must allocate NOL to entities. Complex rules: NOL allocated to loss-generating entities pro-rata. If entity leaves group, it takes its allocated NOL.

**Independent Test:**  
- 2023 standalone incomes: ParentCo -$3M, SubA $1M, SubB $0 → Total -$2M
- 2023 consolidated NOL: $2M (allocated: ParentCo $2M, SubA $0, SubB $0)
- 2024: Group continues consolidated → Use $2M NOL against consolidated income
- 2025: SubA sold, group deconsolidates → ParentCo keeps $2M NOL, SubA/SubB get $0

**Acceptance Criteria:**
- GIVEN consolidated group with net operating loss
- WHEN generating consolidated NOL
- THEN system MUST create consolidated NOL record with:
  - Total consolidated NOL
  - Allocation to loss-generating entities (pro-rata by losses)
  - Carryforward period and expiration date
- AND system MUST track consolidated NOL separately from standalone NOLs
- AND system MUST apply consolidated NOL to future consolidated income
- AND if group deconsolidates:
  - System MUST allocate consolidated NOL to entities per allocation schedule
  - System MUST convert to standalone NOLs for each entity
  - System MUST prevent "orphaned" NOL (all NOL must be allocated)

---

## Functional Requirements

### Affiliated Group Management

**FR-001:** System MUST support creation of affiliated group with attributes:
- Group name, Group ID, Filing entity (parent FEIN)
- Member entities (parent + subsidiaries)
- Ownership structure (parent owns X% of each sub)
- Election date, Election term (typically 5 years binding)

**FR-002:** System MUST validate consolidated filing eligibility:
- Parent owns 80%+ of each subsidiary (by vote and value)
- All entities use same tax year
- All entities use same accounting method
- All entities must consent to consolidation

**FR-003:** System MUST display ownership structure as tree diagram showing parent → subsidiaries with ownership percentages

**FR-004:** System MUST lock consolidation election (cannot change mid-year, binding for election term)

**FR-005:** System MUST support adding/removing entities:
- Add new subsidiary acquired during year (mid-year entry, pro-rate income)
- Remove subsidiary sold during year (mid-year exit, pro-rate income)

### Intercompany Transaction Elimination

**FR-006:** System MUST identify intercompany transactions requiring elimination:
- Intercompany sales of goods (inventory, fixed assets)
- Intercompany services
- Intercompany interest income/expense
- Intercompany royalties and licensing fees
- Intercompany rent
- Intercompany dividends
- Intercompany loans (interest-bearing)

**FR-007:** System MUST require entities to report intercompany transactions with counterparty identification:
- Transaction type, Counterparty (which group member), Amount
- Direction (from ParentCo to SubA vs from SubA to ParentCo)

**FR-008:** System MUST validate matching intercompany transactions:
- If ParentCo reports $1M intercompany sale to SubA, SubA must report $1M intercompany purchase from ParentCo
- If mismatch, flag for correction: "ParentCo reports $1M sale to SubA, but SubA reports $900K purchase. Reconcile difference."

**FR-009:** System MUST eliminate intercompany transactions from consolidated income:
- Remove intercompany revenue from selling entity
- Remove matching intercompany expense/COGS from buying entity
- Net impact on income should be zero for at-cost transactions
- For transactions with markup (e.g., sale above cost), eliminate profit

**FR-010:** System MUST generate elimination schedule showing:
- Transaction type and parties
- Amount eliminated from each entity
- Net impact on consolidated income

**FR-011:** System MUST eliminate intercompany dividends (parent subsidiary, always 100% eliminated)

**FR-012:** System MUST handle deferred intercompany profit:
- If ParentCo sells inventory to SubA for $1.2M (cost $1M), profit is $200K
- If SubA still holds inventory at year-end, defer $200K profit
- When SubA sells to third party, recognize $200K profit

### Consolidated Income Calculation

**FR-013:** System MUST calculate consolidated income:
- Sum of all entities' standalone incomes (before eliminations)
- Minus intercompany eliminations
- Equals consolidated taxable income

**FR-014:** System MUST handle consolidated losses:
- If some entities profitable, some at loss, net to consolidated income or loss
- Losses offset profits automatically in consolidated return

**FR-015:** System MUST apply consolidated adjustments (Schedule X book-tax adjustments at group level)

**FR-016:** System MUST calculate consolidated NOL if group has net loss

### Combined Apportionment

**FR-017:** System MUST calculate combined apportionment factors:
- Property factor: Sum of all OH property / Sum of all total property
- Payroll factor: Sum of all OH payroll / Sum of all total payroll
- Sales factor: Sum of all OH sales / Sum of all total sales

**FR-018:** System MUST handle sourcing elections at consolidated level (Joyce vs Finnigan, throwback rules apply to group as a whole)

**FR-019:** System MUST apply Ohio apportionment formula to combined factors

**FR-020:** System MUST calculate consolidated Ohio taxable income: Consolidated income × Apportionment %

**FR-021:** System MUST generate consolidated Schedule Y showing combined factors

### Tax Liability Allocation

**FR-022:** System MUST calculate consolidated tax liability on consolidated Ohio taxable income

**FR-023:** System MUST support allocation methods:
- Pro-rata by standalone taxable income
- Pro-rata by apportioned income (standalone × apportionment %)
- Pro-rata by Ohio-source income only
- Custom allocation (manual percentages)

**FR-024:** System MUST allocate consolidated tax liability to entities per chosen method

**FR-025:** System MUST allocate penalties and interest using same method

**FR-026:** System MUST generate internal allocation report for intercompany billing purposes

**FR-027:** System MUST support intercompany tax settlement tracking (subsidiary pays parent for allocated share)

### Consolidated Forms & Reporting

**FR-028:** System MUST generate consolidated Form 27 with:
- Filing entity: Parent company FEIN and name
- Designation: "Consolidated Return"
- Schedule showing all member entities with FEINs
- Consolidated income calculation
- Eliminations schedule
- Consolidated Ohio taxable income
- Consolidated tax liability

**FR-029:** System MUST generate consolidated Schedule Y (Apportionment) with combined factors

**FR-030:** System MUST generate consolidated Schedule X (Book-tax adjustments) at group level

**FR-031:** System MUST attach entity-level schedules showing each member's standalone income before consolidation

**FR-032:** System MUST generate intercompany elimination schedule as supporting document

### Consolidated NOL Tracking

**FR-033:** System MUST create consolidated NOL when group has net loss

**FR-034:** System MUST allocate consolidated NOL to loss-generating entities pro-rata by losses

**FR-035:** System MUST track consolidated NOL separately from standalone NOLs

**FR-036:** System MUST apply consolidated NOL in future years to consolidated income (subject to 80% limitation)

**FR-037:** System MUST handle NOL upon deconsolidation:
- Allocate remaining NOL to entities per allocation schedule
- Convert consolidated NOL to standalone NOLs for each entity
- Ensure all NOL is allocated (no orphaned NOL)

### Deconsolidation

**FR-038:** System MUST support deconsolidation when:
- Group elects to terminate consolidation (after binding period)
- Entity sold or ownership drops below 80%
- Group fails to meet eligibility requirements

**FR-039:** System MUST allocate consolidated NOL upon deconsolidation (FR-037)

**FR-040:** System MUST calculate final consolidated return for year of deconsolidation (pro-rate if mid-year)

**FR-041:** System MUST convert entities back to standalone filing for following years

### Validation & Audit Trail

**FR-042:** System MUST validate consolidated return:
- All member entities reported income
- Intercompany transactions reconciled (matching amounts)
- Eliminations net to zero (or explainable profit/loss)
- Apportionment factors sum correctly
- Tax allocation equals total consolidated tax

**FR-043:** System MUST create audit trail for consolidation:
- Consolidation election and member entities
- Intercompany transactions and eliminations
- Income aggregation and adjustments
- Apportionment calculation
- Tax allocation to entities

**FR-044:** System MUST flag potential errors:
- Unmatched intercompany transactions
- Ownership below 80% (disqualifies entity)
- Mismatched tax years or accounting methods
- Negative allocated tax (entity allocated more than total)

---

## Key Entities

### AffiliatedGroup

**Attributes:**
- `groupId` (UUID)
- `groupName` (string): "ParentCo Consolidated Group"
- `filingEntity` (UUID): Parent company (files consolidated return)
- `filingFEIN` (string): Parent's FEIN
- `electionDate` (date): When consolidation elected
- `electionTerm` (number): Years (typically 5)
- `memberEntities` (array): List of GroupMember objects
- `status` (enum): ACTIVE | DECONSOLIDATED | TERMINATED

### GroupMember

**Attributes:**
- `entityId` (UUID): Foreign key to Business
- `entityFEIN` (string)
- `entityName` (string)
- `role` (enum): PARENT | SUBSIDIARY
- `ownershipPercentage` (decimal): Parent's ownership (80-100%)
- `joinDate` (date): When entity joined group
- `leaveDate` (date): When entity left group (if applicable)

### IntercompanyTransaction

**Attributes:**
- `transactionId` (UUID)
- `groupId` (UUID): Foreign key to AffiliatedGroup
- `taxYear` (number)
- `transactionType` (enum): SALES_GOODS | SERVICES | INTEREST | ROYALTIES | RENT | DIVIDENDS
- `sellerEntityId` (UUID): Entity receiving income
- `buyerEntityId` (UUID): Entity paying expense
- `amount` (decimal): Transaction amount
- `eliminationAmount` (decimal): Amount to eliminate (may differ if markup)
- `deferredProfit` (decimal): Profit deferred to future year
- `matched` (boolean): Whether buyer/seller amounts reconcile

### ConsolidatedIncome

**Attributes:**
- `consolidatedIncomeId` (UUID)
- `groupId` (UUID)
- `returnId` (UUID): Consolidated return
- `taxYear` (number)
- `standaloneIncomes` (JSON): Each entity's income before eliminations
- `totalStandaloneIncome` (decimal): Sum of entity incomes
- `intercompanyEliminations` (decimal): Total eliminations
- `consolidatedTaxableIncome` (decimal): After eliminations
- `ohioApportionmentPercentage` (decimal)
- `ohioTaxableIncome` (decimal): Consolidated × Apportionment
- `consolidatedTaxLiability` (decimal)

### TaxAllocation

**Attributes:**
- `allocationId` (UUID)
- `groupId` (UUID)
- `returnId` (UUID)
- `taxYear` (number)
- `allocationMethod` (enum): STANDALONE_INCOME | APPORTIONED_INCOME | OHIO_SOURCE | CUSTOM
- `entityAllocations` (array): List of EntityTaxAllocation objects
- `totalConsolidatedTax` (decimal)

### EntityTaxAllocation

**Attributes:**
- `entityId` (UUID)
- `allocationBasis` (decimal): Standalone income or custom value
- `allocationPercentage` (decimal): % of consolidated tax
- `allocatedTax` (decimal): This entity's share
- `allocatedPenalties` (decimal)
- `allocatedInterest` (decimal)
- `totalAllocated` (decimal)

### ConsolidatedNOL

**Attributes:**
- `consolidatedNOLId` (UUID)
- `groupId` (UUID)
- `taxYear` (number): Year NOL originated
- `totalConsolidatedNOL` (decimal)
- `entityAllocations` (JSON): How NOL allocated to loss-generating entities
- `remainingBalance` (decimal): Available for future years
- `deconsolidationDate` (date): If group deconsolidated, when NOL split

---

## Success Criteria

- **Administrative Efficiency:** Consolidated groups file 1 return instead of N returns (75% reduction in filing burden for 4-entity group)
- **Tax Optimization:** Losses offset profits automatically (vs separate filing where losses may go unused)
- **Accuracy:** 100% of intercompany eliminations reconcile (zero unmatched transactions)
- **Audit Defense:** Consolidated returns with full elimination schedules pass audit with zero adjustments
- **Deconsolidation:** NOL properly allocated when group terminates, no "lost" NOL

---

## Assumptions

- Most consolidated groups are parent + subsidiaries (2-5 entities typical, <10 rare)
- All intercompany transactions are at arm's length or at cost (no complex transfer pricing)
- Ohio allows consolidated filing for municipal tax (check local rules)
- Consolidated election binding for 5 years (cannot terminate early without cause)
- All entities in group are C-Corps (S-Corps and Partnerships have different consolidation rules)

---

## Dependencies

- **Schedule Y Sourcing (Spec 5):** Combined apportionment calculation uses multi-state sourcing logic
- **NOL Carryforward Tracker (Spec 6):** Consolidated NOL tracking and allocation
- **Business Form Library (Spec 8):** Consolidated Form 27 generation with entity schedule
- **Rule Engine (Spec 4):** Consolidation eligibility rules, allocation methods configurable

---

## Out of Scope

- **Federal consolidated returns:** IRS Form 1120 consolidation (different rules, handled by federal tax software)
- **State consolidated returns:** Ohio IT-1120 consolidated (focus on municipal only)
- **International consolidation:** Including foreign subsidiaries (complex, rare)
- **Chain ownership:** Parent → Sub1 → Sub2 (indirect ownership, complex attribution rules)
- **Brother-sister controlled groups:** Common parent but separate entities (different from parent-subsidiary)

---

## Edge Cases

1. **Mid-year acquisition:** Parent acquires SubB on July 1. SubB included in consolidated return for Jul-Dec only (6 months). System pro-rates SubB income by days in group.

2. **Mid-year sale:** Parent sells SubA on September 30. SubA included Jan-Sep (9 months). System generates final consolidated return including SubA, then SubA files standalone for Q4.

3. **Ownership fluctuates around 80%:** Parent owns 82% of SubA at start of year, 78% at end (stock buyback by SubA). Average <80% → SubA disqualified. System alerts: "SubA ownership dropped below 80%. Deconsolidate SubA."

4. **Intercompany markup:** ParentCo sells inventory to SubA for $1.2M (cost $1M). SubA sells to customer for $1.5M. Eliminate $1.2M intercompany sale, defer $200K profit until SubA sells. When SubA sells, recognize $200K.

5. **Circular intercompany transactions:** ParentCo sells to SubA $1M, SubA sells to SubB $900K, SubB sells to ParentCo $800K. System eliminates all three transactions, nets to $1M + $900K + $800K = $2.7M total eliminations.

6. **Negative apportionment factor:** SubA has $5M OH sales but $10M losses (refunds, chargebacks). Negative factor is impossible. System treats as zero, recalculates group apportionment without SubA's negative factor.

7. **Consolidated NOL exceeds individual NOLs:** ParentCo loss $5M, SubA profit $1M → Consolidated NOL $4M. But ParentCo standalone has $5M NOL. If group deconsolidates, ParentCo gets $4M (consolidated), SubA gets $0. ParentCo "loses" $1M of standalone NOL (absorbed in consolidation).

8. **Triple-weighted sales factor:** Municipality changes formula mid-consolidation election. System applies new formula to group: Combined sales factor now triple-weighted. Recalculate apportionment for all years going forward.
