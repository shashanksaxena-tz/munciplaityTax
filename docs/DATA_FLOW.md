# MuniTax Data Flow Documentation

## Overview

This document describes the data flows within the MuniTax system, covering key workflows from tax filing to auditor review.

---

## High-Level Data Flow

```mermaid
flowchart TB
    subgraph "Input Sources"
        USER[User Input]
        DOCS[Tax Documents<br/>W-2, 1099, etc.]
        RULES[Tax Rules<br/>Configuration]
    end

    subgraph "Processing Layer"
        EXTRACT[Document<br/>Extraction]
        VALIDATE[Data<br/>Validation]
        CALC[Tax<br/>Calculation]
        DISCR[Discrepancy<br/>Detection]
    end

    subgraph "Storage Layer"
        SESSION[(Session<br/>Storage)]
        SUBMISSION[(Submission<br/>Storage)]
        AUDIT[(Audit<br/>Trail)]
        LEDGER[(Ledger<br/>Entries)]
    end

    subgraph "Output Layer"
        PDF[PDF<br/>Generation]
        NOTIFY[Notifications]
        REPORT[Reports]
    end

    USER --> VALIDATE
    DOCS --> EXTRACT
    RULES --> CALC
    
    EXTRACT --> VALIDATE
    VALIDATE --> CALC
    CALC --> DISCR
    
    VALIDATE --> SESSION
    CALC --> SESSION
    DISCR --> SUBMISSION
    SUBMISSION --> AUDIT
    
    SESSION --> PDF
    SUBMISSION --> NOTIFY
    AUDIT --> REPORT
    LEDGER --> REPORT
```

---

## Core Data Flow Scenarios

### 1. Individual Tax Filing Flow

```mermaid
sequenceDiagram
    participant User
    participant Frontend
    participant Gateway
    participant Extraction
    participant TaxEngine
    participant Session
    participant PDF

    User->>Frontend: Upload W-2/1099 Documents
    Frontend->>Gateway: POST /extraction/stream
    Gateway->>Extraction: Process Documents
    Extraction->>Extraction: AI Analysis (Gemini)
    Extraction-->>Frontend: Streaming Progress Updates
    Extraction-->>Frontend: Extracted Form Data
    
    Frontend->>Frontend: User Reviews/Edits Data
    
    Frontend->>Gateway: POST /tax-engine/calculate/individual
    Gateway->>TaxEngine: Calculate Tax
    TaxEngine->>TaxEngine: Apply Rules
    TaxEngine->>TaxEngine: Detect Discrepancies
    TaxEngine-->>Gateway: Tax Calculation Result
    Gateway-->>Frontend: Display Results
    
    Frontend->>Gateway: POST /sessions
    Gateway->>Session: Save Session
    Session-->>Gateway: Session ID
    
    Frontend->>Gateway: POST /pdf/generate
    Gateway->>PDF: Generate Tax Return PDF
    PDF-->>Frontend: PDF Download
```

### 2. Business Tax Filing Flow

```mermaid
sequenceDiagram
    participant Business
    participant Frontend
    participant Gateway
    participant TaxEngine
    participant Rule
    participant Submission

    Business->>Frontend: Enter Business Data
    Business->>Frontend: Complete Schedule X (Reconciliation)
    Business->>Frontend: Complete Schedule Y (Allocation)
    
    Frontend->>Gateway: GET /rules/active?tenantId=&entityType=BUSINESS
    Gateway->>Rule: Get Active Business Rules
    Rule-->>Gateway: Business Tax Rules
    Gateway-->>Frontend: Rules Configuration
    
    Frontend->>Gateway: POST /tax-engine/calculate/business
    Gateway->>TaxEngine: Calculate Business Tax
    TaxEngine->>TaxEngine: Calculate Schedule X Adjustments
    TaxEngine->>TaxEngine: Calculate Allocation %
    TaxEngine->>TaxEngine: Apply NOL Carryforward
    TaxEngine->>TaxEngine: Calculate Penalties & Interest
    TaxEngine-->>Gateway: Business Tax Result
    Gateway-->>Frontend: Display Results
    
    Business->>Frontend: Submit Return
    Frontend->>Gateway: POST /submissions
    Gateway->>Submission: Create Submission
    Submission->>Submission: Add to Audit Queue
    Submission-->>Frontend: Confirmation Number
```

### 3. Document Extraction Flow

```mermaid
flowchart TB
    subgraph "Input"
        DOC[PDF/Image Document]
    end

    subgraph "Extraction Service"
        RECV[Receive Document]
        GEMINI[Gemini AI Analysis]
        PARSE[Parse Response]
        CONF[Calculate Confidence]
    end

    subgraph "Form Processing"
        W2[W-2 Extraction]
        F1099[1099 Extraction]
        SCHC[Schedule C Extraction]
        SCHE[Schedule E Extraction]
        SCHF[Schedule F Extraction]
        W2G[W-2G Extraction]
    end

    subgraph "Output"
        FORMS[Extracted Form Objects]
        PROFILE[Taxpayer Profile]
        SCORE[Confidence Scores]
    end

    DOC --> RECV
    RECV --> GEMINI
    GEMINI --> PARSE
    PARSE --> CONF
    
    PARSE --> W2
    PARSE --> F1099
    PARSE --> SCHC
    PARSE --> SCHE
    PARSE --> SCHF
    PARSE --> W2G
    
    W2 --> FORMS
    F1099 --> FORMS
    SCHC --> FORMS
    SCHE --> FORMS
    SCHF --> FORMS
    W2G --> FORMS
    
    PARSE --> PROFILE
    CONF --> SCORE
```

### 4. Auditor Workflow Data Flow

```mermaid
flowchart TB
    subgraph "Submission"
        SUB[Taxpayer Submits Return]
        QUEUE[Add to Audit Queue]
        RISK[Calculate Risk Score]
    end

    subgraph "Review"
        ASSIGN[Assign to Auditor]
        REVIEW[Review Return]
        REPORT[View Audit Report]
    end

    subgraph "Actions"
        APPROVE[Approve Return]
        REJECT[Reject Return]
        REQUEST[Request Documents]
    end

    subgraph "Audit Trail"
        LOG[Log All Actions]
        SIGN[Digital Signature]
        TRAIL[Immutable Trail]
    end

    SUB --> QUEUE
    QUEUE --> RISK
    RISK --> ASSIGN
    ASSIGN --> REVIEW
    REVIEW --> REPORT
    
    REPORT --> APPROVE
    REPORT --> REJECT
    REPORT --> REQUEST
    
    APPROVE --> LOG
    REJECT --> LOG
    REQUEST --> LOG
    
    LOG --> SIGN
    SIGN --> TRAIL
```

---

## Data Transformation Pipeline

### Tax Calculation Data Flow

```mermaid
flowchart LR
    subgraph "Input Data"
        W2[W-2 Forms]
        F1099[1099 Forms]
        SCH[Schedules C/E/F]
        W2G[W-2G Forms]
    end

    subgraph "Aggregation"
        WAGES[Sum Qualifying Wages]
        PROFIT[Sum Net Profit]
        CREDIT[Sum Credits]
    end

    subgraph "Calculation"
        GROSS[Calculate Gross Income]
        LIABLE[Calculate Liability]
        FINAL[Apply Credits]
    end

    subgraph "Output"
        RESULT[Tax Result]
        DISCR[Discrepancy Report]
        BREAK[Tax Breakdown]
    end

    W2 --> WAGES
    F1099 --> PROFIT
    SCH --> PROFIT
    W2G --> PROFIT
    
    WAGES --> GROSS
    PROFIT --> GROSS
    
    GROSS --> LIABLE
    LIABLE --> FINAL
    CREDIT --> FINAL
    
    FINAL --> RESULT
    FINAL --> DISCR
    FINAL --> BREAK
```

### Schedule X Calculation (Business)

```mermaid
flowchart TB
    subgraph "Federal Input"
        FED[Federal Taxable Income]
    end

    subgraph "Add-Backs"
        ADD1[Interest & State Taxes]
        ADD2[Wages Credit]
        ADD3[Capital Losses]
        ADD4[Guaranteed Payments]
        ADD5[Intangible Expenses]
        ADD6[Other Add-Backs]
    end

    subgraph "Deductions"
        DED1[Interest Income]
        DED2[Dividends]
        DED3[Capital Gains]
        DED4[Section 179 Excess]
        DED5[Other Deductions]
    end

    subgraph "Output"
        ADJ[Adjusted Municipal Income]
    end

    FED --> ADJ
    
    ADD1 --> ADJ
    ADD2 --> ADJ
    ADD3 --> ADJ
    ADD4 --> ADJ
    ADD5 --> ADJ
    ADD6 --> ADJ
    
    DED1 --> ADJ
    DED2 --> ADJ
    DED3 --> ADJ
    DED4 --> ADJ
    DED5 --> ADJ
```

---

## Data Storage Flow

### Session Lifecycle

```mermaid
stateDiagram-v2
    [*] --> DRAFT: Create Session
    DRAFT --> IN_PROGRESS: Start Filing
    IN_PROGRESS --> CALCULATED: Calculate Tax
    CALCULATED --> IN_PROGRESS: Edit Data
    CALCULATED --> SUBMITTED: Submit Return
    SUBMITTED --> IN_REVIEW: Auditor Assigned
    IN_REVIEW --> APPROVED: Auditor Approves
    IN_REVIEW --> REJECTED: Auditor Rejects
    REJECTED --> AMENDED: Taxpayer Amends
    AMENDED --> SUBMITTED: Resubmit
    APPROVED --> PAID: Payment Processed
    PAID --> [*]
```

### Audit Trail Data Flow

```mermaid
flowchart LR
    subgraph "Events"
        E1[Submission]
        E2[Assignment]
        E3[Review Start]
        E4[Document Request]
        E5[Approval/Rejection]
    end

    subgraph "Audit Trail Entry"
        ENTRY[Create Entry]
        SIGN[Digital Signature]
        STORE[Store Immutably]
    end

    subgraph "Storage"
        DB[(PostgreSQL)]
    end

    E1 --> ENTRY
    E2 --> ENTRY
    E3 --> ENTRY
    E4 --> ENTRY
    E5 --> ENTRY
    
    ENTRY --> SIGN
    SIGN --> STORE
    STORE --> DB
```

---

## Integration Data Flows

### External System Integration

```mermaid
flowchart TB
    subgraph "MuniTax System"
        EXTRACT[Extraction Service]
        LEDGER[Ledger Service]
        PDF[PDF Service]
    end

    subgraph "External Systems"
        GEMINI[Google Gemini AI]
        PAYMENT[Payment Gateway]
        EMAIL[Email Service]
    end

    subgraph "Data Flow"
        D1[Document Images]
        D2[Extracted Data]
        D3[Payment Request]
        D4[Payment Response]
        D5[Notification]
    end

    D1 --> EXTRACT
    EXTRACT --> GEMINI
    GEMINI --> D2
    D2 --> EXTRACT

    LEDGER --> D3
    D3 --> PAYMENT
    PAYMENT --> D4
    D4 --> LEDGER

    LEDGER --> D5
    D5 --> EMAIL
```

---

## Data Validation Flow

### Discrepancy Detection

```mermaid
flowchart TB
    subgraph "Input Data"
        W2[W-2 Data]
        FED[Federal Return]
        LOCAL[Local Return]
        SCHED[Schedules]
    end

    subgraph "Validation Rules"
        R1[FR-001: Box 18 vs Box 1]
        R2[FR-002: Withholding Rate]
        R3[FR-003: Duplicate Detection]
        R4[FR-006: Schedule C Estimated]
        R5[FR-014: Credit Limits]
        R6[FR-017: Federal Reconciliation]
    end

    subgraph "Output"
        ISSUES[Discrepancy Issues]
        SEV[Severity Levels]
        ACTIONS[Recommended Actions]
    end

    W2 --> R1
    W2 --> R2
    W2 --> R3
    SCHED --> R4
    LOCAL --> R5
    FED --> R6

    R1 --> ISSUES
    R2 --> ISSUES
    R3 --> ISSUES
    R4 --> ISSUES
    R5 --> ISSUES
    R6 --> ISSUES

    ISSUES --> SEV
    ISSUES --> ACTIONS
```

---

## Batch Processing Data Flows

### End-of-Period Processing

```mermaid
flowchart TB
    subgraph "Triggers"
        DAILY[Daily Batch]
        MONTHLY[Month End]
        YEARLY[Year End]
    end

    subgraph "Daily Jobs"
        D1[Update Days in Queue]
        D2[Check Document Deadlines]
        D3[Send Reminders]
    end

    subgraph "Monthly Jobs"
        M1[Generate Reports]
        M2[Withholding Due Dates]
        M3[Interest Calculations]
    end

    subgraph "Yearly Jobs"
        Y1[NOL Expirations]
        Y2[Rate Updates]
        Y3[Compliance Reports]
    end

    DAILY --> D1
    DAILY --> D2
    DAILY --> D3

    MONTHLY --> M1
    MONTHLY --> M2
    MONTHLY --> M3

    YEARLY --> Y1
    YEARLY --> Y2
    YEARLY --> Y3
```

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2025-12-01 | Initial data flow documentation |

---

**Document Owner:** Development Team  
**Last Updated:** December 1, 2025
