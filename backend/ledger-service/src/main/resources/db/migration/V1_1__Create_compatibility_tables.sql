-- V1.1: Create compatibility tables for V2 seed data
-- This migration creates temporary tables to support the V2 seed data script which uses 
-- a different schema (singular table names, different columns) than the V1 migration.
-- These tables will be used to capture the seed data, which will then be migrated 
-- to the correct tables in V2.1.

CREATE TABLE IF NOT EXISTS journal_entry (
    entry_id UUID PRIMARY KEY,
    tenant_id UUID,
    entity_id UUID,
    entry_number VARCHAR(50),
    entry_date DATE,
    description TEXT,
    source_type VARCHAR(50),
    source_id VARCHAR(255), -- V2 uses mixed types (UUID string and custom format)
    entry_status VARCHAR(50),
    total_debits DECIMAL(19,2),
    total_credits DECIMAL(19,2),
    created_at TIMESTAMP,
    created_by VARCHAR(100) -- V2 uses 'SEED_DATA' string
);

CREATE TABLE IF NOT EXISTS journal_entry_line (
    line_id UUID PRIMARY KEY,
    entry_id UUID,
    account_id UUID,
    account_number VARCHAR(50),
    account_name VARCHAR(255),
    debit_amount DECIMAL(19,2),
    credit_amount DECIMAL(19,2),
    description TEXT
);

CREATE TABLE IF NOT EXISTS payment_transaction (
    payment_id UUID PRIMARY KEY,
    tenant_id UUID,
    filer_id UUID,
    amount DECIMAL(19,2),
    payment_method VARCHAR(50),
    payment_status VARCHAR(50),
    provider_transaction_id VARCHAR(255),
    journal_entry_id UUID,
    payment_date TIMESTAMP, -- V2 uses date string which casts to timestamp
    description TEXT,
    test_mode BOOLEAN,
    created_at TIMESTAMP
);
