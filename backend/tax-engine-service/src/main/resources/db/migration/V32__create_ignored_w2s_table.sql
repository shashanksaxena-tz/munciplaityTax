-- Flyway Migration V4: Create ignored_w2s table
-- Feature: Withholding Reconciliation System
-- Purpose: Track W-2 PDFs excluded from reconciliation (AI Transparency - Constitution IV)

CREATE TABLE IF NOT EXISTS ignored_w2s (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    reconciliation_id UUID NOT NULL,
    employer_ein VARCHAR(20),
    employer_name VARCHAR(255),
    employee_ssn_last4 VARCHAR(4),
    ignored_reason VARCHAR(50) NOT NULL CHECK (ignored_reason IN ('WRONG_EIN', 'DUPLICATE', 'EXTRACTION_ERROR', 'INCOMPLETE_DATA')),
    uploaded_file_path VARCHAR(500) NOT NULL,
    metadata JSONB,
    uploaded_at TIMESTAMP NOT NULL,
    reviewed_by UUID,
    reviewed_at TIMESTAMP,
    resolution_action VARCHAR(50) CHECK (resolution_action IN ('REUPLOADED', 'EIN_OVERRIDDEN', 'DELETED', 'KEPT_DUPLICATE')),
    
    -- Foreign key to reconciliation
    CONSTRAINT fk_ignored_w2_reconciliation FOREIGN KEY (reconciliation_id) 
        REFERENCES withholding_reconciliations(id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX idx_ignored_w2_reconciliation ON ignored_w2s(reconciliation_id);
CREATE INDEX idx_ignored_w2_reason ON ignored_w2s(ignored_reason);
CREATE INDEX idx_ignored_w2_uploaded_at ON ignored_w2s(uploaded_at);
CREATE INDEX idx_ignored_w2_metadata ON ignored_w2s USING gin(metadata);

-- Comments
COMMENT ON TABLE ignored_w2s IS 'W-2s excluded from reconciliation with AI extraction metadata (Constitution IV)';
COMMENT ON COLUMN ignored_w2s.metadata IS 'JSON: {confidenceScore, pageNumber, localWages, extractionErrors}';
COMMENT ON COLUMN ignored_w2s.ignored_reason IS 'Why W-2 excluded: WRONG_EIN, DUPLICATE, EXTRACTION_ERROR, INCOMPLETE_DATA';
COMMENT ON COLUMN ignored_w2s.resolution_action IS 'How user resolved: REUPLOADED, EIN_OVERRIDDEN, DELETED, KEPT_DUPLICATE';
