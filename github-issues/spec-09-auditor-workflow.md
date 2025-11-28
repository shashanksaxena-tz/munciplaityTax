# Spec 9: Municipal Tax Auditor Review & Approval Workflow

**Priority:** MEDIUM  
**Feature Branch:** `9-auditor-workflow`  
**Spec Document:** `specs/9-auditor-workflow/spec.md`

## Overview

Implement comprehensive auditor workflow system for municipality tax department staff to review, approve, or reject submitted tax returns, request additional documentation, perform automated and manual audits, track audit history, and manage taxpayer communications.

## Implementation Status

**Current:** 0% - No auditor functionality exists (system is filer-only)  
**Required:** Full auditor workflow with submission queue, review interface, approval workflow, audit tools

## Core Requirements (FR-001 to FR-059)

### Submission Queue Management (FR-001 to FR-006)
- [ ] Maintain submission queue with status: PENDING | IN_REVIEW | AWAITING_DOCUMENTATION | APPROVED | REJECTED | AMENDED
- [ ] Auto-assign priority: HIGH (tax >$50K, discrepancies, amended), MEDIUM ($10K-$50K), LOW (<$10K)
- [ ] Support manual priority override by supervisor
- [ ] Support auditor assignment: Auto-assign, Manual assign, Self-assign
- [ ] Track queue metrics: total pending, avg time in queue, avg time to approval, returns by status
- [ ] Send daily queue summary to auditors

### Return Review Interface (FR-007 to FR-013)
- [ ] Display return with all schedules and supporting documents
- [ ] Support split-screen view: Return (left) vs Supporting Documents (right)
- [ ] Highlight system-detected discrepancies from Spec 3
- [ ] Allow auditor to annotate return: comments, highlights, internal notes
- [ ] Display taxpayer history: prior 3 years, payment history, prior audits, penalties, balances
- [ ] Display supporting documents with inline PDF viewing
- [ ] Support side-by-side comparison for amended returns

### Approval Workflow (FR-014 to FR-019)
- [ ] Require auditor authentication before approval (password or certificate)
- [ ] Generate approval record with auditor e-signature
- [ ] Update return status to APPROVED
- [ ] Notify taxpayer via email with tax due and payment instructions
- [ ] Record approval in immutable audit log
- [ ] Generate approval letter PDF with municipality seal

### Rejection Workflow (FR-020 to FR-028)
- [ ] Require rejection reason dropdown + detailed explanation
- [ ] Support rejection reasons: MISSING_SCHEDULES | CALCULATION_ERRORS | UNSUPPORTED_DEDUCTIONS | MISSING_DOCUMENTATION | INSUFFICIENT_PAYMENT | OTHER
- [ ] Allow auditor to specify required corrections (checklist)
- [ ] Set resubmission deadline (default 30 days)
- [ ] Update return status to REJECTED
- [ ] Notify taxpayer with detailed explanation and deadline
- [ ] Create follow-up task: "Check for resubmission by [deadline]"
- [ ] Send reminder 7 days before deadline, escalate to supervisor if missed

### Documentation Request Management (FR-029 to FR-036)
- [ ] Allow auditor to request additional documentation with document type, description, deadline
- [ ] Update return status to AWAITING_DOCUMENTATION
- [ ] Send email to taxpayer with upload link
- [ ] Track request status: PENDING | RECEIVED | OVERDUE
- [ ] Notify auditor when documentation uploaded
- [ ] Send reminders: 7 days before, 1 day before deadline
- [ ] Mark request OVERDUE if deadline passes
- [ ] Allow auditor to extend deadline or mark as waived

### Automated Audit Checks (FR-037 to FR-041)
- [ ] Perform automated audit checks on all submissions:
  - Year-over-year variance analysis
  - Ratio analysis (profit margin, expense ratios)
  - Peer comparison (same industry/revenue range)
  - Pattern analysis (unusual transaction timing, round numbers)
  - Rule compliance (all discrepancy rules from Spec 3)
- [ ] Calculate risk score: 0-100 (LOW 0-20, MEDIUM 21-60, HIGH 61-100)
- [ ] Generate audit report with flagged items, comparisons, recommended actions
- [ ] Display audit report in review interface
- [ ] Allow auditor to override risk score with justification

### Communication & Messaging (FR-042 to FR-046)
- [ ] Support secure messaging between auditor and taxpayer
- [ ] Log all communications (emails, messages, phone notes)
- [ ] Support email templates: Approval, Rejection, Documentation request, Reminder, Payment reminder
- [ ] Track email delivery status (sent, delivered, bounced, opened)
- [ ] Allow auditor to send custom messages

### Audit Trail & History (FR-047 to FR-051)
- [ ] Create immutable audit log for all actions: submission, assignment, review, approval, rejection, etc.
- [ ] Log with: Event type, User ID, Timestamp, IP address, Action details, Previous/New state
- [ ] Prevent editing or deletion of audit log entries (append-only)
- [ ] Generate audit trail report PDF for legal/compliance
- [ ] Retain audit trail for 7 years (IRS requirement)

### Reporting & Analytics (FR-052 to FR-055)
- [ ] Generate auditor performance reports: Returns reviewed, avg review time, approval/rejection rate
- [ ] Generate compliance reports: Returns by month, common rejection reasons, avg tax liability, discrepancy trends
- [ ] Generate taxpayer reports: Filing compliance, payment compliance, repeat offenders
- [ ] Support custom report builder for managers

### Role-Based Access Control (FR-056 to FR-059)
- [ ] Support roles: AUDITOR | SENIOR_AUDITOR | SUPERVISOR | MANAGER | ADMIN
- [ ] Enforce role permissions:
  - AUDITOR: Review, request docs, recommend approval/rejection
  - SENIOR_AUDITOR: Approve/reject returns (<$50K)
  - SUPERVISOR: Approve/reject any return, override priority, reassign, view team performance
  - MANAGER: All permissions, generate reports, configure audit rules
  - ADMIN: System configuration, user management
- [ ] Require supervisor approval for high-dollar (>$50K) or high-risk returns
- [ ] Log all permission overrides

## User Stories (7 Priority P1-P3)

1. **US-1 (P1):** View Submission Queue with Filtering & Prioritization
2. **US-2 (P1):** Review Return with Side-by-Side Comparison
3. **US-3 (P1):** Approve Return with E-Signature
4. **US-4 (P1):** Reject Return with Detailed Explanation
5. **US-5 (P2):** Request Additional Documentation
6. **US-6 (P2):** Perform Audit with Automated Checks
7. **US-7 (P3):** Track Audit History & Generate Audit Trail

## Key Entities

### AuditQueue
- queueId, returnId, priority (HIGH/MEDIUM/LOW)
- status, submissionDate, assignedAuditorId, assignmentDate
- reviewStartedDate, reviewCompletedDate, riskScore, flaggedIssuesCount
- daysInQueue (calculated)

### AuditAction
- actionId, returnId, auditorId
- actionType (ASSIGNED/REVIEW_STARTED/APPROVED/REJECTED/DOCS_REQUESTED/etc.)
- actionDate, actionDetails (JSON), previousStatus, newStatus
- ipAddress, userAgent

### DocumentRequest
- requestId, returnId, auditorId, requestDate
- documentType, description, deadline, status
- receivedDate, uploadedFiles[]

### AuditReport
- reportId, returnId, generatedDate, riskScore, riskLevel
- flaggedItems[], yearOverYearComparison, peerComparison
- patternAnalysis, recommendedActions[]
- auditorOverride, overrideReason

### AuditTrail
- trailId, returnId, eventType, userId, timestamp
- ipAddress, eventDetails (JSON), digitalSignature
- immutable (always true)

## Success Criteria

- Average time from submission to approval <7 days (vs current manual: 30-45 days)
- Auditors process 20+ returns/day (vs <5 manually)
- Rejection rate <10% (clear guidance reduces errors)
- 95%+ of calculation errors caught by automated checks before manual review
- 100% of actions logged with non-repudiable audit trail
- 80%+ of taxpayers rate communication as clear and helpful

## Edge Cases Documented

- Auditor leaves mid-review
- Simultaneous approval/rejection attempts
- Taxpayer submits while auditor reviewing
- Documentation request fulfilled after approval
- High-risk return auto-approved by junior auditor
- Audit trail storage failure
- Email delivery failure
- Mass rejection (system-wide issue)

## Technical Implementation

### Backend Services
- [ ] AuditQueueService.java
- [ ] AuditActionService.java
- [ ] DocumentRequestService.java
- [ ] AuditReportService.java
- [ ] AuditTrailService.java

### Controllers
- [ ] AuditController.java
  - GET /api/audit/queue
  - POST /api/audit/approve/{returnId}
  - POST /api/audit/reject/{returnId}
  - POST /api/audit/request-docs/{returnId}

### Frontend Components
- [ ] AuditQueue.tsx
- [ ] ReturnReviewInterface.tsx
- [ ] ApprovalDialog.tsx
- [ ] RejectionDialog.tsx
- [ ] DocumentRequestDialog.tsx
- [ ] AuditReportView.tsx
- [ ] AuditTrailView.tsx

## Dependencies

- Enhanced Discrepancy Detection (Spec 3) - Automated audit checks leverage discrepancy rules
- Business Form Library (Spec 8) - Forms generated during review
- Rule Engine (Spec 4) - Audit rules, risk scoring algorithms, approval thresholds
- User Authentication System - Role-based access control, e-signatures

## Out of Scope

- Field audits (in-person business inspections)
- Criminal investigations (fraud, evasion cases)
- Appeals process (taxpayer appeals of audit adjustments)
- Multi-municipality coordination (sharing audit results)

## Related Specs

- Uses: Spec 3 (Discrepancy Detection for automated checks)
- Integrates with: Spec 8 (Form Library for approval letters)
- Feeds into: Spec 12 (Ledger for tracking approved tax liabilities)
