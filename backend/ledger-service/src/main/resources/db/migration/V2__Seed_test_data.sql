-- V2__Seed_test_data.sql
-- Seed demo/testing data for ledger service
-- T090: Create data seeding script with sample filers and transactions

-- ============================================
-- Insert Sample Filers (Test Data)
-- ============================================
-- Note: These are example UUIDs for testing. In production, filers come from the main system.

-- ============================================
-- Insert Chart of Accounts (if not exists)
-- ============================================
-- These accounts should already exist from V1, but we ensure they're available

-- Insert sample filer account configurations
-- (In a real system, these would be created dynamically per filer)

-- ============================================
-- Demo Scenario 1: Fully Paid Filer
-- ============================================
-- Business XYZ Corp: Assessed $10,000, Paid $10,000, Balance $0

-- Sample tax assessment journal entry for Demo Filer 1
-- Note: In production, these are created by TaxAssessmentService
-- This seed data is for demonstration purposes only

INSERT INTO journal_entry (
    entry_id, tenant_id, entity_id, entry_number, entry_date, 
    description, source_type, source_id, entry_status, 
    total_debits, total_credits, created_at, created_by
) VALUES (
    'a0000001-0000-0000-0000-000000000001'::uuid,
    'b0000000-0000-0000-0000-000000000001'::uuid, -- Sample tenant ID
    'c0000001-0000-0000-0000-000000000001'::uuid, -- Sample filer ID
    'JE-2024-00001',
    '2024-01-15',
    'DEMO: Q1 2024 Tax Assessment - Business XYZ Corp',
    'TAX_ASSESSMENT',
    'TR-2024-Q1-DEMO-001',
    'POSTED',
    10000.00,
    10000.00,
    CURRENT_TIMESTAMP,
    'SEED_DATA'
) ON CONFLICT (entry_id) DO NOTHING;

-- Journal entry lines for tax assessment
INSERT INTO journal_entry_line (
    line_id, entry_id, account_id, account_number, account_name,
    debit_amount, credit_amount, description
) VALUES 
(
    'aa000001-0000-0000-0000-000000000001'::uuid,
    'a0000001-0000-0000-0000-000000000001'::uuid,
    'd0000001-0000-0000-0000-000000000001'::uuid,
    '6100',
    'Tax Expense',
    10000.00,
    0.00,
    'Q1 2024 Municipal Tax Expense'
),
(
    'aa000002-0000-0000-0000-000000000001'::uuid,
    'a0000001-0000-0000-0000-000000000001'::uuid,
    'd0000002-0000-0000-0000-000000000001'::uuid,
    '2100',
    'Tax Liability',
    0.00,
    10000.00,
    'Q1 2024 Municipal Tax Liability'
) ON CONFLICT (line_id) DO NOTHING;

-- Payment transaction for Demo Filer 1
INSERT INTO payment_transaction (
    payment_id, tenant_id, filer_id, amount, payment_method,
    payment_status, provider_transaction_id, journal_entry_id,
    payment_date, description, test_mode, created_at
) VALUES (
    'e0000001-0000-0000-0000-000000000001'::uuid,
    'b0000000-0000-0000-0000-000000000001'::uuid,
    'c0000001-0000-0000-0000-000000000001'::uuid,
    10000.00,
    'CREDIT_CARD',
    'APPROVED',
    'mock_ch_demo_001',
    'a0000002-0000-0000-0000-000000000001'::uuid,
    '2024-01-20',
    'DEMO: Payment for Q1 2024 Tax',
    true,
    CURRENT_TIMESTAMP
) ON CONFLICT (payment_id) DO NOTHING;

-- Payment journal entry
INSERT INTO journal_entry (
    entry_id, tenant_id, entity_id, entry_number, entry_date, 
    description, source_type, source_id, entry_status, 
    total_debits, total_credits, created_at, created_by
) VALUES (
    'a0000002-0000-0000-0000-000000000001'::uuid,
    'b0000000-0000-0000-0000-000000000001'::uuid,
    'c0000001-0000-0000-0000-000000000001'::uuid,
    'JE-2024-00002',
    '2024-01-20',
    'DEMO: Payment - Business XYZ Corp Q1 2024',
    'PAYMENT',
    'e0000001-0000-0000-0000-000000000001'::text,
    'POSTED',
    10000.00,
    10000.00,
    CURRENT_TIMESTAMP,
    'SEED_DATA'
) ON CONFLICT (entry_id) DO NOTHING;

INSERT INTO journal_entry_line (
    line_id, entry_id, account_id, account_number, account_name,
    debit_amount, credit_amount, description
) VALUES 
(
    'aa000003-0000-0000-0000-000000000001'::uuid,
    'a0000002-0000-0000-0000-000000000001'::uuid,
    'd0000002-0000-0000-0000-000000000001'::uuid,
    '2100',
    'Tax Liability',
    10000.00,
    0.00,
    'Payment applied to tax liability'
),
(
    'aa000004-0000-0000-0000-000000000001'::uuid,
    'a0000002-0000-0000-0000-000000000001'::uuid,
    'd0000003-0000-0000-0000-000000000001'::uuid,
    '1000',
    'Cash',
    0.00,
    10000.00,
    'Cash received from payment'
) ON CONFLICT (line_id) DO NOTHING;

-- ============================================
-- Demo Scenario 2: Partially Paid Filer
-- ============================================
-- Business ABC Inc: Assessed $15,000, Paid $8,000, Balance $7,000

INSERT INTO journal_entry (
    entry_id, tenant_id, entity_id, entry_number, entry_date, 
    description, source_type, source_id, entry_status, 
    total_debits, total_credits, created_at, created_by
) VALUES (
    'a0000003-0000-0000-0000-000000000001'::uuid,
    'b0000000-0000-0000-0000-000000000001'::uuid,
    'c0000002-0000-0000-0000-000000000001'::uuid, -- Different filer
    'JE-2024-00003',
    '2024-02-15',
    'DEMO: Q1 2024 Tax Assessment - Business ABC Inc',
    'TAX_ASSESSMENT',
    'TR-2024-Q1-DEMO-002',
    'POSTED',
    15000.00,
    15000.00,
    CURRENT_TIMESTAMP,
    'SEED_DATA'
) ON CONFLICT (entry_id) DO NOTHING;

INSERT INTO journal_entry_line (
    line_id, entry_id, account_id, account_number, account_name,
    debit_amount, credit_amount, description
) VALUES 
(
    'aa000005-0000-0000-0000-000000000001'::uuid,
    'a0000003-0000-0000-0000-000000000001'::uuid,
    'd0000001-0000-0000-0000-000000000001'::uuid,
    '6100',
    'Tax Expense',
    15000.00,
    0.00,
    'Q1 2024 Municipal Tax Expense'
),
(
    'aa000006-0000-0000-0000-000000000001'::uuid,
    'a0000003-0000-0000-0000-000000000001'::uuid,
    'd0000002-0000-0000-0000-000000000001'::uuid,
    '2100',
    'Tax Liability',
    0.00,
    15000.00,
    'Q1 2024 Municipal Tax Liability'
) ON CONFLICT (line_id) DO NOTHING;

-- Partial payment
INSERT INTO payment_transaction (
    payment_id, tenant_id, filer_id, amount, payment_method,
    payment_status, provider_transaction_id, journal_entry_id,
    payment_date, description, test_mode, created_at
) VALUES (
    'e0000002-0000-0000-0000-000000000001'::uuid,
    'b0000000-0000-0000-0000-000000000001'::uuid,
    'c0000002-0000-0000-0000-000000000001'::uuid,
    8000.00,
    'ACH',
    'APPROVED',
    'mock_ach_demo_001',
    'a0000004-0000-0000-0000-000000000001'::uuid,
    '2024-02-25',
    'DEMO: Partial payment for Q1 2024 Tax',
    true,
    CURRENT_TIMESTAMP
) ON CONFLICT (payment_id) DO NOTHING;

-- ============================================
-- Demo Scenario 3: Overpayment (Refund Scenario)
-- ============================================
-- Business DEF LLC: Assessed $5,000, Paid $6,000, Refund $1,000

INSERT INTO journal_entry (
    entry_id, tenant_id, entity_id, entry_number, entry_date, 
    description, source_type, source_id, entry_status, 
    total_debits, total_credits, created_at, created_by
) VALUES (
    'a0000005-0000-0000-0000-000000000001'::uuid,
    'b0000000-0000-0000-0000-000000000001'::uuid,
    'c0000003-0000-0000-0000-000000000001'::uuid,
    'JE-2024-00005',
    '2024-03-15',
    'DEMO: Q1 2024 Tax Assessment - Business DEF LLC',
    'TAX_ASSESSMENT',
    'TR-2024-Q1-DEMO-003',
    'POSTED',
    5000.00,
    5000.00,
    CURRENT_TIMESTAMP,
    'SEED_DATA'
) ON CONFLICT (entry_id) DO NOTHING;

-- Overpayment
INSERT INTO payment_transaction (
    payment_id, tenant_id, filer_id, amount, payment_method,
    payment_status, provider_transaction_id, journal_entry_id,
    payment_date, description, test_mode, created_at
) VALUES (
    'e0000003-0000-0000-0000-000000000001'::uuid,
    'b0000000-0000-0000-0000-000000000001'::uuid,
    'c0000003-0000-0000-0000-000000000001'::uuid,
    6000.00,
    'CREDIT_CARD',
    'APPROVED',
    'mock_ch_demo_003',
    'a0000006-0000-0000-0000-000000000001'::uuid,
    '2024-03-20',
    'DEMO: Overpayment for Q1 2024 Tax',
    true,
    CURRENT_TIMESTAMP
) ON CONFLICT (payment_id) DO NOTHING;

-- ============================================
-- Audit Log Entries for Demo Data
-- ============================================
INSERT INTO audit_log (
    log_id, tenant_id, entity_type, entity_id, action,
    user_id, timestamp, details
) VALUES 
(
    'f0000001-0000-0000-0000-000000000001'::uuid,
    'b0000000-0000-0000-0000-000000000001'::uuid,
    'JOURNAL_ENTRY',
    'a0000001-0000-0000-0000-000000000001'::text,
    'CREATE',
    'SEED_DATA',
    '2024-01-15 10:00:00',
    'Demo tax assessment journal entry created'
),
(
    'f0000002-0000-0000-0000-000000000001'::uuid,
    'b0000000-0000-0000-0000-000000000001'::uuid,
    'PAYMENT',
    'e0000001-0000-0000-0000-000000000001'::text,
    'APPROVED',
    'SEED_DATA',
    '2024-01-20 14:30:00',
    'Demo payment approved and processed'
) ON CONFLICT (log_id) DO NOTHING;

-- ============================================
-- Comments
-- ============================================
-- This seed data creates three demo scenarios:
-- 1. Fully paid filer: $10,000 assessed, $10,000 paid, $0 balance
-- 2. Partially paid filer: $15,000 assessed, $8,000 paid, $7,000 outstanding
-- 3. Overpayment filer: $5,000 assessed, $6,000 paid, $1,000 credit balance
--
-- These scenarios are useful for:
-- - Testing account statement generation
-- - Testing reconciliation logic
-- - Testing refund processing
-- - Demonstrating payment workflows
-- - Training users
--
-- Note: This is DEMO DATA ONLY. Remove or disable in production.
-- To disable, set environment variable: SEED_DATA_ENABLED=false
