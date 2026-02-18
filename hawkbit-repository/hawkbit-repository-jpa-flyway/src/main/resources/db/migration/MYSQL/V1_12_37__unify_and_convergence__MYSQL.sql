UPDATE sp_software_module SET name = '' WHERE name IS NULL;
UPDATE sp_software_module_type SET name = '' WHERE name IS NULL;
UPDATE sp_distribution_set SET name = '' WHERE name IS NULL;
UPDATE sp_distribution_set_type SET name = '' WHERE name IS NULL;
UPDATE sp_distribution_set_tag SET name = '' WHERE name IS NULL;
UPDATE sp_target SET name = '' WHERE name IS NULL;
UPDATE sp_target_type SET name = '' WHERE name IS NULL;
UPDATE sp_target_tag SET name = '' WHERE name IS NULL;
UPDATE sp_target_filter_query SET name = '' WHERE name IS NULL;
UPDATE sp_rollout SET name = '' WHERE name IS NULL;
UPDATE sp_rollout_group SET name = '' WHERE name IS NULL;
UPDATE sp_target SET controller_id = '' WHERE controller_id IS NULL;

ALTER TABLE sp_action
    MODIFY active TINYINT(1);

ALTER TABLE sp_distribution_set
    MODIFY name VARCHAR(128) NOT NULL,
    MODIFY deleted TINYINT(1),
    MODIFY required_migration_step TINYINT(1);

ALTER TABLE sp_distribution_set_type
    MODIFY name VARCHAR(128) NOT NULL,
    MODIFY deleted TINYINT(1);

ALTER TABLE sp_distribution_set_tag
    MODIFY name VARCHAR(128) NOT NULL;

ALTER TABLE sp_ds_type_element
    MODIFY mandatory TINYINT(1);

ALTER TABLE sp_sm_metadata
    MODIFY target_visible TINYINT(1);

ALTER TABLE sp_software_module
    MODIFY name VARCHAR(128) NOT NULL,
    MODIFY deleted TINYINT(1);

ALTER TABLE sp_software_module_type
    MODIFY name VARCHAR(128) NOT NULL,
    MODIFY deleted TINYINT(1);

ALTER TABLE sp_target
    MODIFY name VARCHAR(128) NOT NULL,
    MODIFY controller_id VARCHAR(256) NOT NULL,
    MODIFY request_controller_attributes TINYINT(1) NOT NULL;

ALTER TABLE sp_target_type
    MODIFY name VARCHAR(128) NOT NULL,
    ALTER COLUMN type_key DROP DEFAULT;

ALTER TABLE sp_target_tag
    MODIFY name VARCHAR(128) NOT NULL;

ALTER TABLE sp_target_filter_query
    MODIFY name VARCHAR(128) NOT NULL;

ALTER TABLE sp_rollout
    MODIFY name VARCHAR(128) NOT NULL,
    DROP COLUMN group_theshold;

ALTER TABLE sp_rollout_group
    MODIFY name VARCHAR(128) NOT NULL,
    DROP INDEX sp_idx_rollout_group_parent;


SET FOREIGN_KEY_CHECKS=0;

ALTER TABLE sp_action
    DROP FOREIGN KEY fk_action_ds,
    ADD CONSTRAINT fk_action_distribution_set
        FOREIGN KEY (distribution_set) REFERENCES sp_distribution_set (id),
    DROP FOREIGN KEY fk_action_rolloutgroup,
    ADD CONSTRAINT fk_action_rollout_group
        FOREIGN KEY (rollout_group) REFERENCES sp_rollout_group (id),
    DROP FOREIGN KEY fk_targ_act_hist_targ,
    ADD CONSTRAINT fk_action_target
        FOREIGN KEY (target) REFERENCES sp_target (id) ON DELETE CASCADE;

ALTER TABLE sp_action_status
    DROP FOREIGN KEY fk_act_stat_action,
    ADD CONSTRAINT fk_action_status_action
        FOREIGN KEY (action) REFERENCES sp_action (id) ON DELETE CASCADE;

ALTER TABLE sp_action_status_messages
    DROP FOREIGN KEY fk_stat_msg_act_stat,
    ADD CONSTRAINT fk_action_status_messages_action_status
        FOREIGN KEY (action_status) REFERENCES sp_action_status (id) ON DELETE CASCADE;

ALTER TABLE sp_artifact
    DROP FOREIGN KEY fk_assigned_sm,
    ADD CONSTRAINT fk_artifact_software_module
        FOREIGN KEY (software_module) REFERENCES sp_software_module (id) ON DELETE CASCADE;

ALTER TABLE sp_distribution_set
    DROP FOREIGN KEY fk_ds_dstype_ds,
    ADD CONSTRAINT fk_distribution_set_ds_type
        FOREIGN KEY (ds_type) REFERENCES sp_distribution_set_type (id);

ALTER TABLE sp_ds_metadata
    DROP FOREIGN KEY fk_metadata_ds,
    ADD CONSTRAINT fk_ds_metadata_ds
        FOREIGN KEY (ds) REFERENCES sp_distribution_set (id) ON DELETE CASCADE;

ALTER TABLE sp_ds_sm
    DROP FOREIGN KEY fk_ds_module_ds,
    ADD CONSTRAINT fk_ds_sm_ds_id
        FOREIGN KEY (ds_id) REFERENCES sp_distribution_set (id) ON DELETE CASCADE,
    DROP FOREIGN KEY fk_ds_module_module,
    ADD CONSTRAINT fk_ds_sm_sm_id
        FOREIGN KEY (sm_id) REFERENCES sp_software_module (id) ON DELETE CASCADE;

ALTER TABLE sp_ds_tag
    DROP FOREIGN KEY fk_ds_dstag_ds,
    ADD CONSTRAINT fk_ds_tag_ds
        FOREIGN KEY (ds) REFERENCES sp_distribution_set (id) ON DELETE CASCADE,
    DROP FOREIGN KEY fk_ds_dstag_tag,
    ADD CONSTRAINT fk_ds_tag_tag
        FOREIGN KEY (tag) REFERENCES sp_distribution_set_tag (id) ON DELETE CASCADE;

ALTER TABLE sp_ds_type_element
    DROP FOREIGN KEY fk_ds_type_element_element,
    ADD CONSTRAINT fk_ds_type_element_distribution_set_type
        FOREIGN KEY (distribution_set_type) REFERENCES sp_distribution_set_type (id) ON DELETE CASCADE,
    DROP FOREIGN KEY fk_ds_type_element_smtype,
    ADD CONSTRAINT fk_ds_type_element_software_module_type
        FOREIGN KEY (software_module_type) REFERENCES sp_software_module_type (id) ON DELETE CASCADE;

ALTER TABLE sp_rollout
    DROP FOREIGN KEY fk_rollout_ds,
    ADD CONSTRAINT fk_rollout_distribution_set
        FOREIGN KEY (distribution_set) REFERENCES sp_distribution_set (id);

ALTER TABLE sp_rollout_group
    DROP FOREIGN KEY fk_rolloutgroup_rollout,
    ADD CONSTRAINT fk_rollout_group_rollout
        FOREIGN KEY (rollout) REFERENCES sp_rollout (id) ON DELETE CASCADE;

ALTER TABLE sp_rollout_target_group
    DROP FOREIGN KEY fk_rollouttargetgroup_rolloutgroup,
    ADD CONSTRAINT fk_rollout_target_group_rollout_group
        FOREIGN KEY (rollout_group) REFERENCES sp_rollout_group (id) ON DELETE CASCADE,
    DROP FOREIGN KEY fk_rollouttargetgroup_target,
    ADD CONSTRAINT fk_rollout_target_group_target
        FOREIGN KEY (target) REFERENCES sp_target (id) ON DELETE CASCADE;

ALTER TABLE sp_sm_metadata
    DROP FOREIGN KEY fk_metadata_sw,
    ADD CONSTRAINT fk_sm_metadata_sm
        FOREIGN KEY (sm) REFERENCES sp_software_module (id) ON DELETE CASCADE;

ALTER TABLE sp_software_module
    DROP FOREIGN KEY fk_module_type,
    ADD CONSTRAINT fk_software_module_sm_type
        FOREIGN KEY (sm_type) REFERENCES sp_software_module_type (id);

ALTER TABLE sp_target_attributes
    DROP FOREIGN KEY fk_targ_attrib_target,
    ADD CONSTRAINT fk_target_attributes_target
        FOREIGN KEY (target) REFERENCES sp_target (id) ON DELETE CASCADE;

ALTER TABLE sp_target_conf_status
    DROP FOREIGN KEY fk_target_auto_conf,
    ADD CONSTRAINT fk_target_conf_status_target
        FOREIGN KEY (target) REFERENCES sp_target (id) ON DELETE CASCADE;

ALTER TABLE sp_target_filter_query
    DROP FOREIGN KEY fk_filter_auto_assign_ds,
    ADD CONSTRAINT fk_target_filter_query_auto_assign_distribution_set
        FOREIGN KEY (auto_assign_distribution_set) REFERENCES sp_distribution_set (id) ON DELETE SET NULL;

ALTER TABLE sp_target_metadata
    DROP FOREIGN KEY fk_metadata_target,
    ADD CONSTRAINT fk_target_metadata_target
        FOREIGN KEY (target) REFERENCES sp_target (id) ON DELETE CASCADE;

ALTER TABLE sp_target_target_tag
    DROP FOREIGN KEY fk_targ_targtag_tag,
    ADD CONSTRAINT fk_target_target_tag_tag
        FOREIGN KEY (tag) REFERENCES sp_target_tag (id) ON DELETE CASCADE,
    DROP FOREIGN KEY fk_targ_targtag_target,
    ADD CONSTRAINT fk_target_target_tag_target
        FOREIGN KEY (target) REFERENCES sp_target (id) ON DELETE CASCADE;

ALTER TABLE sp_target_type_ds_type
    DROP FOREIGN KEY fk_target_type_relation_ds_type,
    ADD CONSTRAINT fk_target_type_ds_type_distribution_set_type
        FOREIGN KEY (distribution_set_type) REFERENCES sp_distribution_set_type (id) ON DELETE CASCADE,
    DROP FOREIGN KEY fk_target_type_relation_target_type,
    ADD CONSTRAINT fk_target_type_ds_type_target_type
        FOREIGN KEY (target_type) REFERENCES sp_target_type (id) ON DELETE CASCADE;

ALTER TABLE sp_tenant
    DROP FOREIGN KEY fk_tenant_md_default_ds_type,
    ADD CONSTRAINT fk_tenant_default_ds_type
        FOREIGN KEY (default_ds_type) REFERENCES sp_distribution_set_type (id);

SET FOREIGN_KEY_CHECKS=1;
