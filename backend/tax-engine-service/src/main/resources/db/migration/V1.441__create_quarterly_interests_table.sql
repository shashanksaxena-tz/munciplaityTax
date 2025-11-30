-- V1.44: Create quarterly_interests table for per-quarter interest breakdown
--
-- Functional Requirements:
-- FR-029: Quarterly compounding (add accrued interest to principal each quarter)
-- FR-031: Display interest breakdown by quarter
--
-- Multi-tenant isolation per Constitution II

CREATE TABLE IF NOT EXISTS quarterly_interests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    interest_id UUID NOT NULL,
    
    -- Quarter identification
    quarter VARCHAR(10) NOT NULL, -- e.g., "Q1 2024", "Q2 2024"
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    days INTEGER NOT NULL,
    
    -- Compounding calculation
    beginning_balance DECIMAL(15,2) NOT NULL CHECK (beginning_balance >= 0),
    interest_accrued DECIMAL(15,2) NOT NULL CHECK (interest_accrued >= 0),
    ending_balance DECIMAL(15,2) NOT NULL CHECK (ending_balance >= 0),
    
    -- Constraints
    CONSTRAINT check_ending_balance_calculation CHECK (
        ABS(ending_balance - (beginning_balance + interest_accrued)) < 0.01
    ),
    CONSTRAINT check_days_calculation CHECK (days = (end_date - start_date + 1)),
    CONSTRAINT check_quarter_dates CHECK (end_date >= start_date)
);

COMMENT ON TABLE quarterly_interests IS 'Stores interest breakdown by quarter with compounding';
COMMENT ON COLUMN quarterly_interests.quarter IS 'Quarter label like "Q1 2024", "Q2 2024"';
COMMENT ON COLUMN quarterly_interests.beginning_balance IS 'Principal at start of quarter (includes prior interest)';
COMMENT ON COLUMN quarterly_interests.interest_accrued IS 'Balance × Annual Rate × Days / 365';
COMMENT ON COLUMN quarterly_interests.ending_balance IS 'Beginning + Interest (becomes next quarter beginning balance)';
