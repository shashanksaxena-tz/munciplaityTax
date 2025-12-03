-- V1.3: Restore V1 tables if missing
-- This migration ensures that the tables defined in V1 exist, handling cases where
-- V1 was marked as applied but the tables are missing from the database.

-- Create chart of accounts table
CREATE TABLE IF NOT EXISTS chart_of_accounts (
    account_id UUID PRIMARY KEY,
    account_number VARCHAR(50) UNIQUE NOT NULL,
    account_name VARCHAR(255) NOT NULL,
    account_type VARCHAR(20) NOT NULL,
    normal_balance VARCHAR(10) NOT NULL,
    parent_account_id UUID,
    tenant_id UUID NOT NULL,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_parent_account FOREIGN KEY (parent_account_id) REFERENCES chart_of_accounts(account_id)
);

-- Create journal entries table
CREATE TABLE IF NOT EXISTS journal_entries (
    entry_id UUID PRIMARY KEY,
    entry_number VARCHAR(50) UNIQUE NOT NULL,
    entry_date DATE NOT NULL,
    description TEXT NOT NULL,
    source_type VARCHAR(20) NOT NULL,
    source_id UUID,
    status VARCHAR(20) NOT NULL,
    tenant_id UUID NOT NULL,
    entity_id UUID,
    created_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    posted_by UUID,
    posted_at TIMESTAMP,
    reversed_by UUID,
    reversed_at TIMESTAMP,
    reversal_entry_id UUID,
    CONSTRAINT fk_reversal_entry FOREIGN KEY (reversal_entry_id) REFERENCES journal_entries(entry_id) ON DELETE SET NULL
);

-- Create journal entry lines table
CREATE TABLE IF NOT EXISTS journal_entry_lines (
    line_id UUID PRIMARY KEY,
    entry_id UUID NOT NULL,
    account_id UUID NOT NULL,
    line_number INTEGER NOT NULL,
    debit DECIMAL(19,2) NOT NULL DEFAULT 0,
    credit DECIMAL(19,2) NOT NULL DEFAULT 0,
    description TEXT,
    CONSTRAINT fk_entry FOREIGN KEY (entry_id) REFERENCES journal_entries(entry_id) ON DELETE CASCADE,
    CONSTRAINT fk_account FOREIGN KEY (account_id) REFERENCES chart_of_accounts(account_id)
);

-- Create payment transactions table
CREATE TABLE IF NOT EXISTS payment_transactions (
    transaction_id UUID PRIMARY KEY,
    payment_id UUID NOT NULL,
    filer_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    provider_transaction_id VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    authorization_code VARCHAR(255),
    failure_reason TEXT,
    timestamp TIMESTAMP NOT NULL,
    is_test_mode BOOLEAN NOT NULL DEFAULT FALSE,
    card_last4 VARCHAR(4),
    card_brand VARCHAR(50),
    ach_last4 VARCHAR(4),
    journal_entry_id UUID,
    CONSTRAINT fk_journal_entry FOREIGN KEY (journal_entry_id) REFERENCES journal_entries(entry_id)
);

-- Create account balances table
CREATE TABLE IF NOT EXISTS account_balances (
    balance_id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    entity_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    period_start_date DATE NOT NULL,
    period_end_date DATE NOT NULL,
    beginning_balance DECIMAL(19,2) NOT NULL,
    total_debits DECIMAL(19,2) NOT NULL,
    total_credits DECIMAL(19,2) NOT NULL,
    ending_balance DECIMAL(19,2) NOT NULL,
    CONSTRAINT fk_balance_account FOREIGN KEY (account_id) REFERENCES chart_of_accounts(account_id),
    CONSTRAINT uk_account_entity_period UNIQUE (account_id, entity_id, period_start_date, period_end_date)
);

-- Create audit logs table
CREATE TABLE IF NOT EXISTS audit_logs (
    log_id UUID PRIMARY KEY,
    entity_id UUID NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    user_id UUID NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    details TEXT,
    old_value TEXT,
    new_value TEXT,
    reason TEXT,
    tenant_id UUID NOT NULL
);

-- Create indexes for performance (IF NOT EXISTS is not standard SQL for indexes in all versions, but Postgres supports it)
CREATE INDEX IF NOT EXISTS idx_journal_entries_tenant ON journal_entries(tenant_id);
CREATE INDEX IF NOT EXISTS idx_journal_entries_entity ON journal_entries(entity_id);
CREATE INDEX IF NOT EXISTS idx_journal_entries_date ON journal_entries(entry_date);
CREATE INDEX IF NOT EXISTS idx_journal_entries_status ON journal_entries(status);
CREATE INDEX IF NOT EXISTS idx_journal_entry_lines_entry ON journal_entry_lines(entry_id);
CREATE INDEX IF NOT EXISTS idx_journal_entry_lines_account ON journal_entry_lines(account_id);
CREATE INDEX IF NOT EXISTS idx_payment_transactions_filer ON payment_transactions(filer_id);
CREATE INDEX IF NOT EXISTS idx_payment_transactions_tenant ON payment_transactions(tenant_id);
CREATE INDEX IF NOT EXISTS idx_account_balances_entity ON account_balances(entity_id);
CREATE INDEX IF NOT EXISTS idx_account_balances_account ON account_balances(account_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_entity ON audit_logs(entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_timestamp ON audit_logs(timestamp);

-- Insert default chart of accounts if empty
INSERT INTO chart_of_accounts (account_id, account_number, account_name, account_type, normal_balance, tenant_id, description, active)
SELECT gen_random_uuid(), '1000', 'Cash', 'ASSET', 'DEBIT', '00000000-0000-0000-0000-000000000000', 'Filer cash account', TRUE
WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts WHERE account_number = '1000');

INSERT INTO chart_of_accounts (account_id, account_number, account_name, account_type, normal_balance, tenant_id, description, active)
SELECT gen_random_uuid(), '1200', 'Refund Receivable', 'ASSET', 'DEBIT', '00000000-0000-0000-0000-000000000000', 'Refunds due from municipality', TRUE
WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts WHERE account_number = '1200');

INSERT INTO chart_of_accounts (account_id, account_number, account_name, account_type, normal_balance, tenant_id, description, active)
SELECT gen_random_uuid(), '2100', 'Tax Liability - Current Year', 'LIABILITY', 'CREDIT', '00000000-0000-0000-0000-000000000000', 'Current year tax liability', TRUE
WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts WHERE account_number = '2100');

INSERT INTO chart_of_accounts (account_id, account_number, account_name, account_type, normal_balance, tenant_id, description, active)
SELECT gen_random_uuid(), '2110', 'Tax Liability - Prior Years', 'LIABILITY', 'CREDIT', '00000000-0000-0000-0000-000000000000', 'Prior year tax liabilities', TRUE
WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts WHERE account_number = '2110');

INSERT INTO chart_of_accounts (account_id, account_number, account_name, account_type, normal_balance, tenant_id, description, active)
SELECT gen_random_uuid(), '2120', 'Penalty Liability', 'LIABILITY', 'CREDIT', '00000000-0000-0000-0000-000000000000', 'Penalties assessed', TRUE
WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts WHERE account_number = '2120');

INSERT INTO chart_of_accounts (account_id, account_number, account_name, account_type, normal_balance, tenant_id, description, active)
SELECT gen_random_uuid(), '2130', 'Interest Liability', 'LIABILITY', 'CREDIT', '00000000-0000-0000-0000-000000000000', 'Interest on late payments', TRUE
WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts WHERE account_number = '2130');

INSERT INTO chart_of_accounts (account_id, account_number, account_name, account_type, normal_balance, tenant_id, description, active)
SELECT gen_random_uuid(), '6100', 'Tax Expense', 'EXPENSE', 'DEBIT', '00000000-0000-0000-0000-000000000000', 'Tax expense for income statement', TRUE
WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts WHERE account_number = '6100');

INSERT INTO chart_of_accounts (account_id, account_number, account_name, account_type, normal_balance, tenant_id, description, active)
SELECT gen_random_uuid(), '1001', 'Cash - Municipality', 'ASSET', 'DEBIT', '00000000-0000-0000-0000-000000000000', 'Municipality cash account', TRUE
WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts WHERE account_number = '1001');

INSERT INTO chart_of_accounts (account_id, account_number, account_name, account_type, normal_balance, tenant_id, description, active)
SELECT gen_random_uuid(), '1201', 'Accounts Receivable', 'ASSET', 'DEBIT', '00000000-0000-0000-0000-000000000000', 'Taxes due from filers', TRUE
WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts WHERE account_number = '1201');

INSERT INTO chart_of_accounts (account_id, account_number, account_name, account_type, normal_balance, tenant_id, description, active)
SELECT gen_random_uuid(), '2200', 'Refunds Payable', 'LIABILITY', 'CREDIT', '00000000-0000-0000-0000-000000000000', 'Refunds owed to filers', TRUE
WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts WHERE account_number = '2200');

INSERT INTO chart_of_accounts (account_id, account_number, account_name, account_type, normal_balance, tenant_id, description, active)
SELECT gen_random_uuid(), '4100', 'Tax Revenue', 'REVENUE', 'CREDIT', '00000000-0000-0000-0000-000000000000', 'Tax revenue collected', TRUE
WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts WHERE account_number = '4100');

INSERT INTO chart_of_accounts (account_id, account_number, account_name, account_type, normal_balance, tenant_id, description, active)
SELECT gen_random_uuid(), '4200', 'Penalty Revenue', 'REVENUE', 'CREDIT', '00000000-0000-0000-0000-000000000000', 'Penalty revenue', TRUE
WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts WHERE account_number = '4200');

INSERT INTO chart_of_accounts (account_id, account_number, account_name, account_type, normal_balance, tenant_id, description, active)
SELECT gen_random_uuid(), '4300', 'Interest Revenue', 'REVENUE', 'CREDIT', '00000000-0000-0000-0000-000000000000', 'Interest revenue', TRUE
WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts WHERE account_number = '4300');

INSERT INTO chart_of_accounts (account_id, account_number, account_name, account_type, normal_balance, tenant_id, description, active)
SELECT gen_random_uuid(), '5200', 'Refund Expense', 'EXPENSE', 'DEBIT', '00000000-0000-0000-0000-000000000000', 'Refund expense', TRUE
WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts WHERE account_number = '5200');
