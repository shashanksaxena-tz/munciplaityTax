# Rule Management & Ledger Dashboard UI Documentation

## Overview

This document describes the UI features for Rule Management and Ledger Dashboard that connect to the backend services (`rule-service` and `ledger-service`) to provide administrators with the ability to create, approve, update, and reject tax rules, as well as view ledger reports and financial data.

## Backend Integration

The UI is fully integrated with the backend APIs:

- **Rule Service** (`rule-service`): Manages tax rules with temporal and approval workflow support
- **Ledger Service** (`ledger-service`): Handles payments, statements, reconciliation, and trial balance

## Features

### 1. Rule Management Dashboard

The Rule Management Dashboard (`/admin/rules`) provides a complete UI for tax administrators to manage tax calculation rules.

#### Access
- **URL**: `/admin/rules`
- **Required Roles**: `ROLE_TAX_ADMINISTRATOR`, `ROLE_MANAGER`, or `ROLE_ADMIN`
- **Navigation**: Click "Rule Management" card from the main Dashboard

#### Capabilities

1. **View Rules**
   - See all tax rules in a table with name, category, value, effective date, and status
   - Filter by category (TaxRates, IncomeInclusion, Deductions, Penalties, etc.)
   - Filter by status (Pending, Approved, Rejected, Voided)
   - Search by rule name or code

2. **Create Rules**
   - Click "Create Rule" button to open the creation form
   - Fields:
     - Rule Code (e.g., `MUNICIPAL_TAX_RATE`)
     - Rule Name (e.g., "Dublin Municipal Tax Rate")
     - Category (TaxRates, IncomeInclusion, Deductions, Penalties, Filing, Allocation, Withholding, Validation)
     - Value Type (NUMBER, PERCENTAGE, BOOLEAN, ENUM)
     - Value (based on value type)
     - Effective Date
     - End Date (optional)
     - Applies To (INDIVIDUAL, BUSINESS, or ALL)
     - Change Reason (required for audit trail)
     - Ordinance Reference (optional)

3. **Approve/Reject Rules**
   - Pending rules show Approve (✓) and Reject (✗) buttons
   - Approve: Confirms the rule change with e-signature
   - Reject: Requires a rejection reason

4. **Edit Rules**
   - Click the Edit (pencil) icon to modify an existing rule
   - Only rules that haven't been activated can be modified

5. **Void Rules**
   - Click the Void (trash) icon to soft-delete a rule
   - Maintains audit trail by not physically deleting

#### Statistics
- Total Rules count
- Pending Approval count (highlighted if > 0)
- Approved count
- Rejected count

### 2. Ledger Dashboard

The Ledger Dashboard (`/ledger`) provides financial visibility for both taxpayers and municipality staff.

#### Access
- **URL**: `/ledger`
- **Required Roles**: Any authenticated user
- **Navigation**: Click "Ledger & Payments" card from the main Dashboard

#### Role-Based Views

**Filer View** (Individual taxpayers):
- Current Balance
- Amount Due
- Recent Transactions
- Payment Status

**Municipality View** (Staff/Administrators):
- Total Revenue
- Outstanding Accounts Receivable (AR)
- Recent Transactions count
- Trial Balance status
- Total Filers
- Payments Today
- Pending Refunds

#### Quick Actions

**For Filers**:
- Make Payment
- View Statement
- Request Refund
- Audit Trail

**For Municipality**:
- Run Reconciliation
- Trial Balance
- Generate Reports
- View Statements

#### Recent Transactions Table
- Date
- Description
- Type (PAYMENT, ASSESSMENT, REFUND)
- Amount
- Status (completed, posted, approved, pending)

#### Last Reconciliation
- Shows the date of the last reconciliation run
- Button to run a new reconciliation

### 3. Navigation Integration

The main Dashboard now includes three quick-access cards:

1. **Ledger & Payments** (Green) - Available to all users
2. **Auditor Dashboard** (Blue) - Available to auditor roles
3. **Rule Management** (Purple) - Available to admin/manager roles

## Backend APIs

### Rule Service (`/api/rules`)

The frontend connects to these endpoints from `rule-service`:

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/rules` | GET | List rules with optional filters (tenantId, category, status) |
| `/api/rules` | POST | Create a new rule |
| `/api/rules/{ruleId}` | GET | Get rule details |
| `/api/rules/{ruleId}` | PUT | Update a rule |
| `/api/rules/{ruleId}/approve?approverId={id}` | POST | Approve a pending rule |
| `/api/rules/{ruleId}/reject?reason={reason}` | POST | Reject a pending rule |
| `/api/rules/{ruleId}?reason={reason}` | DELETE | Void a rule |
| `/api/rules/active` | GET | Get active rules for a tax year |
| `/api/rules/history/{ruleCode}` | GET | Get rule version history |

### Ledger Service (`/api/v1`)

The frontend connects to these endpoints from `ledger-service`:

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/payments/filer/{filerId}` | GET | Get filer payment history |
| `/api/v1/payments/process` | POST | Process a new payment |
| `/api/v1/statements/filer/{tenantId}/{filerId}` | GET | Get filer account statement |
| `/api/v1/trial-balance?tenantId={id}` | GET | Generate trial balance |
| `/api/v1/reconciliation/report/{tenantId}/{municipalityId}` | GET | Generate reconciliation report |
| `/api/v1/journal-entries/entity/{tenantId}/{entityId}` | GET | Get journal entries |
| `/api/v1/refunds/request` | POST | Request a refund |

## Running the Application

### Prerequisites

1. Start the backend services:
   ```bash
   # Start rule-service
   cd backend/rule-service
   ./mvnw spring-boot:run

   # Start ledger-service
   cd backend/ledger-service
   ./mvnw spring-boot:run
   ```

2. Start the frontend:
   ```bash
   npm run dev
   ```

### Demo Mode (Without Backend)

For testing the UI without running backend services, set `demo_mode` to `true` in localStorage:

```javascript
localStorage.setItem('demo_mode', 'true');
```

This enables a demo user with all admin roles. Note that API calls will fail but the UI will still be functional for navigation and form validation testing.

## Screenshots

### Dashboard with Navigation
![Dashboard](https://github.com/user-attachments/assets/8adae343-66ad-46c1-9f23-c91f7d152edc)

### Rule Management Dashboard (API Connected)
![Rule Management](https://github.com/user-attachments/assets/9fcb97f1-a890-4f4d-bebb-7a686b5149d0)

### Create Rule Form
![Create Rule](https://github.com/user-attachments/assets/5cd9dc05-c7b2-4032-abb7-b677a0cb894d)

### Ledger Dashboard
![Ledger Dashboard](https://github.com/user-attachments/assets/8290ffef-c31b-4009-8e9b-dfa97042d4d0)

## File Changes

- `App.tsx` - Added routes for `/admin/rules` and `/ledger`
- `components/Dashboard.tsx` - Added navigation cards for Ledger, Auditor, and Rule Management
- `components/RuleManagementDashboard.tsx` - Rule CRUD operations connected to rule-service API
- `components/LedgerDashboard.tsx` - Ledger operations connected to ledger-service API
- `contexts/AuthContext.tsx` - Added demo mode support
- `docs/RULE_MANAGEMENT_AND_LEDGER_UI.md` - This documentation

## Security

- Rule Management requires `ROLE_TAX_ADMINISTRATOR`, `ROLE_MANAGER`, or `ROLE_ADMIN`
- Ledger Dashboard is protected by authentication
- Self-approval of rules is prevented (approver must be different from creator)
- All rule changes are logged with audit trail
- All API calls include Authorization header with Bearer token
