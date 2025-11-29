-- Flyway Migration V1.27: Add validation constraints for withholding tables
-- Feature: Withholding Reconciliation System
-- Purpose: Data integrity and business rule enforcement

-- Add constraint for cumulative_withholding_totals
ALTER TABLE dublin.cumulative_withholding_totals 
    ADD CONSTRAINT IF NOT EXISTS check_non_negative_wages 
    CHECK (cumulative_wages_ytd >= 0);

ALTER TABLE dublin.cumulative_withholding_totals 
    ADD CONSTRAINT IF NOT EXISTS check_non_negative_tax 
    CHECK (cumulative_tax_ytd >= 0);

ALTER TABLE dublin.cumulative_withholding_totals 
    ADD CONSTRAINT IF NOT EXISTS check_periods_filed 
    CHECK (periods_filed >= 0);

-- Add constraint for withholding_reconciliations
-- Variance must equal w1 - w2 (within 1 cent for rounding)
ALTER TABLE dublin.withholding_reconciliations 
    ADD CONSTRAINT IF NOT EXISTS check_variance_wages 
    CHECK (ABS(variance_wages - (w1_total_wages - w2_total_wages)) < 0.01);

ALTER TABLE dublin.withholding_reconciliations 
    ADD CONSTRAINT IF NOT EXISTS check_variance_tax 
    CHECK (ABS(variance_tax - (w1_total_tax - w2_total_tax)) < 0.01);

-- If reconciled with variance, must have resolution notes
ALTER TABLE dublin.withholding_reconciliations 
    ADD CONSTRAINT IF NOT EXISTS check_resolution_notes 
    CHECK (
        status != 'RECONCILED' OR 
        variance_wages = 0 OR 
        resolution_notes IS NOT NULL
    );

-- Comments for documentation
COMMENT ON CONSTRAINT check_variance_wages ON dublin.withholding_reconciliations 
    IS 'Ensures variance calculation integrity per FR-006';
COMMENT ON CONSTRAINT check_resolution_notes ON dublin.withholding_reconciliations 
    IS 'Enforces explanation requirement per FR-008';
