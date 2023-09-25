ALTER TABLE sp_target_filter_query ADD COLUMN access_control_context VARCHAR(512);
ALTER TABLE sp_rollout ADD COLUMN access_control_context VARCHAR(512);