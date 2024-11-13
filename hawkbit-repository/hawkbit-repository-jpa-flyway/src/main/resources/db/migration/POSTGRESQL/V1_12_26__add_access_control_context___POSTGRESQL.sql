ALTER TABLE sp_target_filter_query ADD COLUMN access_control_context VARCHAR(4096);
ALTER TABLE sp_rollout ADD COLUMN access_control_context VARCHAR(4096);