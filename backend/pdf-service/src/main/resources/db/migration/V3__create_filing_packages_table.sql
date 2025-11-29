-- Filing Packages Table
-- Manages multi-form filing packages with table of contents and submission tracking

CREATE TABLE IF NOT EXISTS filing_packages (
    package_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100) NOT NULL,
    return_id UUID NOT NULL,  -- Foreign key to tax return
    business_id UUID NOT NULL,
    tax_year INTEGER NOT NULL,
    package_type VARCHAR(20) NOT NULL DEFAULT 'ORIGINAL',  -- ORIGINAL, AMENDED, EXTENSION
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    total_pages INTEGER NOT NULL,
    package_pdf_path VARCHAR(500) NOT NULL,  -- Combined PDF file path
    table_of_contents JSONB NOT NULL DEFAULT '{}',  -- JSON: {formName: pageNumber}
    submission_date TIMESTAMP,
    confirmation_number VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',  -- DRAFT, FINAL, SUBMITTED
    CONSTRAINT chk_package_type CHECK (package_type IN ('ORIGINAL', 'AMENDED', 'EXTENSION')),
    CONSTRAINT chk_package_status CHECK (status IN ('DRAFT', 'FINAL', 'SUBMITTED')),
    CONSTRAINT chk_total_pages_positive CHECK (total_pages > 0)
);

-- Filing Package Forms Junction Table (many-to-many)
CREATE TABLE IF NOT EXISTS filing_package_forms (
    package_id UUID NOT NULL REFERENCES filing_packages(package_id) ON DELETE CASCADE,
    generated_form_id UUID NOT NULL REFERENCES generated_forms(generated_form_id),
    sequence_order INTEGER NOT NULL,  -- Order of forms in package
    start_page INTEGER NOT NULL,  -- Starting page number in combined PDF
    end_page INTEGER NOT NULL,  -- Ending page number in combined PDF
    PRIMARY KEY (package_id, generated_form_id),
    CONSTRAINT chk_page_range CHECK (end_page >= start_page)
);

-- Indexes
CREATE INDEX idx_filing_packages_tenant ON filing_packages(tenant_id);
CREATE INDEX idx_filing_packages_return ON filing_packages(return_id);
CREATE INDEX idx_filing_packages_business ON filing_packages(business_id);
CREATE INDEX idx_filing_packages_year ON filing_packages(tax_year);
CREATE INDEX idx_filing_packages_created ON filing_packages(created_date DESC);
CREATE INDEX idx_filing_package_forms_package ON filing_package_forms(package_id);
CREATE INDEX idx_filing_package_forms_form ON filing_package_forms(generated_form_id);

-- Comments
COMMENT ON TABLE filing_packages IS 'Multi-form filing packages with combined PDF and table of contents';
COMMENT ON COLUMN filing_packages.package_type IS 'Package type: ORIGINAL, AMENDED, or EXTENSION';
COMMENT ON COLUMN filing_packages.table_of_contents IS 'JSON mapping form names to page numbers for navigation';
COMMENT ON COLUMN filing_packages.confirmation_number IS 'E-filing submission confirmation number';
COMMENT ON TABLE filing_package_forms IS 'Junction table linking packages to individual forms';
COMMENT ON COLUMN filing_package_forms.sequence_order IS 'Display order of forms in package (1, 2, 3, ...)';
