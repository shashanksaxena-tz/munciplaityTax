-- Create account_balances table if it doesn't exist (idempotent migration)
-- This handles cases where V1 migration may have failed or been partially applied

CREATE TABLE IF NOT EXISTS account_balances (
    balance_id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    entity_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    period_start_date DATE NOT NULL,
    period_end_date DATE NOT NULL,
    beginning_balance DECIMAL(19,2) NOT NULL DEFAULT 0,
    total_debits DECIMAL(19,2) NOT NULL DEFAULT 0,
    total_credits DECIMAL(19,2) NOT NULL DEFAULT 0,
    ending_balance DECIMAL(19,2) NOT NULL DEFAULT 0
);

-- Add foreign key constraint if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_balance_account'
    ) THEN
        ALTER TABLE account_balances 
        ADD CONSTRAINT fk_balance_account 
        FOREIGN KEY (account_id) REFERENCES chart_of_accounts(account_id);
    END IF;
END $$;

-- Add unique constraint if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_account_entity_period'
    ) THEN
        ALTER TABLE account_balances 
        ADD CONSTRAINT uk_account_entity_period 
        UNIQUE (account_id, entity_id, period_start_date, period_end_date);
    END IF;
END $$;

-- Create index if it doesn't exist
CREATE INDEX IF NOT EXISTS idx_account_balances_entity ON account_balances(entity_id);
CREATE INDEX IF NOT EXISTS idx_account_balances_account ON account_balances(account_id);
