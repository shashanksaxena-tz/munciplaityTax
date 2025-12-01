-- Flyway Migration V9: Create nol_carrybacks table
-- Feature: Net Operating Loss (NOL) Carryforward & Carryback Tracking System
-- Purpose: Track NOL carryback elections (CARES Act provision)

CREATE TABLE IF NOT EXISTS nol_carrybacks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    nol_id UUID NOT NULL,
    carryback_year INTEGER NOT NULL CHECK (carryback_year >= 2000 AND carryback_year <= 2100),
    prior_year_taxable_income DECIMAL(15,2) NOT NULL CHECK (prior_year_taxable_income >= 0),
    nol_applied DECIMAL(15,2) NOT NULL CHECK (nol_applied > 0 AND nol_applied <= prior_year_taxable_income),
    prior_year_tax_rate DECIMAL(5,4) NOT NULL CHECK (prior_year_tax_rate > 0),
    refund_amount DECIMAL(15,2) NOT NULL CHECK (refund_amount >= 0),
    prior_year_return_id UUID NOT NULL,
    carryback_form_id UUID,
    refund_status VARCHAR(20) NOT NULL DEFAULT 'CLAIMED' CHECK (refund_status IN ('CLAIMED', 'APPROVED', 'DENIED', 'PAID')),
    refund_date DATE,
    filed_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    -- Foreign key constraints
    CONSTRAINT fk_nol_carryback_nol FOREIGN KEY (nol_id) REFERENCES nols(id) ON DELETE CASCADE,
    
    -- Business rules
    CONSTRAINT check_refund_date CHECK (
        (refund_status = 'PAID' AND refund_date IS NOT NULL) OR
        (refund_status != 'PAID' AND refund_date IS NULL)
    )
);

-- Create indexes for performance
CREATE INDEX idx_nol_carryback_nol ON nol_carrybacks(nol_id);
CREATE INDEX idx_nol_carryback_year ON nol_carrybacks(carryback_year);
CREATE INDEX idx_nol_carryback_status ON nol_carrybacks(refund_status);
CREATE INDEX idx_nol_carryback_tenant ON nol_carrybacks(tenant_id);

-- Add comments
COMMENT ON TABLE nol_carrybacks IS 'CARES Act carryback tracking: 2018-2020 losses can be carried back 5 years';
COMMENT ON COLUMN nol_carrybacks.refund_amount IS 'Refund = nol_applied × prior_year_tax_rate';
COMMENT ON COLUMN nol_carrybacks.refund_status IS 'CLAIMED → APPROVED → PAID or DENIED';
