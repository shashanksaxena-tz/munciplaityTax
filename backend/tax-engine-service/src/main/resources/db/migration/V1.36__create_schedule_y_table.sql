-- Migration V1.30: Create schedule_y table for multi-state apportionment
-- Feature: Schedule Y Multi-State Sourcing
-- Purpose: Store apportionment schedule data with factor calculations and elections

CREATE TABLE IF NOT EXISTS schedule_y (
    schedule_y_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    return_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    tax_year INTEGER NOT NULL,
    apportionment_formula VARCHAR(50) NOT NULL CHECK (apportionment_formula IN (
        'TRADITIONAL_THREE_FACTOR',
        'FOUR_FACTOR_DOUBLE_SALES',
        'SINGLE_SALES_FACTOR',
        'CUSTOM'
    )),
    formula_weights JSONB,
    property_factor_percentage DECIMAL(5,2) CHECK (property_factor_percentage >= 0 AND property_factor_percentage <= 100),
    payroll_factor_percentage DECIMAL(5,2) CHECK (payroll_factor_percentage >= 0 AND payroll_factor_percentage <= 100),
    sales_factor_percentage DECIMAL(5,2) CHECK (sales_factor_percentage >= 0 AND sales_factor_percentage <= 100),
    final_apportionment_percentage DECIMAL(5,2) NOT NULL CHECK (final_apportionment_percentage >= 0 AND final_apportionment_percentage <= 100),
    sourcing_method_election VARCHAR(30) NOT NULL CHECK (sourcing_method_election IN (
        'FINNIGAN',
        'JOYCE'
    )),
    throwback_election VARCHAR(20) CHECK (throwback_election IN (
        'THROWBACK',
        'THROWOUT',
        'NONE'
    )),
    service_sourcing_method VARCHAR(30) CHECK (service_sourcing_method IN (
        'MARKET_BASED',
        'COST_OF_PERFORMANCE'
    )),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN (
        'DRAFT',
        'FILED',
        'AMENDED',
        'UNDER_REVIEW',
        'APPROVED'
    )),
    amends_schedule_y_id UUID REFERENCES schedule_y(schedule_y_id),
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL,
    last_modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by UUID,
    CONSTRAINT fk_schedule_y_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id)
);

-- Indexes for performance
CREATE INDEX idx_schedule_y_return_id ON schedule_y(return_id);
CREATE INDEX idx_schedule_y_tenant_id ON schedule_y(tenant_id);
CREATE INDEX idx_schedule_y_tax_year ON schedule_y(tax_year);
CREATE INDEX idx_schedule_y_status ON schedule_y(status);
CREATE INDEX idx_schedule_y_created_date ON schedule_y(created_date);

-- Comments for documentation
COMMENT ON TABLE schedule_y IS 'Multi-state apportionment schedule (Form 27-Y) with factor calculations and sourcing elections';
COMMENT ON COLUMN schedule_y.apportionment_formula IS 'Formula type: TRADITIONAL_THREE_FACTOR, FOUR_FACTOR_DOUBLE_SALES (Ohio default), SINGLE_SALES_FACTOR, CUSTOM';
COMMENT ON COLUMN schedule_y.formula_weights IS 'JSONB: {"property": 1, "payroll": 1, "sales": 2} for double-weighted sales';
COMMENT ON COLUMN schedule_y.sourcing_method_election IS 'FINNIGAN (include all group sales) or JOYCE (only nexus entities)';
COMMENT ON COLUMN schedule_y.throwback_election IS 'THROWBACK (add to numerator), THROWOUT (exclude), or NONE';
COMMENT ON COLUMN schedule_y.service_sourcing_method IS 'MARKET_BASED (customer location) or COST_OF_PERFORMANCE (employee location)';
COMMENT ON COLUMN schedule_y.amends_schedule_y_id IS 'References original Schedule Y if this is an amendment';
