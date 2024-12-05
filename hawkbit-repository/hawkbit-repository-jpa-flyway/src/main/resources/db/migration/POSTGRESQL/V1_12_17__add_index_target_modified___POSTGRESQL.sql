CREATE INDEX sp_idx_target_05
ON sp_target
USING BTREE (tenant, last_modified_at);