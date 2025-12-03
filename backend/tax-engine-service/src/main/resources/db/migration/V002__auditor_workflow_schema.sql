-- Auditor Workflow Schema
-- Create tables to support tax return auditing workflow
-- Note: In microservices architecture, we store return_id as a reference string
-- instead of a foreign key since submissions table is in a different service

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
    -- Removed FK constraint: CONSTRAINT fk_return FOREIGN KEY (return_id) REFERENCES submissions(id)
    -- In microservices, we maintain referential integrity at the application level
    CONSTRAINT chk_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT chk_status CHECK (status IN ('PENDING', 'IN_REVIEW', 'AWAITING_DOCUMENTATION', 'APPROVED', 'REJECTED', 'AMENDED')),
    CONSTRAINT chk_risk_score CHECK (risk_score >= 0 AND risk_score <= 100)
);

-- Create indexes for audit_queue
CREATE INDEX IF NOT EXISTS idx_audit_queue_return_id ON audit_queue(return_id);
CREATE INDEX IF NOT EXISTS idx_audit_queue_status ON audit_queue(status);
CREATE INDEX IF NOT EXISTS idx_audit_queue_assigned_auditor ON audit_queue(assigned_auditor_id);
CREATE INDEX IF NOT EXISTS idx_audit_queue_tenant ON audit_queue(tenant_id);
CREATE INDEX IF NOT EXISTS idx_audit_queue_submission_date ON audit_queue(submission_date);
