-- Insert default form templates for 2024 tax year
-- These templates use placeholder paths - actual templates need to be uploaded to storage

-- Form 27-EXT: Extension Request
INSERT INTO form_templates (
    form_code, form_name, template_file_path, revision_date, applicable_years,
    field_mappings, validation_rules, created_by
) VALUES (
    '27-EXT',
    'Extension Request',
    'templates/2024/Form-27-EXT-2024.pdf',
    '2024-01-01',
    ARRAY[2024, 2025],
    '{
        "businessName": "txt_business_name",
        "fein": "txt_fein",
        "address": "txt_address",
        "city": "txt_city",
        "state": "txt_state",
        "zip": "txt_zip",
        "taxYear": "txt_tax_year",
        "estimatedTax": "txt_estimated_tax",
        "priorPayments": "txt_prior_payments",
        "balanceDue": "txt_balance_due",
        "amountPaid": "txt_amount_paid",
        "extensionReason": "txt_reason",
        "preparerName": "txt_preparer_name",
        "preparerSignature": "txt_preparer_signature",
        "prepareDate": "txt_prepare_date"
    }'::jsonb,
    '{
        "requiredFields": ["businessName", "fein", "taxYear", "estimatedTax"],
        "calculations": {
            "balanceDue": "estimatedTax - priorPayments"
        },
        "validations": {
            "fein": {"pattern": "^[0-9]{2}-[0-9]{7}$", "message": "FEIN must be in format XX-XXXXXXX"},
            "estimatedTax": {"min": 0, "message": "Estimated tax must be positive"},
            "taxYear": {"min": 2020, "max": 2030, "message": "Invalid tax year"}
        }
    }'::jsonb,
    'SYSTEM'
);

-- Form 27-ES: Estimated Tax Vouchers (Q1-Q4)
INSERT INTO form_templates (
    form_code, form_name, template_file_path, revision_date, applicable_years,
    field_mappings, validation_rules, created_by
) VALUES (
    '27-ES',
    'Estimated Tax Voucher',
    'templates/2024/Form-27-ES-2024.pdf',
    '2024-01-01',
    ARRAY[2024, 2025],
    '{
        "businessName": "txt_business_name",
        "fein": "txt_fein",
        "address": "txt_address",
        "taxYear": "txt_tax_year",
        "quarter": "txt_quarter",
        "dueDate": "txt_due_date",
        "paymentAmount": "txt_payment_amount",
        "voucherNumber": "txt_voucher_number"
    }'::jsonb,
    '{
        "requiredFields": ["businessName", "fein", "taxYear", "quarter", "dueDate", "paymentAmount"],
        "validations": {
            "quarter": {"enum": [1, 2, 3, 4], "message": "Quarter must be 1, 2, 3, or 4"},
            "paymentAmount": {"min": 0, "message": "Payment amount must be positive"}
        }
    }'::jsonb,
    'SYSTEM'
);

-- Form 27-NOL: NOL Schedule
INSERT INTO form_templates (
    form_code, form_name, template_file_path, revision_date, applicable_years,
    field_mappings, validation_rules, created_by
) VALUES (
    '27-NOL',
    'Net Operating Loss Schedule',
    'templates/2024/Form-27-NOL-2024.pdf',
    '2024-01-01',
    ARRAY[2024, 2025],
    '{
        "businessName": "txt_business_name",
        "fein": "txt_fein",
        "taxYear": "txt_tax_year",
        "nolVintages": "table_nol_vintages",
        "taxableIncomeBeforeNOL": "txt_taxable_income_before_nol",
        "maxNOLDeduction": "txt_max_nol_deduction",
        "nolDeductionClaimed": "txt_nol_deduction_claimed",
        "taxableIncomeAfterNOL": "txt_taxable_income_after_nol",
        "totalCarryforward": "txt_total_carryforward"
    }'::jsonb,
    '{
        "requiredFields": ["businessName", "fein", "taxYear", "taxableIncomeBeforeNOL"],
        "calculations": {
            "maxNOLDeduction": "taxableIncomeBeforeNOL * 0.8",
            "taxableIncomeAfterNOL": "taxableIncomeBeforeNOL - nolDeductionClaimed"
        },
        "validations": {
            "nolDeductionClaimed": {
                "maxPercentage": 0.8,
                "message": "NOL deduction cannot exceed 80% of taxable income"
            }
        }
    }'::jsonb,
    'SYSTEM'
);

-- Form 27-W1: Withholding Report
INSERT INTO form_templates (
    form_code, form_name, template_file_path, revision_date, applicable_years,
    field_mappings, validation_rules, created_by
) VALUES (
    '27-W1',
    'Quarterly Withholding Report',
    'templates/2024/Form-27-W1-2024.pdf',
    '2024-01-01',
    ARRAY[2024, 2025],
    '{
        "businessName": "txt_business_name",
        "fein": "txt_fein",
        "quarter": "txt_quarter",
        "year": "txt_year",
        "employees": "table_employees",
        "quarterlyTotalWages": "txt_quarterly_total_wages",
        "quarterlyTotalTax": "txt_quarterly_total_tax",
        "ytdTotalWages": "txt_ytd_total_wages",
        "ytdTotalTax": "txt_ytd_total_tax",
        "paymentDue": "txt_payment_due",
        "dueDate": "txt_due_date"
    }'::jsonb,
    '{
        "requiredFields": ["businessName", "fein", "quarter", "year", "quarterlyTotalWages", "quarterlyTotalTax"],
        "validations": {
            "quarter": {"enum": [1, 2, 3, 4], "message": "Quarter must be 1, 2, 3, or 4"}
        }
    }'::jsonb,
    'SYSTEM'
);

-- Form 27-Y: Apportionment Schedule
INSERT INTO form_templates (
    form_code, form_name, template_file_path, revision_date, applicable_years,
    field_mappings, validation_rules, created_by
) VALUES (
    '27-Y',
    'Apportionment Schedule',
    'templates/2024/Form-27-Y-2024.pdf',
    '2024-01-01',
    ARRAY[2024, 2025],
    '{
        "businessName": "txt_business_name",
        "fein": "txt_fein",
        "taxYear": "txt_tax_year",
        "propertyOhio": "txt_property_ohio",
        "propertyTotal": "txt_property_total",
        "propertyFactor": "txt_property_factor",
        "payrollOhio": "txt_payroll_ohio",
        "payrollTotal": "txt_payroll_total",
        "payrollFactor": "txt_payroll_factor",
        "salesOhio": "txt_sales_ohio",
        "salesTotal": "txt_sales_total",
        "salesFactor": "txt_sales_factor",
        "apportionmentPercentage": "txt_apportionment_percentage"
    }'::jsonb,
    '{
        "requiredFields": ["businessName", "fein", "taxYear"],
        "calculations": {
            "propertyFactor": "propertyOhio / propertyTotal",
            "payrollFactor": "payrollOhio / payrollTotal",
            "salesFactor": "salesOhio / salesTotal"
        }
    }'::jsonb,
    'SYSTEM'
);

-- Form 27-X: Schedule X Book-Tax Adjustments
INSERT INTO form_templates (
    form_code, form_name, template_file_path, revision_date, applicable_years,
    field_mappings, validation_rules, created_by
) VALUES (
    '27-X',
    'Schedule X Book-Tax Adjustments',
    'templates/2024/Form-27-X-2024.pdf',
    '2024-01-01',
    ARRAY[2024, 2025],
    '{
        "businessName": "txt_business_name",
        "fein": "txt_fein",
        "taxYear": "txt_tax_year",
        "bookIncome": "txt_book_income",
        "addBacks": "table_add_backs",
        "deductions": "table_deductions",
        "totalAddBacks": "txt_total_add_backs",
        "totalDeductions": "txt_total_deductions",
        "municipalTaxableIncome": "txt_municipal_taxable_income"
    }'::jsonb,
    '{
        "requiredFields": ["businessName", "fein", "taxYear", "bookIncome"],
        "calculations": {
            "municipalTaxableIncome": "bookIncome + totalAddBacks - totalDeductions"
        }
    }'::jsonb,
    'SYSTEM'
);
