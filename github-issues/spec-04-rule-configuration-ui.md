# Spec 4: Dynamic Rule Configuration System

**Priority:** HIGH  
**Feature Branch:** `4-rule-configuration-ui`  
**Spec Document:** `specs/4-rule-configuration-ui/spec.md`

## Overview

Create dynamic rule configuration UI to stop hardcoding tax rules - build rule management system with versioning, temporal rules (effective dates), tenant-specific overrides, and JSON-based rule storage for both individual and business tax calculations.

## Implementation Status

**Current:** 0% - All rules hardcoded in constants.ts and Java code  
**Required:** Full rule management system with UI, versioning, and database storage

## Core Requirements (FR-001 to FR-059)

### Rule Definition & Storage (FR-001 to FR-005)
- [ ] Create TaxRule entity in PostgreSQL with jsonb storage
- [ ] Store rules with: ruleId, ruleName, category, type, value, effectiveDate, endDate, tenantId, entityType
- [ ] Support rule categories: Tax Rates, Income Inclusion, Deductions, Penalties, Filing Requirements, etc.
- [ ] Support value types: Number, Percentage, Enum, Boolean, Formula, Conditional
- [ ] Implement rule dependency tracking (Rule A references Rule B)

### Temporal Rules / Effective Dating (FR-006 to FR-010)
- [ ] Enforce effective date logic: use rules where effectiveDate ≤ tax year ≤ endDate
- [ ] Prevent overlapping rules for same ruleId + tenantId
- [ ] Support mid-year effective dates with pro-rating
- [ ] Support expiration dates (sunset clauses) with automatic reversion
- [ ] Block retroactive rule changes after returns filed

### Multi-Tenant Support (FR-011 to FR-014)
- [ ] Support tenant-specific rules (Dublin, Columbus, Cleveland, GLOBAL)
- [ ] Implement tenant inheritance (global rules inherited unless overridden)
- [ ] Build tenant comparison view (side-by-side rule comparison)
- [ ] Support tenant-specific effective dates

### Entity-Specific Rules (FR-015 to FR-017)
- [ ] Target rules by entity type: [All | C-Corp Only | S-Corp | Partnership | Individual | LLC]
- [ ] Evaluate entity-specific rules before general rules
- [ ] Support multi-criteria rules: "C-Corp AND Income > $1M"

### Rule Configuration UI (FR-018 to FR-024)
- [ ] Build RuleConfigurationDashboard component
- [ ] Create RuleEditor component with all fields
- [ ] Implement rule search/filter by category, tenant, entity type, effective date
- [ ] Build "What-If Analysis" tool to test rule changes on sample returns
- [ ] Create rule versioning UI with timeline and diff view
- [ ] Implement rule approval workflow (Pending → Approved)
- [ ] Validate and prevent rule conflicts

### Formula & Conditional Rules (FR-025 to FR-028)
- [ ] Support formula rules: "wages * municipalRate" or "MAX(minimumTax, income * rate)"
- [ ] Support conditional rules: "IF income > 1000000 THEN minimumTax = 5000 ELSE 50"
- [ ] Validate formulas before saving (parse and check variable references)
- [ ] Provide formula testing tool with sample inputs

### Integration with Tax Calculators (FR-029 to FR-032)
- [ ] Refactor IndividualTaxCalculator.java to read from TaxRulesConfig (not hardcoded)
- [ ] Refactor BusinessTaxCalculator.java to read from BusinessTaxRulesConfig
- [ ] Implement rule caching (Redis or in-memory) to avoid database query per calculation
- [ ] Provide rule refresh API to invalidate cache on rule changes

### Audit Trail & Reporting (FR-033 to FR-037)
- [ ] Log all rule changes with who/what/when/why
- [ ] Support point-in-time rule query: "What was rate on 2022-06-15?"
- [ ] Generate Rule Change Report PDF with before/after values and affected taxpayer counts
- [ ] Track rule usage: count returns calculated using each rule version
- [ ] Support rule rollback within 24 hours of change

## User Stories (5 Priority P1-P3)

1. **US-1 (P1):** Tax Administrator Updates Municipal Rate for New Tax Year
2. **US-2 (P1):** CPA Firm Manages Rules for Multiple Municipal Clients
3. **US-3 (P2):** Individual Tax Rules Configure W-2 Qualifying Wages Logic
4. **US-4 (P2):** Configure Income Inclusion Rules for Business Entities
5. **US-5 (P3):** Version Control and Audit Trail for Rule Changes

## Key Entities

### TaxRule
- ruleId, ruleName, ruleCode, category, valueType, value
- effectiveDate, endDate, tenantId, entityTypes[]
- createdBy, createdDate, modifiedBy, approvalStatus
- changeReason, ordinanceReference, version, previousVersionId
- dependsOn[] (array of ruleIds)

### RuleChangeLog
- logId, ruleId, changeType (CREATE/UPDATE/DELETE/APPROVE/REJECT)
- oldValue, newValue, changedFields[], changedBy, changeDate
- affectedReturnsCount, impactEstimate

## Success Criteria

- Tax administrators update rates without developer involvement (0 code deployments)
- Rule changes take effect according to configured dates with 100% accuracy
- Tax calculations retrieve rules from database in under 100ms (with caching)
- Multi-tenant deployments support 10+ municipalities with independent rule sets
- Rule version history provides complete audit trail for compliance reviews
- What-If Analysis shows impact of rule changes on sample returns within 5 seconds

## Technical Implementation

### Database Schema
- [ ] PostgreSQL table: tax_rules with jsonb column for flexible schema
- [ ] Indexes on: ruleCode, tenantId, effectiveDate, entityTypes
- [ ] Foreign key constraints for referential integrity

### Backend Services
- [ ] RuleConfigService - CRUD operations for rules
- [ ] RuleCacheService - Redis caching layer
- [ ] RuleValidationService - validate rules before saving
- [ ] RuleVersioningService - track versions and changes
- [ ] RuleEvaluationService - evaluate formulas and conditionals

### Frontend Components
- [ ] RuleConfigurationDashboard.tsx
- [ ] RuleEditor.tsx with form fields
- [ ] RuleTimeline.tsx for version history
- [ ] RuleComparisonView.tsx for tenant comparison
- [ ] WhatIfAnalysisTool.tsx

## Migration Strategy

1. Export current constants.ts rules to JSON format
2. Create migration script to populate tax_rules table
3. Test rule retrieval in sandbox environment
4. Gradual rollout: start with test municipalities
5. Monitor performance and cache hit rates
6. Deprecate hardcoded constants after full migration

## Dependencies

- PostgreSQL with jsonb support
- Redis for caching (optional but recommended)
- AuthContext extended for TAX_ADMINISTRATOR role
- All tax calculators (Individual, Business) refactored

## Edge Cases Documented

- Overlapping effective dates
- Retroactive rule changes (blocked if returns filed)
- Rule dependencies (cascading updates)
- Mid-year rate change (pro-rating)
- Rule expiration/sunset clause
- Complex conditional rules
- Grandfathered rules (entity-specific overrides)
- Multi-criteria rules

## Related Specs

- Used by: ALL other specs (provides rule engine for all calculations)
- Critical for: Spec 7 (Penalty rates), Spec 5 (Apportionment formulas)
- Enables: Multi-tenant deployments
