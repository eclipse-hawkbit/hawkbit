CREATE INDEX sp_idx_rollouttargetgroup_target_id
    ON sp_rollouttargetgroup
    USING BTREE (target_id);

CREATE INDEX sp_idx_target_attributes_target_id
    ON sp_target_attributes
    USING BTREE (target_id);

CREATE INDEX sp_idx_action_target
    ON sp_action
    USING BTREE (target);
