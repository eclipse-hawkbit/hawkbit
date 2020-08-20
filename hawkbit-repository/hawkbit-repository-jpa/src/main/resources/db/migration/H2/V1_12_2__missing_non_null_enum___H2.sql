ALTER TABLE sp_target ALTER COLUMN update_status integer not null;
ALTER TABLE sp_rollout ALTER COLUMN action_type integer not null;
ALTER TABLE sp_action ALTER COLUMN action_type integer not null;