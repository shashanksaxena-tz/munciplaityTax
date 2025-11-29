-- Generated Forms Table
-- Tracks all generated form instances with versioning and audit trail

CREATE TABLE IF NOT EXISTS generated_forms (
    generated_form_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100) NOT NULL,  -- Always required for generated forms
    template_id UUID NOT NULL REFERENCES form_templates(template_id),
    return_id UUID NOT NULL,  -- Foreign key to tax return (cross-service reference)
    business_id UUID NOT NULL,  -- Business that owns this form
    form_code VARCHAR(20) NOT NULL,
    tax_year INTEGER NOT NULL,
    version INTEGER NOT NULL DEFAULT 1,  -- Increments with each regeneration
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',  -- DRAFT, FINAL, SUBMITTED, AMENDED, SUPERSEDED
    generated_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    generated_by VARCHAR(100) NOT NULL,  -- User ID who generated the form
    pdf_file_path VARCHAR(500) NOT NULL,  -- Location of generated PDF
    xml_file_path VARCHAR(500),  -- XML data file for e-filing (optional)
    is_watermarked BOOLEAN NOT NULL DEFAULT true,  -- Whether PDF has DRAFT watermark
    page_count INTEGER NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    form_data JSONB NOT NULL DEFAULT '{}',  -- Complete form data used for generation
    superseded_by UUID REFERENCES generated_forms(generated_form_id),  -- Link to newer version
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT chk_status CHECK (status IN ('DRAFT', 'FINAL', 'SUBMITTED', 'AMENDED', 'SUPERSEDED')),
    CONSTRAINT chk_version_positive CHECK (version > 0),
    CONSTRAINT chk_page_count_positive CHECK (page_count > 0),
    CONSTRAINT chk_file_size_positive CHECK (file_size_bytes > 0)
);

-- Indexes for generated form queries
CREATE INDEX idx_generated_forms_tenant ON generated_forms(tenant_id);
CREATE INDEX idx_generated_forms_return ON generated_forms(return_id);
CREATE INDEX idx_generated_forms_business ON generated_forms(business_id);
CREATE INDEX idx_generated_forms_code_year ON generated_forms(form_code, tax_year);
CREATE INDEX idx_generated_forms_status ON generated_forms(status);
CREATE INDEX idx_generated_forms_generated_date ON generated_forms(generated_date DESC);
CREATE UNIQUE INDEX idx_generated_forms_unique_version ON generated_forms(return_id, form_code, version);

-- Comments
COMMENT ON TABLE generated_forms IS 'All generated form instances with full audit trail and versioning';
COMMENT ON COLUMN generated_forms.version IS 'Version number incremented with each regeneration';
COMMENT ON COLUMN generated_forms.status IS 'Form lifecycle status: DRAFT (with watermark), FINAL, SUBMITTED, AMENDED, SUPERSEDED';
COMMENT ON COLUMN generated_forms.is_watermarked IS 'True if PDF contains DRAFT - NOT FOR FILING watermark';
COMMENT ON COLUMN generated_forms.form_data IS 'Complete JSON snapshot of data used to generate this form';
COMMENT ON COLUMN generated_forms.superseded_by IS 'Link to newer version if this form was superseded';
