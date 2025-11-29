-- V1: Create tax_rules table
-- This table stores all tax rule configurations with temporal support and multi-tenant isolation

CREATE TABLE tax_rules (
    rule_id UUID PRIMARY KEY,
    rule_code VARCHAR(100) NOT NULL,
    rule_name VARCHAR(255) NOT NULL,
    category VARCHAR(50) NOT NULL,
    value_type VARCHAR(50) NOT NULL,
    value JSONB NOT NULL,
    effective_date DATE NOT NULL,
    end_date DATE,
    tenant_id VARCHAR(50) NOT NULL,
    entity_types VARCHAR[] NOT NULL DEFAULT ARRAY['ALL'],
    applies_to TEXT,
    version INTEGER NOT NULL DEFAULT 1,
    previous_version_id UUID REFERENCES tax_rules(rule_id),
    depends_on UUID[],
    approval_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approved_by VARCHAR(100),
    approval_date TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_by VARCHAR(100),
    modified_date TIMESTAMP,
    change_reason TEXT NOT NULL,
    ordinance_reference TEXT,
    
    -- Check constraints for enum values
    CONSTRAINT chk_category CHECK (category IN (
        'TaxRates', 'IncomeInclusion', 'Deductions', 'Penalties', 
        'Filing', 'Allocation', 'Withholding', 'Validation'
    )),
    CONSTRAINT chk_value_type CHECK (value_type IN (
        'NUMBER', 'PERCENTAGE', 'ENUM', 'BOOLEAN', 'FORMULA', 'CONDITIONAL'
    )),
    CONSTRAINT chk_approval_status CHECK (approval_status IN (
        'PENDING', 'APPROVED', 'REJECTED', 'VOIDED'
    )),
    CONSTRAINT chk_date_range CHECK (
        end_date IS NULL OR effective_date <= end_date
    ),
    CONSTRAINT chk_approval_complete CHECK (
        (approval_status = 'APPROVED' AND approved_by IS NOT NULL AND approval_date IS NOT NULL) OR
        (approval_status != 'APPROVED')
    )
);

-- Indexes for performance optimization

-- Primary temporal query: find active rules for tenant at specific date
CREATE INDEX idx_tax_rules_temporal ON tax_rules(tenant_id, effective_date, end_date)
WHERE approval_status = 'APPROVED';

-- Rule code lookup for dependency resolution
CREATE INDEX idx_tax_rules_code ON tax_rules(rule_code, tenant_id, approval_status);

-- Approval workflow queries
CREATE INDEX idx_tax_rules_approval ON tax_rules(approval_status, created_date);

-- Version chain traversal
CREATE INDEX idx_tax_rules_version_chain ON tax_rules(previous_version_id)
WHERE previous_version_id IS NOT NULL;

-- Category and entity type filtering
CREATE INDEX idx_tax_rules_category ON tax_rules(category, tenant_id)
WHERE approval_status = 'APPROVED';

-- Comments for documentation
COMMENT ON TABLE tax_rules IS 'Stores configurable tax rules with temporal effective dating and version control';
COMMENT ON COLUMN tax_rules.rule_code IS 'System code used in tax calculations (e.g., MUNICIPAL_RATE)';
COMMENT ON COLUMN tax_rules.value IS 'JSONB field storing rule value, structure depends on value_type';
COMMENT ON COLUMN tax_rules.effective_date IS 'Date when rule becomes active (inclusive)';
COMMENT ON COLUMN tax_rules.end_date IS 'Date when rule expires (inclusive), NULL = active indefinitely';
COMMENT ON COLUMN tax_rules.tenant_id IS 'Tenant identifier for multi-tenant isolation (e.g., dublin, columbus)';
COMMENT ON COLUMN tax_rules.entity_types IS 'Entity types this rule applies to (e.g., [C-CORP, S-CORP])';
COMMENT ON COLUMN tax_rules.applies_to IS 'Additional targeting criteria (e.g., income > 1000000)';
COMMENT ON COLUMN tax_rules.previous_version_id IS 'Links to previous version, forming version chain';
COMMENT ON COLUMN tax_rules.approval_status IS 'Workflow status: PENDING, APPROVED, REJECTED, VOIDED';
