ALTER TABLE sp_action ADD external_ref VARCHAR(512);
CREATE INDEX sp_idx_action_external_ref ON sp_action (external_ref);

    