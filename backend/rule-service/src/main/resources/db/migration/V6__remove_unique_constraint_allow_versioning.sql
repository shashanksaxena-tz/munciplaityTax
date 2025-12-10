-- ============================================================================
-- Migration V6: Remove unique constraint to allow rule versioning
-- ============================================================================
-- This migration removes the uk_tax_rules_code_tenant constraint that was
-- preventing multiple versions of the same rule from existing.
-- 
-- Business requirement: Rules should support versioning with different
-- effective dates (e.g., MUNICIPAL_TAX_RATE effective 2024-01-01 to 2025-12-09
-- and another version effective 2025-12-10 onwards).
--
-- The constraint was originally added for idempotent seeding, but it prevents
-- normal rule versioning operations.
-- ============================================================================

-- Drop the unique constraint on (rule_code, tenant_id)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_tax_rules_code_tenant'
    ) THEN
        ALTER TABLE tax_rules DROP CONSTRAINT uk_tax_rules_code_tenant;
        RAISE NOTICE 'Dropped constraint uk_tax_rules_code_tenant to allow rule versioning';
    ELSE
        RAISE NOTICE 'Constraint uk_tax_rules_code_tenant does not exist, skipping drop';
    END IF;
END $$;

-- Add a comment explaining the versioning strategy
COMMENT ON TABLE tax_rules IS 'Tax rules with support for versioning. Multiple versions of the same rule_code can exist with different effective_date ranges. The system will select the appropriate version based on the query date.';

-- Optional: Add a check constraint to ensure effective_date < end_date (if end_date is set)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_tax_rules_date_order'
    ) THEN
        ALTER TABLE tax_rules ADD CONSTRAINT chk_tax_rules_date_order 
            CHECK (end_date IS NULL OR effective_date < end_date);
        RAISE NOTICE 'Added constraint chk_tax_rules_date_order';
    END IF;
EXCEPTION
    WHEN duplicate_object THEN 
        RAISE NOTICE 'Constraint chk_tax_rules_date_order already exists';
END $$;
