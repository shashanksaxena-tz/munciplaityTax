-- Flyway Migration V27: Create penalty_audit_log table for immutable audit trail
--
-- Functional Requirements:
-- Constitution III: Audit Trail Immutability
-- FR-045: Audit log for all penalty/interest actions
--
-- Multi-tenant isolation per Constitution II

CREATE TABLE IF NOT EXISTS penalty_audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    
    -- Entity being audited
    entity_type VARCHAR(50) NOT NULL CHECK (entity_type IN ('PENALTY', 'INTEREST', 'ESTIMATED_TAX', 'ABATEMENT', 'PAYMENT_ALLOCATION')),
    entity_id UUID NOT NULL,
    
    -- Action details
    action VARCHAR(50) NOT NULL CHECK (action IN ('ASSESSED', 'CALCULATED', 'ABATED', 'PAYMENT_APPLIED', 'RECALCULATED')),
    actor_id UUID NOT NULL,
    actor_role VARCHAR(20) NOT NULL CHECK (actor_role IN ('TAXPAYER', 'AUDITOR', 'SYSTEM')),
    
    -- Change tracking
    description TEXT NOT NULL,
    old_value JSONB,
    new_value JSONB,
    
    -- Request metadata
    ip_address VARCHAR(45),
    user_agent VARCHAR(255),
    
    -- Immutable timestamp
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Create indexes
CREATE INDEX idx_penalty_audit_entity ON penalty_audit_logs(entity_type, entity_id);
CREATE INDEX idx_penalty_audit_actor ON penalty_audit_logs(actor_id);
CREATE INDEX idx_penalty_audit_created_at ON penalty_audit_logs(created_at);
CREATE INDEX idx_penalty_audit_tenant ON penalty_audit_logs(tenant_id);

-- No UPDATE or DELETE allowed on this table (Constitution III)
COMMENT ON TABLE penalty_audit_logs IS 'Immutable audit trail for all penalty and interest actions - NEVER UPDATE OR DELETE';
COMMENT ON COLUMN penalty_audit_logs.entity_type IS 'Type of entity: PENALTY, INTEREST, ESTIMATED_TAX, ABATEMENT, PAYMENT_ALLOCATION';
COMMENT ON COLUMN penalty_audit_logs.action IS 'Action performed: ASSESSED, CALCULATED, ABATED, PAYMENT_APPLIED, RECALCULATED';
COMMENT ON COLUMN penalty_audit_logs.actor_role IS 'Role of actor: TAXPAYER, AUDITOR (municipality staff), SYSTEM (automated)';
COMMENT ON COLUMN penalty_audit_logs.old_value IS 'Previous state as JSON (if update)';
COMMENT ON COLUMN penalty_audit_logs.new_value IS 'New state as JSON (if create/update)';
