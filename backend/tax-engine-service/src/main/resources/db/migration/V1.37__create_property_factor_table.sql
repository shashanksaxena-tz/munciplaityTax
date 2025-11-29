-- Migration V1.31: Create property_factor table
-- Feature: Schedule Y Multi-State Sourcing
-- Purpose: Store property factor calculation details for apportionment

CREATE TABLE IF NOT EXISTS property_factor (
    property_factor_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    schedule_y_id UUID NOT NULL REFERENCES schedule_y(schedule_y_id) ON DELETE CASCADE,
    ohio_real_property DECIMAL(15,2) DEFAULT 0 CHECK (ohio_real_property >= 0),
    ohio_tangible_personal_property DECIMAL(15,2) DEFAULT 0 CHECK (ohio_tangible_personal_property >= 0),
    ohio_rented_property_rent DECIMAL(15,2) DEFAULT 0 CHECK (ohio_rented_property_rent >= 0),
    ohio_rented_property_capitalized DECIMAL(15,2) DEFAULT 0 CHECK (ohio_rented_property_capitalized >= 0),
    total_ohio_property DECIMAL(15,2) NOT NULL CHECK (total_ohio_property >= 0),
    total_property_everywhere DECIMAL(15,2) NOT NULL CHECK (total_property_everywhere > 0),
    property_factor_percentage DECIMAL(5,2) NOT NULL CHECK (property_factor_percentage >= 0 AND property_factor_percentage <= 100),
    averaging_method VARCHAR(30) NOT NULL DEFAULT 'AVERAGE_BEGINNING_ENDING' CHECK (averaging_method IN (
        'AVERAGE_BEGINNING_ENDING',
        'MONTHLY_AVERAGE',
        'DAILY_AVERAGE'
    )),
    beginning_of_year_value DECIMAL(15,2),
    end_of_year_value DECIMAL(15,2),
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_property_factor_schedule_y_id ON property_factor(schedule_y_id);

-- Comments
COMMENT ON TABLE property_factor IS 'Property factor calculation: (Ohio property) / (Total property everywhere) × 100%';
COMMENT ON COLUMN property_factor.ohio_real_property IS 'Land and buildings owned in Ohio';
COMMENT ON COLUMN property_factor.ohio_tangible_personal_property IS 'Equipment, vehicles, inventory in Ohio';
COMMENT ON COLUMN property_factor.ohio_rented_property_rent IS 'Annual rent for Ohio property';
COMMENT ON COLUMN property_factor.ohio_rented_property_capitalized IS 'Annual rent × 8 (standard capitalization rate)';
COMMENT ON COLUMN property_factor.averaging_method IS 'Method for calculating average property values over tax year';
