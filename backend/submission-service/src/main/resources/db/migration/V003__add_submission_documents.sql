-- Add submission_documents table for document attachment support
CREATE TABLE IF NOT EXISTS submission_documents (
    id VARCHAR(36) PRIMARY KEY,
    submission_id VARCHAR(255) NOT NULL,
    document_id VARCHAR(255) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    form_type VARCHAR(50),
    file_size BIGINT,
    mime_type VARCHAR(100),
    upload_date TIMESTAMP NOT NULL,
    extraction_result TEXT,
    extraction_confidence DOUBLE PRECISION,
    page_count INTEGER,
    field_provenance TEXT,
    tenant_id VARCHAR(50),
    CONSTRAINT fk_submission FOREIGN KEY (submission_id) REFERENCES submissions(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_submission_documents_submission_id ON submission_documents(submission_id);
CREATE INDEX IF NOT EXISTS idx_submission_documents_tenant_id ON submission_documents(tenant_id);
CREATE INDEX IF NOT EXISTS idx_submission_documents_submission_tenant ON submission_documents(submission_id, tenant_id);

COMMENT ON TABLE submission_documents IS 'Documents attached to tax submissions for auditor review';
COMMENT ON COLUMN submission_documents.document_id IS 'Reference to document in storage/extraction service';
COMMENT ON COLUMN submission_documents.form_type IS 'Type of tax form (W-2, 1099-NEC, 1099-MISC, etc.)';
COMMENT ON COLUMN submission_documents.extraction_result IS 'JSON string of extraction results from AI service';
COMMENT ON COLUMN submission_documents.extraction_confidence IS 'Confidence score from extraction service (0.0-1.0)';
COMMENT ON COLUMN submission_documents.page_count IS 'Number of pages in the document';
COMMENT ON COLUMN submission_documents.field_provenance IS 'JSON string containing field-level extraction provenance data (locations, bounding boxes, confidence scores) for PDF highlighting';
