-- Flyway Migration V6: Create withholding_audit_log table
-- Feature: Withholding Reconciliation System
-- Purpose: Immutable audit trail for all withholding actions (Constitution Principle III)

CREATE TABLE IF NOT EXISTS withholding_audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    entity_type VARCHAR(50) NOT NULL CHECK (entity_type IN ('W1_FILING', 'RECONCILIATION', 'CUMULATIVE_TOTALS', 'PAYMENT')),
    entity_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL CHECK (action IN ('FILED', 'AMENDED', 'RECONCILED', 'PAYMENT_RECEIVED', 'CUMULATIVE_UPDATED', 'DISCREPANCY_RESOLVED')),
    actor_id UUID NOT NULL,
    actor_role VARCHAR(20) NOT NULL CHECK (actor_role IN ('BUSINESS', 'AUDITOR', 'SYSTEM')),
    description TEXT NOT NULL,
    old_value JSONB,
    new_value JSONB,
    ip_address VARCHAR(45),
    user_agent VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexes for audit queries
CREATE INDEX idx_withholding_audit_entity ON withholding_audit_log(entity_type, entity_id);
CREATE INDEX idx_withholding_audit_actor ON withholding_audit_log(actor_id);
CREATE INDEX idx_withholding_audit_created_at ON withholding_audit_log(created_at);
CREATE INDEX idx_withholding_audit_tenant ON withholding_audit_log(tenant_id);
CREATE INDEX idx_withholding_audit_action ON withholding_audit_log(action);

-- Comments
COMMENT ON TABLE withholding_audit_log IS 'Immutable audit trail per Constitution III. 7-year retention per IRS IRC § 6001. WARNING: Never DELETE records. Archive to cold storage after 10 years per data retention policy';
COMMENT ON COLUMN withholding_audit_log.old_value IS 'Previous state (JSON) before action. NULL for CREATE actions';
COMMENT ON COLUMN withholding_audit_log.new_value IS 'New state (JSON) after action. NULL for DELETE actions';
COMMENT ON COLUMN withholding_audit_log.actor_role IS 'BUSINESS (owner/accountant), AUDITOR (municipality), SYSTEM (automated)';
