-- Form Templates Table
-- Stores PDF form template metadata, field mappings, and validation rules

CREATE TABLE IF NOT EXISTS form_templates (
    template_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100),  -- NULL for shared templates across tenants
    form_code VARCHAR(20) NOT NULL,  -- "27", "27-EXT", "27-ES", "27-NOL", etc.
    form_name VARCHAR(255) NOT NULL,  -- "Extension Request", "Estimated Tax Voucher", etc.
    template_file_path VARCHAR(500) NOT NULL,  -- Path to PDF template in storage
    revision_date DATE NOT NULL,  -- Form version date (e.g., 2024-01-01 for "Rev. 01/2024")
    applicable_years INTEGER[] NOT NULL,  -- Array of years this template is valid for [2024, 2025]
    field_mappings JSONB NOT NULL DEFAULT '{}',  -- Map database fields to PDF form fields
    validation_rules JSONB NOT NULL DEFAULT '{}',  -- Required fields, formats, cross-field validations
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    CONSTRAINT uk_form_template UNIQUE (tenant_id, form_code, revision_date)
);

-- Indexes for form template queries
CREATE INDEX idx_form_templates_tenant ON form_templates(tenant_id) WHERE tenant_id IS NOT NULL;
CREATE INDEX idx_form_templates_code_year ON form_templates(form_code, applicable_years);
CREATE INDEX idx_form_templates_active ON form_templates(is_active) WHERE is_active = true;

-- Comments
COMMENT ON TABLE form_templates IS 'PDF form template metadata with field mappings and validation rules';
COMMENT ON COLUMN form_templates.tenant_id IS 'Tenant ID for customized templates, NULL for shared templates';
COMMENT ON COLUMN form_templates.form_code IS 'Unique form identifier: 27, 27-EXT, 27-ES, 27-NOL, 27-W1, 27-Y, 27-X, 27-PA';
COMMENT ON COLUMN form_templates.field_mappings IS 'JSON mapping database field names to PDF AcroForm field names';
COMMENT ON COLUMN form_templates.validation_rules IS 'JSON validation rules: required fields, formats, calculations';
COMMENT ON COLUMN form_templates.applicable_years IS 'Array of tax years this template can be used for';
