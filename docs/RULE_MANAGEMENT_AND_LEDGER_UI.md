# Rule Management & Ledger Dashboard UI Documentation

## Overview

This document describes the UI features for Rule Management and Ledger Dashboard that connect to the backend services (`rule-service` and `ledger-service`) to provide administrators with the ability to create, approve, update, and reject tax rules, as well as view ledger reports and financial data.

## Backend Integration

The UI is fully integrated with the backend APIs:

- **Rule Service** (`rule-service`): Manages tax rules with temporal and approval workflow support
- **Ledger Service** (`ledger-service`): Handles payments, statements, reconciliation, and trial balance
- **Tax Engine Service** (`tax-engine-service`): Consumes rules for tax calculations

All rule logic has been moved from the frontend to the backend. The frontend no longer contains hardcoded tax rules - it retrieves them from the rule-service API.

## Comprehensive Tax Rules

The system includes 35+ comprehensive Ohio municipality tax rules organized into categories:

### Tax Rate Rules
- Municipal tax rates for Individual and Business entities
- Credit limit rates
- 35+ Ohio municipality locality rates for Schedule Y reciprocity calculations

### W-2 Qualifying Wages Rules
Controls which W-2 box determines the municipal tax base:
- `HIGHEST_OF_ALL` - Uses highest of Box 1, 5, or 18 (default)
- `BOX_5_MEDICARE` - Always use Medicare wages
- `BOX_18_LOCAL` - Always use local wages
- `BOX_1_FEDERAL` - Always use federal wages

### Income Inclusion Rules
- Schedule C (Self-Employment)
- Schedule E (Rental/Royalties)
- Schedule F (Farm Income)
- W-2G (Gambling)
- 1099-MISC/NEC

### Penalty Rules
- Late filing penalty ($25 fixed)
- Underpayment penalty (15%)
- Annual interest rate (7%)
- Safe harbor percentage (90%)

### Business Rules
- Allocation method (3-factor, single-sales, double-weighted)
- NOL carryforward enable/disable
- NOL offset cap (50%)
- Intangible expense rate (5%)

### Filing Rules
- Filing threshold
- Extension days (180)
- Quarterly estimate threshold ($200)

## Ohio Municipality Locality Rates

The system includes tax rates for 35+ Ohio municipalities for Schedule Y reciprocity calculations:

| Municipality | Rate | County |
|-------------|------|--------|
| Columbus | 2.5% | Franklin |
| Cleveland | 2.5% | Cuyahoga |
| Cincinnati | 2.1% | Hamilton |
| Toledo | 2.25% | Lucas |
| Akron | 2.5% | Summit |
| Dayton | 2.5% | Montgomery |
| Dublin | 2.0% | Franklin |
| Westerville | 2.0% | Franklin |
| Hilliard | 2.5% | Franklin |
| Upper Arlington | 2.5% | Franklin |
| Grandview Heights | 2.5% | Franklin |
| Bexley | 2.5% | Franklin |
| Worthington | 2.5% | Franklin |
| Gahanna | 2.5% | Franklin |
| Reynoldsburg | 2.5% | Franklin |
| Grove City | 2.0% | Franklin |
| Pickerington | 1.5% | Fairfield |
| New Albany | 2.0% | Franklin |
| Delaware | 1.85% | Delaware |
| Powell | 1.0% | Delaware |
| Marysville | 1.5% | Union |
| Springfield | 2.4% | Clark |
| Youngstown | 2.75% | Mahoning |
| Canton | 2.5% | Stark |
| Parma | 3.0% | Cuyahoga |
| Lorain | 2.5% | Lorain |
| Elyria | 2.25% | Lorain |
| Mansfield | 2.0% | Richland |
| Newark | 1.75% | Licking |
| Lancaster | 1.5% | Fairfield |
| Zanesville | 2.0% | Muskingum |
| Chillicothe | 1.85% | Ross |
| Marion | 1.85% | Marion |
| Findlay | 1.5% | Hancock |
| Lima | 1.5% | Allen |
| Sandusky | 1.5% | Erie |
| Fremont | 1.4% | Sandusky |
| Tiffin | 1.5% | Seneca |

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
   - Filter by entity type (Individual, Business, C-Corp, S-Corp, LLC)
   - Filter by status (Pending, Approved, Rejected, Voided)
   - Search by rule name or code
   - System/default rules are marked with a purple "DEFAULT" badge

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
   - System/default rules cannot be voided (only modified)

6. **Documentation**
   - Click "Documentation" button to view comprehensive rule configuration guide
   - Includes explanations of all rule categories, value types, and best practices

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

## Database Migrations

The comprehensive tax rules are seeded via database migration:

```
backend/rule-service/src/main/resources/db/migration/V4__seed_comprehensive_tax_rules.sql
```

This migration creates 50+ default rules including:
- Municipal tax rates
- W2 wage selection rules
- Income inclusion toggles
- Penalty configurations
- Business allocation settings
- Ohio municipality locality rates

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

This enables:
- Demo user with all admin roles
- Mock data for 35+ tax rules
- Simulated approve/reject/void operations
- Mock ledger dashboard data

## File Changes

- `App.tsx` - Added routes for `/admin/rules` and `/ledger`
- `components/Dashboard.tsx` - Added navigation cards for Ledger, Auditor, and Rule Management
- `components/RuleManagementDashboard.tsx` - Comprehensive rule management with 35+ mock rules, entity type filter, DEFAULT badge, documentation modal
- `components/LedgerDashboard.tsx` - Ledger operations connected to ledger-service API with mock data fallback
- `contexts/AuthContext.tsx` - Added demo mode support
- `types.ts` - Added `isSystem` field to TaxRule interface
- `backend/rule-service/src/main/resources/db/migration/V4__seed_comprehensive_tax_rules.sql` - Comprehensive tax rules seed data
- `docs/RULE_MANAGEMENT_AND_LEDGER_UI.md` - This documentation

## Security

- Rule Management requires `ROLE_TAX_ADMINISTRATOR`, `ROLE_MANAGER`, or `ROLE_ADMIN`
- Ledger Dashboard is protected by authentication
- Self-approval of rules is prevented (approver must be different from creator)
- All rule changes are logged with audit trail
- All API calls include Authorization header with Bearer token
- System/default rules cannot be deleted (only modified)
