-- Flyway Migration V1.31: Create nol_usages table
-- Feature: Net Operating Loss (NOL) Carryforward & Carryback Tracking System
-- Purpose: Track NOL utilization across tax years

-- Create nol_usages table
CREATE TABLE IF NOT EXISTS dublin.nol_usages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    nol_id UUID NOT NULL,
    return_id UUID NOT NULL,
    usage_year INTEGER NOT NULL CHECK (usage_year >= 2000 AND usage_year <= 2100),
    taxable_income_before_nol DECIMAL(15,2) NOT NULL CHECK (taxable_income_before_nol >= 0),
    nol_limitation_percentage DECIMAL(5,2) NOT NULL CHECK (nol_limitation_percentage > 0 AND nol_limitation_percentage <= 100),
    maximum_nol_deduction DECIMAL(15,2) NOT NULL CHECK (maximum_nol_deduction >= 0),
    actual_nol_deduction DECIMAL(15,2) NOT NULL CHECK (actual_nol_deduction >= 0 AND actual_nol_deduction <= maximum_nol_deduction),
    taxable_income_after_nol DECIMAL(15,2) NOT NULL CHECK (taxable_income_after_nol >= 0),
    tax_savings DECIMAL(15,2) NOT NULL CHECK (tax_savings >= 0),
    ordering_method VARCHAR(20) NOT NULL DEFAULT 'FIFO' CHECK (ordering_method IN ('FIFO', 'MANUAL_OVERRIDE')),
    override_reason TEXT,
    usage_date TIMESTAMP NOT NULL DEFAULT NOW(),
    
    -- Foreign key constraints
    CONSTRAINT fk_nol_usage_nol FOREIGN KEY (nol_id) REFERENCES dublin.nols(id) ON DELETE CASCADE,
    
    -- Business rules
    CONSTRAINT check_ordering_override CHECK (
        (ordering_method = 'FIFO' AND override_reason IS NULL) OR
        (ordering_method = 'MANUAL_OVERRIDE' AND override_reason IS NOT NULL)
    ),
    CONSTRAINT check_income_after_nol CHECK (
        taxable_income_after_nol = taxable_income_before_nol - actual_nol_deduction
    )
);

-- Create indexes for performance
CREATE INDEX idx_nol_usage_nol ON dublin.nol_usages(nol_id);
CREATE INDEX idx_nol_usage_return ON dublin.nol_usages(return_id);
CREATE INDEX idx_nol_usage_year ON dublin.nol_usages(usage_year);
CREATE INDEX idx_nol_usage_tenant ON dublin.nol_usages(tenant_id);

-- Add comments
COMMENT ON TABLE dublin.nol_usages IS 'NOL utilization tracking with FIFO ordering and 80% limitation';
COMMENT ON COLUMN dublin.nol_usages.nol_limitation_percentage IS 'Post-2017: 80%, Pre-2018: 100%';
COMMENT ON COLUMN dublin.nol_usages.ordering_method IS 'FIFO (oldest first) or manual override for tax planning';
