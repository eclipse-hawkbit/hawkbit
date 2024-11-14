ALTER TABLE sp_action_status ADD COLUMN code INTEGER;
CREATE INDEX sp_idx_action_status_03
    ON sp_action_status
    USING BTREE (tenant, code);