ALTER TABLE sp_rollout ADD is_dynamic BIT;
ALTER TABLE sp_rolloutgroup ADD is_dynamic BIT NOT NULL DEFAULT 0;

UPDATE sp_rollout SET weight = 1000 WHERE weight IS NULL;
UPDATE sp_action SET weight = 1000 WHERE weight IS NULL;
UPDATE sp_target_filter_query SET auto_assign_weight = 1000 WHERE auto_assign_weight IS NULL;
ALTER TABLE sp_rollout ALTER COLUMN weight INT NOT NULL;
ALTER TABLE sp_action ALTER COLUMN weight INT NOT NULL;
ALTER TABLE sp_target_filter_query ALTER COLUMN auto_assign_weight INT NOT NULL;