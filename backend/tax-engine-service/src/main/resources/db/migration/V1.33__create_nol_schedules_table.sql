-- Flyway Migration V1.33: Create nol_schedules table
-- Feature: Net Operating Loss (NOL) Carryforward & Carryback Tracking System
-- Purpose: Store consolidated NOL schedules for tax returns

-- Create nol_schedules table
CREATE TABLE IF NOT EXISTS dublin.nol_schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    business_id UUID NOT NULL,
    return_id UUID NOT NULL,
    tax_year INTEGER NOT NULL CHECK (tax_year >= 2000 AND tax_year <= 2100),
    total_beginning_balance DECIMAL(15,2) NOT NULL CHECK (total_beginning_balance >= 0),
    new_nol_generated DECIMAL(15,2) NOT NULL DEFAULT 0 CHECK (new_nol_generated >= 0),
    total_available_nol DECIMAL(15,2) NOT NULL CHECK (total_available_nol >= 0),
    nol_deduction DECIMAL(15,2) NOT NULL DEFAULT 0 CHECK (nol_deduction >= 0 AND nol_deduction <= total_available_nol),
    expired_nol DECIMAL(15,2) NOT NULL DEFAULT 0 CHECK (expired_nol >= 0),
    total_ending_balance DECIMAL(15,2) NOT NULL CHECK (total_ending_balance >= 0),
    limitation_percentage DECIMAL(5,2) NOT NULL CHECK (limitation_percentage > 0 AND limitation_percentage <= 100),
    taxable_income_before_nol DECIMAL(15,2) NOT NULL,
    taxable_income_after_nol DECIMAL(15,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    -- Business rules
    CONSTRAINT check_nol_schedule_balance CHECK (
        total_available_nol = total_beginning_balance + new_nol_generated
    ),
    CONSTRAINT check_nol_schedule_ending CHECK (
        total_ending_balance = total_available_nol - nol_deduction - expired_nol
    ),
    CONSTRAINT check_nol_schedule_income CHECK (
        taxable_income_after_nol = taxable_income_before_nol - nol_deduction
    ),
    -- Unique constraint: one schedule per return
    CONSTRAINT uq_nol_schedule_return UNIQUE (return_id)
);

-- Create indexes for performance
CREATE INDEX idx_nol_schedule_return ON dublin.nol_schedules(return_id);
CREATE INDEX idx_nol_schedule_business_year ON dublin.nol_schedules(business_id, tax_year);
CREATE INDEX idx_nol_schedule_tenant ON dublin.nol_schedules(tenant_id);

-- Add comments
COMMENT ON TABLE dublin.nol_schedules IS 'Consolidated NOL schedule (Form 27-NOL) showing beginning balance, additions, deductions, and ending balance';
COMMENT ON COLUMN dublin.nol_schedules.limitation_percentage IS 'Post-2017: 80%, Pre-2018: 100%';
COMMENT ON COLUMN dublin.nol_schedules.total_ending_balance IS 'Carried forward to next year as beginning balance';
