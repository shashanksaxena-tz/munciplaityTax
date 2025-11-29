-- Flyway Migration V1.34: Create nol_expiration_alerts table
-- Feature: Net Operating Loss (NOL) Carryforward & Carryback Tracking System
-- Purpose: Alert users of NOLs approaching expiration

-- Create nol_expiration_alerts table
CREATE TABLE IF NOT EXISTS dublin.nol_expiration_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    business_id UUID NOT NULL,
    nol_id UUID NOT NULL,
    tax_year INTEGER NOT NULL CHECK (tax_year >= 2000 AND tax_year <= 2100),
    nol_balance DECIMAL(15,2) NOT NULL CHECK (nol_balance > 0),
    expiration_date DATE NOT NULL,
    years_until_expiration DECIMAL(3,1) NOT NULL CHECK (years_until_expiration >= 0),
    severity_level VARCHAR(20) NOT NULL CHECK (severity_level IN ('CRITICAL', 'WARNING', 'INFO')),
    alert_message TEXT NOT NULL,
    dismissed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    -- Foreign key constraints
    CONSTRAINT fk_nol_alert_nol FOREIGN KEY (nol_id) REFERENCES dublin.nols(id) ON DELETE CASCADE
);

-- Create indexes for performance
CREATE INDEX idx_nol_alert_business ON dublin.nol_expiration_alerts(business_id);
CREATE INDEX idx_nol_alert_nol ON dublin.nol_expiration_alerts(nol_id);
CREATE INDEX idx_nol_alert_severity ON dublin.nol_expiration_alerts(severity_level);
CREATE INDEX idx_nol_alert_dismissed ON dublin.nol_expiration_alerts(dismissed) WHERE dismissed = FALSE;
CREATE INDEX idx_nol_alert_expiration ON dublin.nol_expiration_alerts(expiration_date);

-- Add comments
COMMENT ON TABLE dublin.nol_expiration_alerts IS 'Alerts for NOLs expiring within 3 years (CRITICAL ≤1yr, WARNING 1-2yr, INFO 2-3yr)';
COMMENT ON COLUMN dublin.nol_expiration_alerts.years_until_expiration IS 'Calculated: (expiration_date - current_date) / 365';
COMMENT ON COLUMN dublin.nol_expiration_alerts.dismissed IS 'User has acknowledged alert';
