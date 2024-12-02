ALTER TABLE sp_action ADD COLUMN timestamp BIGINT;
UPDATE sp_action SET timestamp = created_at;