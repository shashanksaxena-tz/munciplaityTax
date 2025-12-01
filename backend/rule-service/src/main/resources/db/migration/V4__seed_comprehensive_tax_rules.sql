-- V4: Seed comprehensive tax rules for Ohio municipalities
-- This migration creates a complete set of municipal tax rules covering:
-- 1. Tax Rates - Municipal tax rates for various Ohio municipalities
-- 2. W2 Wage Selection Rules - Which W2 box to use for qualifying wages
-- 3. Income Inclusion Rules - What types of income are taxable
-- 4. Penalty Rules - Late filing, underpayment penalties
-- 5. Business Rules - NOL, allocation, apportionment
-- 6. Locality/Reciprocity Rules - Schedule Y credits for each municipality

-- ============================================================================
-- SECTION 0: ADD is_system COLUMN FIRST
-- ============================================================================

-- Add is_system column to identify default/system rules
ALTER TABLE tax_rules ADD COLUMN IF NOT EXISTS is_system BOOLEAN DEFAULT FALSE;

-- Add index for filtering system rules
CREATE INDEX IF NOT EXISTS idx_tax_rules_system ON tax_rules(is_system, tenant_id);

-- Add comment for new column
COMMENT ON COLUMN tax_rules.is_system IS 'Indicates if this is a default/system rule (cannot be deleted, only modified)';

-- ============================================================================
-- SECTION 1: INDIVIDUAL TAX RATE RULES
-- ============================================================================

-- Dublin Municipal Tax Rate (Individual)
INSERT INTO tax_rules (rule_id, rule_code, rule_name, category, value_type, value, 
    effective_date, tenant_id, entity_types, approval_status, approved_by, approval_date,
    created_by, created_date, change_reason, ordinance_reference, is_system)
VALUES 
(gen_random_uuid(), 'MUNICIPAL_TAX_RATE', 'Dublin Municipal Tax Rate', 'TAX_RATES', 'PERCENTAGE',
    '{"scalar": 2.0, "unit": "percent"}'::jsonb, '2024-01-01', 'dublin', ARRAY['INDIVIDUAL'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Initial system rule', 'Dublin Income Tax Ordinance 2024', TRUE),

-- Credit Limit Rate (Individual)
(gen_random_uuid(), 'MUNICIPAL_CREDIT_LIMIT_RATE', 'Municipal Credit Limit Rate', 'TAX_RATES', 'PERCENTAGE',
    '{"scalar": 2.0, "unit": "percent"}'::jsonb, '2024-01-01', 'dublin', ARRAY['INDIVIDUAL'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Initial system rule', 'Dublin Income Tax Ordinance 2024', TRUE),

-- ============================================================================
-- SECTION 2: BUSINESS TAX RATE RULES  
-- ============================================================================

-- Dublin Municipal Tax Rate (Business)
(gen_random_uuid(), 'BUSINESS_MUNICIPAL_TAX_RATE', 'Dublin Business Municipal Tax Rate', 'TAX_RATES', 'PERCENTAGE',
    '{"scalar": 2.0, "unit": "percent"}'::jsonb, '2024-01-01', 'dublin', ARRAY['BUSINESS', 'C-CORP', 'S-CORP', 'LLC', 'PARTNERSHIP'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Initial system rule', 'Dublin Income Tax Ordinance 2024'),

-- Minimum Tax (Business)
(gen_random_uuid(), 'MINIMUM_TAX', 'Minimum Business Tax', 'TAX_RATES', 'NUMBER',
    '{"scalar": 0}'::jsonb, '2024-01-01', 'dublin', ARRAY['BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Initial system rule', 'Dublin Income Tax Ordinance 2024'),

-- ============================================================================
-- SECTION 3: W2 QUALIFYING WAGES RULES
-- ============================================================================

-- Default W2 Wage Selection Rule
(gen_random_uuid(), 'W2_QUALIFYING_WAGES_RULE', 'W2 Qualifying Wages Selection Rule', 'INCOME_INCLUSION', 'ENUM',
    '{"option": "HIGHEST_OF_ALL", "allowedValues": ["HIGHEST_OF_ALL", "BOX_5_MEDICARE", "BOX_18_LOCAL", "BOX_1_FEDERAL"], 
      "description": "Determines which W-2 box determines the municipal tax base"}'::jsonb, 
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 
    'Dublin uses highest of Box 1, 5, or 18 for maximum tax base', 'Dublin Income Tax Ordinance 2024'),

-- ============================================================================
-- SECTION 4: INCOME INCLUSION RULES (Individual)
-- ============================================================================

-- Schedule C Income (Self-Employment)
(gen_random_uuid(), 'INCLUDE_SCHEDULE_C', 'Include Schedule C Income', 'INCOME_INCLUSION', 'BOOLEAN',
    '{"flag": true, "description": "Schedule C business income from self-employment"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Self-employment income is taxable', 'Ohio Rev. Code 718.01'),

-- Schedule E Income (Rentals)
(gen_random_uuid(), 'INCLUDE_SCHEDULE_E', 'Include Schedule E Income', 'INCOME_INCLUSION', 'BOOLEAN',
    '{"flag": true, "description": "Schedule E rental and royalty income"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Rental income is taxable', 'Ohio Rev. Code 718.01'),

-- Schedule F Income (Farm)
(gen_random_uuid(), 'INCLUDE_SCHEDULE_F', 'Include Schedule F Income', 'INCOME_INCLUSION', 'BOOLEAN',
    '{"flag": true, "description": "Schedule F farming income"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Farm income is taxable', 'Ohio Rev. Code 718.01'),

-- W-2G (Gambling)
(gen_random_uuid(), 'INCLUDE_W2G', 'Include W-2G Gambling Income', 'INCOME_INCLUSION', 'BOOLEAN',
    '{"flag": true, "description": "W-2G gambling and lottery winnings"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Gambling winnings are taxable', 'Ohio Rev. Code 718.01'),

-- Form 1099-MISC/NEC
(gen_random_uuid(), 'INCLUDE_1099', 'Include 1099 Income', 'INCOME_INCLUSION', 'BOOLEAN',
    '{"flag": true, "description": "1099-MISC and 1099-NEC contractor income"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL'],
    'APPROVED', 'system', NOW(), 'system', NOW(), '1099 income is taxable', 'Ohio Rev. Code 718.01'),

-- ============================================================================
-- SECTION 5: PENALTY RULES
-- ============================================================================

-- Late Filing Penalty Rate
(gen_random_uuid(), 'PENALTY_RATE_LATE_FILING', 'Late Filing Penalty Rate', 'PENALTIES', 'NUMBER',
    '{"scalar": 25.00, "unit": "dollars", "description": "Fixed penalty for late filing"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Standard late filing penalty', 'Dublin Income Tax Ordinance 2024'),

-- Underpayment Penalty Rate
(gen_random_uuid(), 'PENALTY_RATE_UNDERPAYMENT', 'Underpayment Penalty Rate', 'PENALTIES', 'PERCENTAGE',
    '{"scalar": 15.0, "unit": "percent", "description": "Penalty rate for underpayment of estimated tax"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Standard underpayment penalty', 'Ohio Rev. Code 718.27'),

-- Interest Rate
(gen_random_uuid(), 'INTEREST_RATE_ANNUAL', 'Annual Interest Rate', 'PENALTIES', 'PERCENTAGE',
    '{"scalar": 7.0, "unit": "percent", "description": "Annual interest rate on unpaid taxes"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Standard interest rate', 'Ohio Rev. Code 718.28'),

-- Safe Harbor Percentage
(gen_random_uuid(), 'SAFE_HARBOR_PERCENT', 'Safe Harbor Percentage', 'PENALTIES', 'PERCENTAGE',
    '{"scalar": 90.0, "unit": "percent", "description": "Minimum percentage of tax that must be paid to avoid underpayment penalty"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Standard safe harbor', 'Ohio Rev. Code 718.08'),

-- ============================================================================
-- SECTION 6: BUSINESS ALLOCATION/APPORTIONMENT RULES
-- ============================================================================

-- Allocation Method
(gen_random_uuid(), 'ALLOCATION_METHOD', 'Business Allocation Method', 'ALLOCATION', 'ENUM',
    '{"option": "3_FACTOR", "allowedValues": ["3_FACTOR", "SINGLE_SALES", "DOUBLE_WEIGHTED_SALES"], 
      "description": "Method for allocating multi-state business income"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Standard 3-factor allocation', 'Ohio Rev. Code 718.02'),

-- Sales Factor Weight
(gen_random_uuid(), 'ALLOCATION_SALES_FACTOR_WEIGHT', 'Sales Factor Weight', 'ALLOCATION', 'NUMBER',
    '{"scalar": 1, "description": "Weight of sales factor in allocation formula"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Standard equal weighting', 'Ohio Rev. Code 718.02'),

-- ============================================================================
-- SECTION 7: NOL (NET OPERATING LOSS) RULES
-- ============================================================================

-- Enable NOL Carryforward
(gen_random_uuid(), 'ENABLE_NOL', 'Enable NOL Carryforward', 'DEDUCTIONS', 'BOOLEAN',
    '{"flag": true, "description": "Allow Net Operating Loss carryforward deduction"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'NOL allowed per Ohio law', 'Ohio Rev. Code 718.01'),

-- NOL Offset Cap
(gen_random_uuid(), 'NOL_OFFSET_CAP_PERCENT', 'NOL Offset Cap Percentage', 'DEDUCTIONS', 'PERCENTAGE',
    '{"scalar": 50.0, "unit": "percent", "description": "Maximum percentage of taxable income that can be offset by NOL"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), '50% cap per Ohio law', 'Ohio Rev. Code 718.01'),

-- Intangible Expense Rate
(gen_random_uuid(), 'INTANGIBLE_EXPENSE_RATE', 'Intangible Expense Deduction Rate', 'DEDUCTIONS', 'PERCENTAGE',
    '{"scalar": 5.0, "unit": "percent", "description": "Allowable deduction rate for intangible income expenses"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Intangible expense add-back limit', 'Ohio Rev. Code 718.01'),

-- ============================================================================
-- SECTION 8: ROUNDING AND PRECISION RULES
-- ============================================================================

-- Enable Rounding
(gen_random_uuid(), 'ENABLE_ROUNDING', 'Enable Tax Rounding', 'VALIDATION', 'BOOLEAN',
    '{"flag": true, "description": "Round final tax due to whole dollars"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Standard rounding practice', 'Dublin Income Tax Ordinance 2024'),

-- ============================================================================
-- SECTION 9: OHIO MUNICIPALITY TAX RATES (for Schedule Y reciprocity)
-- These are used for calculating credits for taxes paid to other municipalities
-- ============================================================================

-- Columbus
(gen_random_uuid(), 'LOCALITY_RATE_COLUMBUS', 'Columbus Tax Rate', 'TAX_RATES', 'PERCENTAGE',
    '{"scalar": 2.5, "unit": "percent", "municipalityCode": "COLUMBUS", "municipalityName": "Columbus", "county": "Franklin", "state": "OH"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Columbus municipal rate', 'Columbus Income Tax Ordinance'),

-- Cleveland
(gen_random_uuid(), 'LOCALITY_RATE_CLEVELAND', 'Cleveland Tax Rate', 'TAX_RATES', 'PERCENTAGE',
    '{"scalar": 2.5, "unit": "percent", "municipalityCode": "CLEVELAND", "municipalityName": "Cleveland", "county": "Cuyahoga", "state": "OH"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Cleveland municipal rate', 'Cleveland Income Tax Ordinance'),

-- Cincinnati
(gen_random_uuid(), 'LOCALITY_RATE_CINCINNATI', 'Cincinnati Tax Rate', 'TAX_RATES', 'PERCENTAGE',
    '{"scalar": 2.1, "unit": "percent", "municipalityCode": "CINCINNATI", "municipalityName": "Cincinnati", "county": "Hamilton", "state": "OH"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Cincinnati municipal rate', 'Cincinnati Income Tax Ordinance'),

-- Toledo
(gen_random_uuid(), 'LOCALITY_RATE_TOLEDO', 'Toledo Tax Rate', 'TAX_RATES', 'PERCENTAGE',
    '{"scalar": 2.25, "unit": "percent", "municipalityCode": "TOLEDO", "municipalityName": "Toledo", "county": "Lucas", "state": "OH"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Toledo municipal rate', 'Toledo Income Tax Ordinance'),

-- Akron
(gen_random_uuid(), 'LOCALITY_RATE_AKRON', 'Akron Tax Rate', 'TAX_RATES', 'PERCENTAGE',
    '{"scalar": 2.5, "unit": "percent", "municipalityCode": "AKRON", "municipalityName": "Akron", "county": "Summit", "state": "OH"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Akron municipal rate', 'Akron Income Tax Ordinance'),

-- Dayton
(gen_random_uuid(), 'LOCALITY_RATE_DAYTON', 'Dayton Tax Rate', 'TAX_RATES', 'PERCENTAGE',
    '{"scalar": 2.5, "unit": "percent", "municipalityCode": "DAYTON", "municipalityName": "Dayton", "county": "Montgomery", "state": "OH"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Dayton municipal rate', 'Dayton Income Tax Ordinance'),

-- Westerville
(gen_random_uuid(), 'LOCALITY_RATE_WESTERVILLE', 'Westerville Tax Rate', 'TAX_RATES', 'PERCENTAGE',
    '{"scalar": 2.0, "unit": "percent", "municipalityCode": "WESTERVILLE", "municipalityName": "Westerville", "county": "Franklin", "state": "OH"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Westerville municipal rate', 'Westerville Income Tax Ordinance'),

-- Hilliard
(gen_random_uuid(), 'LOCALITY_RATE_HILLIARD', 'Hilliard Tax Rate', 'TAX_RATES', 'PERCENTAGE',
    '{"scalar": 2.5, "unit": "percent", "municipalityCode": "HILLIARD", "municipalityName": "Hilliard", "county": "Franklin", "state": "OH"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Hilliard municipal rate', 'Hilliard Income Tax Ordinance'),

-- Upper Arlington
(gen_random_uuid(), 'LOCALITY_RATE_UPPER_ARLINGTON', 'Upper Arlington Tax Rate', 'TAX_RATES', 'PERCENTAGE',
    '{"scalar": 2.5, "unit": "percent", "municipalityCode": "UPPER_ARLINGTON", "municipalityName": "Upper Arlington", "county": "Franklin", "state": "OH"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Upper Arlington municipal rate', 'Upper Arlington Income Tax Ordinance'),

-- Grandview Heights
(gen_random_uuid(), 'LOCALITY_RATE_GRANDVIEW_HEIGHTS', 'Grandview Heights Tax Rate', 'TAX_RATES', 'PERCENTAGE',
    '{"scalar": 2.5, "unit": "percent", "municipalityCode": "GRANDVIEW_HEIGHTS", "municipalityName": "Grandview Heights", "county": "Franklin", "state": "OH"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Grandview Heights municipal rate', 'Grandview Heights Income Tax Ordinance'),

-- Bexley
(gen_random_uuid(), 'LOCALITY_RATE_BEXLEY', 'Bexley Tax Rate', 'TAX_RATES', 'PERCENTAGE',
    '{"scalar": 2.5, "unit": "percent", "municipalityCode": "BEXLEY", "municipalityName": "Bexley", "county": "Franklin", "state": "OH"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Bexley municipal rate', 'Bexley Income Tax Ordinance'),

-- Worthington
(gen_random_uuid(), 'LOCALITY_RATE_WORTHINGTON', 'Worthington Tax Rate', 'TAX_RATES', 'PERCENTAGE',
    '{"scalar": 2.5, "unit": "percent", "municipalityCode": "WORTHINGTON", "municipalityName": "Worthington", "county": "Franklin", "state": "OH"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Worthington municipal rate', 'Worthington Income Tax Ordinance'),

-- Gahanna
(gen_random_uuid(), 'LOCALITY_RATE_GAHANNA', 'Gahanna Tax Rate', 'TAX_RATES', 'PERCENTAGE',
    '{"scalar": 2.5, "unit": "percent", "municipalityCode": "GAHANNA", "municipalityName": "Gahanna", "county": "Franklin", "state": "OH"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Gahanna municipal rate', 'Gahanna Income Tax Ordinance'),

-- Reynoldsburg
(gen_random_uuid(), 'LOCALITY_RATE_REYNOLDSBURG', 'Reynoldsburg Tax Rate', 'TAX_RATES', 'PERCENTAGE',
    '{"scalar": 2.5, "unit": "percent", "municipalityCode": "REYNOLDSBURG", "municipalityName": "Reynoldsburg", "county": "Franklin", "state": "OH"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Reynoldsburg municipal rate', 'Reynoldsburg Income Tax Ordinance'),

-- Grove City
(gen_random_uuid(), 'LOCALITY_RATE_GROVE_CITY', 'Grove City Tax Rate', 'TAX_RATES', 'PERCENTAGE',
    '{"scalar": 2.0, "unit": "percent", "municipalityCode": "GROVE_CITY", "municipalityName": "Grove City", "county": "Franklin", "state": "OH"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Grove City municipal rate', 'Grove City Income Tax Ordinance'),

-- Pickerington
(gen_random_uuid(), 'LOCALITY_RATE_PICKERINGTON', 'Pickerington Tax Rate', 'TAX_RATES', 'PERCENTAGE',
    '{"scalar": 1.5, "unit": "percent", "municipalityCode": "PICKERINGTON", "municipalityName": "Pickerington", "county": "Fairfield", "state": "OH"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Pickerington municipal rate', 'Pickerington Income Tax Ordinance'),

-- New Albany
(gen_random_uuid(), 'LOCALITY_RATE_NEW_ALBANY', 'New Albany Tax Rate', 'TAX_RATES', 'PERCENTAGE',
    '{"scalar": 2.0, "unit": "percent", "municipalityCode": "NEW_ALBANY", "municipalityName": "New Albany", "county": "Franklin", "state": "OH"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'New Albany municipal rate', 'New Albany Income Tax Ordinance'),

-- Delaware
(gen_random_uuid(), 'LOCALITY_RATE_DELAWARE', 'Delaware Tax Rate', 'TAX_RATES', 'PERCENTAGE',
    '{"scalar": 1.85, "unit": "percent", "municipalityCode": "DELAWARE", "municipalityName": "Delaware", "county": "Delaware", "state": "OH"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Delaware municipal rate', 'Delaware Income Tax Ordinance'),

-- Powell
(gen_random_uuid(), 'LOCALITY_RATE_POWELL', 'Powell Tax Rate', 'TAX_RATES', 'PERCENTAGE',
    '{"scalar": 1.0, "unit": "percent", "municipalityCode": "POWELL", "municipalityName": "Powell", "county": "Delaware", "state": "OH"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Powell municipal rate', 'Powell Income Tax Ordinance'),

-- Marysville
(gen_random_uuid(), 'LOCALITY_RATE_MARYSVILLE', 'Marysville Tax Rate', 'TAX_RATES', 'PERCENTAGE',
    '{"scalar": 1.5, "unit": "percent", "municipalityCode": "MARYSVILLE", "municipalityName": "Marysville", "county": "Union", "state": "OH"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Marysville municipal rate', 'Marysville Income Tax Ordinance'),

-- Springfield
(gen_random_uuid(), 'LOCALITY_RATE_SPRINGFIELD', 'Springfield Tax Rate', 'TAX_RATES', 'PERCENTAGE',
    '{"scalar": 2.4, "unit": "percent", "municipalityCode": "SPRINGFIELD", "municipalityName": "Springfield", "county": "Clark", "state": "OH"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Springfield municipal rate', 'Springfield Income Tax Ordinance'),

-- Youngstown
(gen_random_uuid(), 'LOCALITY_RATE_YOUNGSTOWN', 'Youngstown Tax Rate', 'TAX_RATES', 'PERCENTAGE',
    '{"scalar": 2.75, "unit": "percent", "municipalityCode": "YOUNGSTOWN", "municipalityName": "Youngstown", "county": "Mahoning", "state": "OH"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Youngstown municipal rate', 'Youngstown Income Tax Ordinance'),

-- Canton
(gen_random_uuid(), 'LOCALITY_RATE_CANTON', 'Canton Tax Rate', 'TAX_RATES', 'PERCENTAGE',
    '{"scalar": 2.5, "unit": "percent", "municipalityCode": "CANTON", "municipalityName": "Canton", "county": "Stark", "state": "OH"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Canton municipal rate', 'Canton Income Tax Ordinance'),

-- Parma
(gen_random_uuid(), 'LOCALITY_RATE_PARMA', 'Parma Tax Rate', 'TAX_RATES', 'PERCENTAGE',
    '{"scalar": 3.0, "unit": "percent", "municipalityCode": "PARMA", "municipalityName": "Parma", "county": "Cuyahoga", "state": "OH"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Parma municipal rate', 'Parma Income Tax Ordinance'),

-- Lorain
(gen_random_uuid(), 'LOCALITY_RATE_LORAIN', 'Lorain Tax Rate', 'TAX_RATES', 'PERCENTAGE',
    '{"scalar": 2.5, "unit": "percent", "municipalityCode": "LORAIN", "municipalityName": "Lorain", "county": "Lorain", "state": "OH"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Lorain municipal rate', 'Lorain Income Tax Ordinance'),

-- Elyria
(gen_random_uuid(), 'LOCALITY_RATE_ELYRIA', 'Elyria Tax Rate', 'TAX_RATES', 'PERCENTAGE',
    '{"scalar": 2.25, "unit": "percent", "municipalityCode": "ELYRIA", "municipalityName": "Elyria", "county": "Lorain", "state": "OH"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Elyria municipal rate', 'Elyria Income Tax Ordinance'),

-- Mansfield
(gen_random_uuid(), 'LOCALITY_RATE_MANSFIELD', 'Mansfield Tax Rate', 'TAX_RATES', 'PERCENTAGE',
    '{"scalar": 2.0, "unit": "percent", "municipalityCode": "MANSFIELD", "municipalityName": "Mansfield", "county": "Richland", "state": "OH"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Mansfield municipal rate', 'Mansfield Income Tax Ordinance'),

-- Newark
(gen_random_uuid(), 'LOCALITY_RATE_NEWARK', 'Newark Tax Rate', 'TAX_RATES', 'PERCENTAGE',
    '{"scalar": 1.75, "unit": "percent", "municipalityCode": "NEWARK", "municipalityName": "Newark", "county": "Licking", "state": "OH"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Newark municipal rate', 'Newark Income Tax Ordinance'),

-- Lancaster
(gen_random_uuid(), 'LOCALITY_RATE_LANCASTER', 'Lancaster Tax Rate', 'TAX_RATES', 'PERCENTAGE',
    '{"scalar": 1.5, "unit": "percent", "municipalityCode": "LANCASTER", "municipalityName": "Lancaster", "county": "Fairfield", "state": "OH"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Lancaster municipal rate', 'Lancaster Income Tax Ordinance'),

-- Zanesville
(gen_random_uuid(), 'LOCALITY_RATE_ZANESVILLE', 'Zanesville Tax Rate', 'TAX_RATES', 'PERCENTAGE',
    '{"scalar": 2.0, "unit": "percent", "municipalityCode": "ZANESVILLE", "municipalityName": "Zanesville", "county": "Muskingum", "state": "OH"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Zanesville municipal rate', 'Zanesville Income Tax Ordinance'),

-- Chillicothe
(gen_random_uuid(), 'LOCALITY_RATE_CHILLICOTHE', 'Chillicothe Tax Rate', 'TAX_RATES', 'PERCENTAGE',
    '{"scalar": 1.85, "unit": "percent", "municipalityCode": "CHILLICOTHE", "municipalityName": "Chillicothe", "county": "Ross", "state": "OH"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Chillicothe municipal rate', 'Chillicothe Income Tax Ordinance'),

-- Marion
(gen_random_uuid(), 'LOCALITY_RATE_MARION', 'Marion Tax Rate', 'TAX_RATES', 'PERCENTAGE',
    '{"scalar": 1.85, "unit": "percent", "municipalityCode": "MARION", "municipalityName": "Marion", "county": "Marion", "state": "OH"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Marion municipal rate', 'Marion Income Tax Ordinance'),

-- Findlay
(gen_random_uuid(), 'LOCALITY_RATE_FINDLAY', 'Findlay Tax Rate', 'TAX_RATES', 'PERCENTAGE',
    '{"scalar": 1.5, "unit": "percent", "municipalityCode": "FINDLAY", "municipalityName": "Findlay", "county": "Hancock", "state": "OH"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Findlay municipal rate', 'Findlay Income Tax Ordinance'),

-- Lima
(gen_random_uuid(), 'LOCALITY_RATE_LIMA', 'Lima Tax Rate', 'TAX_RATES', 'PERCENTAGE',
    '{"scalar": 1.5, "unit": "percent", "municipalityCode": "LIMA", "municipalityName": "Lima", "county": "Allen", "state": "OH"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Lima municipal rate', 'Lima Income Tax Ordinance'),

-- Sandusky
(gen_random_uuid(), 'LOCALITY_RATE_SANDUSKY', 'Sandusky Tax Rate', 'TAX_RATES', 'PERCENTAGE',
    '{"scalar": 1.5, "unit": "percent", "municipalityCode": "SANDUSKY", "municipalityName": "Sandusky", "county": "Erie", "state": "OH"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Sandusky municipal rate', 'Sandusky Income Tax Ordinance'),

-- Fremont
(gen_random_uuid(), 'LOCALITY_RATE_FREMONT', 'Fremont Tax Rate', 'TAX_RATES', 'PERCENTAGE',
    '{"scalar": 1.4, "unit": "percent", "municipalityCode": "FREMONT", "municipalityName": "Fremont", "county": "Sandusky", "state": "OH"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Fremont municipal rate', 'Fremont Income Tax Ordinance'),

-- Tiffin
(gen_random_uuid(), 'LOCALITY_RATE_TIFFIN', 'Tiffin Tax Rate', 'TAX_RATES', 'PERCENTAGE',
    '{"scalar": 1.5, "unit": "percent", "municipalityCode": "TIFFIN", "municipalityName": "Tiffin", "county": "Seneca", "state": "OH"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Tiffin municipal rate', 'Tiffin Income Tax Ordinance'),

-- ============================================================================
-- SECTION 10: FILING RULES
-- ============================================================================

-- Filing Threshold
(gen_random_uuid(), 'FILING_THRESHOLD', 'Filing Threshold Amount', 'FILING', 'NUMBER',
    '{"scalar": 0, "unit": "dollars", "description": "Minimum income threshold requiring a tax return to be filed"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'All income is taxable', 'Dublin Income Tax Ordinance 2024'),

-- Filing Due Date
(gen_random_uuid(), 'FILING_DUE_DATE', 'Filing Due Date Rule', 'FILING', 'ENUM',
    '{"option": "APRIL_15", "allowedValues": ["APRIL_15", "APRIL_18", "FEDERAL_PLUS_30"], "description": "Annual filing due date"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Standard April 15 deadline', 'Ohio Rev. Code 718.05'),

-- Extension Days
(gen_random_uuid(), 'EXTENSION_DAYS', 'Extension Period Days', 'FILING', 'NUMBER',
    '{"scalar": 180, "unit": "days", "description": "Number of days extension granted with federal extension"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), '6-month extension with federal', 'Ohio Rev. Code 718.05'),

-- Quarterly Estimate Threshold
(gen_random_uuid(), 'QUARTERLY_ESTIMATE_THRESHOLD', 'Quarterly Estimate Threshold', 'FILING', 'NUMBER',
    '{"scalar": 200, "unit": "dollars", "description": "Tax liability threshold requiring quarterly estimated payments"}'::jsonb,
    '2024-01-01', 'dublin', ARRAY['INDIVIDUAL', 'BUSINESS'],
    'APPROVED', 'system', NOW(), 'system', NOW(), 'Standard quarterly estimate threshold', 'Ohio Rev. Code 718.08');

-- Mark all rules created by 'system' as system rules
UPDATE tax_rules SET is_system = TRUE WHERE created_by = 'system';
