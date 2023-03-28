DROP INDEX sp_idx_action_status_01;
DROP INDEX sp_idx_rollout_01;
DROP INDEX sp_idx_rolloutgroup_01;
DROP INDEX sp_idx_target_02;
DROP INDEX sp_idx_target_filter_query_01;
DROP INDEX sp_idx_distribution_set_01;
DROP INDEX sp_idx_distribution_set_02;
CREATE INDEX sp_idx_distribution_set_01 ON sp_distribution_set (tenant, deleted, complete);
