-- See POSTGRESQL/V1_20_2__action_rollout_indexes__POSTGRESQL.sql for rationale.
CREATE INDEX sp_idx_action_rollout_status       ON sp_action (tenant, rollout, status);
CREATE INDEX sp_idx_action_rollout_group_status ON sp_action (tenant, rollout_group, status);
