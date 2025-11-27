-- Flyway Migration V1.20: Create w1_filings table
-- Feature: Withholding Reconciliation System
-- Purpose: Store individual W-1 withholding return filings

-- Create w1_filings table
CREATE TABLE IF NOT EXISTS dublin.w1_filings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    business_id UUID NOT NULL,
    tax_year INTEGER NOT NULL CHECK (tax_year >= 2020),
    filing_frequency VARCHAR(20) NOT NULL CHECK (filing_frequency IN ('DAILY', 'SEMI_MONTHLY', 'MONTHLY', 'QUARTERLY')),
    period VARCHAR(10) NOT NULL,
    period_start_date DATE NOT NULL,
    period_end_date DATE NOT NULL CHECK (period_end_date >= period_start_date),
    due_date DATE NOT NULL CHECK (due_date >= period_end_date),
    filing_date TIMESTAMP NOT NULL,
    gross_wages DECIMAL(15,2) NOT NULL CHECK (gross_wages >= 0),
    taxable_wages DECIMAL(15,2) NOT NULL CHECK (taxable_wages >= 0),
    tax_rate DECIMAL(5,4) NOT NULL CHECK (tax_rate > 0),
    tax_due DECIMAL(15,2) NOT NULL CHECK (tax_due >= 0),
    adjustments DECIMAL(15,2) DEFAULT 0,
    total_amount_due DECIMAL(15,2) NOT NULL,
    is_amended BOOLEAN NOT NULL DEFAULT FALSE,
    amends_filing_id UUID,
    amendment_reason TEXT,
    employee_count INTEGER CHECK (employee_count >= 0),
    status VARCHAR(20) NOT NULL DEFAULT 'FILED' CHECK (status IN ('FILED', 'PAID', 'OVERDUE', 'AMENDED')),
    late_filing_penalty DECIMAL(15,2) DEFAULT 0,
    underpayment_penalty DECIMAL(15,2) DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    -- Foreign key constraints
    CONSTRAINT fk_w1_amends_filing FOREIGN KEY (amends_filing_id) REFERENCES dublin.w1_filings(id),
    
    -- Business rules
    CONSTRAINT check_amended_filing CHECK (
        (is_amended = FALSE AND amends_filing_id IS NULL) OR
        (is_amended = TRUE AND amends_filing_id IS NOT NULL)
    )
);

-- Create indexes for performance
CREATE INDEX idx_w1_business_year ON dublin.w1_filings(business_id, tax_year);
CREATE INDEX idx_w1_tenant_year ON dublin.w1_filings(tenant_id, tax_year);
CREATE INDEX idx_w1_due_date ON dublin.w1_filings(due_date);
CREATE INDEX idx_w1_filing_date ON dublin.w1_filings(filing_date);
CREATE INDEX idx_w1_status ON dublin.w1_filings(status);
CREATE INDEX idx_w1_amended ON dublin.w1_filings(is_amended) WHERE is_amended = TRUE;

-- Unique constraint: Cannot file same period twice (unless amended)
CREATE UNIQUE INDEX unique_w1_filing ON dublin.w1_filings(business_id, tax_year, period, is_amended)
    WHERE is_amended = FALSE;

-- Comments for documentation
COMMENT ON TABLE dublin.w1_filings IS 'W-1 withholding return filings per FR-001, FR-003, FR-013';
COMMENT ON COLUMN dublin.w1_filings.tenant_id IS 'Multi-tenant isolation per Constitution Principle II';
COMMENT ON COLUMN dublin.w1_filings.amends_filing_id IS 'References original filing if amended (FR-003)';
COMMENT ON COLUMN dublin.w1_filings.late_filing_penalty IS 'FR-011: 5% per month, max 25%, min $50 if tax due > $200';
COMMENT ON COLUMN dublin.w1_filings.created_at IS 'Immutable audit timestamp per Constitution Principle III';
