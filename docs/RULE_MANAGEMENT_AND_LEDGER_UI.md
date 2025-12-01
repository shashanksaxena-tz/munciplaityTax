# Rule Management & Ledger Dashboard UI Documentation

## Overview

This document describes the new UI features for Rule Management and Ledger Dashboard that allow administrators to create, approve, update, and reject tax rules, as well as view ledger reports and financial data.

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

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/rules` | GET | List rules with optional filters |
| `/api/rules` | POST | Create a new rule |
| `/api/rules/{id}` | GET | Get rule details |
| `/api/rules/{id}` | PUT | Update a rule |
| `/api/rules/{id}/approve` | POST | Approve a pending rule |
| `/api/rules/{id}/reject` | POST | Reject a pending rule |
| `/api/rules/{id}` | DELETE | Void a rule |

### Ledger Service (`/api/v1/ledger`)

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/ledger/dashboard/filer/{id}` | GET | Get filer dashboard metrics |
| `/api/v1/ledger/dashboard/municipality/{id}` | GET | Get municipality dashboard metrics |
| `/api/v1/ledger/transactions/recent` | GET | Get recent transactions |
| `/api/v1/payments/process` | POST | Process a payment |
| `/api/v1/refunds/request` | POST | Request a refund |
| `/api/v1/trial-balance` | GET | Generate trial balance |
| `/api/v1/reconciliation/report` | GET | Generate reconciliation report |

## Demo Mode

For testing without a running backend, set `demo_mode` to `true` in localStorage:

```javascript
localStorage.setItem('demo_mode', 'true');
```

This enables a demo user with all admin roles and displays mock data in the dashboards.

## Screenshots

### Dashboard with Navigation
![Dashboard](https://github.com/user-attachments/assets/8adae343-66ad-46c1-9f23-c91f7d152edc)

### Rule Management Dashboard
![Rule Management](https://github.com/user-attachments/assets/2d5fccda-49e5-45ba-a713-03974de01ba4)

### Create Rule Form
![Create Rule](https://github.com/user-attachments/assets/5cd9dc05-c7b2-4032-abb7-b677a0cb894d)

### Ledger Dashboard
![Ledger Dashboard](https://github.com/user-attachments/assets/8290ffef-c31b-4009-8e9b-dfa97042d4d0)

## File Changes

- `App.tsx` - Added routes for `/admin/rules` and `/ledger`
- `components/Dashboard.tsx` - Added navigation cards for Ledger, Auditor, and Rule Management
- `components/RuleManagementDashboard.tsx` - New component for rule CRUD operations
- `components/LedgerDashboard.tsx` - Enhanced with mock data fallback
- `contexts/AuthContext.tsx` - Added demo mode support
- `docs/RULE_MANAGEMENT_AND_LEDGER_UI.md` - This documentation

## Security

- Rule Management requires `ROLE_TAX_ADMINISTRATOR`, `ROLE_MANAGER`, or `ROLE_ADMIN`
- Ledger Dashboard is protected by authentication
- Self-approval of rules is prevented (approver must be different from creator)
- All rule changes are logged with audit trail
