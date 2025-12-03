-- Auditor Workflow Database Schema
-- This migration adds the necessary tables and columns for the auditor workflow system

-- Add audit-related columns to submissions table (if not exists)
ALTER TABLE IF EXISTS submissions 
ADD COLUMN IF NOT EXISTS taxpayer_id VARCHAR(50),
ADD COLUMN IF NOT EXISTS discrepancy_count INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS gross_receipts DECIMAL(12, 2),
ADD COLUMN IF NOT EXISTS net_profit DECIMAL(12, 2),
ADD COLUMN IF NOT EXISTS filed_date TIMESTAMP,
ADD COLUMN IF NOT EXISTS due_date DATE;

-- Data migration for tax_year: convert non-integer values to NULL before altering type
-- This ensures data integrity before type conversion
DO $$
BEGIN
    -- Check if tax_year column exists and is not already INTEGER type
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'submissions' 
        AND column_name = 'tax_year' 
        AND data_type != 'integer'
    ) THEN
        -- Clean invalid data: set non-numeric tax_year values to NULL
        UPDATE submissions SET tax_year = NULL 
        WHERE tax_year IS NOT NULL AND tax_year !~ '^[0-9]+$';
        
        -- Convert column type
        ALTER TABLE submissions ALTER COLUMN tax_year TYPE INTEGER USING tax_year::integer;
    END IF;
END $$;

-- Create audit_queue table
CREATE TABLE IF NOT EXISTS audit_queue (
    queue_id VARCHAR(36) PRIMARY KEY,
    return_id VARCHAR(50) NOT NULL,
    priority VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    submission_date TIMESTAMP NOT NULL,
    assigned_auditor_id VARCHAR(50),
    assignment_date TIMESTAMP,
    review_started_date TIMESTAMP,
    review_completed_date TIMESTAMP,
    risk_score INTEGER NOT NULL DEFAULT 0,
    flagged_issues_count INTEGER NOT NULL DEFAULT 0,
    tenant_id VARCHAR(50),
    CONSTRAINT chk_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT chk_status CHECK (status IN ('PENDING', 'IN_REVIEW', 'AWAITING_DOCUMENTATION', 'APPROVED', 'REJECTED', 'AMENDED')),
    CONSTRAINT chk_risk_score CHECK (risk_score >= 0 AND risk_score <= 100)
);

CREATE INDEX IF NOT EXISTS idx_audit_queue_status ON audit_queue(status);
CREATE INDEX IF NOT EXISTS idx_audit_queue_priority ON audit_queue(priority);
CREATE INDEX IF NOT EXISTS idx_audit_queue_auditor ON audit_queue(assigned_auditor_id);
CREATE INDEX IF NOT EXISTS idx_audit_queue_return ON audit_queue(return_id);

-- Create audit_actions table
CREATE TABLE IF NOT EXISTS audit_actions (
    action_id VARCHAR(36) PRIMARY KEY,
    return_id VARCHAR(50) NOT NULL,
    auditor_id VARCHAR(50) NOT NULL,
    action_type VARCHAR(30) NOT NULL,
    action_date TIMESTAMP NOT NULL,
    action_details TEXT,
    previous_status VARCHAR(30),
    new_status VARCHAR(30),
    ip_address VARCHAR(45),
    user_agent VARCHAR(255),
    tenant_id VARCHAR(50),
    CONSTRAINT chk_action_type CHECK (action_type IN ('ASSIGNED', 'REVIEW_STARTED', 'REVIEW_COMPLETED', 'APPROVED', 'REJECTED', 'DOCS_REQUESTED', 'ANNOTATED', 'ESCALATED', 'PRIORITY_CHANGED', 'REASSIGNED'))
);

CREATE INDEX IF NOT EXISTS idx_audit_actions_return ON audit_actions(return_id);
CREATE INDEX IF NOT EXISTS idx_audit_actions_date ON audit_actions(action_date);

-- Create audit_trail table (immutable)
CREATE TABLE IF NOT EXISTS audit_trail (
    trail_id VARCHAR(36) PRIMARY KEY,
    return_id VARCHAR(50) NOT NULL,
    event_type VARCHAR(30) NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    ip_address VARCHAR(45),
    event_details TEXT,
    digital_signature VARCHAR(512),
    immutable BOOLEAN NOT NULL DEFAULT true,
    tenant_id VARCHAR(50),
    CONSTRAINT chk_event_type CHECK (event_type IN ('SUBMISSION', 'ASSIGNMENT', 'REVIEW_STARTED', 'REVIEW_COMPLETED', 'APPROVAL', 'REJECTION', 'AMENDMENT', 'PAYMENT', 'COMMUNICATION', 'ESCALATION', 'DOCUMENT_REQUEST', 'DOCUMENT_RECEIVED', 'PRIORITY_CHANGE', 'STATUS_CHANGE', 'ANNOTATION_ADDED'))
);

CREATE INDEX IF NOT EXISTS idx_audit_trail_return ON audit_trail(return_id);
CREATE INDEX IF NOT EXISTS idx_audit_trail_timestamp ON audit_trail(timestamp);

-- Prevent updates and deletes on audit_trail (PostgreSQL specific)
CREATE RULE audit_trail_no_update AS ON UPDATE TO audit_trail DO INSTEAD NOTHING;
CREATE RULE audit_trail_no_delete AS ON DELETE TO audit_trail DO INSTEAD NOTHING;

-- Create audit_reports table
CREATE TABLE IF NOT EXISTS audit_reports (
    report_id VARCHAR(36) PRIMARY KEY,
    return_id VARCHAR(50) NOT NULL,
    generated_date TIMESTAMP NOT NULL,
    risk_score INTEGER NOT NULL DEFAULT 0,
    risk_level VARCHAR(10) NOT NULL,
    year_over_year_comparison TEXT,
    peer_comparison TEXT,
    pattern_analysis TEXT,
    auditor_override BOOLEAN NOT NULL DEFAULT false,
    override_reason TEXT,
    tenant_id VARCHAR(50),
    CONSTRAINT chk_report_risk_score CHECK (risk_score >= 0 AND risk_score <= 100),
    CONSTRAINT chk_risk_level CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH'))
);

CREATE INDEX IF NOT EXISTS idx_audit_reports_return ON audit_reports(return_id);
CREATE INDEX IF NOT EXISTS idx_audit_reports_risk ON audit_reports(risk_level);

-- Create audit_report_flagged_items table (one-to-many)
CREATE TABLE IF NOT EXISTS audit_report_flagged_items (
    id SERIAL PRIMARY KEY,
    report_id VARCHAR(36) NOT NULL,
    flagged_item TEXT NOT NULL,
    CONSTRAINT fk_flagged_report FOREIGN KEY (report_id) REFERENCES audit_reports(report_id) ON DELETE CASCADE
);

-- Create audit_report_recommended_actions table (one-to-many)
CREATE TABLE IF NOT EXISTS audit_report_recommended_actions (
    id SERIAL PRIMARY KEY,
    report_id VARCHAR(36) NOT NULL,
    recommended_action TEXT NOT NULL,
    CONSTRAINT fk_action_report FOREIGN KEY (report_id) REFERENCES audit_reports(report_id) ON DELETE CASCADE
);

-- Create document_requests table
CREATE TABLE IF NOT EXISTS document_requests (
    request_id VARCHAR(36) PRIMARY KEY,
    return_id VARCHAR(50) NOT NULL,
    auditor_id VARCHAR(50) NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    request_date TIMESTAMP NOT NULL,
    deadline DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    received_date TIMESTAMP,
    tenant_id VARCHAR(50),
    CONSTRAINT chk_doc_type CHECK (document_type IN ('GENERAL_LEDGER', 'BANK_STATEMENTS', 'DEPRECIATION_SCHEDULE', 'CONTRACTS', 'INVOICES', 'RECEIPTS', 'PAYROLL_RECORDS', 'TAX_RETURNS_PRIOR_YEAR', 'OTHER')),
    CONSTRAINT chk_doc_status CHECK (status IN ('PENDING', 'RECEIVED', 'OVERDUE', 'WAIVED'))
);

CREATE INDEX IF NOT EXISTS idx_document_requests_return ON document_requests(return_id);
CREATE INDEX IF NOT EXISTS idx_document_requests_status ON document_requests(status);
CREATE INDEX IF NOT EXISTS idx_document_requests_deadline ON document_requests(deadline);

-- Create document_request_files table (uploaded files)
CREATE TABLE IF NOT EXISTS document_request_files (
    id SERIAL PRIMARY KEY,
    request_id VARCHAR(36) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    upload_date TIMESTAMP NOT NULL,
    CONSTRAINT fk_file_request FOREIGN KEY (request_id) REFERENCES document_requests(request_id) ON DELETE CASCADE
);

-- Add comments for documentation
COMMENT ON TABLE audit_queue IS 'Queue of tax returns awaiting audit review';
COMMENT ON TABLE audit_actions IS 'Log of all audit actions performed by auditors';
COMMENT ON TABLE audit_trail IS 'Immutable audit trail for compliance and legal purposes';
COMMENT ON TABLE audit_reports IS 'Automated audit reports with risk analysis';
COMMENT ON TABLE document_requests IS 'Requests for additional documentation from taxpayers';

-- Grant appropriate permissions (adjust as needed for your deployment)
-- GRANT SELECT, INSERT ON audit_queue TO auditor_role;
-- GRANT SELECT ON audit_trail TO auditor_role;
-- GRANT ALL ON audit_queue TO admin_role;
