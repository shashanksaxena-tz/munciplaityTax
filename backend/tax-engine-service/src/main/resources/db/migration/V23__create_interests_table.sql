-- Flyway Migration V23: Create interests table for compound quarterly interest calculation
--
-- Functional Requirements:
-- FR-027 to FR-032: Interest calculation with quarterly compounding
--
-- Multi-tenant isolation per Constitution II

CREATE TABLE IF NOT EXISTS interests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    return_id UUID NOT NULL,
    
    -- Interest period
    tax_due_date DATE NOT NULL,
    unpaid_tax_amount DECIMAL(15,2) NOT NULL CHECK (unpaid_tax_amount >= 0),
    
    -- Interest rate details
    annual_interest_rate DECIMAL(5,4) NOT NULL CHECK (annual_interest_rate >= 0),
    compounding_frequency VARCHAR(20) NOT NULL DEFAULT 'QUARTERLY' CHECK (compounding_frequency = 'QUARTERLY'),
    
    -- Calculation period (end_date is exclusive in financial interest calculations)
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    total_days INTEGER NOT NULL,
    
    -- Result
    total_interest DECIMAL(15,2) NOT NULL CHECK (total_interest >= 0),
    
    -- Audit trail
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    -- Constraints
    CONSTRAINT check_interest_dates CHECK (end_date >= start_date),
    -- Note: total_days = end_date - start_date (end_date exclusive, per financial convention)
    CONSTRAINT check_total_days_calculation CHECK (total_days = (end_date - start_date))
);

-- Create indexes
CREATE INDEX idx_interest_return ON interests(return_id);
CREATE INDEX idx_interest_tenant ON interests(tenant_id);
CREATE INDEX idx_interest_start_date ON interests(start_date);
CREATE INDEX idx_interest_end_date ON interests(end_date);

-- Comments
COMMENT ON TABLE interests IS 'Stores interest calculations on unpaid tax with quarterly compounding';
COMMENT ON COLUMN interests.annual_interest_rate IS 'Retrieved from rule engine (federal short-term rate + 3%, typically 3-8%)';
COMMENT ON COLUMN interests.compounding_frequency IS 'Always QUARTERLY per IRS standard';
COMMENT ON COLUMN interests.total_interest IS 'Sum of all quarterly compound interest';
COMMENT ON COLUMN interests.total_days IS 'Days between start_date and end_date (end_date exclusive per financial convention)';
