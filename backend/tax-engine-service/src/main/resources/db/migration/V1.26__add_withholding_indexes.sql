-- Flyway Migration V1.26: Add additional indexes for withholding tables
-- Feature: Withholding Reconciliation System
-- Purpose: Performance optimization for complex queries

-- Additional indexes for cumulative_withholding_totals
CREATE INDEX IF NOT EXISTS idx_cumulative_updated_at 
    ON dublin.cumulative_withholding_totals(updated_at);
CREATE INDEX IF NOT EXISTS idx_cumulative_on_track 
    ON dublin.cumulative_withholding_totals(on_track_indicator) 
    WHERE on_track_indicator = FALSE;

-- Additional indexes for withholding_reconciliations
CREATE INDEX IF NOT EXISTS idx_reconciliation_discrepancy 
    ON dublin.withholding_reconciliations(status) 
    WHERE status = 'DISCREPANCY';

-- Additional indexes for ignored_w2s
CREATE INDEX IF NOT EXISTS idx_ignored_w2_reason 
    ON dublin.ignored_w2s(ignored_reason);
CREATE INDEX IF NOT EXISTS idx_ignored_w2_uploaded_at 
    ON dublin.ignored_w2s(uploaded_at);

-- JSONB index for ignored_w2s metadata (for AI extraction confidence filtering)
CREATE INDEX IF NOT EXISTS idx_ignored_w2_metadata 
    ON dublin.ignored_w2s USING gin(metadata);

-- Additional indexes for withholding_audit_log
CREATE INDEX IF NOT EXISTS idx_audit_entity 
    ON dublin.withholding_audit_log(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_actor 
    ON dublin.withholding_audit_log(actor_id);
CREATE INDEX IF NOT EXISTS idx_audit_created_at 
    ON dublin.withholding_audit_log(created_at);
CREATE INDEX IF NOT EXISTS idx_audit_tenant 
    ON dublin.withholding_audit_log(tenant_id);

-- Comments for documentation
COMMENT ON INDEX dublin.idx_cumulative_on_track IS 'Partial index for off-track businesses (performance optimization)';
COMMENT ON INDEX dublin.idx_reconciliation_discrepancy IS 'Partial index for unresolved discrepancies';
COMMENT ON INDEX dublin.idx_ignored_w2_metadata IS 'GIN index for JSONB metadata queries';
