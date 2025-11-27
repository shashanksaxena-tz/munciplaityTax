-- Flyway Migration V1.21: Create cumulative_withholding_totals table
-- Feature: Withholding Reconciliation System
-- Purpose: Cached YTD cumulative totals for performance (Research R2)

CREATE TABLE IF NOT EXISTS dublin.cumulative_withholding_totals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    business_id UUID NOT NULL,
    tax_year INTEGER NOT NULL,
    periods_filed INTEGER NOT NULL DEFAULT 0 CHECK (periods_filed >= 0),
    cumulative_wages_ytd DECIMAL(15,2) NOT NULL DEFAULT 0 CHECK (cumulative_wages_ytd >= 0),
    cumulative_tax_ytd DECIMAL(15,2) NOT NULL DEFAULT 0 CHECK (cumulative_tax_ytd >= 0),
    cumulative_adjustments_ytd DECIMAL(15,2) NOT NULL DEFAULT 0,
    last_filing_date TIMESTAMP,
    estimated_annual_wages DECIMAL(15,2),
    projected_annual_wages DECIMAL(15,2),
    on_track_indicator BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Unique constraint: One cumulative record per business + tax year
CREATE UNIQUE INDEX unique_cumulative_business_year ON dublin.cumulative_withholding_totals(business_id, tax_year);

-- Indexes for performance
CREATE INDEX idx_cumulative_tenant_year ON dublin.cumulative_withholding_totals(tenant_id, tax_year);
CREATE INDEX idx_cumulative_updated_at ON dublin.cumulative_withholding_totals(updated_at);
CREATE INDEX idx_cumulative_on_track ON dublin.cumulative_withholding_totals(on_track_indicator) WHERE on_track_indicator = FALSE;

-- Comments
COMMENT ON TABLE dublin.cumulative_withholding_totals IS 'Cached YTD totals per FR-002, FR-005. Event-driven updates on W-1 filing.';
COMMENT ON COLUMN dublin.cumulative_withholding_totals.periods_filed IS 'Count of W-1 filings for tax year';
COMMENT ON COLUMN dublin.cumulative_withholding_totals.projected_annual_wages IS 'FR-005: Based on run rate (cumulative / periods * expected periods)';
COMMENT ON COLUMN dublin.cumulative_withholding_totals.on_track_indicator IS 'FR-005: Within 15% of estimated wages';
