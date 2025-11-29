-- Migration V1.38: Add constraints and validation rules for apportionment
-- Feature: Schedule Y Multi-State Sourcing
-- Purpose: Enforce data integrity and business rules for apportionment calculations

-- Schedule Y constraints
ALTER TABLE schedule_y
    ADD CONSTRAINT chk_schedule_y_formula_weights_valid 
    CHECK (
        apportionment_formula = 'CUSTOM' AND formula_weights IS NOT NULL
        OR apportionment_formula != 'CUSTOM'
    );

-- Ensure percentage consistency
ALTER TABLE schedule_y
    ADD CONSTRAINT chk_schedule_y_percentages_range
    CHECK (
        final_apportionment_percentage = 
        COALESCE(property_factor_percentage, 0) + 
        COALESCE(payroll_factor_percentage, 0) + 
        COALESCE(sales_factor_percentage, 0)
        OR apportionment_formula = 'CUSTOM'
    );

-- Property factor constraints
ALTER TABLE property_factor
    ADD CONSTRAINT chk_property_factor_ohio_total
    CHECK (
        total_ohio_property = 
        ohio_real_property + 
        ohio_tangible_personal_property + 
        ohio_rented_property_capitalized
    );

ALTER TABLE property_factor
    ADD CONSTRAINT chk_property_factor_rented_capitalization
    CHECK (
        ohio_rented_property_capitalized = ohio_rented_property_rent * 8
        OR ohio_rented_property_rent = 0
    );

ALTER TABLE property_factor
    ADD CONSTRAINT chk_property_factor_percentage_calc
    CHECK (
        total_property_everywhere = 0 
        OR property_factor_percentage = ROUND((total_ohio_property / total_property_everywhere * 100), 2)
    );

-- Payroll factor constraints
ALTER TABLE payroll_factor
    ADD CONSTRAINT chk_payroll_factor_ohio_total
    CHECK (
        total_ohio_payroll = 
        ohio_w2_wages + 
        ohio_contractor_payments + 
        ohio_officer_compensation
    );

ALTER TABLE payroll_factor
    ADD CONSTRAINT chk_payroll_factor_percentage_calc
    CHECK (
        total_payroll_everywhere = 0 
        OR payroll_factor_percentage = ROUND((total_ohio_payroll / total_payroll_everywhere * 100), 2)
    );

ALTER TABLE payroll_factor
    ADD CONSTRAINT chk_payroll_factor_employee_counts
    CHECK (ohio_employee_count <= employee_count);

-- Sales factor constraints
ALTER TABLE sales_factor
    ADD CONSTRAINT chk_sales_factor_ohio_total
    CHECK (
        total_ohio_sales = 
        ohio_sales_tangible_goods + 
        ohio_sales_services + 
        ohio_sales_rental_income + 
        ohio_sales_interest + 
        ohio_sales_royalties + 
        ohio_sales_other + 
        throwback_adjustment
    );

ALTER TABLE sales_factor
    ADD CONSTRAINT chk_sales_factor_percentage_calc
    CHECK (
        total_sales_everywhere = 0 
        OR sales_factor_percentage = ROUND((total_ohio_sales / total_sales_everywhere * 100), 2)
    );

-- Sale transaction constraints
ALTER TABLE sale_transaction
    ADD CONSTRAINT chk_sale_transaction_allocated_amount
    CHECK (allocated_amount <= sale_amount);

ALTER TABLE sale_transaction
    ADD CONSTRAINT chk_sale_transaction_states
    CHECK (
        sourcing_method = 'THROWBACK' AND allocated_state = origin_state
        OR sourcing_method = 'DESTINATION' AND allocated_state = destination_state
        OR sourcing_method IN ('MARKET_BASED', 'COST_OF_PERFORMANCE', 'PRO_RATA', 'THROWOUT')
    );

-- Nexus tracking constraints
ALTER TABLE nexus_tracking
    ADD CONSTRAINT chk_nexus_tracking_dates
    CHECK (
        nexus_terminated_date IS NULL 
        OR nexus_terminated_date >= nexus_established_date
    );

ALTER TABLE nexus_tracking
    ADD CONSTRAINT chk_nexus_tracking_economic_threshold
    CHECK (
        has_nexus = FALSE 
        OR sales_in_state >= COALESCE(economic_nexus_threshold, 0)
        OR property_in_state > 0
        OR payroll_in_state > 0
        OR employee_count_in_state > 0
    );

-- Comments
COMMENT ON CONSTRAINT chk_schedule_y_formula_weights_valid ON schedule_y 
    IS 'Custom formula requires formula_weights JSONB configuration';
COMMENT ON CONSTRAINT chk_property_factor_rented_capitalization ON property_factor 
    IS 'Rented property capitalized at 8x annual rent (standard multiplier)';
COMMENT ON CONSTRAINT chk_sale_transaction_states ON sale_transaction 
    IS 'Validate sourcing method matches state allocation logic';
