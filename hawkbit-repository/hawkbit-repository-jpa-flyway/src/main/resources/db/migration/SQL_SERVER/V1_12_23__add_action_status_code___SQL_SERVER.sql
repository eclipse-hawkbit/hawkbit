ALTER TABLE sp_action_status ADD code INT;
CREATE INDEX sp_idx_action_status_03 ON sp_action_status (tenant, code);