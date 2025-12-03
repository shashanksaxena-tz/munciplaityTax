-- V4: Change tenant_id to VARCHAR to match other services and frontend
-- This migration alters tenant_id columns from UUID to VARCHAR(50)

-- 1. Alter chart_of_accounts
ALTER TABLE chart_of_accounts ALTER COLUMN tenant_id TYPE VARCHAR(50);

-- 2. Alter journal_entries
ALTER TABLE journal_entries ALTER COLUMN tenant_id TYPE VARCHAR(50);

-- 3. Alter payment_transactions (if exists)
DO $$ 
BEGIN 
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'payment_transactions') THEN
        ALTER TABLE payment_transactions ALTER COLUMN tenant_id TYPE VARCHAR(50);
    END IF;
END $$;

-- 4. Alter audit_logs (if exists)
DO $$ 
BEGIN 
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'audit_logs') THEN
        ALTER TABLE audit_logs ALTER COLUMN tenant_id TYPE VARCHAR(50);
    END IF;
END $$;

-- 5. Alter audit_log (compatibility table, if exists)
DO $$ 
BEGIN 
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'audit_log') THEN
        ALTER TABLE audit_log ALTER COLUMN tenant_id TYPE VARCHAR(50);
    END IF;
END $$;
