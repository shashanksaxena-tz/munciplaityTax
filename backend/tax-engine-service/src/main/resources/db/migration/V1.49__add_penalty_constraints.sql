-- V1.49: Add foreign key constraints for penalty tables
--
-- Referential integrity for penalty domain
--
-- Note: tenant_id foreign keys assume tenants table exists
-- Note: return_id foreign keys assume tax_returns table exists
-- Note: Some constraints deferred until integration phase

-- Quarterly underpayments → Estimated tax penalties
ALTER TABLE quarterly_underpayments
    ADD CONSTRAINT fk_quarterly_underpayment_estimated_penalty
    FOREIGN KEY (estimated_penalty_id) REFERENCES estimated_tax_penalties(id)
    ON DELETE CASCADE;

-- Quarterly interests → Interests
ALTER TABLE quarterly_interests
    ADD CONSTRAINT fk_quarterly_interest_interest
    FOREIGN KEY (interest_id) REFERENCES interests(id)
    ON DELETE CASCADE;

-- Penalty abatements → Penalties (optional relationship)
-- NULL penalty_id means abating all penalties on return
ALTER TABLE penalty_abatements
    ADD CONSTRAINT fk_penalty_abatement_penalty
    FOREIGN KEY (penalty_id) REFERENCES penalties(id)
    ON DELETE SET NULL;

-- Additional constraints will be added during integration phase:
-- - penalties.return_id → tax_returns.id
-- - estimated_tax_penalties.return_id → tax_returns.id
-- - interests.return_id → tax_returns.id
-- - penalty_abatements.return_id → tax_returns.id
-- - payment_allocations.return_id → tax_returns.id
-- - penalties.tenant_id → tenants.id
-- - etc.

COMMENT ON CONSTRAINT fk_quarterly_underpayment_estimated_penalty ON quarterly_underpayments IS 'Cascade delete: Remove quarterly breakdowns when parent penalty deleted';
COMMENT ON CONSTRAINT fk_quarterly_interest_interest ON quarterly_interests IS 'Cascade delete: Remove quarterly breakdowns when parent interest deleted';
COMMENT ON CONSTRAINT fk_penalty_abatement_penalty ON penalty_abatements IS 'Set NULL on delete: Abatement remains if specific penalty deleted (may apply to other penalties)';
