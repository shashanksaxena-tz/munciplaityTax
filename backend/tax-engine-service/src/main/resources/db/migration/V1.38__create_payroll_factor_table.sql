-- Migration V1.32: Create payroll_factor table
-- Feature: Schedule Y Multi-State Sourcing
-- Purpose: Store payroll factor calculation details for apportionment

CREATE TABLE IF NOT EXISTS payroll_factor (
    payroll_factor_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    schedule_y_id UUID NOT NULL REFERENCES schedule_y(schedule_y_id) ON DELETE CASCADE,
    ohio_w2_wages DECIMAL(15,2) DEFAULT 0 CHECK (ohio_w2_wages >= 0),
    ohio_contractor_payments DECIMAL(15,2) DEFAULT 0 CHECK (ohio_contractor_payments >= 0),
    ohio_officer_compensation DECIMAL(15,2) DEFAULT 0 CHECK (ohio_officer_compensation >= 0),
    total_ohio_payroll DECIMAL(15,2) NOT NULL CHECK (total_ohio_payroll >= 0),
    total_payroll_everywhere DECIMAL(15,2) NOT NULL CHECK (total_payroll_everywhere > 0),
    payroll_factor_percentage DECIMAL(5,2) NOT NULL CHECK (payroll_factor_percentage >= 0 AND payroll_factor_percentage <= 100),
    employee_count INTEGER DEFAULT 0 CHECK (employee_count >= 0),
    ohio_employee_count INTEGER DEFAULT 0 CHECK (ohio_employee_count >= 0 AND ohio_employee_count <= employee_count),
    remote_employee_allocation JSONB,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_payroll_factor_schedule_y_id ON payroll_factor(schedule_y_id);

-- Comments
COMMENT ON TABLE payroll_factor IS 'Payroll factor calculation: (Ohio payroll) / (Total payroll everywhere) × 100%';
COMMENT ON COLUMN payroll_factor.ohio_w2_wages IS 'W-2 wages for employees working primarily in Ohio';
COMMENT ON COLUMN payroll_factor.ohio_contractor_payments IS '1099-NEC payments to Ohio contractors';
COMMENT ON COLUMN payroll_factor.ohio_officer_compensation IS 'Officer compensation allocated to Ohio';
COMMENT ON COLUMN payroll_factor.remote_employee_allocation IS 'JSONB: {"OH": 500000, "CA": 300000, "NY": 200000} - payroll by state for remote workers';
