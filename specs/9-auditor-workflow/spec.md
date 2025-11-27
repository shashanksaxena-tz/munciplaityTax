# Auditor Workflow System

**Feature Name:** Municipal Tax Auditor Review & Approval Workflow  
**Priority:** MEDIUM  
**Status:** Specification  
**Created:** 2025-11-27

---

## Overview

Implement comprehensive auditor workflow system for municipality tax department staff to review, approve, or reject submitted tax returns, request additional documentation, perform automated and manual audits, track audit history, and manage taxpayer communications. This feature is essential for municipalities to efficiently process returns, ensure compliance, and maintain audit trails.

**Current State:** No auditor functionality exists (0% complete). System is filer-only. No submission queue, review interface, approval workflow, or audit tools.

**Target Users:** Municipal tax auditors, tax department managers, compliance officers, audit supervisors.

---

## User Scenarios & Testing

### US-1: View Submission Queue with Filtering & Prioritization (P1 - Critical)

**User Story:**  
As a tax auditor, I want to view a prioritized queue of submitted returns with filters for return type, submission date, amount due, and review status, so that I can efficiently process returns starting with highest priority (large amounts, potential issues, approaching deadlines).

**Business Context:**  
Auditors typically receive 100-1000 returns per filing season. Need prioritization: high-dollar returns first, flagged returns (discrepancies detected), approaching deadlines, amended returns. Manual sorting wastes hours daily.

**Acceptance Criteria:**
- GIVEN submitted returns awaiting review
- WHEN auditor accesses submission queue
- THEN system MUST display table with columns: Taxpayer Name, FEIN, Return Type (Individual/Business), Tax Year, Submission Date, Tax Due, Status, Priority, Assigned To
- AND system MUST support filters: Status (Pending/In Review/Approved/Rejected), Return Type, Date Range, Tax Due Range, Priority (High/Medium/Low), Assigned Auditor
- AND system MUST support sorting by any column
- AND system MUST highlight high-priority returns (tax due >$50K, flagged discrepancies, amended returns)
- AND system MUST show count: "125 returns pending review (15 high priority)"

---

### US-2: Review Return with Side-by-Side Comparison (P1 - Critical)

**User Story:**  
As an auditor reviewing a business return, I want to see the taxpayer's reported income alongside supporting documents (W-2s, 1120, K-1s) in a split-screen view, with discrepancies highlighted, so that I can quickly verify accuracy and identify issues.

**Business Context:**  
Manual audit involves comparing return data to source documents. System should auto-detect discrepancies (already implemented in Spec 3) and present side-by-side for auditor verification. Saves 30-60 minutes per return.

**Acceptance Criteria:**
- GIVEN return selected from queue
- WHEN auditor opens for review
- THEN system MUST display split-screen:
  - Left pane: Submitted tax return (Form 27 with all schedules)
  - Right pane: Supporting documents (uploaded PDFs, extracted data)
- AND system MUST highlight discrepancies detected by system (from Spec 3):
  - W-2 Box 18 (local wages) ≠ Box 1 (federal wages)
  - Schedule C income ≠ federal Schedule C
  - NOL deduction > available balance
  - Municipal credit > tax liability
- AND system MUST display discrepancy panel: Issue type, Severity (HIGH/MEDIUM/LOW), Reported value, Expected value, Explanation
- AND system MUST allow auditor to mark discrepancies as: Accepted (taxpayer correct), Rejected (requires correction), Needs Clarification

---

### US-3: Approve Return with E-Signature (P1 - Critical)

**User Story:**  
As an auditor who has reviewed a return and confirmed it is accurate and complete, I want to approve the return with my digital signature, automatically notify the taxpayer, and move the return to "Approved" status, so that the taxpayer knows their filing is accepted.

**Business Context:**  
Approval triggers taxpayer notification, finalizes tax liability, and allows payment processing. Digital signature provides non-repudiation (auditor cannot deny approving the return).

**Acceptance Criteria:**
- GIVEN return reviewed and no issues found
- WHEN auditor clicks "Approve Return"
- THEN system MUST prompt for approval confirmation:
  - "Are you sure you want to approve this return? Tax liability: $12,500."
  - Checkbox: "I have reviewed all schedules and supporting documents"
  - E-signature (enter password or use certificate)
- AND system MUST update return status to "APPROVED"
- AND system MUST record audit trail: Approved by [Auditor Name], Date/Time, E-signature hash
- AND system MUST send email to taxpayer: "Your 2024 tax return has been approved. Tax due: $12,500. Payment due: [date]."
- AND system MUST remove return from pending queue

---

### US-4: Reject Return with Detailed Explanation (P1 - Critical)

**User Story:**  
As an auditor who identified errors in a return (e.g., missing Schedule X, incorrect NOL calculation), I want to reject the return with a detailed explanation of issues, so that the taxpayer can correct and resubmit.

**Business Context:**  
Rejections must be specific and actionable. Vague rejection ("return incomplete") frustrates taxpayers. Detailed rejection ("Schedule X missing - must provide book-tax reconciliation") allows quick correction.

**Acceptance Criteria:**
- GIVEN return reviewed and issues identified
- WHEN auditor clicks "Reject Return"
- THEN system MUST display rejection form:
  - Rejection reason dropdown: Missing Schedules, Calculation Errors, Unsupported Deductions, Missing Documentation, Other
  - Detailed explanation (required text field, min 50 chars): "Schedule X (book-tax adjustments) is missing. Please complete and resubmit."
  - List of required corrections (checkboxes): ☐ Attach Schedule X, ☐ Correct NOL calculation, ☐ Provide depreciation schedules
  - Deadline for resubmission (default: 30 days)
- AND system MUST update return status to "REJECTED"
- AND system MUST record audit trail: Rejected by [Auditor Name], Date/Time, Reason
- AND system MUST send email to taxpayer with rejection details and resubmission deadline
- AND system MUST create follow-up task: "Check for resubmission by [deadline]"

---

### US-5: Request Additional Documentation (P2 - High Value)

**User Story:**  
As an auditor reviewing a high-dollar return ($100K+ tax due), I want to request additional supporting documentation (e.g., general ledger, depreciation schedules, contracts) directly through the system, track when requested and received, and follow up if not provided, so that I can complete a thorough audit.

**Business Context:**  
High-dollar returns and returns with red flags often require additional documentation beyond initial submission. Email communication is inefficient and not tracked. In-system requests create audit trail.

**Acceptance Criteria:**
- GIVEN return under review
- WHEN auditor requests additional documentation
- THEN system MUST display document request form:
  - Document type dropdown: General Ledger, Bank Statements, Depreciation Schedules, Contracts, Invoices, Other
  - Specific request (text field): "Please provide depreciation schedule for all assets >$50K"
  - Deadline (date picker, default 14 days)
- AND system MUST update return status to "AWAITING DOCUMENTATION"
- AND system MUST send email to taxpayer with request details and upload link
- AND system MUST track request status: PENDING | RECEIVED | OVERDUE
- AND system MUST notify auditor when documentation uploaded
- AND system MUST send reminder email if deadline approaches without response (7 days before, 1 day before)

---

### US-6: Perform Audit with Automated Checks (P2 - High Value)

**User Story:**  
As an auditor, I want the system to automatically perform standard audit checks (ratio analysis, trend analysis, peer comparison) and generate an audit report highlighting anomalies, so that I can focus my manual review on high-risk areas.

**Business Context:**  
Automated audits identify outliers: income decreased 50% YoY (possible underreporting), deductions 3× higher than industry average, unusual patterns (all income in Q4). Human auditor investigates flagged items.

**Acceptance Criteria:**
- GIVEN return submitted
- WHEN system performs automated audit
- THEN system MUST run checks:
  - **Year-over-year comparison:** Income/expenses changed >30% → Flag for review
  - **Ratio analysis:** Profit margin, expense ratios compared to industry benchmarks
  - **Peer comparison:** Similar businesses (same NAICS, revenue range) - identify outliers
  - **Pattern analysis:** Unusual transaction patterns (90% expenses in December, round-number income)
  - **Rule compliance:** All validation rules from Spec 3 (discrepancy detection)
- AND system MUST generate audit report:
  - Risk score: LOW (0-20) | MEDIUM (21-60) | HIGH (61-100)
  - Flagged items with severity and explanation
  - Recommended actions: Request documentation, Perform field audit, Approve with conditions
- AND system MUST display audit report to auditor
- AND system MUST prioritize returns with HIGH risk scores in queue

---

### US-7: Track Audit History & Generate Audit Trail (P3 - Future)

**User Story:**  
As a tax department manager, I want to view complete audit history for any taxpayer (all returns submitted, auditor actions, communications, adjustments made) and generate audit trail reports for legal/compliance purposes.

**Business Context:**  
Audit trail required for legal defense, taxpayer appeals, IRS information requests. Must document who did what when and why. Immutable log prevents tampering.

**Acceptance Criteria:**
- GIVEN taxpayer selected
- WHEN manager views audit history
- THEN system MUST display chronological timeline:
  - Return submissions (date, type, tax due)
  - Auditor actions (reviewed, approved, rejected, requested docs)
  - Taxpayer responses (amended return, uploaded docs, paid tax)
  - System events (discrepancy detected, penalty assessed, refund issued)
  - Communications (emails sent/received, messages)
- AND system MUST allow filtering by event type, date range, auditor
- AND system MUST generate audit trail report (PDF) with:
  - Taxpayer identification
  - Complete event log with timestamps
  - User identifiers (who performed each action)
  - Digital signatures (e-signature hashes)
  - Document references (return IDs, PDF hashes)
- AND audit trail MUST be immutable (append-only log, cannot edit/delete entries)

---

## Functional Requirements

### Submission Queue Management

**FR-001:** System MUST maintain submission queue for all submitted returns with status: PENDING | IN_REVIEW | AWAITING_DOCUMENTATION | APPROVED | REJECTED | AMENDED

**FR-002:** System MUST auto-assign priority based on criteria:
- HIGH: Tax due >$50K, Discrepancies detected (severity=HIGH), Amended returns, Audit selected returns
- MEDIUM: Tax due $10K-$50K, Discrepancies (severity=MEDIUM)
- LOW: Tax due <$10K, No discrepancies

**FR-003:** System MUST support manual priority override by supervisor

**FR-004:** System MUST support auditor assignment:
- Auto-assign: Round-robin distribution, workload balancing
- Manual assign: Supervisor assigns specific returns to auditors
- Self-assign: Auditor claims return from queue

**FR-005:** System MUST track queue metrics:
- Total pending returns
- Average time in queue (days)
- Average time to approval (days from submission to approval)
- Returns by status (pending, in review, approved, rejected)
- Returns by auditor (workload distribution)

**FR-006:** System MUST send daily queue summary to auditors: "You have 15 returns pending review (3 high priority)"

### Return Review Interface

**FR-007:** System MUST display return with all schedules and supporting documents

**FR-008:** System MUST support split-screen view: Return (left) vs Supporting Documents (right)

**FR-009:** System MUST highlight system-detected discrepancies from Spec 3 (Enhanced Discrepancy Detection)

**FR-010:** System MUST allow auditor to annotate return:
- Add comments to specific lines
- Highlight sections for follow-up
- Attach internal notes (not visible to taxpayer)

**FR-011:** System MUST display taxpayer history:
- Prior 3 years' returns
- Payment history (on-time, late, delinquent)
- Prior audit results
- Penalty/interest assessed
- Outstanding balances

**FR-012:** System MUST display supporting documents with inline viewing (PDF viewer embedded)

**FR-013:** System MUST support side-by-side comparison for amended returns: Original vs Amended with diff highlighting

### Approval Workflow

**FR-014:** System MUST require auditor authentication before approval (password or certificate)

**FR-015:** System MUST generate approval record with:
- Return ID
- Auditor name and ID
- Approval date/time
- E-signature (digital signature hash)
- Approved tax liability (final amount)

**FR-016:** System MUST update return status to APPROVED

**FR-017:** System MUST notify taxpayer via email: "Return approved. Tax due: $X. Payment instructions: [link]."

**FR-018:** System MUST record approval in immutable audit log

**FR-019:** System MUST generate approval letter (PDF) with municipality seal and auditor signature

### Rejection Workflow

**FR-020:** System MUST require rejection reason (dropdown + text explanation)

**FR-021:** System MUST support multiple rejection reasons: MISSING_SCHEDULES | CALCULATION_ERRORS | UNSUPPORTED_DEDUCTIONS | MISSING_DOCUMENTATION | INSUFFICIENT_PAYMENT | OTHER

**FR-022:** System MUST allow auditor to specify required corrections (checklist)

**FR-023:** System MUST set resubmission deadline (default 30 days, configurable)

**FR-024:** System MUST update return status to REJECTED

**FR-025:** System MUST notify taxpayer with detailed rejection explanation and resubmission deadline

**FR-026:** System MUST create follow-up task for auditor: "Check for resubmission by [deadline]"

**FR-027:** System MUST send reminder to taxpayer 7 days before deadline if no resubmission

**FR-028:** System MUST escalate to supervisor if deadline passes without resubmission

### Documentation Request Management

**FR-029:** System MUST allow auditor to request additional documentation with fields: Document type, Description, Deadline

**FR-030:** System MUST update return status to AWAITING_DOCUMENTATION

**FR-031:** System MUST send email to taxpayer with:
- Document request details
- Upload link (secure portal)
- Deadline

**FR-032:** System MUST track request status: PENDING | RECEIVED | OVERDUE

**FR-033:** System MUST notify auditor when documentation uploaded

**FR-034:** System MUST send reminders to taxpayer: 7 days before deadline, 1 day before deadline

**FR-035:** System MUST mark request OVERDUE if deadline passes

**FR-036:** System MUST allow auditor to extend deadline or mark request as waived

### Automated Audit Checks

**FR-037:** System MUST perform automated audit checks on all submissions:
- Year-over-year variance analysis (income, expenses, deductions)
- Ratio analysis (profit margin, expense ratios)
- Peer comparison (same industry/revenue range)
- Pattern analysis (unusual transaction timing, round numbers)
- Rule compliance (all discrepancy rules from Spec 3)

**FR-038:** System MUST calculate risk score: 0-100 scale
- 0-20: LOW (routine return, no issues)
- 21-60: MEDIUM (minor discrepancies, standard review)
- 61-100: HIGH (major discrepancies, field audit recommended)

**FR-039:** System MUST generate audit report with:
- Risk score
- Flagged items (description, severity, recommended action)
- Comparison to prior years
- Peer comparison results
- Recommended audit procedures

**FR-040:** System MUST display audit report in review interface

**FR-041:** System MUST allow auditor to override risk score with justification

### Communication & Messaging

**FR-042:** System MUST support secure messaging between auditor and taxpayer

**FR-043:** System MUST log all communications (emails, messages, phone notes)

**FR-044:** System MUST support email templates:
- Approval notification
- Rejection notification
- Documentation request
- Reminder (deadline approaching)
- Payment reminder

**FR-045:** System MUST track email delivery status (sent, delivered, bounced, opened)

**FR-046:** System MUST allow auditor to send custom messages to taxpayer

### Audit Trail & History

**FR-047:** System MUST create immutable audit log for all actions:
- Return submission
- Auditor assignment
- Review started/completed
- Discrepancy flags
- Documentation requests
- Taxpayer responses
- Approval/rejection
- Amendments
- Payments

**FR-048:** System MUST log with attributes: Event type, User ID, Timestamp, IP address, Action details, Previous state, New state

**FR-049:** System MUST prevent editing or deletion of audit log entries (append-only)

**FR-050:** System MUST generate audit trail report (PDF) for legal/compliance purposes

**FR-051:** System MUST retain audit trail for 7 years (IRS requirement)

### Reporting & Analytics

**FR-052:** System MUST generate auditor performance reports:
- Returns reviewed per auditor
- Average review time
- Approval rate vs rejection rate
- Documentation requests per auditor
- Timeliness (% reviewed within SLA)

**FR-053:** System MUST generate compliance reports:
- Returns approved/rejected by month
- Common rejection reasons
- Average tax liability by return type
- Discrepancy trends (which rules triggered most often)
- High-risk returns (audit candidates)

**FR-054:** System MUST generate taxpayer reports:
- Filing compliance rate (% of expected returns received)
- Payment compliance rate (% paid on time)
- Repeat offenders (multiple rejections/penalties)

**FR-055:** System MUST support custom report builder for managers

### Role-Based Access Control

**FR-056:** System MUST support roles: AUDITOR | SENIOR_AUDITOR | SUPERVISOR | MANAGER | ADMIN

**FR-057:** System MUST enforce role permissions:
- AUDITOR: Review returns, request docs, recommend approval/rejection
- SENIOR_AUDITOR: Approve/reject returns (<$50K), all auditor permissions
- SUPERVISOR: Approve/reject any return, override priority, reassign returns, view team performance
- MANAGER: All permissions, generate compliance reports, configure audit rules
- ADMIN: System configuration, user management

**FR-058:** System MUST require supervisor approval for high-dollar returns (>$50K) or high-risk returns

**FR-059:** System MUST log all permission overrides (e.g., manager approves return assigned to auditor)

---

## Key Entities

### AuditQueue

**Attributes:**
- `queueId` (UUID)
- `returnId` (UUID): Foreign key to TaxReturn
- `priority` (enum): HIGH | MEDIUM | LOW
- `status` (enum): PENDING | IN_REVIEW | AWAITING_DOCUMENTATION | APPROVED | REJECTED
- `submissionDate` (timestamp)
- `assignedAuditorId` (UUID): Foreign key to User (auditor)
- `assignmentDate` (timestamp)
- `reviewStartedDate` (timestamp)
- `reviewCompletedDate` (timestamp)
- `riskScore` (number): 0-100
- `flaggedIssuesCount` (number)
- `daysInQueue` (number): Calculated field

### AuditAction

**Attributes:**
- `actionId` (UUID)
- `returnId` (UUID)
- `auditorId` (UUID)
- `actionType` (enum): ASSIGNED | REVIEW_STARTED | REVIEW_COMPLETED | APPROVED | REJECTED | DOCS_REQUESTED | ANNOTATED | ESCALATED
- `actionDate` (timestamp)
- `actionDetails` (JSON): Context-specific data (e.g., rejection reason, doc request)
- `previousStatus` (string)
- `newStatus` (string)
- `ipAddress` (string)
- `userAgent` (string)

### DocumentRequest

**Attributes:**
- `requestId` (UUID)
- `returnId` (UUID)
- `auditorId` (UUID)
- `requestDate` (timestamp)
- `documentType` (enum): GENERAL_LEDGER | BANK_STATEMENTS | DEPRECIATION_SCHEDULE | CONTRACTS | INVOICES | OTHER
- `description` (text): Specific request details
- `deadline` (date)
- `status` (enum): PENDING | RECEIVED | OVERDUE | WAIVED
- `receivedDate` (timestamp)
- `uploadedFiles` (array): File references

### AuditReport

**Attributes:**
- `reportId` (UUID)
- `returnId` (UUID)
- `generatedDate` (timestamp)
- `riskScore` (number): 0-100
- `riskLevel` (enum): LOW | MEDIUM | HIGH
- `flaggedItems` (array): List of issues found
- `yearOverYearComparison` (JSON): Income/expense variance
- `peerComparison` (JSON): Industry benchmarks
- `patternAnalysis` (JSON): Unusual patterns detected
- `recommendedActions` (array): Suggested audit procedures
- `auditorOverride` (boolean): Whether auditor changed risk score
- `overrideReason` (string)

### AuditTrail

**Attributes:**
- `trailId` (UUID)
- `returnId` (UUID)
- `eventType` (enum): SUBMISSION | ASSIGNMENT | REVIEW | APPROVAL | REJECTION | AMENDMENT | PAYMENT | COMMUNICATION | ESCALATION
- `userId` (UUID): Who performed action
- `timestamp` (timestamp)
- `ipAddress` (string)
- `eventDetails` (JSON): Full context
- `digitalSignature` (string): E-signature hash (for approvals)
- `immutable` (boolean): Always true (cannot edit)

---

## Success Criteria

- **Queue Processing:** Average time from submission to approval <7 days (vs current manual process: 30-45 days)
- **Auditor Efficiency:** Auditors process 20+ returns/day (vs <5 manually)
- **Rejection Rate:** <10% of returns rejected (clear guidance reduces errors)
- **Automated Detection:** 95%+ of calculation errors caught by automated checks before manual review
- **Audit Trail Compliance:** 100% of actions logged with non-repudiable audit trail (legal compliance)
- **Taxpayer Satisfaction:** 80%+ of taxpayers rate communication as clear and helpful

---

## Assumptions

- Municipal tax department has 5-10 auditors processing 1000-5000 returns annually
- High-dollar returns (>$50K tax) require supervisor approval (dual-review)
- Automated audit checks run asynchronously (don't block submission)
- Audit trail retained for 7 years (IRS/legal requirement)
- Digital signatures use PKI certificates or secure password authentication

---

## Dependencies

- **Enhanced Discrepancy Detection (Spec 3):** Automated audit checks leverage discrepancy rules
- **Business Form Library (Spec 8):** Forms generated during review process
- **Rule Engine (Spec 4):** Audit rules, risk scoring algorithms, approval thresholds configurable
- **User Authentication System:** Role-based access control, e-signatures

---

## Out of Scope

- **Field audits:** In-person business inspections (handled outside system)
- **Criminal investigations:** Fraud, evasion cases (refer to law enforcement)
- **Appeals process:** Taxpayer appeals of audit adjustments (separate legal workflow)
- **Multi-municipality coordination:** Sharing audit results across municipalities (privacy concerns)

---

## Edge Cases

1. **Auditor leaves mid-review:** Return IN_REVIEW for 30 days, auditor no longer employed. Supervisor reassigns to new auditor.

2. **Simultaneous approval/rejection:** Two auditors accidentally open same return. System locks return when first auditor takes action, displays error to second.

3. **Taxpayer submits while auditor reviewing:** Taxpayer amends return while auditor reviewing original. System notifies auditor: "Amended return submitted. Switch to new version?"

4. **Documentation request fulfilled after approval:** Auditor approves return, then taxpayer uploads requested docs. System archives docs, no action needed.

5. **High-risk return auto-approved by junior auditor:** System should require supervisor approval for risk score >60. If auditor bypasses, system sends alert to supervisor.

6. **Audit trail storage failure:** Database corruption prevents audit log entry. System MUST fail the action (cannot approve return without audit trail) and alert admin.

7. **Email delivery failure:** Approval email bounces. System retries 3 times, then marks as UNDELIVERABLE and creates manual task: "Contact taxpayer by phone."

8. **Mass rejection (system-wide issue):** Municipality changes rule (e.g., new schedule required). 500 returns must be rejected. System supports bulk rejection with standard message.
