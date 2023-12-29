ALTER TABLE sp_rollout ADD dynamic BIT;
ALTER TABLE sp_rolloutgroup ADD dynamic BIT NOT NULL DEFAULT 0;