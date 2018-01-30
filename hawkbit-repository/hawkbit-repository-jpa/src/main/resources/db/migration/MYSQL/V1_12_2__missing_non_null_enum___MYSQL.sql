ALTER TABLE sp_target CHANGE COLUMN update_status update_status integer not null;
ALTER TABLE sp_rollout CHANGE COLUMN action_type action_type integer not null;
ALTER TABLE sp_action CHANGE COLUMN action_type action_type integer not null;