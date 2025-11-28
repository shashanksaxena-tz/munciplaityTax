-- Flyway Migration V1.30: Create nols table
-- Feature: Net Operating Loss (NOL) Carryforward & Carryback Tracking System
-- Purpose: Store net operating loss records with multi-year tracking

-- Create nols table
CREATE TABLE IF NOT EXISTS dublin.nols (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    business_id UUID NOT NULL,
    tax_year INTEGER NOT NULL CHECK (tax_year >= 2000 AND tax_year <= 2100),
    jurisdiction VARCHAR(20) NOT NULL CHECK (jurisdiction IN ('FEDERAL', 'STATE_OHIO', 'MUNICIPALITY')),
    municipality_code VARCHAR(10),
    entity_type VARCHAR(20) NOT NULL CHECK (entity_type IN ('C_CORP', 'S_CORP', 'PARTNERSHIP', 'SOLE_PROP')),
    original_nol_amount DECIMAL(15,2) NOT NULL CHECK (original_nol_amount >= 0),
    current_nol_balance DECIMAL(15,2) NOT NULL CHECK (current_nol_balance >= 0 AND current_nol_balance <= original_nol_amount),
    used_amount DECIMAL(15,2) NOT NULL DEFAULT 0 CHECK (used_amount >= 0),
    expired_amount DECIMAL(15,2) NOT NULL DEFAULT 0 CHECK (expired_amount >= 0),
    expiration_date DATE,
    carryforward_years INTEGER CHECK (carryforward_years > 0),
    is_carried_back BOOLEAN NOT NULL DEFAULT FALSE,
    carryback_amount DECIMAL(15,2) NOT NULL DEFAULT 0 CHECK (carryback_amount >= 0),
    carryback_refund DECIMAL(15,2) NOT NULL DEFAULT 0 CHECK (carryback_refund >= 0),
    apportionment_percentage DECIMAL(5,2) CHECK (apportionment_percentage >= 0 AND apportionment_percentage <= 100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    -- Business rules
    CONSTRAINT check_nol_balance CHECK (
        original_nol_amount = current_nol_balance + used_amount + expired_amount + carryback_amount
    ),
    CONSTRAINT check_carryback CHECK (
        (is_carried_back = FALSE AND carryback_amount = 0 AND carryback_refund = 0) OR
        (is_carried_back = TRUE AND carryback_amount > 0)
    ),
    CONSTRAINT check_apportionment CHECK (
        (jurisdiction IN ('STATE_OHIO', 'MUNICIPALITY') AND apportionment_percentage IS NOT NULL) OR
        (jurisdiction = 'FEDERAL' AND apportionment_percentage IS NULL)
    ),
    CONSTRAINT check_municipality_code CHECK (
        (jurisdiction = 'MUNICIPALITY' AND municipality_code IS NOT NULL) OR
        (jurisdiction != 'MUNICIPALITY' AND municipality_code IS NULL)
    )
);

-- Create indexes for performance
CREATE INDEX idx_nol_business_year ON dublin.nols(business_id, tax_year);
CREATE INDEX idx_nol_jurisdiction ON dublin.nols(jurisdiction, municipality_code);
CREATE INDEX idx_nol_expiration ON dublin.nols(expiration_date) WHERE expiration_date IS NOT NULL;
CREATE INDEX idx_nol_tenant_business ON dublin.nols(tenant_id, business_id);
CREATE INDEX idx_nol_balance ON dublin.nols(current_nol_balance) WHERE current_nol_balance > 0;

-- Add comments
COMMENT ON TABLE dublin.nols IS 'Net Operating Loss tracking with multi-year carryforward and carryback support';
COMMENT ON COLUMN dublin.nols.expiration_date IS 'Pre-2018 NOLs expire after 20 years, post-2017 NOLs are indefinite (NULL)';
COMMENT ON COLUMN dublin.nols.is_carried_back IS 'CARES Act provision: 2018-2020 losses can be carried back 5 years';
COMMENT ON COLUMN dublin.nols.apportionment_percentage IS 'Ohio apportionment % for multi-state businesses (state NOL = federal NOL × %)';
