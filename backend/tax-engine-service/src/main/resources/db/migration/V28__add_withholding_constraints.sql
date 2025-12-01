-- Flyway Migration V28: Add additional constraints for withholding tables
-- Feature: Withholding Reconciliation System
-- Purpose: Data integrity and business rule enforcement

-- If reconciled with variance, must have resolution notes
-- This constraint enforces FR-008: Businesses must explain discrepancies when reconciling
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'check_resolution_notes'
    ) THEN
        ALTER TABLE withholding_reconciliations 
            ADD CONSTRAINT check_resolution_notes 
            CHECK (
                status != 'RECONCILED' OR 
                variance_wages = 0 OR 
                resolution_notes IS NOT NULL
            );
    END IF;
END $$;

-- Comments for documentation
COMMENT ON CONSTRAINT check_resolution_notes ON withholding_reconciliations 
    IS 'Enforces explanation requirement per FR-008: RECONCILED status with variance requires resolution_notes';
