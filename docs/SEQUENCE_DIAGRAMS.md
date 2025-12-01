# MuniTax Sequence Diagrams

## Overview

This document contains detailed sequence diagrams for key workflows in the MuniTax system.

---

## 1. User Authentication Flow

```mermaid
sequenceDiagram
    autonumber
    participant User
    participant Frontend
    participant Gateway
    participant Auth
    participant DB as PostgreSQL
    participant Redis

    User->>Frontend: Enter credentials
    Frontend->>Gateway: POST /api/v1/auth/login
    Gateway->>Auth: Forward login request
    Auth->>DB: Find user by username
    DB-->>Auth: User record
    Auth->>Auth: Validate password (BCrypt)
    
    alt Password Valid
        Auth->>Auth: Generate JWT token
        Auth->>Redis: Cache session
        Auth-->>Gateway: JWT token + user info
        Gateway-->>Frontend: 200 OK + token
        Frontend->>Frontend: Store token in localStorage
        Frontend-->>User: Redirect to dashboard
    else Password Invalid
        Auth-->>Gateway: 401 Unauthorized
        Gateway-->>Frontend: 401 error
        Frontend-->>User: Show error message
    end
```

---

## 2. Individual Tax Return Filing

```mermaid
sequenceDiagram
    autonumber
    participant Taxpayer
    participant Frontend
    participant Gateway
    participant Extraction
    participant Gemini as Google Gemini
    participant TaxEngine
    participant Session
    participant PDF

    Taxpayer->>Frontend: Upload tax documents (W-2, 1099)
    Frontend->>Gateway: POST /extraction/stream
    Gateway->>Extraction: Forward documents
    Extraction->>Gemini: Send for AI extraction
    
    loop Streaming Progress
        Gemini-->>Extraction: Extraction progress
        Extraction-->>Frontend: SSE: Progress updates
    end
    
    Gemini-->>Extraction: Complete extraction results
    Extraction->>Extraction: Parse and validate
    Extraction-->>Frontend: Extracted form data
    
    Frontend-->>Taxpayer: Display extracted data for review
    Taxpayer->>Frontend: Review and confirm/edit data
    
    Frontend->>Gateway: POST /tax-engine/calculate/individual
    Gateway->>TaxEngine: Calculate tax
    TaxEngine->>TaxEngine: Process W-2 forms
    TaxEngine->>TaxEngine: Process Schedule X (business income)
    TaxEngine->>TaxEngine: Process Schedule Y (credits)
    TaxEngine->>TaxEngine: Run discrepancy detection
    TaxEngine-->>Gateway: Tax calculation result
    Gateway-->>Frontend: Tax summary + discrepancies
    
    Frontend-->>Taxpayer: Display results
    
    alt Accept Results
        Taxpayer->>Frontend: Save session
        Frontend->>Gateway: POST /sessions
        Gateway->>Session: Create session
        Session-->>Gateway: Session ID
        Gateway-->>Frontend: Session saved
        
        Taxpayer->>Frontend: Generate PDF
        Frontend->>Gateway: POST /pdf/generate
        Gateway->>PDF: Generate tax return PDF
        PDF-->>Frontend: PDF download
    else Reject/Edit
        Taxpayer->>Frontend: Edit data
        Frontend->>Gateway: POST /tax-engine/calculate/individual
        Note over TaxEngine: Recalculate...
    end
```

---

## 3. Business Tax Return Filing

```mermaid
sequenceDiagram
    autonumber
    participant Business
    participant Frontend
    participant Gateway
    participant Rule
    participant TaxEngine
    participant Submission
    participant Audit

    Business->>Frontend: Start business tax filing
    Frontend->>Gateway: GET /rules/active?entityType=BUSINESS
    Gateway->>Rule: Get active business rules
    Rule->>Rule: Query rules by tenant + date
    Rule-->>Gateway: Business tax rules
    Gateway-->>Frontend: Rules configuration
    
    Business->>Frontend: Enter Schedule X data
    Note over Business,Frontend: Add-backs: Interest, State taxes,<br/>Guaranteed payments, etc.
    Note over Business,Frontend: Deductions: Interest income,<br/>Dividends, Capital gains, etc.
    
    Business->>Frontend: Enter Schedule Y data
    Note over Business,Frontend: Property factor: Dublin/Everywhere
    Note over Business,Frontend: Payroll factor: Dublin/Everywhere
    Note over Business,Frontend: Sales factor: Dublin/Everywhere
    
    Frontend->>Gateway: POST /tax-engine/calculate/business
    Gateway->>TaxEngine: Calculate business tax
    
    TaxEngine->>TaxEngine: Calculate adjusted federal income
    TaxEngine->>TaxEngine: Calculate allocation percentage
    TaxEngine->>TaxEngine: Apply NOL carryforward
    TaxEngine->>TaxEngine: Calculate tax due
    TaxEngine->>TaxEngine: Calculate penalties & interest
    TaxEngine-->>Gateway: Business tax result
    Gateway-->>Frontend: Tax summary
    
    Frontend-->>Business: Display results
    
    Business->>Frontend: Submit return
    Frontend->>Gateway: POST /submissions
    Gateway->>Submission: Create submission
    Submission->>Submission: Validate return
    Submission->>Audit: Add to audit queue
    Audit->>Audit: Calculate risk score
    Submission-->>Gateway: Confirmation number
    Gateway-->>Frontend: Submission confirmed
    Frontend-->>Business: Show confirmation
```

---

## 4. Auditor Review Workflow

```mermaid
sequenceDiagram
    autonumber
    participant Auditor
    participant Frontend
    participant Gateway
    participant Submission
    participant Audit
    participant DB as PostgreSQL

    Auditor->>Frontend: Access audit dashboard
    Frontend->>Gateway: GET /audit/queue?status=PENDING
    Gateway->>Submission: Get queue items
    Submission->>DB: Query audit_queue
    DB-->>Submission: Queue items
    Submission-->>Gateway: Paginated queue
    Gateway-->>Frontend: Queue data
    Frontend-->>Auditor: Display submission queue
    
    Auditor->>Frontend: Select return to review
    Frontend->>Gateway: POST /audit/assign
    Gateway->>Audit: Assign auditor
    Audit->>DB: Update audit_queue
    Audit->>Audit: Log assignment event
    Audit-->>Gateway: Assignment confirmed
    Gateway-->>Frontend: Return assigned
    
    Frontend->>Gateway: GET /audit/report/{returnId}
    Gateway->>Audit: Get audit report
    Audit->>DB: Query risk score, flags
    Audit-->>Gateway: Audit report
    Gateway-->>Frontend: Risk assessment + flags
    Frontend-->>Auditor: Display return details + report
    
    alt Approve Return
        Auditor->>Frontend: Enter e-signature
        Frontend->>Gateway: POST /audit/approve
        Gateway->>Audit: Process approval
        Audit->>Audit: Validate e-signature
        Audit->>DB: Update status to APPROVED
        Audit->>Audit: Log approval with signature hash
        Audit-->>Gateway: Approval confirmed
        Gateway-->>Frontend: Success
        Frontend-->>Auditor: Return approved
    else Reject Return
        Auditor->>Frontend: Enter rejection reason
        Frontend->>Gateway: POST /audit/reject
        Gateway->>Audit: Process rejection
        Audit->>DB: Update status to REJECTED
        Audit->>Audit: Log rejection event
        Audit-->>Gateway: Rejection confirmed
        Gateway-->>Frontend: Success
        Frontend-->>Auditor: Return rejected
    else Request Documents
        Auditor->>Frontend: Specify document request
        Frontend->>Gateway: POST /audit/request-docs
        Gateway->>Audit: Create document request
        Audit->>DB: Insert document_request
        Audit->>Audit: Update status to AWAITING_DOCUMENTATION
        Audit-->>Gateway: Request created
        Gateway-->>Frontend: Success
        Frontend-->>Auditor: Document request sent
    end
```

---

## 5. Tax Rule Configuration

```mermaid
sequenceDiagram
    autonumber
    participant Admin
    participant Frontend
    participant Gateway
    participant Rule
    participant DB as PostgreSQL
    participant Redis

    Admin->>Frontend: Access rule configuration
    Frontend->>Gateway: GET /rules?tenantId=dublin&category=TAX_RATES
    Gateway->>Rule: Get rules
    Rule->>DB: Query tax_rules
    DB-->>Rule: Existing rules
    Rule-->>Gateway: Rules list
    Gateway-->>Frontend: Display rules
    Frontend-->>Admin: Show rule grid
    
    Admin->>Frontend: Create new rule
    Frontend->>Gateway: POST /rules
    Gateway->>Rule: Create rule request
    Rule->>Rule: Validate rule data
    Rule->>Rule: Check date range conflicts
    
    alt Validation Passes
        Rule->>DB: Insert tax_rule (status: PENDING)
        Rule->>DB: Insert rule_change_log
        Rule-->>Gateway: Rule created
        Gateway-->>Frontend: Success + rule ID
        Frontend-->>Admin: Rule created (pending approval)
    else Validation Fails
        Rule-->>Gateway: 400 validation error
        Gateway-->>Frontend: Error details
        Frontend-->>Admin: Show validation errors
    end
    
    Admin->>Frontend: Approve rule
    Frontend->>Gateway: POST /rules/{id}/approve
    Gateway->>Rule: Approve rule
    Rule->>DB: Update approval_status = APPROVED
    Rule->>DB: Log approval in change_log
    Rule->>Redis: Invalidate rule cache
    Rule-->>Gateway: Rule approved
    Gateway-->>Frontend: Success
    Frontend-->>Admin: Rule is now active
```

---

## 6. Document Extraction Flow

```mermaid
sequenceDiagram
    autonumber
    participant User
    participant Frontend
    participant Gateway
    participant Extraction
    participant Gemini as Google Gemini AI

    User->>Frontend: Select file(s) to upload
    Frontend->>Frontend: Validate file type (PDF/Image)
    Frontend->>Gateway: GET /extraction/stream?fileName={name}
    Gateway->>Extraction: Initialize extraction
    
    Extraction->>Extraction: Read document bytes
    Extraction->>Gemini: POST with extraction prompt
    Note over Extraction,Gemini: Comprehensive 200+ line prompt<br/>for form identification and extraction
    
    loop Real-time Streaming
        Gemini-->>Extraction: Partial response chunk
        Extraction->>Extraction: Parse JSON chunk
        Extraction-->>Frontend: SSE: status, progress, log
        Frontend-->>User: Update progress UI
    end
    
    Gemini-->>Extraction: Final extraction result
    Extraction->>Extraction: Parse form data
    Extraction->>Extraction: Calculate confidence scores
    Extraction->>Extraction: Build TaxFormData objects
    
    Extraction-->>Frontend: Complete extraction result
    Note over Frontend: Includes: forms[], profile,<br/>confidence, detectedForms[]
    
    Frontend->>Frontend: Map to TypeScript types
    Frontend-->>User: Display extracted data for review
    
    alt Low Confidence Detected
        Frontend-->>User: Highlight uncertain fields
        User->>Frontend: Manually correct values
    end
```

---

## 7. Payment Processing Flow

```mermaid
sequenceDiagram
    autonumber
    participant Taxpayer
    participant Frontend
    participant Gateway
    participant Ledger
    participant Payment as Mock Payment Provider
    participant DB as PostgreSQL

    Taxpayer->>Frontend: Click "Pay Now"
    Frontend->>Gateway: GET /ledger/account/{taxpayerId}
    Gateway->>Ledger: Get account statement
    Ledger->>DB: Query account_balances
    Ledger-->>Gateway: Balance due
    Gateway-->>Frontend: Amount owed
    Frontend-->>Taxpayer: Display payment amount
    
    Taxpayer->>Frontend: Enter payment details
    Frontend->>Gateway: POST /ledger/payments
    Gateway->>Ledger: Process payment
    Ledger->>Payment: Submit payment request
    
    Payment->>Payment: Validate card/ACH
    Payment->>Payment: Process transaction
    
    alt Payment Success
        Payment-->>Ledger: Success + confirmation
        Ledger->>DB: Create journal_entry (debit cash, credit liability)
        Ledger->>DB: Update account_balances
        Ledger->>DB: Insert payment_transaction
        Ledger-->>Gateway: Payment confirmed
        Gateway-->>Frontend: Success + receipt
        Frontend-->>Taxpayer: Show receipt
    else Payment Failed
        Payment-->>Ledger: Failure reason
        Ledger-->>Gateway: Payment failed
        Gateway-->>Frontend: Error message
        Frontend-->>Taxpayer: Show error, retry option
    end
```

---

## 8. NOL Carryforward Processing

```mermaid
sequenceDiagram
    autonumber
    participant Business
    participant Frontend
    participant Gateway
    participant TaxEngine
    participant NOL as NOL Service
    participant DB as PostgreSQL

    Business->>Frontend: File return with loss
    Frontend->>Gateway: POST /tax-engine/calculate/business
    Gateway->>TaxEngine: Calculate business tax
    
    TaxEngine->>TaxEngine: Calculate allocated income
    
    alt Allocated Income < 0 (Loss Year)
        TaxEngine->>NOL: Create NOL record
        NOL->>DB: Insert NOL entry
        Note over NOL,DB: Store: amount, originYear,<br/>expirationYear (20 years)
        NOL-->>TaxEngine: NOL created
        TaxEngine->>TaxEngine: Tax due = 0
    else Allocated Income > 0 (Profit Year)
        TaxEngine->>NOL: Get available NOL
        NOL->>DB: Query NOL records
        NOL-->>TaxEngine: Available NOL amount
        
        TaxEngine->>TaxEngine: Calculate max NOL offset
        Note over TaxEngine: Limited to 50% of allocated income
        
        TaxEngine->>TaxEngine: Apply NOL
        TaxEngine->>NOL: Record NOL usage
        NOL->>DB: Update NOL balance
        NOL->>DB: Insert NOL usage record
        
        TaxEngine->>TaxEngine: Calculate tax on reduced income
    end
    
    TaxEngine-->>Gateway: Tax result + NOL details
    Gateway-->>Frontend: Results
    Frontend-->>Business: Show tax + NOL status
```

---

## 9. Multi-Tenant Session Management

```mermaid
sequenceDiagram
    autonumber
    participant User
    participant Frontend
    participant Gateway
    participant Tenant
    participant Session
    participant DB as PostgreSQL

    User->>Frontend: Login with tenant context
    Frontend->>Gateway: POST /auth/login
    Note over Gateway: JWT includes tenantId claim
    
    Frontend->>Gateway: POST /sessions
    Note over Gateway: X-Tenant-ID header
    Gateway->>Session: Create session request
    Session->>Session: Validate tenant access
    Session->>DB: Insert into {tenant}_sessions
    Note over DB: Schema-per-tenant isolation
    Session-->>Gateway: Session ID
    Gateway-->>Frontend: Session created
    
    Frontend->>Gateway: GET /sessions/{id}
    Gateway->>Session: Get session
    Session->>Session: Verify tenant context
    Session->>DB: Query from tenant schema
    Session-->>Gateway: Session data
    Gateway-->>Frontend: Session details
    
    User->>Frontend: Switch tenant context
    Frontend->>Gateway: GET /tenants/available
    Gateway->>Tenant: List user's tenants
    Tenant->>DB: Query user_tenants
    Tenant-->>Gateway: Available tenants
    Gateway-->>Frontend: Tenant list
    
    User->>Frontend: Select different tenant
    Frontend->>Gateway: POST /auth/switch-tenant
    Gateway->>Gateway: Generate new JWT with new tenantId
    Gateway-->>Frontend: New token
    Frontend->>Frontend: Update localStorage
    Frontend-->>User: Refresh dashboard for new tenant
```

---

## 10. PDF Generation Flow

```mermaid
sequenceDiagram
    autonumber
    participant User
    participant Frontend
    participant Gateway
    participant PDF as PDF Service
    participant TaxEngine

    User->>Frontend: Request PDF generation
    Frontend->>Gateway: POST /pdf/generate/tax-return
    Note over Gateway: Include sessionId + returnType
    
    Gateway->>PDF: Generate request
    PDF->>TaxEngine: Get calculation result
    TaxEngine-->>PDF: Tax data
    
    PDF->>PDF: Initialize PDFBox document
    PDF->>PDF: Load Dublin 1040 template
    
    loop Populate Sections
        PDF->>PDF: Section A: Taxable Income
        PDF->>PDF: Section B: Tax Calculation
        PDF->>PDF: Section C: Balance Due/Refund
    end
    
    PDF->>PDF: Add header (tax year, amendment)
    PDF->>PDF: Add taxpayer info block
    PDF->>PDF: Add signature section
    
    PDF->>PDF: Generate byte array
    PDF-->>Gateway: PDF bytes + filename
    Gateway-->>Frontend: Content-Disposition: attachment
    Frontend-->>User: Download PDF
```

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2025-12-01 | Initial sequence diagram documentation |

---

**Document Owner:** Development Team  
**Last Updated:** December 1, 2025
