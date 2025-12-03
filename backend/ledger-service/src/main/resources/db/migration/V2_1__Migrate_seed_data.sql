-- V2.1: Migrate seed data from compatibility tables to real tables
-- This migration moves the data inserted by V2 into the temporary compatibility tables
-- to the actual tables created in V1, performing necessary transformations.

-- 1. Migrate Journal Entries
INSERT INTO journal_entries (
    entry_id,
    entry_number,
    entry_date,
    description,
    source_type,
    source_id,
    status,
    tenant_id,
    entity_id,
    created_by,
    created_at
)
SELECT
    entry_id,
    entry_number,
    entry_date,
    description,
    source_type,
    -- Cast source_id to UUID if it looks like one, otherwise NULL
    CASE 
        WHEN source_id ~ '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$' 
        THEN source_id::uuid 
        ELSE NULL 
    END,
    entry_status,
    tenant_id,
    entity_id,
    -- Use a system UUID for 'SEED_DATA' or other string values
    '00000000-0000-0000-0000-000000000000'::uuid,
    created_at
FROM journal_entry
ON CONFLICT (entry_id) DO NOTHING;

-- 2. Migrate Journal Entry Lines
INSERT INTO journal_entry_lines (
    line_id,
    entry_id,
    account_id,
    line_number,
    debit,
    credit,
    description
)
SELECT
    line_id,
    entry_id,
    -- Look up account_id by number from chart_of_accounts to match V1 IDs
    -- If not found, use the provided account_id (which might fail FK if not exists, but we assume V1 covers standard accounts)
    COALESCE(
        (SELECT account_id FROM chart_of_accounts WHERE account_number = jel.account_number LIMIT 1),
        jel.account_id
    ),
    -- Generate line number since V2 doesn't provide it
    ROW_NUMBER() OVER (PARTITION BY entry_id ORDER BY line_id),
    debit_amount,
    credit_amount,
    description
FROM journal_entry_line jel
ON CONFLICT (line_id) DO NOTHING;

-- 3. Migrate Payment Transactions
INSERT INTO payment_transactions (
    transaction_id,
    payment_id,
    filer_id,
    tenant_id,
    provider_transaction_id,
    status,
    payment_method,
    amount,
    timestamp,
    is_test_mode,
    journal_entry_id
)
SELECT
    payment_id, -- Use payment_id as transaction_id
    payment_id, -- Also use it as payment_id
    filer_id,
    tenant_id,
    provider_transaction_id,
    payment_status,
    payment_method,
    amount,
    COALESCE(payment_date, created_at),
    COALESCE(test_mode, false),
    -- Check if journal entry exists, otherwise NULL to avoid FK violation
    (SELECT entry_id FROM journal_entries WHERE entry_id = pt.journal_entry_id)
FROM payment_transaction pt
ON CONFLICT (transaction_id) DO NOTHING;

-- 4. Migrate Audit Logs
INSERT INTO audit_logs (
    log_id,
    entity_id,
    entity_type,
    action,
    user_id,
    timestamp,
    details,
    tenant_id
)
SELECT
    log_id,
    -- Cast entity_id to UUID if possible
    CASE 
        WHEN entity_id ~ '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$' 
        THEN entity_id::uuid 
        ELSE NULL 
    END,
    entity_type,
    action,
    -- Use system UUID for 'SEED_DATA'
    '00000000-0000-0000-0000-000000000000'::uuid,
    timestamp,
    details,
    tenant_id
FROM audit_log
ON CONFLICT (log_id) DO NOTHING;

-- 5. Drop compatibility tables
DROP TABLE journal_entry;
DROP TABLE journal_entry_line;
DROP TABLE payment_transaction;
DROP TABLE audit_log;
