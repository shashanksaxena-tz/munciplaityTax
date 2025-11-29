-- Flyway Migration V1.26: Add additional indexes for withholding tables
-- Feature: Withholding Reconciliation System
-- Purpose: Performance optimization for complex queries
-- Note: Most indexes are already created in table creation migrations (V1.21-V1.25)
-- This migration adds only the audit_action index which was missing

-- Additional index for withholding_audit_log
-- This index is new and not present in V1.25
CREATE INDEX IF NOT EXISTS idx_audit_action 
    ON dublin.withholding_audit_log(action);

-- Comments for documentation
COMMENT ON INDEX dublin.idx_audit_action IS 'Index for filtering audit logs by action type (FILED, AMENDED, etc.)';
