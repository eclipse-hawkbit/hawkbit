ALTER TABLE sp_action ADD COLUMN timestamp bigint;
UPDATE sp_action SET timestamp = created_at;