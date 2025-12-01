-- Flyway Migration V26: Create payment_allocations table for tracking payment application
--
-- Functional Requirements:
-- FR-040 to FR-043: Payment allocation order (Tax → Penalties → Interest)
--
-- Multi-tenant isolation per Constitution II

CREATE TABLE IF NOT EXISTS payment_allocations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    return_id UUID NOT NULL,
    
    -- Payment details
    payment_date DATE NOT NULL,
    payment_amount DECIMAL(15,2) NOT NULL CHECK (payment_amount > 0),
    
    -- Allocation breakdown
    applied_to_tax DECIMAL(15,2) NOT NULL CHECK (applied_to_tax >= 0),
    applied_to_penalties DECIMAL(15,2) NOT NULL CHECK (applied_to_penalties >= 0),
    applied_to_interest DECIMAL(15,2) NOT NULL CHECK (applied_to_interest >= 0),
    
    -- Remaining balances after this payment
    remaining_tax_balance DECIMAL(15,2) NOT NULL CHECK (remaining_tax_balance >= 0),
    remaining_penalty_balance DECIMAL(15,2) NOT NULL CHECK (remaining_penalty_balance >= 0),
    remaining_interest_balance DECIMAL(15,2) NOT NULL CHECK (remaining_interest_balance >= 0),
    
    -- Allocation strategy
    allocation_order VARCHAR(20) NOT NULL DEFAULT 'TAX_FIRST' CHECK (allocation_order = 'TAX_FIRST'),
    
    -- Audit trail
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    
    -- Constraints
    CONSTRAINT check_payment_allocation_sum CHECK (
        ABS(payment_amount - (applied_to_tax + applied_to_penalties + applied_to_interest)) < 0.01
    ),
    CONSTRAINT check_allocation_non_negative CHECK (
        applied_to_tax >= 0 AND applied_to_penalties >= 0 AND applied_to_interest >= 0
    )
);

-- Create indexes
CREATE INDEX idx_payment_allocation_return ON payment_allocations(return_id);
CREATE INDEX idx_payment_allocation_date ON payment_allocations(payment_date);
CREATE INDEX idx_payment_allocation_tenant ON payment_allocations(tenant_id);

-- Comments
COMMENT ON TABLE payment_allocations IS 'Tracks how payments are allocated to tax, penalties, and interest per IRS standard ordering';
COMMENT ON COLUMN payment_allocations.allocation_order IS 'Always TAX_FIRST: Payment applied to tax principal first, then penalties, then interest';
COMMENT ON COLUMN payment_allocations.remaining_tax_balance IS 'Tax balance remaining after this payment applied';
