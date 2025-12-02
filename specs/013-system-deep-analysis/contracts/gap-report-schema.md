# Gap Report Schema

**Purpose**: Defines the structure for the prioritized gap analysis output.

---

## Report Structure

```markdown
# System Gap Report

**Generated**: [DATE]
**Analysis Scope**: MuniTax Platform  
**Total Gaps Identified**: [COUNT]

## Executive Summary

| Severity | Count | Description |
|----------|-------|-------------|
| 🔴 CRITICAL | X | Blocks primary user journeys |
| 🟠 HIGH | Y | Significant features incomplete |
| 🟡 MEDIUM | Z | Features exist with gaps |
| 🟢 LOW | W | Documentation/polish issues |

## Critical Gaps (Immediate Action Required)

### GAP-001: [Title]

**Severity**: 🔴 CRITICAL  
**Category**: [API/UI/INTEGRATION/DOCUMENTATION/SECURITY/LOGIC]  
**Affected Journeys**: [List of journeys]  
**Affected Services**: [List of services]  
**Existing Spec**: [Spec # or "New"]

**Description**:
[Detailed description of the gap]

**Impact**:
[What happens because of this gap]

**Remediation**:
1. [Step 1]
2. [Step 2]
3. [Step 3]

**Estimated Effort**: [SMALL/MEDIUM/LARGE/XLARGE]

---

[Repeat for each gap, organized by severity]

## Cross-Reference with Existing Specs

| Gap ID | Related Spec | Status | Notes |
|--------|--------------|--------|-------|
| GAP-001 | Spec 12 | In Progress | Double-entry ledger |
| GAP-002 | New Spec Needed | - | Auditor workflow |

## Remediation Roadmap

### Phase 1: Critical Gaps (Weeks 1-2)
- GAP-001: [Title]
- GAP-002: [Title]

### Phase 2: High Priority (Weeks 3-6)
- GAP-003: [Title]
- GAP-004: [Title]

### Phase 3: Medium Priority (Weeks 7-10)
- GAP-005: [Title]
- GAP-006: [Title]
```

---

## Field Definitions

### Gap Entry

```yaml
id: string              # Unique identifier (GAP-001)
title: string           # Brief title
description: string     # Detailed description
category: API | UI | INTEGRATION | DOCUMENTATION | SECURITY | LOGIC
severity: CRITICAL | HIGH | MEDIUM | LOW
affectedJourneys: string[]    # User journeys affected
affectedServices: string[]    # Backend services affected
affectedComponents: string[]  # UI components affected
existingSpec: string | null   # Reference to specs 1-12 if applicable
impact: string                # Business impact description
remediation: RemediationStep[]
estimatedEffort: SMALL | MEDIUM | LARGE | XLARGE
priority: number              # 1-100 (higher = more urgent)
```

### Remediation Step

```yaml
order: number           # Step sequence
action: string          # What to do
details: string         # How to do it
owner: string | null    # Suggested owner
dependencies: string[]  # Prerequisites
```

---

## Severity Definitions

| Severity | Definition | Examples |
|----------|------------|----------|
| CRITICAL | Blocks primary user journeys; users cannot complete core tasks | Payment integration missing; Auditor workflow 0% |
| HIGH | Significant feature incomplete; major functionality gaps | Schedule X only 6 fields; Rule engine disconnected |
| MEDIUM | Feature exists but with limitations; workarounds available | Limited validation; Missing edge cases |
| LOW | Documentation, polish, or minor issues; does not affect functionality | Swagger missing; Code comments |

---

## Effort Definitions

| Effort | Story Points | Duration | Description |
|--------|--------------|----------|-------------|
| SMALL | 1-3 | 1-3 days | Configuration, simple fix, documentation |
| MEDIUM | 5-8 | 1-2 weeks | New component, service integration |
| LARGE | 13-21 | 2-4 weeks | New feature, multi-service changes |
| XLARGE | 34+ | 1+ months | Major subsystem, architectural change |

---

## Example Output

```markdown
### GAP-001: Payment Gateway Integration Missing

**Severity**: 🔴 CRITICAL  
**Category**: INTEGRATION  
**Affected Journeys**: Individual Tax Filing, Business Net Profits  
**Affected Services**: ledger-service  
**Existing Spec**: Spec 12 (Double-Entry Ledger)

**Description**:
The system has no payment integration. Users can calculate taxes and submit returns, but cannot pay their tax liability through the application. The ledger-service has mock payment code but no actual payment gateway integration.

**Impact**:
- Municipality cannot collect tax revenue through the application
- Users must pay through separate channels
- No automated receipt generation
- Payment status tracking not available

**Remediation**:
1. Add Stripe SDK to ledger-service (pom.xml)
2. Create PaymentGatewayService with mock mode toggle
3. Implement POST /api/v1/payments endpoint
4. Create PaymentForm.tsx frontend component
5. Integrate with ledger double-entry system
6. Add receipt generation via pdf-service

**Estimated Effort**: LARGE

---

### GAP-002: Auditor Workflow Not Implemented

**Severity**: 🔴 CRITICAL  
**Category**: UI  
**Affected Journeys**: Auditor Review  
**Affected Services**: submission-service  
**Existing Spec**: Spec 9 (Auditor Workflow)

**Description**:
The auditor workflow is 0% implemented. Auditors cannot view pending returns, assign cases, review submissions, approve/reject, request documents, or provide e-signatures.

**Impact**:
- Tax returns cannot be officially processed
- Municipality cannot verify submissions
- Compliance process is entirely manual

**Remediation**:
1. Implement AuditorDashboard.tsx with queue view
2. Add case assignment logic in submission-service
3. Create ReviewPanel.tsx for return examination
4. Add approve/reject workflow with reasons
5. Implement document request system
6. Add e-signature capture

**Estimated Effort**: XLARGE
```
