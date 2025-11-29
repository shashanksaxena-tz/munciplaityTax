-- Flyway Migration V1.27: Add validation constraints for withholding tables
-- Feature: Withholding Reconciliation System
-- Purpose: Data integrity and business rule enforcement
-- Note: Most constraints are already created in table creation migrations (V1.21, V1.22)
-- This migration adds only the check_resolution_notes constraint which was missing

-- If reconciled with variance, must have resolution notes
-- This constraint enforces FR-008: Businesses must explain discrepancies when reconciling
ALTER TABLE dublin.withholding_reconciliations 
    ADD CONSTRAINT IF NOT EXISTS check_resolution_notes 
    CHECK (
        status != 'RECONCILED' OR 
        variance_wages = 0 OR 
        resolution_notes IS NOT NULL
    );

-- Comments for documentation
COMMENT ON CONSTRAINT check_resolution_notes ON dublin.withholding_reconciliations 
    IS 'Enforces explanation requirement per FR-008: RECONCILED status with variance requires resolution_notes';
