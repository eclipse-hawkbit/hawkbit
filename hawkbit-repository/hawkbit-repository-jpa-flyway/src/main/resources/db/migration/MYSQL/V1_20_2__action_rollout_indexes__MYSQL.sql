-- See POSTGRESQL/V1_20_2__action_rollout_indexes__POSTGRESQL.sql for rationale.
-- MySQL ≤ 8.x has no native CREATE INDEX IF NOT EXISTS, so guard via INFORMATION_SCHEMA.

SET @stmt := IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
        WHERE table_schema = DATABASE()
          AND table_name = 'sp_action'
          AND index_name = 'sp_idx_action_rollout_status') = 0,
    'CREATE INDEX sp_idx_action_rollout_status ON sp_action (tenant, rollout, status)',
    'SELECT 1');
PREPARE _create_idx FROM @stmt;
EXECUTE _create_idx;
DEALLOCATE PREPARE _create_idx;

SET @stmt := IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
        WHERE table_schema = DATABASE()
          AND table_name = 'sp_action'
          AND index_name = 'sp_idx_action_rollout_group') = 0,
    'CREATE INDEX sp_idx_action_rollout_group ON sp_action (tenant, rollout_group)',
    'SELECT 1');
PREPARE _create_idx FROM @stmt;
EXECUTE _create_idx;
DEALLOCATE PREPARE _create_idx;
