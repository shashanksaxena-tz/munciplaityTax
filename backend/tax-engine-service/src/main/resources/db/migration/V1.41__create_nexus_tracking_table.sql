-- Migration V1.35: Create nexus_tracking table
-- Feature: Schedule Y Multi-State Sourcing
-- Purpose: Track nexus status by state/municipality for throwback determination

CREATE TABLE IF NOT EXISTS nexus_tracking (
    nexus_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    tax_year INTEGER NOT NULL,
    state VARCHAR(2) NOT NULL,
    municipality VARCHAR(100),
    has_nexus BOOLEAN NOT NULL DEFAULT FALSE,
    nexus_reasons VARCHAR(50)[],
    sales_in_state DECIMAL(15,2) DEFAULT 0 CHECK (sales_in_state >= 0),
    property_in_state DECIMAL(15,2) DEFAULT 0 CHECK (property_in_state >= 0),
    payroll_in_state DECIMAL(15,2) DEFAULT 0 CHECK (payroll_in_state >= 0),
    employee_count_in_state INTEGER DEFAULT 0 CHECK (employee_count_in_state >= 0),
    economic_nexus_threshold DECIMAL(15,2),
    transaction_count_in_state INTEGER DEFAULT 0,
    nexus_established_date DATE,
    nexus_terminated_date DATE,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_nexus_tracking_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id),
    CONSTRAINT uk_nexus_tracking_business_state_year UNIQUE (business_id, state, municipality, tax_year)
);

-- Indexes
CREATE INDEX idx_nexus_tracking_business_id ON nexus_tracking(business_id);
CREATE INDEX idx_nexus_tracking_tenant_id ON nexus_tracking(tenant_id);
CREATE INDEX idx_nexus_tracking_state ON nexus_tracking(state);
CREATE INDEX idx_nexus_tracking_has_nexus ON nexus_tracking(has_nexus);
CREATE INDEX idx_nexus_tracking_tax_year ON nexus_tracking(tax_year);

-- Comments
COMMENT ON TABLE nexus_tracking IS 'Tracks nexus status in each state/municipality for throwback rule determination';
COMMENT ON COLUMN nexus_tracking.nexus_reasons IS 'Array of reasons: PHYSICAL_PRESENCE, EMPLOYEE_PRESENCE, ECONOMIC_NEXUS, FACTOR_PRESENCE';
COMMENT ON COLUMN nexus_tracking.sales_in_state IS 'Total sales in state for economic nexus threshold tracking';
COMMENT ON COLUMN nexus_tracking.economic_nexus_threshold IS 'Economic nexus threshold for this state (e.g., $500,000)';
COMMENT ON COLUMN nexus_tracking.transaction_count_in_state IS 'Transaction count for states with transaction-based thresholds (e.g., 200 transactions)';
