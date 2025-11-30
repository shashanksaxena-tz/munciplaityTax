-- V1.40: Create penalties table for late filing and late payment penalties
-- 
-- Functional Requirements:
-- FR-001 to FR-006: Late filing penalty calculation (5% per month, max 25%)
-- FR-007 to FR-011: Late payment penalty calculation (1% per month, max 25%)
-- FR-012 to FR-014: Combined penalty cap (max 5% per month when both apply)
--
-- Multi-tenant isolation: All queries scoped to tenant_id
-- Audit trail: created_at, created_by, updated_at immutable per Constitution III

CREATE TABLE IF NOT EXISTS penalties (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    return_id UUID NOT NULL,
    
    -- Penalty details
    penalty_type VARCHAR(50) NOT NULL CHECK (penalty_type IN ('LATE_FILING', 'LATE_PAYMENT', 'ESTIMATED_UNDERPAYMENT', 'OTHER')),
    assessment_date DATE NOT NULL,
    tax_due_date DATE NOT NULL,
    actual_date DATE NOT NULL,
    months_late INTEGER NOT NULL CHECK (months_late >= 0 AND months_late <= 120),
    
    -- Financial amounts
    unpaid_tax_amount DECIMAL(15,2) NOT NULL CHECK (unpaid_tax_amount >= 0),
    penalty_rate DECIMAL(5,4) NOT NULL CHECK (penalty_rate >= 0),
    penalty_amount DECIMAL(15,2) NOT NULL CHECK (penalty_amount >= 0),
    maximum_penalty DECIMAL(15,2) NOT NULL CHECK (maximum_penalty >= 0),
    
    -- Abatement tracking
    is_abated BOOLEAN NOT NULL DEFAULT FALSE,
    abatement_reason TEXT,
    abatement_date DATE,
    
    -- Audit trail (Constitution III: Immutability)
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    -- Constraints
    CONSTRAINT check_penalty_cap CHECK (penalty_amount <= maximum_penalty),
    CONSTRAINT check_maximum_penalty_calculation CHECK (maximum_penalty = unpaid_tax_amount * 0.25::DECIMAL(5,4)),
    CONSTRAINT check_late_date CHECK (actual_date >= tax_due_date),
    CONSTRAINT check_abatement_fields CHECK (
        (is_abated = FALSE) OR 
        (is_abated = TRUE AND abatement_reason IS NOT NULL AND abatement_date IS NOT NULL)
    )
);

-- Comments for documentation
COMMENT ON TABLE penalties IS 'Stores penalty assessments for tax returns (late filing, late payment, estimated underpayment)';
COMMENT ON COLUMN penalties.penalty_type IS 'Type of penalty: LATE_FILING (5%/month), LATE_PAYMENT (1%/month), ESTIMATED_UNDERPAYMENT, OTHER';
COMMENT ON COLUMN penalties.months_late IS 'Calculated months late, rounded up to next full month. Max 120 months (10 years per business rule)';
COMMENT ON COLUMN penalties.unpaid_tax_amount IS 'Tax balance subject to penalty at time of assessment';
COMMENT ON COLUMN penalties.penalty_rate IS 'Rate per month (0.05 for filing, 0.01 for payment)';
COMMENT ON COLUMN penalties.penalty_amount IS 'Calculated penalty amount, capped at maximum_penalty';
COMMENT ON COLUMN penalties.maximum_penalty IS 'Cap at 25% of unpaid tax (5 months for filing, 25 months for payment)';
COMMENT ON COLUMN penalties.is_abated IS 'Whether penalty was abated for reasonable cause';
