ALTER TABLE sp_rollout ADD COLUMN is_dynamic BOOLEAN;
ALTER TABLE sp_rolloutgroup ADD COLUMN is_dynamic BOOLEAN NOT NULL DEFAULT false;

UPDATE sp_rollout SET weight = 1000 WHERE weight IS NULL;
UPDATE sp_action SET weight = 1000 WHERE weight IS NULL;
UPDATE sp_target_filter_query SET auto_assign_weight = 1000 WHERE auto_assign_weight IS NULL;
ALTER TABLE sp_rollout MODIFY COLUMN weight INT NOT NULL;
ALTER TABLE sp_action MODIFY COLUMN weight INT NOT NULL;
ALTER TABLE sp_target_filter_query MODIFY COLUMN auto_assign_weight INT NOT NULL;