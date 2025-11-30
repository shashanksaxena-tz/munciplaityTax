-- V1.41: Create estimated_tax_penalties table for quarterly underpayment penalties
--
-- Functional Requirements:
-- FR-015 to FR-019: Safe harbor evaluation (90% current year OR 100%/110% prior year)
-- FR-020 to FR-026: Quarterly underpayment penalty calculation
--
-- Multi-tenant isolation per Constitution II

CREATE TABLE IF NOT EXISTS estimated_tax_penalties (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    return_id UUID NOT NULL,
    
    -- Tax year details
    tax_year INTEGER NOT NULL,
    annual_tax_liability DECIMAL(15,2) NOT NULL CHECK (annual_tax_liability >= 0),
    prior_year_tax_liability DECIMAL(15,2) NOT NULL CHECK (prior_year_tax_liability >= 0),
    agi DECIMAL(15,2) NOT NULL,
    
    -- Safe harbor evaluation
    safe_harbor_1_met BOOLEAN NOT NULL DEFAULT FALSE,
    safe_harbor_2_met BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- Calculation details
    calculation_method VARCHAR(20) NOT NULL CHECK (calculation_method IN ('STANDARD', 'ANNUALIZED_INCOME')),
    total_penalty DECIMAL(15,2) NOT NULL DEFAULT 0 CHECK (total_penalty >= 0),
    
    -- Audit trail
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    -- Constraints
    CONSTRAINT unique_estimated_penalty_return UNIQUE(return_id),
    CONSTRAINT check_safe_harbor_no_penalty CHECK (
        NOT (safe_harbor_1_met = TRUE OR safe_harbor_2_met = TRUE) OR total_penalty = 0
    )
);

COMMENT ON TABLE estimated_tax_penalties IS 'Stores estimated tax underpayment penalties with safe harbor evaluation';
COMMENT ON COLUMN estimated_tax_penalties.safe_harbor_1_met IS 'TRUE if paid >= 90% of current year tax';
COMMENT ON COLUMN estimated_tax_penalties.safe_harbor_2_met IS 'TRUE if paid >= 100% of prior year tax (110% if AGI > $150K)';
COMMENT ON COLUMN estimated_tax_penalties.calculation_method IS 'STANDARD (25% per quarter) or ANNUALIZED_INCOME (based on income timing)';
