-- Fix audit_queue table by removing cross-service foreign key constraint
-- In microservices architecture, foreign keys across service boundaries should not exist
-- This migration removes the problematic constraint

-- Drop the foreign key constraint if it exists
ALTER TABLE IF EXISTS audit_queue
DROP CONSTRAINT IF EXISTS fk_return;

-- Add comment to document why we don't have FK
COMMENT ON COLUMN audit_queue.return_id IS 'References submission ID from submission-service. Referential integrity maintained at application level in microservices architecture.';
