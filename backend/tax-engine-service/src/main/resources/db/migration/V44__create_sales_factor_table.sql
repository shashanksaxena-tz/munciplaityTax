-- Flyway Migration V16: Create sales_factor table
-- Feature: Schedule Y Multi-State Sourcing
-- Purpose: Store sales factor calculation details for apportionment

CREATE TABLE IF NOT EXISTS sales_factor (
    sales_factor_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    schedule_y_id UUID NOT NULL REFERENCES schedule_y(schedule_y_id) ON DELETE CASCADE,
    ohio_sales_tangible_goods DECIMAL(15,2) DEFAULT 0 CHECK (ohio_sales_tangible_goods >= 0),
    ohio_sales_services DECIMAL(15,2) DEFAULT 0 CHECK (ohio_sales_services >= 0),
    ohio_sales_rental_income DECIMAL(15,2) DEFAULT 0 CHECK (ohio_sales_rental_income >= 0),
    ohio_sales_interest DECIMAL(15,2) DEFAULT 0 CHECK (ohio_sales_interest >= 0),
    ohio_sales_royalties DECIMAL(15,2) DEFAULT 0 CHECK (ohio_sales_royalties >= 0),
    ohio_sales_other DECIMAL(15,2) DEFAULT 0 CHECK (ohio_sales_other >= 0),
    throwback_adjustment DECIMAL(15,2) DEFAULT 0,
    total_ohio_sales DECIMAL(15,2) NOT NULL CHECK (total_ohio_sales >= 0),
    total_sales_everywhere DECIMAL(15,2) NOT NULL CHECK (total_sales_everywhere >= 0),
    sales_factor_percentage DECIMAL(5,2) NOT NULL CHECK (sales_factor_percentage >= 0 AND sales_factor_percentage <= 100),
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_sales_factor_schedule_y_id ON sales_factor(schedule_y_id);

-- Comments
COMMENT ON TABLE sales_factor IS 'Sales factor calculation: (Ohio sales) / (Total sales everywhere) × 100%';
COMMENT ON COLUMN sales_factor.ohio_sales_tangible_goods IS 'Sales of physical products delivered to Ohio customers';
COMMENT ON COLUMN sales_factor.ohio_sales_services IS 'Service revenue sourced to Ohio (market-based or cost-of-performance)';
COMMENT ON COLUMN sales_factor.ohio_sales_rental_income IS 'Rental income from Ohio property';
COMMENT ON COLUMN sales_factor.ohio_sales_interest IS 'Interest income sourced to Ohio';
COMMENT ON COLUMN sales_factor.ohio_sales_royalties IS 'Royalty income sourced to Ohio';
COMMENT ON COLUMN sales_factor.throwback_adjustment IS 'Sales thrown back to Ohio (no nexus in destination state)';
