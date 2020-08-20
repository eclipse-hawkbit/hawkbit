ALTER TABLE sp_action ADD COLUMN weight INT;
ALTER TABLE sp_rollout ADD COLUMN weight INT;
ALTER TABLE sp_target_filter_query ADD COLUMN auto_assign_weight INT;
