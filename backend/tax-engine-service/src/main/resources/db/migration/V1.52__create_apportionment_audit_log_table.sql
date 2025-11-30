-- Migration V1.52: Create apportionment_audit_log table
-- Feature: Schedule Y Multi-State Sourcing
-- Purpose: Immutable audit trail for apportionment calculations and election changes

CREATE TABLE IF NOT EXISTS apportionment_audit_log (
    audit_log_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    schedule_y_id UUID NOT NULL REFERENCES schedule_y(schedule_y_id),
    tenant_id UUID NOT NULL,
    change_type VARCHAR(30) NOT NULL CHECK (change_type IN (
        'ELECTION_CHANGED',
        'FACTOR_RECALCULATED',
        'TRANSACTION_ADDED',
        'TRANSACTION_MODIFIED',
        'NEXUS_CHANGED',
        'SCHEDULE_FILED',
        'SCHEDULE_AMENDED',
        'CALCULATION_ADJUSTMENT'
    )),
    entity_type VARCHAR(50),
    entity_id UUID,
    changed_by UUID NOT NULL,
    change_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    old_value TEXT,
    new_value TEXT,
    change_reason TEXT,
    affected_calculation VARCHAR(100),
    ip_address INET,
    user_agent TEXT,
    CONSTRAINT fk_apportionment_audit_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(tenant_id)
);

-- Indexes for audit queries
CREATE INDEX idx_apportionment_audit_schedule_y_id ON apportionment_audit_log(schedule_y_id);
CREATE INDEX idx_apportionment_audit_tenant_id ON apportionment_audit_log(tenant_id);
CREATE INDEX idx_apportionment_audit_change_type ON apportionment_audit_log(change_type);
CREATE INDEX idx_apportionment_audit_change_date ON apportionment_audit_log(change_date);
CREATE INDEX idx_apportionment_audit_changed_by ON apportionment_audit_log(changed_by);

-- Comments
COMMENT ON TABLE apportionment_audit_log IS 'Immutable audit trail for apportionment calculations, elections, and factor changes (7-year retention for IRS compliance)';
COMMENT ON COLUMN apportionment_audit_log.change_type IS 'Type of change: ELECTION_CHANGED, FACTOR_RECALCULATED, TRANSACTION_ADDED, NEXUS_CHANGED, etc.';
COMMENT ON COLUMN apportionment_audit_log.entity_type IS 'Type of entity changed: PropertyFactor, PayrollFactor, SalesFactor, SaleTransaction, NexusTracking';
COMMENT ON COLUMN apportionment_audit_log.entity_id IS 'UUID of the specific entity that was changed';
COMMENT ON COLUMN apportionment_audit_log.old_value IS 'Previous value (JSON format for complex objects)';
COMMENT ON COLUMN apportionment_audit_log.new_value IS 'New value (JSON format for complex objects)';
COMMENT ON COLUMN apportionment_audit_log.affected_calculation IS 'Which calculation was affected: property_factor, payroll_factor, sales_factor, final_apportionment';
