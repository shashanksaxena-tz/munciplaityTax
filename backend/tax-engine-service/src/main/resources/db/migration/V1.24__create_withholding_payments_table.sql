-- Flyway Migration V1.24: Create withholding_payments table
-- Feature: Withholding Reconciliation System
-- Purpose: Track payments made against W-1 withholding filings
-- FR-020: Payment tracking integration with payment gateway

CREATE TABLE IF NOT EXISTS dublin.withholding_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    w1_filing_id UUID NOT NULL,
    payment_date TIMESTAMP NOT NULL,
    payment_amount DECIMAL(15,2) NOT NULL CHECK (payment_amount > 0),
    payment_method VARCHAR(50) NOT NULL CHECK (payment_method IN ('ACH', 'CHECK', 'CREDIT_CARD', 'WIRE_TRANSFER')),
    transaction_id VARCHAR(100) NOT NULL,
    confirmation_number VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED')),
    failure_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    -- Foreign key constraints
    CONSTRAINT fk_payment_w1_filing FOREIGN KEY (w1_filing_id) REFERENCES dublin.w1_filings(id)
);

-- Create indexes for performance
CREATE INDEX idx_payment_w1_filing ON dublin.withholding_payments(w1_filing_id);
CREATE INDEX idx_payment_transaction_id ON dublin.withholding_payments(transaction_id);
CREATE INDEX idx_payment_date ON dublin.withholding_payments(payment_date);
CREATE INDEX idx_payment_status ON dublin.withholding_payments(status);
CREATE INDEX idx_payment_tenant ON dublin.withholding_payments(tenant_id);

-- Comments for documentation
COMMENT ON TABLE dublin.withholding_payments IS 'Payment tracking for W-1 filings per FR-020';
COMMENT ON COLUMN dublin.withholding_payments.tenant_id IS 'Multi-tenant isolation per Constitution Principle II';
COMMENT ON COLUMN dublin.withholding_payments.transaction_id IS 'Payment gateway transaction identifier';
