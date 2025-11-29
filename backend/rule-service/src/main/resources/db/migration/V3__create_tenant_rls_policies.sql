-- V3: Create tenant Row-Level Security policies
-- This ensures multi-tenant data isolation at the database level

-- Enable Row-Level Security on tax_rules table
ALTER TABLE tax_rules ENABLE ROW LEVEL SECURITY;

-- Policy: Users can only access their tenant's rules (or GLOBAL rules)
-- This policy applies to all operations (SELECT, INSERT, UPDATE, DELETE)
CREATE POLICY tenant_isolation ON tax_rules
FOR ALL
USING (
    tenant_id = current_setting('app.tenant_id', true) OR 
    tenant_id = 'GLOBAL'
);

-- Policy: Only TAX_ADMINISTRATOR role can modify rules
-- This enforces role-based access control at the database level
CREATE POLICY admin_write_only ON tax_rules
FOR ALL
WITH CHECK (current_setting('app.user_role', true) = 'TAX_ADMINISTRATOR');

-- Note: RLS policies will be enforced when application sets session variables:
-- SET app.tenant_id = 'dublin';
-- SET app.user_role = 'TAX_ADMINISTRATOR';

-- Create function to set tenant context (helper for application layer)
CREATE OR REPLACE FUNCTION set_tenant_context(p_tenant_id VARCHAR, p_user_role VARCHAR)
RETURNS VOID AS $$
BEGIN
    PERFORM set_config('app.tenant_id', p_tenant_id, false);
    PERFORM set_config('app.user_role', p_user_role, false);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

COMMENT ON FUNCTION set_tenant_context IS 'Helper function to set tenant context for RLS policies';

-- Grant execute permission to application user only (not PUBLIC)
-- Note: This assumes application connects with a database user named 'munitax_app'
-- Adjust the username based on actual application database configuration
-- SECURITY: Do not grant to PUBLIC in production
-- GRANT EXECUTE ON FUNCTION set_tenant_context TO munitax_app;
