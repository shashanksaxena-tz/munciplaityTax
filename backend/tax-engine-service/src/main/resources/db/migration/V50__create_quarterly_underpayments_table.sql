-- Flyway Migration V22: Create quarterly_underpayments table for per-quarter underpayment details
--
-- Functional Requirements:
-- FR-022: Calculate underpayment per quarter (Required - Actual)
-- FR-024: Apply overpayments from later quarters to earlier underpayments
--
-- Multi-tenant isolation per Constitution II

CREATE TABLE IF NOT EXISTS quarterly_underpayments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    estimated_penalty_id UUID NOT NULL,
    
    -- Quarter identification
    quarter VARCHAR(2) NOT NULL CHECK (quarter IN ('Q1', 'Q2', 'Q3', 'Q4')),
    due_date DATE NOT NULL,
    
    -- Payment tracking
    required_payment DECIMAL(15,2) NOT NULL CHECK (required_payment >= 0),
    actual_payment DECIMAL(15,2) NOT NULL CHECK (actual_payment >= 0),
    underpayment DECIMAL(15,2) NOT NULL, -- Can be negative if overpaid
    
    -- Penalty calculation
    quarters_unpaid INTEGER NOT NULL CHECK (quarters_unpaid >= 0),
    penalty_rate DECIMAL(5,4) NOT NULL CHECK (penalty_rate >= 0),
    penalty_amount DECIMAL(15,2) NOT NULL CHECK (penalty_amount >= 0),
    
    -- Constraints
    CONSTRAINT unique_quarterly_underpayment UNIQUE(estimated_penalty_id, quarter),
    CONSTRAINT check_underpayment_calculation CHECK (underpayment = (required_payment - actual_payment)),
    CONSTRAINT fk_quarterly_underpayment_estimated_penalty FOREIGN KEY (estimated_penalty_id) 
        REFERENCES estimated_tax_penalties(id) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX idx_quarterly_underpayment_penalty ON quarterly_underpayments(estimated_penalty_id);
CREATE INDEX idx_quarterly_underpayment_quarter ON quarterly_underpayments(quarter);
CREATE INDEX idx_quarterly_underpayment_due_date ON quarterly_underpayments(due_date);

-- Comments
COMMENT ON TABLE quarterly_underpayments IS 'Stores underpayment details for each quarter (Q1-Q4)';
COMMENT ON COLUMN quarterly_underpayments.quarter IS 'Quarter: Q1 (Apr 15), Q2 (Jun 15), Q3 (Sep 15), Q4 (Jan 15)';
COMMENT ON COLUMN quarterly_underpayments.underpayment IS 'Required minus Actual; negative value means overpayment';
COMMENT ON COLUMN quarterly_underpayments.quarters_unpaid IS 'Number of quarters from due date to filing date';
