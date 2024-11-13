ALTER TABLE sp_action_status DROP INDEX sp_idx_action_status_01;
ALTER TABLE sp_rollout DROP INDEX sp_idx_rollout_01;
ALTER TABLE sp_rolloutgroup DROP INDEX sp_idx_rolloutgroup_01;
ALTER TABLE sp_target DROP INDEX sp_idx_target_02;
ALTER TABLE sp_target_filter_query DROP INDEX sp_idx_target_filter_query_01;
ALTER TABLE sp_distribution_set DROP INDEX sp_idx_distribution_set_01;
ALTER TABLE sp_distribution_set DROP INDEX sp_idx_distribution_set_02;
CREATE INDEX sp_idx_distribution_set_01 ON sp_distribution_set (tenant, deleted, complete);
