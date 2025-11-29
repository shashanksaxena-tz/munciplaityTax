-- Flyway Migration V1.35: Create nol_amendments table
-- Feature: Net Operating Loss (NOL) Carryforward & Carryback Tracking System
-- Purpose: Track amended returns that impact NOL calculations

-- Create nol_amendments table
CREATE TABLE IF NOT EXISTS dublin.nol_amendments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    nol_id UUID NOT NULL,
    original_return_id UUID NOT NULL,
    amended_return_id UUID NOT NULL,
    amendment_date DATE NOT NULL,
    original_nol DECIMAL(15,2) NOT NULL CHECK (original_nol >= 0),
    amended_nol DECIMAL(15,2) NOT NULL CHECK (amended_nol >= 0),
    nol_change DECIMAL(15,2) NOT NULL,
    reason_for_amendment TEXT NOT NULL,
    affected_years VARCHAR(100),
    estimated_refund DECIMAL(15,2) DEFAULT 0 CHECK (estimated_refund >= 0),
    cascading_amendments TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    -- Foreign key constraints
    CONSTRAINT fk_nol_amendment_nol FOREIGN KEY (nol_id) REFERENCES dublin.nols(id) ON DELETE CASCADE,
    
    -- Business rules
    CONSTRAINT check_nol_change CHECK (
        nol_change = amended_nol - original_nol
    )
);

-- Create indexes for performance
CREATE INDEX idx_nol_amendment_nol ON dublin.nol_amendments(nol_id);
CREATE INDEX idx_nol_amendment_original ON dublin.nol_amendments(original_return_id);
CREATE INDEX idx_nol_amendment_amended ON dublin.nol_amendments(amended_return_id);
CREATE INDEX idx_nol_amendment_date ON dublin.nol_amendments(amendment_date);
CREATE INDEX idx_nol_amendment_tenant ON dublin.nol_amendments(tenant_id);

-- Add comments
COMMENT ON TABLE dublin.nol_amendments IS 'Track amended returns that change NOL amounts with cascading effects on subsequent years';
COMMENT ON COLUMN dublin.nol_amendments.nol_change IS 'Positive = NOL increased, Negative = NOL decreased/eliminated';
COMMENT ON COLUMN dublin.nol_amendments.affected_years IS 'Comma-separated list of years that used this NOL (may need amendment)';
COMMENT ON COLUMN dublin.nol_amendments.cascading_amendments IS 'Comma-separated list of amended return IDs filed for subsequent years';
