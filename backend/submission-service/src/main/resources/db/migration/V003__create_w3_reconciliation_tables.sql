-- W-3 Year-End Reconciliation Schema
-- This migration adds tables for tracking annual W-3 reconciliation between W-1 filings and W-2 forms

CREATE SCHEMA IF NOT EXISTS dublin;

-- Create w3_reconciliations table
CREATE TABLE IF NOT EXISTS dublin.w3_reconciliations (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    business_id VARCHAR(255) NOT NULL,
    tax_year INTEGER NOT NULL,
    total_w1_tax DECIMAL(15, 2) NOT NULL,
    total_w2_tax DECIMAL(15, 2) NOT NULL,
    discrepancy DECIMAL(15, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    w1_filing_count INTEGER,
    w2_form_count INTEGER,
    total_employees INTEGER,
    late_filing_penalty DECIMAL(15, 2) DEFAULT 0.00,
    missing_filing_penalty DECIMAL(15, 2) DEFAULT 0.00,
    total_penalties DECIMAL(15, 2) DEFAULT 0.00,
    due_date DATE NOT NULL,
    filing_date TIMESTAMP,
    is_submitted BOOLEAN NOT NULL DEFAULT false,
    confirmation_number VARCHAR(50),
    notes TEXT,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    
    -- Note: Tax year constraint set to 2020 as minimum supported year
    -- If older years need support, this constraint should be updated
    CONSTRAINT chk_w3_tax_year CHECK (tax_year >= 2020 AND tax_year <= 2099),
    CONSTRAINT chk_w3_status CHECK (status IN ('BALANCED', 'UNBALANCED')),
    CONSTRAINT chk_w3_total_w1_tax CHECK (total_w1_tax >= 0),
    CONSTRAINT chk_w3_total_w2_tax CHECK (total_w2_tax >= 0),
    CONSTRAINT chk_w3_penalties CHECK (total_penalties >= 0),
    CONSTRAINT uq_w3_business_year UNIQUE (business_id, tax_year, tenant_id)
);

-- Create junction table for W-3 to W-1 filings relationship
CREATE TABLE IF NOT EXISTS dublin.w3_w1_filings (
    w3_reconciliation_id UUID NOT NULL,
    w1_filing_id VARCHAR(255) NOT NULL,
    
    PRIMARY KEY (w3_reconciliation_id, w1_filing_id),
    CONSTRAINT fk_w3_reconciliation FOREIGN KEY (w3_reconciliation_id) 
        REFERENCES dublin.w3_reconciliations(id) ON DELETE CASCADE
);

-- Create indexes for efficient querying
CREATE INDEX IF NOT EXISTS idx_w3_tenant ON dublin.w3_reconciliations(tenant_id);
CREATE INDEX IF NOT EXISTS idx_w3_business ON dublin.w3_reconciliations(business_id);
CREATE INDEX IF NOT EXISTS idx_w3_tax_year ON dublin.w3_reconciliations(tax_year);
CREATE INDEX IF NOT EXISTS idx_w3_status ON dublin.w3_reconciliations(status);
CREATE INDEX IF NOT EXISTS idx_w3_filing_date ON dublin.w3_reconciliations(filing_date);
CREATE INDEX IF NOT EXISTS idx_w3_w1_w1_filing ON dublin.w3_w1_filings(w1_filing_id);

-- Add comments for documentation
COMMENT ON TABLE dublin.w3_reconciliations IS 'Annual W-3 reconciliation between W-1 filings and W-2 forms';
COMMENT ON COLUMN dublin.w3_reconciliations.total_w1_tax IS 'Sum of all W-1 tax amounts for the year';
COMMENT ON COLUMN dublin.w3_reconciliations.total_w2_tax IS 'Sum of Box 19 from all employee W-2 forms';
COMMENT ON COLUMN dublin.w3_reconciliations.discrepancy IS 'Difference between W-1 and W-2 totals (W1 - W2)';
COMMENT ON COLUMN dublin.w3_reconciliations.status IS 'BALANCED if discrepancy < $1.00, otherwise UNBALANCED';
COMMENT ON TABLE dublin.w3_w1_filings IS 'Links W-3 reconciliation to individual W-1 filings included';
