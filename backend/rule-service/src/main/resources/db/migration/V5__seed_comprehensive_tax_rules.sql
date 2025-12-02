-- V5: Seed comprehensive tax rules for Ohio municipalities
-- NOTE: This is idempotent - it will skip if data already exists from previous V4 deployment
-- If you deployed a previous commit that had V4, this migration will detect and skip seeding

-- ============================================================================
-- SECTION 0: ADD is_system COLUMN FIRST (IF NOT EXISTS)
-- ============================================================================

-- Add is_system column to identify default/system rules
ALTER TABLE tax_rules ADD COLUMN IF NOT EXISTS is_system BOOLEAN DEFAULT FALSE;

-- Add index for filtering system rules (IF NOT EXISTS)
CREATE INDEX IF NOT EXISTS idx_tax_rules_system ON tax_rules(is_system, tenant_id);

-- Add comment for new column
COMMENT ON COLUMN tax_rules.is_system IS 'Indicates if this is a default/system rule (cannot be deleted, only modified)';

-- ============================================================================
-- SECTION 1-10: Seed tax rules using INSERT ON CONFLICT to be idempotent
-- ============================================================================

-- Create a unique constraint on rule_code + tenant_id if not exists
-- This allows us to use ON CONFLICT for idempotent inserts
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_tax_rules_code_tenant'
    ) THEN
        ALTER TABLE tax_rules ADD CONSTRAINT uk_tax_rules_code_tenant UNIQUE (rule_code, tenant_id);
    END IF;
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

-- Dublin Municipal Tax Rate (Individual)
INSERT INTO tax_rules (rule_id, rule_code, rule_name, category, value_type, value, 
    effective_date, tenant_id, entity_types, approval_status, approved_by, approval_date,
    created_by, created_date, change_reason, ordinance_reference, is_system)
VALUES (gen_random_uuid(), 'MUNICIPAL_TAX_RATE', 'Dublin Municipal Tax Rate', 'TAX_RATES', 'PERCENTAGE',
    '{"scalar": 2.0, "unit": "percent"}'::jsonb, '2024-01-01', 'dublin', ARRAY['INDIVIDUAL'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Initial system rule', 'Dublin Income Tax Ordinance 2024', TRUE)
ON CONFLICT (rule_code, tenant_id) DO UPDATE SET is_system = TRUE;

-- Credit Limit Rate (Individual)
INSERT INTO tax_rules (rule_id, rule_code, rule_name, category, value_type, value, 
    effective_date, tenant_id, entity_types, approval_status, approved_by, approval_date,
    created_by, created_date, change_reason, ordinance_reference, is_system)
VALUES (gen_random_uuid(), 'MUNICIPAL_CREDIT_LIMIT_RATE', 'Municipal Credit Limit Rate', 'TAX_RATES', 'PERCENTAGE',
    '{"scalar": 2.0, "unit": "percent"}'::jsonb, '2024-01-01', 'dublin', ARRAY['INDIVIDUAL'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Initial system rule', 'Dublin Income Tax Ordinance 2024', TRUE)
ON CONFLICT (rule_code, tenant_id) DO UPDATE SET is_system = TRUE;

-- Dublin Municipal Tax Rate (Business)
INSERT INTO tax_rules (rule_id, rule_code, rule_name, category, value_type, value, 
    effective_date, tenant_id, entity_types, approval_status, approved_by, approval_date,
    created_by, created_date, change_reason, ordinance_reference, is_system)
VALUES (gen_random_uuid(), 'BUSINESS_MUNICIPAL_TAX_RATE', 'Dublin Business Municipal Tax Rate', 'TAX_RATES', 'PERCENTAGE',
    '{"scalar": 2.0, "unit": "percent"}'::jsonb, '2024-01-01', 'dublin', ARRAY['BUSINESS', 'C-CORP', 'S-CORP', 'LLC', 'PARTNERSHIP'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Initial system rule', 'Dublin Income Tax Ordinance 2024', TRUE)
ON CONFLICT (rule_code, tenant_id) DO UPDATE SET is_system = TRUE;

-- W2 Qualifying Wages Rule
INSERT INTO tax_rules (rule_id, rule_code, rule_name, category, value_type, value, 
    effective_date, tenant_id, entity_types, approval_status, approved_by, approval_date,
    created_by, created_date, change_reason, ordinance_reference, is_system)
VALUES (gen_random_uuid(), 'W2_QUALIFYING_WAGES_RULE', 'W2 Qualifying Wages Selection Rule', 'INCOME_INCLUSION', 'ENUM',
    '{"option": "HIGHEST_OF_ALL", "allowedValues": ["HIGHEST_OF_ALL", "BOX_5_MEDICARE", "BOX_18_LOCAL", "BOX_1_FEDERAL"]}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Dublin uses highest of Box 1, 5, or 18', 'Dublin Income Tax Ordinance 2024', TRUE)
ON CONFLICT (rule_code, tenant_id) DO UPDATE SET is_system = TRUE;

-- Schedule C Income
INSERT INTO tax_rules (rule_id, rule_code, rule_name, category, value_type, value, 
    effective_date, tenant_id, entity_types, approval_status, approved_by, approval_date,
    created_by, created_date, change_reason, ordinance_reference, is_system)
VALUES (gen_random_uuid(), 'INCLUDE_SCHEDULE_C', 'Include Schedule C Income', 'INCOME_INCLUSION', 'BOOLEAN',
    '{"flag": true}'::jsonb, '2024-01-01', 'dublin', ARRAY['INDIVIDUAL'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Self-employment income is taxable', 'Ohio Rev. Code 718.01', TRUE)
ON CONFLICT (rule_code, tenant_id) DO UPDATE SET is_system = TRUE;

-- Schedule E Income
INSERT INTO tax_rules (rule_id, rule_code, rule_name, category, value_type, value, 
    effective_date, tenant_id, entity_types, approval_status, approved_by, approval_date,
    created_by, created_date, change_reason, ordinance_reference, is_system)
VALUES (gen_random_uuid(), 'INCLUDE_SCHEDULE_E', 'Include Schedule E Income', 'INCOME_INCLUSION', 'BOOLEAN',
    '{"flag": true}'::jsonb, '2024-01-01', 'dublin', ARRAY['INDIVIDUAL'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Rental income is taxable', 'Ohio Rev. Code 718.01', TRUE)
ON CONFLICT (rule_code, tenant_id) DO UPDATE SET is_system = TRUE;

-- Late Filing Penalty
INSERT INTO tax_rules (rule_id, rule_code, rule_name, category, value_type, value, 
    effective_date, tenant_id, entity_types, approval_status, approved_by, approval_date,
    created_by, created_date, change_reason, ordinance_reference, is_system)
VALUES (gen_random_uuid(), 'PENALTY_RATE_LATE_FILING', 'Late Filing Penalty Rate', 'PENALTIES', 'NUMBER',
    '{"scalar": 25.00, "unit": "dollars"}'::jsonb, '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Standard late filing penalty', 'Dublin Income Tax Ordinance 2024', TRUE)
ON CONFLICT (rule_code, tenant_id) DO UPDATE SET is_system = TRUE;

-- Underpayment Penalty
INSERT INTO tax_rules (rule_id, rule_code, rule_name, category, value_type, value, 
    effective_date, tenant_id, entity_types, approval_status, approved_by, approval_date,
    created_by, created_date, change_reason, ordinance_reference, is_system)
VALUES (gen_random_uuid(), 'PENALTY_RATE_UNDERPAYMENT', 'Underpayment Penalty Rate', 'PENALTIES', 'PERCENTAGE',
    '{"scalar": 15.0, "unit": "percent"}'::jsonb, '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Standard underpayment penalty', 'Ohio Rev. Code 718.27', TRUE)
ON CONFLICT (rule_code, tenant_id) DO UPDATE SET is_system = TRUE;

-- Interest Rate
INSERT INTO tax_rules (rule_id, rule_code, rule_name, category, value_type, value, 
    effective_date, tenant_id, entity_types, approval_status, approved_by, approval_date,
    created_by, created_date, change_reason, ordinance_reference, is_system)
VALUES (gen_random_uuid(), 'INTEREST_RATE_ANNUAL', 'Annual Interest Rate', 'PENALTIES', 'PERCENTAGE',
    '{"scalar": 7.0, "unit": "percent"}'::jsonb, '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Standard interest rate', 'Ohio Rev. Code 718.28', TRUE)
ON CONFLICT (rule_code, tenant_id) DO UPDATE SET is_system = TRUE;

-- Safe Harbor
INSERT INTO tax_rules (rule_id, rule_code, rule_name, category, value_type, value, 
    effective_date, tenant_id, entity_types, approval_status, approved_by, approval_date,
    created_by, created_date, change_reason, ordinance_reference, is_system)
VALUES (gen_random_uuid(), 'SAFE_HARBOR_PERCENT', 'Safe Harbor Percentage', 'PENALTIES', 'PERCENTAGE',
    '{"scalar": 90.0, "unit": "percent"}'::jsonb, '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Standard safe harbor', 'Ohio Rev. Code 718.08', TRUE)
ON CONFLICT (rule_code, tenant_id) DO UPDATE SET is_system = TRUE;

-- NOL Offset Cap
INSERT INTO tax_rules (rule_id, rule_code, rule_name, category, value_type, value, 
    effective_date, tenant_id, entity_types, approval_status, approved_by, approval_date,
    created_by, created_date, change_reason, ordinance_reference, is_system)
VALUES (gen_random_uuid(), 'NOL_OFFSET_CAP_PERCENT', 'NOL Offset Cap Percentage', 'DEDUCTIONS', 'PERCENTAGE',
    '{"scalar": 50.0, "unit": "percent"}'::jsonb, '2024-01-01', 'dublin', ARRAY['BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), '50% cap per Ohio law', 'Ohio Rev. Code 718.01', TRUE)
ON CONFLICT (rule_code, tenant_id) DO UPDATE SET is_system = TRUE;

-- Columbus Locality Rate
INSERT INTO tax_rules (rule_id, rule_code, rule_name, category, value_type, value, 
    effective_date, tenant_id, entity_types, approval_status, approved_by, approval_date,
    created_by, created_date, change_reason, ordinance_reference, is_system)
VALUES (gen_random_uuid(), 'LOCALITY_RATE_COLUMBUS', 'Columbus Tax Rate', 'TAX_RATES', 'PERCENTAGE',
    '{"scalar": 2.5, "unit": "percent", "municipalityCode": "COLUMBUS", "municipalityName": "Columbus"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Columbus municipal rate', 'Columbus Income Tax Ordinance', TRUE)
ON CONFLICT (rule_code, tenant_id) DO UPDATE SET is_system = TRUE;

-- Cleveland Locality Rate
INSERT INTO tax_rules (rule_id, rule_code, rule_name, category, value_type, value, 
    effective_date, tenant_id, entity_types, approval_status, approved_by, approval_date,
    created_by, created_date, change_reason, ordinance_reference, is_system)
VALUES (gen_random_uuid(), 'LOCALITY_RATE_CLEVELAND', 'Cleveland Tax Rate', 'TAX_RATES', 'PERCENTAGE',
    '{"scalar": 2.5, "unit": "percent", "municipalityCode": "CLEVELAND", "municipalityName": "Cleveland"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Cleveland municipal rate', 'Cleveland Income Tax Ordinance', TRUE)
ON CONFLICT (rule_code, tenant_id) DO UPDATE SET is_system = TRUE;

-- Cincinnati Locality Rate
INSERT INTO tax_rules (rule_id, rule_code, rule_name, category, value_type, value, 
    effective_date, tenant_id, entity_types, approval_status, approved_by, approval_date,
    created_by, created_date, change_reason, ordinance_reference, is_system)
VALUES (gen_random_uuid(), 'LOCALITY_RATE_CINCINNATI', 'Cincinnati Tax Rate', 'TAX_RATES', 'PERCENTAGE',
    '{"scalar": 2.1, "unit": "percent", "municipalityCode": "CINCINNATI", "municipalityName": "Cincinnati"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Cincinnati municipal rate', 'Cincinnati Income Tax Ordinance', TRUE)
ON CONFLICT (rule_code, tenant_id) DO UPDATE SET is_system = TRUE;

-- Toledo Locality Rate
INSERT INTO tax_rules (rule_id, rule_code, rule_name, category, value_type, value, 
    effective_date, tenant_id, entity_types, approval_status, approved_by, approval_date,
    created_by, created_date, change_reason, ordinance_reference, is_system)
VALUES (gen_random_uuid(), 'LOCALITY_RATE_TOLEDO', 'Toledo Tax Rate', 'TAX_RATES', 'PERCENTAGE',
    '{"scalar": 2.25, "unit": "percent", "municipalityCode": "TOLEDO", "municipalityName": "Toledo"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Toledo municipal rate', 'Toledo Income Tax Ordinance', TRUE)
ON CONFLICT (rule_code, tenant_id) DO UPDATE SET is_system = TRUE;

-- Akron Locality Rate
INSERT INTO tax_rules (rule_id, rule_code, rule_name, category, value_type, value, 
    effective_date, tenant_id, entity_types, approval_status, approved_by, approval_date,
    created_by, created_date, change_reason, ordinance_reference, is_system)
VALUES (gen_random_uuid(), 'LOCALITY_RATE_AKRON', 'Akron Tax Rate', 'TAX_RATES', 'PERCENTAGE',
    '{"scalar": 2.5, "unit": "percent", "municipalityCode": "AKRON", "municipalityName": "Akron"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Akron municipal rate', 'Akron Income Tax Ordinance', TRUE)
ON CONFLICT (rule_code, tenant_id) DO UPDATE SET is_system = TRUE;

-- Dayton Locality Rate
INSERT INTO tax_rules (rule_id, rule_code, rule_name, category, value_type, value, 
    effective_date, tenant_id, entity_types, approval_status, approved_by, approval_date,
    created_by, created_date, change_reason, ordinance_reference, is_system)
VALUES (gen_random_uuid(), 'LOCALITY_RATE_DAYTON', 'Dayton Tax Rate', 'TAX_RATES', 'PERCENTAGE',
    '{"scalar": 2.5, "unit": "percent", "municipalityCode": "DAYTON", "municipalityName": "Dayton"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Dayton municipal rate', 'Dayton Income Tax Ordinance', TRUE)
ON CONFLICT (rule_code, tenant_id) DO UPDATE SET is_system = TRUE;
