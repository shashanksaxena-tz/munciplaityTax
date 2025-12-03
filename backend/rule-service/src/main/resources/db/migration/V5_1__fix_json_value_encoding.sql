-- Fix double-encoded JSON values in tax_rules table
-- The value column contains JSON strings instead of JSON objects
-- This migration converts them to proper JSONB

UPDATE tax_rules
SET value = value::text::jsonb
WHERE jsonb_typeof(value) = 'string';
