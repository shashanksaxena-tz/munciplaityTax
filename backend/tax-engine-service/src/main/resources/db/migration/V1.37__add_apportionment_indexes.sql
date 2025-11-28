-- Migration V1.37: Add indexes for apportionment queries
-- Feature: Schedule Y Multi-State Sourcing
-- Purpose: Optimize query performance for apportionment calculations and reporting

-- Composite indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_schedule_y_tenant_tax_year ON schedule_y(tenant_id, tax_year);
CREATE INDEX IF NOT EXISTS idx_schedule_y_return_status ON schedule_y(return_id, status);

-- Property factor indexes
CREATE INDEX IF NOT EXISTS idx_property_factor_ohio_property ON property_factor(total_ohio_property);
CREATE INDEX IF NOT EXISTS idx_property_factor_percentage ON property_factor(property_factor_percentage);

-- Payroll factor indexes
CREATE INDEX IF NOT EXISTS idx_payroll_factor_ohio_payroll ON payroll_factor(total_ohio_payroll);
CREATE INDEX IF NOT EXISTS idx_payroll_factor_percentage ON payroll_factor(payroll_factor_percentage);

-- Sales factor indexes
CREATE INDEX IF NOT EXISTS idx_sales_factor_ohio_sales ON sales_factor(total_ohio_sales);
CREATE INDEX IF NOT EXISTS idx_sales_factor_percentage ON sales_factor(sales_factor_percentage);
CREATE INDEX IF NOT EXISTS idx_sales_factor_throwback ON sales_factor(throwback_adjustment) WHERE throwback_adjustment > 0;

-- Sale transaction indexes for reporting
CREATE INDEX IF NOT EXISTS idx_sale_transaction_sale_type ON sale_transaction(sale_type);
CREATE INDEX IF NOT EXISTS idx_sale_transaction_sourcing_method ON sale_transaction(sourcing_method);
CREATE INDEX IF NOT EXISTS idx_sale_transaction_nexus ON sale_transaction(has_destination_nexus);

-- Nexus tracking composite indexes
CREATE INDEX IF NOT EXISTS idx_nexus_tracking_business_tax_year ON nexus_tracking(business_id, tax_year);
CREATE INDEX IF NOT EXISTS idx_nexus_tracking_state_has_nexus ON nexus_tracking(state, has_nexus);

-- Audit log composite indexes for compliance queries
CREATE INDEX IF NOT EXISTS idx_apportionment_audit_schedule_change ON apportionment_audit_log(schedule_y_id, change_type, change_date);
CREATE INDEX IF NOT EXISTS idx_apportionment_audit_entity ON apportionment_audit_log(entity_type, entity_id);

-- Comments
COMMENT ON INDEX idx_schedule_y_tenant_tax_year IS 'Optimize queries for tenant-specific Schedule Y filings by tax year';
COMMENT ON INDEX idx_sales_factor_throwback IS 'Partial index for throwback adjustments reporting';
COMMENT ON INDEX idx_apportionment_audit_schedule_change IS 'Optimize audit trail queries by schedule and change type';
