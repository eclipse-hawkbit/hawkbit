ALTER TABLE sp_target ADD COLUMN target_group VARCHAR(256);
CREATE INDEX sp_idx_target_group ON sp_target (tenant, target_group);