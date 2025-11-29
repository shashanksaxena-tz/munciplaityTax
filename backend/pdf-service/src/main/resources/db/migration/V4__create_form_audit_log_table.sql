-- Form Audit Log Table
-- Immutable audit trail for all form generation and lifecycle events

CREATE TABLE IF NOT EXISTS form_audit_log (
    audit_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100) NOT NULL,
    generated_form_id UUID REFERENCES generated_forms(generated_form_id),
    package_id UUID REFERENCES filing_packages(package_id),
    event_type VARCHAR(50) NOT NULL,  -- GENERATED, REGENERATED, STATUS_CHANGED, DOWNLOADED, etc.
    event_description TEXT NOT NULL,
    actor_id VARCHAR(100) NOT NULL,  -- User ID who performed action
    actor_role VARCHAR(50),  -- User role at time of action
    event_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    old_value TEXT,  -- Previous value (for status changes)
    new_value TEXT,  -- New value (for status changes)
    metadata JSONB DEFAULT '{}',  -- Additional event context
    ip_address VARCHAR(45),  -- IPv4 or IPv6
    user_agent VARCHAR(500)
);

-- Indexes for audit log queries
CREATE INDEX idx_form_audit_tenant ON form_audit_log(tenant_id);
CREATE INDEX idx_form_audit_form ON form_audit_log(generated_form_id) WHERE generated_form_id IS NOT NULL;
CREATE INDEX idx_form_audit_package ON form_audit_log(package_id) WHERE package_id IS NOT NULL;
CREATE INDEX idx_form_audit_event_type ON form_audit_log(event_type);
CREATE INDEX idx_form_audit_timestamp ON form_audit_log(event_timestamp DESC);
CREATE INDEX idx_form_audit_actor ON form_audit_log(actor_id);

-- Comments
COMMENT ON TABLE form_audit_log IS 'Immutable audit trail for all form generation and lifecycle events';
COMMENT ON COLUMN form_audit_log.event_type IS 'Event type: GENERATED, REGENERATED, STATUS_CHANGED, DOWNLOADED, SUBMITTED, etc.';
COMMENT ON COLUMN form_audit_log.actor_id IS 'User ID who performed the action';
COMMENT ON COLUMN form_audit_log.metadata IS 'Additional event context (JSON): version changed, validation errors, etc.';
