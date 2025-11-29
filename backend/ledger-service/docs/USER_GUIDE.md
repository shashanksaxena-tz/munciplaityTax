# User Guide - Ledger Service

This guide provides step-by-step instructions for using the Ledger Service.

## Table of Contents

1. [For Filers (Businesses & Individuals)](#for-filers)
2. [For Municipality Finance Officers](#for-municipality-finance-officers)
3. [For System Administrators](#for-system-administrators)
4. [Common Tasks](#common-tasks)
5. [Troubleshooting](#troubleshooting)
6. [FAQs](#faqs)

---

## For Filers

### Making a Tax Payment

#### Step 1: Access the Payment Portal

1. Log in to your account at https://app.munitax.com
2. Navigate to "Payments" from the main menu
3. Click "Make a Payment"

#### Step 2: Enter Payment Details

**Test Mode Indicator**: If you see "TEST MODE: No real charges", you're in the testing environment.

1. Select payment amount (or view suggested amount from tax return)
2. Choose payment method:
   - Credit Card
   - ACH (Bank Account)
   - Check
   - Wire Transfer

#### Step 3: Complete Payment Information

**For Credit Card:**
- Card Number: Enter 16-digit card number
- Expiration Date: MM/YY format
- CVV: 3 or 4 digit security code
- Cardholder Name

**Test Cards** (Test Mode Only):
- `4111-1111-1111-1111`: Always approved
- `4000-0000-0000-0002`: Always declined (for testing error handling)

**For ACH:**
- Routing Number: 9-digit bank routing number
- Account Number: Your bank account number
- Account Type: Checking or Savings

#### Step 4: Review and Submit

1. Review payment summary
2. Click "Submit Payment"
3. Wait for confirmation (usually instant)

#### Step 5: Save Your Receipt

1. Receipt displays immediately after successful payment
2. Receipt includes:
   - Transaction ID
   - Payment Amount
   - Date & Time
   - Journal Entry Number
3. Click "Download Receipt" (PDF) or "Email Receipt"

### Viewing Your Account Statement

1. Navigate to "Account" → "Statement"
2. Select date range (default: Current year)
3. View all transactions:
   - Tax Assessments (charges)
   - Payments (credits)
   - Running Balance
   - Current Balance

**Understanding Your Statement:**
- **Debit** (positive): Amount you owe (tax assessment)
- **Credit** (negative): Payment you made
- **Current Balance $0**: Account is paid in full
- **Current Balance > $0**: Amount still owed
- **Current Balance < $0**: Overpayment (eligible for refund)

### Requesting a Refund

**When to Request a Refund:**
- You've overpaid your tax bill
- Your tax return was amended showing lower tax due
- You made a duplicate payment by mistake

#### Step 1: Check Eligibility

1. View your Account Statement
2. Verify you have a negative balance (credit balance)
3. Calculate refund amount available

#### Step 2: Submit Refund Request

1. Navigate to "Refunds" → "Request Refund"
2. Enter refund amount (cannot exceed your credit balance)
3. Select refund method:
   - **ACH (Fastest)**: 3-5 business days
   - **Check**: 7-10 business days
   - **Wire Transfer**: 1-2 business days (may have fees)
4. Provide banking details (for ACH/Wire) or mailing address (for Check)
5. Enter reason for refund request
6. Click "Submit Request"

#### Step 3: Wait for Approval

1. Refund request goes to municipality for approval
2. You'll receive email notification when status changes
3. Typical approval time: 1-3 business days

#### Step 4: Receive Your Refund

Once approved and issued:
- ACH: Funds in your account within 3-5 business days
- Check: Mailed to your address on file
- Wire: 1-2 business days

**Check Refund Status:** Navigate to "Refunds" → "My Refunds"

### Viewing Audit Trail

Every action on your account is logged. To view:

1. Navigate to "Account" → "Audit Trail"
2. See chronological list of all activities:
   - Tax assessments
   - Payments made
   - Refund requests
   - Account modifications
3. Each entry shows: Date, Time, Action, User, Details

---

## For Municipality Finance Officers

### Running Reconciliation

**Purpose:** Verify that all filer accounts match the municipality's accounts receivable.

#### Step 1: Navigate to Reconciliation

1. Log in to admin portal
2. Go to "Finance" → "Reconciliation"

#### Step 2: Run Reconciliation Report

1. Select "As of Date" (typically month-end or quarter-end)
2. Choose scope:
   - **All Filers**: Municipality-wide reconciliation
   - **Individual Filer**: Drill-down for specific account
3. Click "Run Reconciliation"

#### Step 3: Review Results

**Successful Reconciliation:**
- Status: "RECONCILED"
- Variance: $0.00
- Municipality AR = Sum of all Filer Liabilities

**Failed Reconciliation:**
- Status: "VARIANCE DETECTED"
- Shows specific discrepancies
- Lists affected filer accounts

#### Step 4: Investigate Discrepancies

If variance detected:

1. Review "Discrepancy Details" section
2. For each discrepancy:
   - Check filer's account statement
   - Verify journal entries exist
   - Look for missing or unrecorded transactions
3. Use "Drill-Down" feature to see individual filer reconciliation

#### Step 5: Resolve Issues

Common resolutions:
- Record missing tax assessment
- Verify payment was properly journaled
- Check for timing differences (transactions in transit)
- Investigate potential data entry errors

### Generating Trial Balance

**Purpose:** Verify all accounts balance (Total Debits = Total Credits)

#### Step 1: Access Trial Balance

1. Navigate to "Finance" → "Reports" → "Trial Balance"

#### Step 2: Generate Report

1. Select "As of Date"
2. Optional filters:
   - Account Type (Assets, Liabilities, Revenue, Expenses)
   - Account Range
3. Click "Generate Trial Balance"

#### Step 3: Review Report

**Report Shows:**
- All active accounts
- Debit balances
- Credit balances
- Total debits vs. total credits
- Balance status: BALANCED or UNBALANCED

**What to Look For:**
- ✅ **BALANCED**: Total Debits = Total Credits (Good!)
- ❌ **UNBALANCED**: Investigate immediately - indicates data integrity issue

#### Step 4: Drill-Down (if needed)

1. Click on any account to see detail
2. View all journal entries affecting that account
3. Verify entries are correct and complete

### Managing Refunds

#### Approve Refund Request

1. Navigate to "Finance" → "Refunds" → "Pending Requests"
2. Click on refund request to review
3. Verify:
   - Filer has credit balance
   - Refund amount doesn't exceed credit
   - Banking details are correct
4. Add approval notes
5. Click "Approve" or "Deny"

#### Issue Approved Refund

1. Navigate to "Finance" → "Refunds" → "Approved"
2. Select refund(s) to issue
3. Click "Issue Refund"
4. System processes refund and updates ledger
5. Filer receives notification

### Exporting Reports

All reports can be exported:

1. Generate report as usual
2. Click "Export" button
3. Choose format:
   - PDF: For printing/sharing
   - CSV: For Excel analysis
   - Excel: Full formatting
4. Download file

---

## For System Administrators

### Configuring Payment Mode

**Switch between TEST and PRODUCTION mode:**

1. Edit `/home/runner/work/munciplaityTax/munciplaityTax/backend/ledger-service/src/main/resources/application.properties`
2. Set `ledger.payment.mode=TEST` or `ledger.payment.mode=PRODUCTION`
3. Restart service

**WARNING:** Only use PRODUCTION mode with real payment gateway credentials configured.

### Monitoring System Health

#### Health Check Endpoints

```bash
# Overall health
curl http://localhost:8087/actuator/health

# Database health
curl http://localhost:8087/actuator/health/db

# Disk space
curl http://localhost:8087/actuator/health/diskSpace
```

#### Metrics

```bash
# JVM memory usage
curl http://localhost:8087/actuator/metrics/jvm.memory.used

# Database connection pool
curl http://localhost:8087/actuator/metrics/hikaricp.connections

# HTTP request metrics
curl http://localhost:8087/actuator/metrics/http.server.requests
```

### Managing Data Seeding

**Enable/Disable Test Data:**

Test data is loaded from `V2__Seed_test_data.sql` migration.

To disable in production:
1. Set environment variable: `SEED_DATA_ENABLED=false`
2. Or comment out the V2 migration file

To generate additional test data:
```bash
psql -h localhost -U postgres -d munitax_ledger -f src/test/resources/data-generator.sql
```

### Backup and Recovery

#### Database Backup

```bash
# Backup
pg_dump -h localhost -U postgres -d munitax_ledger -F c -f backup_$(date +%Y%m%d).dump

# Restore
pg_restore -h localhost -U postgres -d munitax_ledger -c backup_YYYYMMDD.dump
```

#### Audit Log Retention

Audit logs are retained per configuration:
- Default: 7 years (2555 days)
- Configure: `ledger.audit.retention-days=2555`

---

## Common Tasks

### Task: Find a Specific Payment

1. Note the payment transaction ID or date
2. Navigate to "Transactions" → "Search"
3. Enter search criteria
4. Click on transaction to view details

### Task: Verify Balance for Filer

1. Navigate to "Filers" → "Search"
2. Enter filer name or ID
3. Click "View Account Statement"
4. Current balance shown at top

### Task: Generate Month-End Reports

1. Run Trial Balance for last day of month
2. Run Reconciliation for last day of month
3. Export both reports to PDF
4. Save to monthly reporting folder

### Task: Troubleshoot Failed Payment

1. Go to "Payments" → "Failed Payments"
2. Locate the payment
3. Check error message
4. Common causes:
   - Declined credit card
   - Insufficient funds (ACH)
   - Invalid account information
   - Network timeout
5. Contact filer to retry with correct information

---

## Troubleshooting

### Problem: Payment shows as "Pending"

**Causes:**
- ACH payments take 3-5 days to clear
- Payment gateway processing delay

**Solution:**
- Check payment status in 1 hour
- For ACH, wait 3-5 business days
- If still pending after expected time, contact support

### Problem: Balance doesn't match expected

**Solution:**
1. View full account statement
2. Verify all transactions are included
3. Check date range filter
4. Look for pending transactions
5. Run reconciliation to identify discrepancies

### Problem: Cannot request refund

**Causes:**
- No credit balance available
- Refund already requested
- Account has holds or restrictions

**Solution:**
1. Check current balance (must be negative/credit)
2. Verify no pending refund exists
3. Contact municipality if issue persists

### Problem: Journal entries out of balance

**This should never happen!** If it does:
1. Immediately notify system administrator
2. Do not process more transactions until resolved
3. Run trial balance to identify scope of issue
4. Review audit trail for recent changes
5. May require database fix and investigation

---

## FAQs

**Q: How long does a payment take to process?**  
A: Credit card payments are instant. ACH payments take 3-5 business days. Checks and wires depend on your financial institution.

**Q: Can I make a partial payment?**  
A: Yes! Enter any amount up to your full balance due.

**Q: What if I overpay by accident?**  
A: You can request a refund for the overpayment amount. The credit will stay on your account until you request a refund or it's applied to future tax bills.

**Q: Are test card payments real?**  
A: No! When you see "TEST MODE" indicator, no real charges are made. Test cards are for development and training only.

**Q: How do I know my payment was successful?**  
A: You'll see a confirmation screen immediately with a receipt. You'll also receive an email confirmation. The payment will appear on your account statement.

**Q: Can I view past years' statements?**  
A: Yes! Select the date range when viewing your account statement. Historical data is available for all years.

**Q: What's a journal entry number?**  
A: It's a unique reference number (like "JE-2024-00125") for tracking every financial transaction. It links your payment to the accounting records.

**Q: How secure is my payment information?**  
A: Very secure! The system uses industry-standard encryption. Payment card data is tokenized and never stored in plain text.

**Q: Who can see my account information?**  
A: Only you and authorized municipality finance officers can access your account. All access is logged in the audit trail.

**Q: What if I need help?**  
A: Contact support@munitax.com or call the municipality finance office during business hours.

---

## Getting More Help

- **Technical Support**: support@munitax.com
- **API Documentation**: http://localhost:8087/swagger-ui.html
- **System Status**: http://status.munitax.com
- **Training Videos**: http://help.munitax.com/videos

---

*Last Updated: November 2024*  
*Version: 1.0.0*
