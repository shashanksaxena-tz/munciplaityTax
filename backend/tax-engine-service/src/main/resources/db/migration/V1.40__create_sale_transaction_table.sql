-- Migration V1.34: Create sale_transaction table
-- Feature: Schedule Y Multi-State Sourcing
-- Purpose: Store individual sale transactions for detailed sourcing tracking

CREATE TABLE IF NOT EXISTS sale_transaction (
    transaction_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sales_factor_id UUID NOT NULL REFERENCES sales_factor(sales_factor_id) ON DELETE CASCADE,
    transaction_date DATE NOT NULL,
    customer_name VARCHAR(255),
    sale_amount DECIMAL(15,2) NOT NULL CHECK (sale_amount >= 0),
    sale_type VARCHAR(30) NOT NULL CHECK (sale_type IN (
        'TANGIBLE_GOODS',
        'SERVICES',
        'RENTAL_INCOME',
        'INTEREST',
        'ROYALTIES',
        'OTHER'
    )),
    origin_state VARCHAR(2),
    destination_state VARCHAR(2),
    sourcing_method VARCHAR(30) NOT NULL CHECK (sourcing_method IN (
        'DESTINATION',
        'MARKET_BASED',
        'COST_OF_PERFORMANCE',
        'THROWBACK',
        'THROWOUT',
        'PRO_RATA'
    )),
    has_destination_nexus BOOLEAN DEFAULT FALSE,
    allocated_state VARCHAR(2) NOT NULL,
    allocated_amount DECIMAL(15,2) NOT NULL CHECK (allocated_amount >= 0),
    customer_location_details JSONB,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_sale_transaction_sales_factor_id ON sale_transaction(sales_factor_id);
CREATE INDEX idx_sale_transaction_date ON sale_transaction(transaction_date);
CREATE INDEX idx_sale_transaction_allocated_state ON sale_transaction(allocated_state);

-- Comments
COMMENT ON TABLE sale_transaction IS 'Individual sale transactions with sourcing method and state allocation';
COMMENT ON COLUMN sale_transaction.sale_type IS 'Type of sale: tangible goods, services, rental, interest, royalties, other';
COMMENT ON COLUMN sale_transaction.origin_state IS 'State where goods shipped from or services performed';
COMMENT ON COLUMN sale_transaction.destination_state IS 'State where goods delivered or customer located';
COMMENT ON COLUMN sale_transaction.sourcing_method IS 'Method used to allocate sale: DESTINATION, MARKET_BASED, COST_OF_PERFORMANCE, THROWBACK, THROWOUT, PRO_RATA';
COMMENT ON COLUMN sale_transaction.has_destination_nexus IS 'Whether business has nexus in destination state (determines throwback eligibility)';
COMMENT ON COLUMN sale_transaction.allocated_state IS 'Final state assignment after applying sourcing rules';
COMMENT ON COLUMN sale_transaction.allocated_amount IS 'Amount allocated to state (may differ from sale_amount for pro-rated services)';
COMMENT ON COLUMN sale_transaction.customer_location_details IS 'JSONB: Multi-location customer breakdown if applicable';
