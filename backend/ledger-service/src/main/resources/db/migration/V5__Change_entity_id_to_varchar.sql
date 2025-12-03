-- V5: Change entity_id to VARCHAR to support non-UUID identifiers
-- This migration alters entity_id columns from UUID to VARCHAR(50)

-- 1. Alter journal_entries
ALTER TABLE journal_entries ALTER COLUMN entity_id TYPE VARCHAR(50);

-- 2. Alter other tables if they have entity_id (none found in V1)
