ALTER TABLE sp_rollout ADD column deleted boolean;

UPDATE sp_rollout set deleted = 0;