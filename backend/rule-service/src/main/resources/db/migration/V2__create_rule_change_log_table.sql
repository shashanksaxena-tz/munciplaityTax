-- V2: Create rule_change_log table
-- This is an append-only audit trail for all rule modifications

CREATE TABLE rule_change_log (
    log_id UUID PRIMARY KEY,
    rule_id UUID NOT NULL REFERENCES tax_rules(rule_id) ON DELETE RESTRICT,
    change_type VARCHAR(20) NOT NULL,
    old_value JSONB,
    new_value JSONB NOT NULL,
    changed_fields VARCHAR[] NOT NULL,
    changed_by VARCHAR(100) NOT NULL,
    change_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    change_reason TEXT NOT NULL,
    affected_returns_count INTEGER DEFAULT 0,
    impact_estimate JSONB,
    
    -- Check constraint for change type enum
    CONSTRAINT chk_change_type CHECK (change_type IN (
        'CREATE', 'UPDATE', 'DELETE', 'APPROVE', 'REJECT', 'VOID', 'ROLLBACK'
    ))
);

-- Indexes for audit queries

-- Query history for specific rule
CREATE INDEX idx_rule_change_log_rule ON rule_change_log(rule_id, change_date DESC);

-- Query by date range for audit reports
CREATE INDEX idx_rule_change_log_date ON rule_change_log(change_date DESC);

-- Query by user (who changed what)
CREATE INDEX idx_rule_change_log_user ON rule_change_log(changed_by, change_date DESC);

-- Query by change type
CREATE INDEX idx_rule_change_log_type ON rule_change_log(change_type, change_date DESC);

-- Trigger to prevent updates/deletes (enforce append-only)
CREATE OR REPLACE FUNCTION prevent_rule_change_log_modification()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'rule_change_log is append-only. UPDATE and DELETE operations are not allowed.';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER no_update_rule_change_log
BEFORE UPDATE OR DELETE ON rule_change_log
FOR EACH ROW EXECUTE FUNCTION prevent_rule_change_log_modification();

-- Comments for documentation
COMMENT ON TABLE rule_change_log IS 'Immutable audit trail for all tax rule changes';
COMMENT ON COLUMN rule_change_log.change_type IS 'Type of change: CREATE, UPDATE, DELETE, APPROVE, REJECT, VOID, ROLLBACK';
COMMENT ON COLUMN rule_change_log.old_value IS 'Complete rule state before change (NULL for CREATE)';
COMMENT ON COLUMN rule_change_log.new_value IS 'Complete rule state after change';
COMMENT ON COLUMN rule_change_log.changed_fields IS 'Array of field names that changed';
COMMENT ON COLUMN rule_change_log.impact_estimate IS 'JSONB with estimated impact metrics';
COMMENT ON TRIGGER no_update_rule_change_log ON rule_change_log IS 'Enforces append-only behavior for audit compliance';
