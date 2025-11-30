-- data-generator.sql
-- T091: Script to generate realistic test data (100 filers, 1000 transactions)
-- WARNING: This generates a large amount of test data. Use only in test environments.
--
-- Usage:
--   psql -h localhost -U postgres -d munitax_ledger -f data-generator.sql
--
-- Or call from Java tests using Spring's @Sql annotation

-- ============================================
-- Configuration
-- ============================================
-- Number of filers to generate
DO $$
DECLARE
    num_filers INTEGER := 100;
    num_transactions_per_filer INTEGER := 10;
    base_tenant_id UUID := 'b0000000-0000-0000-0000-000000000001'::uuid;
    base_date DATE := '2024-01-01'::date;
    filer_id UUID;
    entry_id UUID;
    payment_id UUID;
    line_id_1 UUID;
    line_id_2 UUID;
    tax_amount DECIMAL(15,2);
    payment_amount DECIMAL(15,2);
    entry_number TEXT;
    i INTEGER;
    j INTEGER;
    current_entry_num INTEGER := 10000;
BEGIN
    RAISE NOTICE 'Starting data generation: % filers, % transactions per filer', num_filers, num_transactions_per_filer;
    
    -- Loop through filers
    FOR i IN 1..num_filers LOOP
        filer_id := gen_random_uuid();
        
        RAISE NOTICE 'Creating filer % of %', i, num_filers;
        
        -- Loop through transactions for this filer
        FOR j IN 1..num_transactions_per_filer LOOP
            current_entry_num := current_entry_num + 1;
            entry_id := gen_random_uuid();
            payment_id := gen_random_uuid();
            line_id_1 := gen_random_uuid();
            line_id_2 := gen_random_uuid();
            entry_number := 'JE-2024-' || LPAD(current_entry_num::TEXT, 5, '0');
            
            -- Random tax amount between $1,000 and $50,000
            tax_amount := (1000 + (random() * 49000))::DECIMAL(15,2);
            
            -- Payment amount: 90% of the time full payment, 10% partial or over
            IF random() < 0.9 THEN
                payment_amount := tax_amount; -- Full payment
            ELSIF random() < 0.5 THEN
                payment_amount := (tax_amount * (0.3 + random() * 0.6))::DECIMAL(15,2); -- Partial (30-90%)
            ELSE
                payment_amount := (tax_amount * (1.0 + random() * 0.2))::DECIMAL(15,2); -- Overpayment (100-120%)
            END IF;
            
            -- Create tax assessment journal entry
            INSERT INTO journal_entry (
                entry_id, tenant_id, entity_id, entry_number, entry_date, 
                description, source_type, source_id, entry_status, 
                total_debits, total_credits, created_at, created_by
            ) VALUES (
                entry_id,
                base_tenant_id,
                filer_id,
                entry_number,
                base_date + ((i * num_transactions_per_filer + j - 1) * 3), -- Spread over time
                'Generated: Q' || ((j % 4) + 1)::TEXT || ' 2024 Tax Assessment - Filer ' || i::TEXT,
                'TAX_ASSESSMENT',
                'TR-2024-GEN-' || i::TEXT || '-' || j::TEXT,
                'POSTED',
                tax_amount,
                tax_amount,
                CURRENT_TIMESTAMP,
                'DATA_GENERATOR'
            );
            
            -- Create journal entry lines
            INSERT INTO journal_entry_line (
                line_id, entry_id, account_id, account_number, account_name,
                debit_amount, credit_amount, description
            ) VALUES 
            (
                line_id_1,
                entry_id,
                gen_random_uuid(),
                '6100',
                'Tax Expense',
                tax_amount,
                0.00,
                'Tax expense for generated entry'
            ),
            (
                line_id_2,
                entry_id,
                gen_random_uuid(),
                '2100',
                'Tax Liability',
                0.00,
                tax_amount,
                'Tax liability for generated entry'
            );
            
            -- Create payment transaction (if making payment)
            IF payment_amount > 0 THEN
                INSERT INTO payment_transaction (
                    payment_id, tenant_id, filer_id, amount, payment_method,
                    payment_status, provider_transaction_id, journal_entry_id,
                    payment_date, description, test_mode, created_at
                ) VALUES (
                    payment_id,
                    base_tenant_id,
                    filer_id,
                    payment_amount,
                    CASE WHEN random() < 0.7 THEN 'CREDIT_CARD' ELSE 'ACH' END,
                    'APPROVED',
                    'mock_gen_' || payment_id::TEXT,
                    gen_random_uuid(), -- Payment entry ID (would be created by service)
                    base_date + ((i * num_transactions_per_filer + j - 1) * 3) + 5, -- 5 days after assessment
                    'Generated: Payment for Filer ' || i::TEXT || ' Transaction ' || j::TEXT,
                    true,
                    CURRENT_TIMESTAMP
                );
                
                -- Create payment journal entry
                current_entry_num := current_entry_num + 1;
                entry_number := 'JE-2024-' || LPAD(current_entry_num::TEXT, 5, '0');
                
                INSERT INTO journal_entry (
                    entry_id, tenant_id, entity_id, entry_number, entry_date, 
                    description, source_type, source_id, entry_status, 
                    total_debits, total_credits, created_at, created_by
                ) VALUES (
                    gen_random_uuid(),
                    base_tenant_id,
                    filer_id,
                    entry_number,
                    base_date + ((i * num_transactions_per_filer + j - 1) * 3) + 5,
                    'Generated: Payment - Filer ' || i::TEXT,
                    'PAYMENT',
                    payment_id::TEXT,
                    'POSTED',
                    payment_amount,
                    payment_amount,
                    CURRENT_TIMESTAMP,
                    'DATA_GENERATOR'
                );
            END IF;
            
            -- Log progress every 100 transactions
            IF (i * num_transactions_per_filer + j) % 100 = 0 THEN
                RAISE NOTICE 'Processed % transactions', (i * num_transactions_per_filer + j);
            END IF;
        END LOOP;
    END LOOP;
    
    RAISE NOTICE 'Data generation complete!';
    RAISE NOTICE 'Total filers: %', num_filers;
    RAISE NOTICE 'Total transactions: %', num_filers * num_transactions_per_filer;
    RAISE NOTICE 'Total journal entries: %', current_entry_num - 10000;
END $$;

-- ============================================
-- Verify Generated Data
-- ============================================
SELECT 
    'Journal Entries' as entity,
    COUNT(*) as count
FROM journal_entry
WHERE created_by = 'DATA_GENERATOR'
UNION ALL
SELECT 
    'Payment Transactions' as entity,
    COUNT(*) as count
FROM payment_transaction
WHERE description LIKE 'Generated:%'
UNION ALL
SELECT 
    'Unique Filers' as entity,
    COUNT(DISTINCT entity_id) as count
FROM journal_entry
WHERE created_by = 'DATA_GENERATOR';

-- ============================================
-- Sample Queries to Test the Data
-- ============================================

-- Find filers with outstanding balances
/*
SELECT 
    entity_id as filer_id,
    SUM(CASE WHEN source_type = 'TAX_ASSESSMENT' THEN total_debits ELSE -total_credits END) as balance
FROM journal_entry
WHERE created_by = 'DATA_GENERATOR'
GROUP BY entity_id
HAVING SUM(CASE WHEN source_type = 'TAX_ASSESSMENT' THEN total_debits ELSE -total_credits END) > 0
ORDER BY balance DESC
LIMIT 10;
*/

-- Find overpayments (credit balances)
/*
SELECT 
    entity_id as filer_id,
    SUM(CASE WHEN source_type = 'TAX_ASSESSMENT' THEN total_debits ELSE -total_credits END) as balance
FROM journal_entry
WHERE created_by = 'DATA_GENERATOR'
GROUP BY entity_id
HAVING SUM(CASE WHEN source_type = 'TAX_ASSESSMENT' THEN total_debits ELSE -total_credits END) < 0
ORDER BY balance ASC
LIMIT 10;
*/

-- Transaction volume by date
/*
SELECT 
    entry_date,
    COUNT(*) as transaction_count,
    SUM(total_debits) as total_amount
FROM journal_entry
WHERE created_by = 'DATA_GENERATOR'
GROUP BY entry_date
ORDER BY entry_date;
*/

-- ============================================
-- Cleanup (if needed)
-- ============================================
/*
-- WARNING: This will delete ALL generated test data
-- Uncomment to run:

DELETE FROM journal_entry_line 
WHERE entry_id IN (
    SELECT entry_id FROM journal_entry WHERE created_by = 'DATA_GENERATOR'
);

DELETE FROM journal_entry WHERE created_by = 'DATA_GENERATOR';
DELETE FROM payment_transaction WHERE description LIKE 'Generated:%';
DELETE FROM audit_log WHERE user_id = 'DATA_GENERATOR';

RAISE NOTICE 'Generated test data deleted';
*/
