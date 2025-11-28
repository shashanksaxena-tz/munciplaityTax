# Unplanned Specs Status Report

**Generated:** 2025-11-28  
**Purpose:** Identify which specifications are yet to be planned and fully implemented  
**Repository:** munciplaityTax

---

## Executive Summary

This report provides a comprehensive analysis of the 12 specifications in the MuniTax system, identifying which have complete planning artifacts (plan.md and tasks.md) and their implementation status.

**Quick Stats:**
- **Total Specifications:** 12
- **Specs with Full Planning (spec.md + plan.md + tasks.md):** 6 (50%)
- **Specs Lacking Planning Artifacts:** 6 (50%)
- **Specs with Partial/Full Implementation:** 3
- **Specs with No Implementation:** 9

---

## Specification Status Matrix

| # | Specification Name | Priority | spec.md | plan.md | tasks.md | Implementation Status |
|---|-------------------|----------|---------|---------|----------|----------------------|
| 1 | Withholding Reconciliation | CRITICAL | ✅ | ✅ | ✅ | 🟡 Partial (~33%) |
| 2 | Expand Schedule X to 27 Fields | CRITICAL | ✅ | ✅ | ✅ | 🟡 Partial (~65%) |
| 3 | Enhanced Discrepancy Detection | CRITICAL | ✅ | ✅ | ✅ | 🔴 Not Started |
| 4 | Rule Configuration UI | HIGH | ✅ | ✅ | ✅ | 🔴 Not Started |
| 5 | Schedule Y Sourcing Rules | HIGH | ✅ | ❌ | ❌ | 🔴 Not Started |
| 6 | NOL Carryforward Tracker | HIGH | ✅ | ❌ | ❌ | 🔴 Not Started |
| 7 | Enhanced Penalty/Interest | HIGH | ✅ | ✅ | ✅ | 🔴 Not Started |
| 8 | Business Form Library | HIGH | ✅ | ✅ | ✅ | 🟢 Complete |
| 9 | Auditor Workflow | CRITICAL | ✅ | ❌ | ❌ | 🔴 Not Started |
| 10 | JEDD Zone Support | MEDIUM | ✅ | ❌ | ❌ | 🔴 Not Started |
| 11 | Consolidated Returns | MEDIUM | ✅ | ❌ | ❌ | 🔴 Not Started |
| 12 | Double-Entry Ledger System | MEDIUM | ✅ | ❌ | ❌ | 🔴 Not Started |

**Legend:**
- ✅ = Complete
- ❌ = Missing
- 🟢 = Complete Implementation
- 🟡 = Partial Implementation
- 🔴 = Not Started

---

## Detailed Analysis

### Category 1: Specs with Complete Planning (6 specs)

These specifications have all three artifacts (spec.md, plan.md, tasks.md) and are ready for implementation or in progress:

#### 1. Withholding Reconciliation (Spec 1) - CRITICAL
- **Status:** 🟡 Partial Implementation (~33% complete)
- **Planning Artifacts:** ✅ Complete (spec.md, plan.md, tasks.md)
- **Implementation Details:**
  - Backend foundation complete (domain models, repositories, services)
  - Database migrations complete (5 tables)
  - ~3,500 lines of production code
  - Missing: Backend controllers, event publishing, frontend components, testing
- **Next Steps:** Complete backend services, add REST controllers, implement comprehensive testing
- **Estimated Work Remaining:** 16-24 hours

#### 2. Expand Schedule X to 27 Fields (Spec 2) - CRITICAL
- **Status:** 🟡 Partial Implementation (~65% complete)
- **Planning Artifacts:** ✅ Complete (spec.md, plan.md, tasks.md)
- **Implementation Details:**
  - Backend services complete (calculation, validation, auto-calculation)
  - Frontend utilities and components complete
  - API endpoints implemented
  - Missing: AI extraction (Gemini API), PDF generation, full UI integration
- **Next Steps:** Complete UI integration, implement AI extraction, add PDF generation
- **Estimated Work Remaining:** 40-56 hours

#### 3. Enhanced Discrepancy Detection (Spec 3) - CRITICAL
- **Status:** 🔴 Not Started
- **Planning Artifacts:** ✅ Complete (spec.md, plan.md, tasks.md)
- **Implementation Details:** Planning complete, implementation not yet started
- **Next Steps:** Begin implementation according to tasks.md
- **Dependencies:** Requires Spec 2 (Schedule X) for M-1 reconciliation validation

#### 4. Rule Configuration UI (Spec 4) - HIGH
- **Status:** 🔴 Not Started
- **Planning Artifacts:** ✅ Complete (spec.md, plan.md, tasks.md)
- **Implementation Details:** Planning complete, implementation not yet started
- **Next Steps:** Begin implementation of dynamic rule engine
- **Note:** Critical for replacing hardcoded rules (currently 0% configurable)

#### 7. Enhanced Penalty/Interest (Spec 7) - HIGH
- **Status:** 🔴 Not Started
- **Planning Artifacts:** ✅ Complete (spec.md, plan.md, tasks.md)
- **Implementation Details:** Planning complete, implementation not yet started
- **Next Steps:** Begin implementation of enhanced penalty calculation
- **Dependencies:** Requires Spec 4 (rule engine) for penalty rates

#### 8. Business Form Library (Spec 8) - HIGH
- **Status:** 🟢 Complete Implementation
- **Planning Artifacts:** ✅ Complete (spec.md, plan.md, tasks.md)
- **Implementation Details:** Tax form generation system implemented
- **Achievement:** First spec to reach 100% completion

---

### Category 2: Specs Lacking Planning Artifacts (6 specs)

These specifications have spec.md but are missing plan.md and tasks.md. They require planning before implementation can begin:

#### 5. Schedule Y Sourcing Rules (Spec 5) - HIGH PRIORITY
- **Status:** 🔴 Not Planned
- **Missing:** plan.md, tasks.md
- **Spec Details:**
  - Feature: Multi-State Income Sourcing & Apportionment
  - Functional Requirements: 50 FRs
  - Key Entities: 6 entities
  - User Stories: Focus on Joyce vs Finnigan election, throwback rules, market-based sourcing
- **Business Impact:** Required for 40%+ of business filers with multi-state operations
- **Dependencies:** Requires Spec 4 (rule engine) for sourcing elections
- **Recommended Priority:** HIGH - Essential for production deployment at scale

#### 6. NOL Carryforward Tracker (Spec 6) - HIGH PRIORITY
- **Status:** 🔴 Not Planned
- **Missing:** plan.md, tasks.md
- **Spec Details:**
  - Feature: Net Operating Loss tracking with carryback/carryforward
  - Functional Requirements: 47 FRs
  - Key Entities: 6 entities
  - User Stories: Multi-year NOL tracking (up to 20 years)
- **Business Impact:** Required for multi-year tax optimization
- **Dependencies:** Requires Spec 2 (Schedule X) for NOL calculation
- **Recommended Priority:** HIGH - Needed for multi-year tax planning

#### 9. Auditor Workflow (Spec 9) - CRITICAL PRIORITY
- **Status:** 🔴 Not Planned
- **Missing:** plan.md, tasks.md
- **Spec Details:**
  - Feature: Municipality auditor review and approval workflow
  - Functional Requirements: 59 FRs
  - Key Entities: 5 entities
  - User Stories: Auditor dashboard, submission review, audit trail
- **Business Impact:** Blocks municipality adoption (currently 0% complete)
- **Dependencies:** Requires Specs 1-8 (audits all features)
- **Recommended Priority:** CRITICAL - Essential for basic system operation
- **Note:** Some implementation documentation exists (AUDITOR_WORKFLOW_IMPLEMENTATION.md)

#### 10. JEDD Zone Support (Spec 10) - MEDIUM PRIORITY
- **Status:** 🔴 Not Planned
- **Missing:** plan.md, tasks.md
- **Spec Details:**
  - Feature: Joint Economic Development District tax allocation
  - Functional Requirements: 35 FRs
  - Key Entities: 5 entities
  - User Stories: JEDD zone detection, multi-jurisdiction allocation
- **Business Impact:** Ohio-specific for joint economic development districts
- **Dependencies:** Requires Spec 5 (JEDD zones use apportionment logic)
- **Recommended Priority:** MEDIUM - Specialized use case

#### 11. Consolidated Returns (Spec 11) - MEDIUM PRIORITY
- **Status:** 🔴 Not Planned
- **Missing:** plan.md, tasks.md
- **Spec Details:**
  - Feature: Consolidated filing for affiliated corporate groups
  - Functional Requirements: 44 FRs
  - Key Entities: 8 entities
  - User Stories: Group management, intercompany eliminations
- **Business Impact:** Affects <10% of filers (corporate groups)
- **Dependencies:** Requires Specs 2, 5, 6 (consolidated returns aggregate Schedule X, Schedule Y, NOLs)
- **Recommended Priority:** MEDIUM - Specific use case

#### 12. Double-Entry Ledger System (Spec 12) - MEDIUM PRIORITY
- **Status:** 🔴 Not Planned
- **Missing:** plan.md, tasks.md
- **Spec Details:**
  - Feature: Mock payment provider and ledger system
  - Functional Requirements: 55 FRs
  - Key Entities: 10 entities
  - User Stories: Payment processing, account statements, reconciliation
- **Business Impact:** Testing and reconciliation support
- **Dependencies:** Requires Spec 7 (ledger records penalties and interest)
- **Recommended Priority:** MEDIUM - Support system for testing
- **Note:** Some implementation documentation exists (IMPLEMENTATION_SUMMARY_LEDGER.md)

---

## Prioritized Implementation Roadmap

Based on the specs/README.md and current implementation status, here's the recommended sequence:

### Phase 1: Complete In-Progress Work (Immediate)
**Timeline:** 2-3 months  
**Goal:** Finish partially implemented critical specs

1. **Complete Spec 2 (Schedule X)** - 5-7 days remaining
   - Integrate full UI into wizard
   - Implement AI extraction with Gemini
   - Add comprehensive testing

2. **Complete Spec 1 (Withholding)** - 2-3 weeks remaining
   - Finish backend services (Cumulative, Reconciliation)
   - Add REST controllers
   - Implement frontend components
   - Add comprehensive testing

### Phase 2: Critical Planning & Implementation (3-4 months)
**Goal:** Plan and implement critical missing specs

3. **Plan & Implement Spec 9 (Auditor Workflow)** - 6 weeks
   - **PLANNING NEEDED:** Create plan.md and tasks.md
   - Implement auditor dashboard
   - Add submission review workflow
   - Implement audit trail

4. **Plan & Implement Spec 3 (Discrepancy Detection)** - 3 weeks
   - Already planned, ready for implementation
   - Add 10+ validation rules
   - Implement severity levels and resolution workflow

5. **Implement Spec 4 (Rule Configuration UI)** - 4 weeks
   - Already planned, ready for implementation
   - Replace hardcoded rules with dynamic rule engine
   - Enable multi-tenant rule configuration

### Phase 3: High Priority Planning & Implementation (4-5 months)
**Goal:** Enable production use at scale

6. **Plan & Implement Spec 5 (Schedule Y Sourcing)** - 5 weeks
   - **PLANNING NEEDED:** Create plan.md and tasks.md
   - Multi-state apportionment
   - Joyce/Finnigan elections
   - Throwback/throwout rules

7. **Plan & Implement Spec 6 (NOL Carryforward)** - 4 weeks
   - **PLANNING NEEDED:** Create plan.md and tasks.md
   - Multi-year NOL tracking
   - Carryback/carryforward logic
   - Expiration alerts

8. **Implement Spec 7 (Enhanced Penalty/Interest)** - 4 weeks
   - Already planned, ready for implementation
   - Quarterly estimated tax penalties
   - Interest calculation enhancements

### Phase 4: Medium Priority Features (3-4 months)
**Goal:** Support advanced use cases

9. **Plan & Implement Spec 10 (JEDD Zone Support)** - 4 weeks
   - **PLANNING NEEDED:** Create plan.md and tasks.md
   - JEDD zone detection
   - Multi-municipality allocation

10. **Plan & Implement Spec 11 (Consolidated Returns)** - 5 weeks
    - **PLANNING NEEDED:** Create plan.md and tasks.md
    - Corporate group management
    - Intercompany eliminations

11. **Plan & Implement Spec 12 (Ledger System)** - 5 weeks
    - **PLANNING NEEDED:** Create plan.md and tasks.md
    - Mock payment provider
    - Double-entry ledger
    - Reconciliation reports

---

## Action Items Summary

### Immediate Actions (This Week)

1. **Complete Spec 2 (Schedule X) implementation** to reach MVP for critical business functionality
2. **Begin planning for Spec 9 (Auditor Workflow)** - CRITICAL missing piece
   - Run `speckit-plan` to generate plan.md
   - Run `speckit-tasks` to generate tasks.md
3. **Continue Spec 1 (Withholding) implementation** to finish backend services

### Short-Term Actions (Next 2-4 Weeks)

4. **Plan Spec 5 (Schedule Y Sourcing)** - HIGH priority for multi-state businesses
   - Run `speckit-plan` to generate plan.md
   - Run `speckit-tasks` to generate tasks.md
5. **Plan Spec 6 (NOL Carryforward)** - HIGH priority for multi-year tracking
   - Run `speckit-plan` to generate plan.md
   - Run `speckit-tasks` to generate tasks.md
6. **Begin Spec 3 & 4 implementation** (already planned)

### Medium-Term Actions (Next 1-3 Months)

7. **Plan remaining specs (10, 11, 12)** for medium priority features
8. **Implement Specs 5-7** for production readiness
9. **Complete Spec 9** for municipality adoption

---

## Planning Tools Available

The repository has SpecKit tools available for generating planning artifacts:

- **speckit-plan**: Generate plan.md from spec.md
- **speckit-tasks**: Generate tasks.md from plan.md and spec.md
- **speckit-clarify**: Identify underspecified areas and clarify requirements
- **speckit-analyze**: Cross-artifact consistency analysis

### How to Generate Planning Artifacts

For each spec missing plan.md and tasks.md:

```bash
# Navigate to spec directory
cd specs/[spec-number]-[spec-name]/

# Generate plan.md
speckit-plan "Generate implementation plan for [spec-name]"

# Generate tasks.md
speckit-tasks "Generate actionable tasks for [spec-name]"

# Optional: Analyze for consistency
speckit-analyze "Analyze spec, plan, and tasks for consistency"
```

---

## Risk Assessment

### High Risk - Blocking Production

1. **Spec 9 (Auditor Workflow) - Not Planned**
   - Risk: Blocks municipality adoption
   - Impact: Cannot deploy to production without auditor review capability
   - Mitigation: Prioritize planning and implementation immediately

2. **Spec 3 (Discrepancy Detection) - Planned but Not Implemented**
   - Risk: Only 20% of errors caught before submission
   - Impact: High error rate affects system credibility
   - Mitigation: Implement after Spec 2 completion

### Medium Risk - Limits Scale

3. **Spec 4 (Rule Engine) - Planned but Not Implemented**
   - Risk: 0% of rules configurable (all hardcoded)
   - Impact: Cannot support multi-tenant deployments at scale
   - Mitigation: Implement in Phase 2

4. **Spec 5 (Schedule Y) - Not Planned**
   - Risk: Cannot support 40%+ of multi-state filers
   - Impact: Limits market size and revenue potential
   - Mitigation: Plan and implement in Phase 3

### Low Risk - Advanced Features

5. **Specs 10-12 - Not Planned**
   - Risk: Cannot support specialized use cases
   - Impact: Limits feature completeness but doesn't block core functionality
   - Mitigation: Plan and implement in Phase 4

---

## Resource Allocation Recommendations

Based on the analysis, recommended resource allocation:

### Critical Path (3 FTE)
- 1 FTE: Complete Spec 2 (Schedule X)
- 1 FTE: Complete Spec 1 (Withholding)
- 1 FTE: Plan & implement Spec 9 (Auditor Workflow)

### High Priority (2 FTE)
- 1 FTE: Implement Specs 3-4 (Discrepancy Detection, Rule Engine)
- 1 FTE: Plan & implement Specs 5-6 (Schedule Y, NOL)

### Medium Priority (1 FTE)
- 1 FTE: Plan & implement Specs 10-12 (JEDD, Consolidated, Ledger)

**Total Team Size:** 6 FTE for parallel execution across all priorities

---

## Success Metrics

### Planning Completion Metrics
- **Target:** 100% of specs have plan.md and tasks.md (currently 50%)
- **Timeline:** 4 weeks to plan all 6 remaining specs

### Implementation Completion Metrics
- **Phase 1 Complete:** Specs 1-2 at 100% (2-3 months)
- **Phase 2 Complete:** Specs 3-4, 9 at 100% (3-4 months)
- **Phase 3 Complete:** Specs 5-7 at 100% (4-5 months)
- **Phase 4 Complete:** Specs 10-12 at 100% (3-4 months)

### System Readiness Metrics
- **MVP Ready:** After Phase 1 (Specs 1-2 complete)
- **Production Ready:** After Phase 2 (Specs 1-4, 9 complete)
- **Full Feature Set:** After Phase 4 (All specs complete)

---

## Conclusion

**Current State:**
- 12 specifications defined with complete spec.md files
- 6 specifications (50%) have complete planning artifacts
- 6 specifications (50%) lack planning artifacts (plan.md, tasks.md)
- 3 specifications have partial/full implementation
- 9 specifications have no implementation yet

**Critical Blockers:**
1. Spec 9 (Auditor Workflow) - Not planned, blocks municipality adoption
2. Spec 5 (Schedule Y) - Not planned, limits 40%+ of potential filers
3. Spec 6 (NOL Carryforward) - Not planned, limits multi-year functionality

**Immediate Next Steps:**
1. Complete in-progress work (Specs 1-2)
2. Plan critical missing specs (Specs 5, 6, 9)
3. Begin implementation of planned specs (Specs 3-4, 7)

**Timeline to Production:**
- MVP: 2-3 months (Specs 1-2 complete)
- Production-Ready: 6-8 months (Specs 1-4, 9 complete)
- Full System: 12-15 months (All specs complete)

---

**Document Version:** 1.0  
**Last Updated:** 2025-11-28  
**Next Review:** After Phase 1 completion (Specs 1-2)
