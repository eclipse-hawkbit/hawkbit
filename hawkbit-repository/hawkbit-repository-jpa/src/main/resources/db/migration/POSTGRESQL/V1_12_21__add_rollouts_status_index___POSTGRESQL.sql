CREATE INDEX sp_idx_rollout_status_tenant
ON sp_rollout
USING BTREE (tenant, status);