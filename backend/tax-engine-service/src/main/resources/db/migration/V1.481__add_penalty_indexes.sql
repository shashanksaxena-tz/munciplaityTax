-- V1.48: Add indexes for penalty tables
--
-- Performance optimization per data-model.md
--
-- Target: All penalty/interest queries < 500ms per success criteria

-- Penalties table indexes
CREATE INDEX idx_penalty_return ON penalties(return_id);
CREATE INDEX idx_penalty_tenant_year ON penalties(tenant_id, assessment_date);
CREATE INDEX idx_penalty_type ON penalties(penalty_type);
CREATE INDEX idx_penalty_abated ON penalties(is_abated) WHERE is_abated = TRUE;
CREATE INDEX idx_penalty_due_date ON penalties(tax_due_date);

-- Estimated tax penalties indexes
CREATE INDEX idx_estimated_penalty_tenant_year ON estimated_tax_penalties(tenant_id, tax_year);
CREATE INDEX idx_estimated_penalty_safe_harbor ON estimated_tax_penalties(safe_harbor_1_met, safe_harbor_2_met);

-- Quarterly underpayments indexes
CREATE INDEX idx_quarterly_underpayment_penalty ON quarterly_underpayments(estimated_penalty_id);
CREATE INDEX idx_quarterly_underpayment_quarter ON quarterly_underpayments(quarter);
CREATE INDEX idx_quarterly_underpayment_due_date ON quarterly_underpayments(due_date);

-- Interests table indexes
CREATE INDEX idx_interest_return ON interests(return_id);
CREATE INDEX idx_interest_tenant ON interests(tenant_id);
CREATE INDEX idx_interest_start_date ON interests(start_date);
CREATE INDEX idx_interest_end_date ON interests(end_date);

-- Quarterly interests indexes
CREATE INDEX idx_quarterly_interest_parent ON quarterly_interests(interest_id);
CREATE INDEX idx_quarterly_interest_quarter ON quarterly_interests(quarter);
CREATE INDEX idx_quarterly_interest_dates ON quarterly_interests(start_date, end_date);

-- Penalty abatements indexes
CREATE INDEX idx_abatement_return ON penalty_abatements(return_id);
CREATE INDEX idx_abatement_penalty ON penalty_abatements(penalty_id);
CREATE INDEX idx_abatement_status ON penalty_abatements(status);
CREATE INDEX idx_abatement_reason ON penalty_abatements(reason);
CREATE INDEX idx_abatement_reviewed_by ON penalty_abatements(reviewed_by);

-- Payment allocations indexes
CREATE INDEX idx_payment_allocation_return ON payment_allocations(return_id);
CREATE INDEX idx_payment_allocation_date ON payment_allocations(payment_date);
CREATE INDEX idx_payment_allocation_tenant ON payment_allocations(tenant_id);

-- Penalty audit logs indexes
CREATE INDEX idx_penalty_audit_entity ON penalty_audit_logs(entity_type, entity_id);
CREATE INDEX idx_penalty_audit_actor ON penalty_audit_logs(actor_id);
CREATE INDEX idx_penalty_audit_created_at ON penalty_audit_logs(created_at);
CREATE INDEX idx_penalty_audit_tenant ON penalty_audit_logs(tenant_id);
