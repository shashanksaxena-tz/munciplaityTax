-- V1.2: Create compatibility table for audit_log seed data
-- This migration creates a temporary table for audit_log to support the V2 seed data script 
-- which uses singular table name 'audit_log' instead of 'audit_logs'.

CREATE TABLE IF NOT EXISTS audit_log (
    log_id UUID PRIMARY KEY,
    tenant_id UUID,
    entity_type VARCHAR(50),
    entity_id VARCHAR(255), -- V2 uses text for entity_id
    action VARCHAR(50),
    user_id VARCHAR(100), -- V2 uses 'SEED_DATA' string
    timestamp TIMESTAMP,
    details TEXT
);
