ALTER TABLE sp_rollout ADD COLUMN deleted BOOLEAN;

UPDATE sp_rollout SET deleted = 0;
