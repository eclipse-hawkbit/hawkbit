-- Add indexes covering rollout-monitoring queries on sp_action.
--
-- existsByRolloutId, getStatusCountByRolloutId, getStatusCountByRolloutGroupId
-- (and similar JPA queries) filter by rollout / rollout_group; the baseline did
-- not index either column, so Postgres falls back to Seq Scan over sp_action on
-- every monitoring poll. With 16k action rows the group-count query takes
-- ~500 ms without the index and ~27 ms with it (Index Only Scan, Heap Fetches: 0).
--
-- IF NOT EXISTS guards against deployments that already created these indexes
-- out-of-band (e.g. via a forked repeatable migration applied prior to this
-- versioned migration landing).
CREATE INDEX IF NOT EXISTS sp_idx_action_rollout_status ON sp_action (tenant, rollout, status);
CREATE INDEX IF NOT EXISTS sp_idx_action_rollout_group  ON sp_action (tenant, rollout_group);
