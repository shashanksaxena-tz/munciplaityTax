-- Flyway Migration V1.22: Create withholding_reconciliations table
-- Feature: Withholding Reconciliation System
-- Purpose: Year-end reconciliation comparing W-1 totals to W-2/W-3 totals

CREATE TABLE IF NOT EXISTS dublin.withholding_reconciliations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    business_id UUID NOT NULL,
    tax_year INTEGER NOT NULL,
    w1_total_wages DECIMAL(15,2) NOT NULL,
    w2_total_wages DECIMAL(15,2) NOT NULL,
    w1_total_tax DECIMAL(15,2) NOT NULL,
    w2_total_tax DECIMAL(15,2) NOT NULL,
    variance_wages DECIMAL(15,2) NOT NULL,
    variance_tax DECIMAL(15,2) NOT NULL,
    variance_percentage DECIMAL(5,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED' CHECK (status IN ('NOT_STARTED', 'IN_PROGRESS', 'DISCREPANCY', 'RECONCILED')),
    reconciliation_date TIMESTAMP,
    resolution_notes TEXT,
    w2_count INTEGER NOT NULL DEFAULT 0,
    w2_employee_count INTEGER NOT NULL DEFAULT 0,
    ignored_w2_count INTEGER DEFAULT 0,
    approved_by UUID,
    approved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    -- Variance must equal w1 - w2 within tolerance
    CONSTRAINT check_variance_wages CHECK (ABS(variance_wages - (w1_total_wages - w2_total_wages)) < 0.01),
    CONSTRAINT check_variance_tax CHECK (ABS(variance_tax - (w1_total_tax - w2_total_tax)) < 0.01)
);

-- Unique constraint: One reconciliation per business + tax year
CREATE UNIQUE INDEX unique_reconciliation_business_year ON dublin.withholding_reconciliations(business_id, tax_year);

-- Indexes
CREATE INDEX idx_reconciliation_tenant_year ON dublin.withholding_reconciliations(tenant_id, tax_year);
CREATE INDEX idx_reconciliation_status ON dublin.withholding_reconciliations(status);
CREATE INDEX idx_reconciliation_discrepancy ON dublin.withholding_reconciliations(status) WHERE status = 'DISCREPANCY';

-- Comments
COMMENT ON TABLE dublin.withholding_reconciliations IS 'Year-end W-1 to W-2/W-3 reconciliation per FR-006, FR-007, FR-009';
COMMENT ON COLUMN dublin.withholding_reconciliations.variance_wages IS 'w1_total_wages - w2_total_wages. Flag if > $100 or > 1% (FR-006)';
COMMENT ON COLUMN dublin.withholding_reconciliations.resolution_notes IS 'Required explanation if RECONCILED with variance (FR-008)';
COMMENT ON COLUMN dublin.withholding_reconciliations.ignored_w2_count IS 'W-2s not matched to business EIN (Constitution IV - AI Transparency)';
