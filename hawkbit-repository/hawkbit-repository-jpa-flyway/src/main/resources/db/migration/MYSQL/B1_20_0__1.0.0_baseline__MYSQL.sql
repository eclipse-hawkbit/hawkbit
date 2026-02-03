-- hawkbit 1.0.0 MySQL database migration script baseline --

CREATE TABLE SP_LOCK (
    LOCK_KEY CHAR(36) NOT NULL,
    REGION VARCHAR(100) NOT NULL,
    CLIENT_ID CHAR(36) DEFAULT NULL,
    CREATED_DATE DATETIME(6) NOT NULL,
    PRIMARY KEY (LOCK_KEY, REGION)
);

CREATE TABLE sp_action (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at BIGINT DEFAULT NULL,
    created_by VARCHAR(64) DEFAULT NULL,
    last_modified_at BIGINT DEFAULT NULL,
    last_modified_by VARCHAR(64) DEFAULT NULL,
    optlock_revision BIGINT DEFAULT NULL,
    tenant VARCHAR(40) NOT NULL,
    active BIT(1) DEFAULT NULL,
    forced_time BIGINT DEFAULT NULL,
    status INT NOT NULL,
    distribution_set BIGINT NOT NULL,
    target BIGINT NOT NULL,
    rollout BIGINT DEFAULT NULL,
    rollout_group BIGINT DEFAULT NULL,
    action_type INT NOT NULL,
    maintenance_cron_schedule VARCHAR(40) DEFAULT NULL,
    maintenance_duration VARCHAR(40) DEFAULT NULL,
    maintenance_time_zone VARCHAR(40) DEFAULT NULL,
    external_ref VARCHAR(512) DEFAULT NULL,
    weight INT NOT NULL,
    initiated_by VARCHAR(64) NOT NULL,
    last_action_status_code INT DEFAULT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE sp_action_status (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at BIGINT DEFAULT NULL,
    created_by VARCHAR(64) DEFAULT NULL,
    last_modified_at BIGINT DEFAULT NULL,
    last_modified_by VARCHAR(64) DEFAULT NULL,
    optlock_revision BIGINT DEFAULT NULL,
    tenant VARCHAR(40) NOT NULL,
    target_occurred_at BIGINT NOT NULL,
    status INT NOT NULL,
    action BIGINT NOT NULL,
    code INT DEFAULT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE sp_action_status_messages (
    action_status BIGINT NOT NULL,
    detail_message VARCHAR(512) NOT NULL
);

CREATE TABLE sp_artifact (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at BIGINT DEFAULT NULL,
    created_by VARCHAR(64) DEFAULT NULL,
    last_modified_at BIGINT DEFAULT NULL,
    last_modified_by VARCHAR(64) DEFAULT NULL,
    optlock_revision BIGINT DEFAULT NULL,
    tenant VARCHAR(40) NOT NULL,
    md5_hash VARCHAR(32) DEFAULT NULL,
    file_size BIGINT DEFAULT NULL,
    provided_file_name VARCHAR(256) DEFAULT NULL,
    sha1_hash VARCHAR(40) NOT NULL,
    software_module BIGINT NOT NULL,
    sha256_hash CHAR(64) DEFAULT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE sp_distribution_set (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at BIGINT DEFAULT NULL,
    created_by VARCHAR(64) DEFAULT NULL,
    last_modified_at BIGINT DEFAULT NULL,
    last_modified_by VARCHAR(64) DEFAULT NULL,
    optlock_revision BIGINT DEFAULT NULL,
    tenant VARCHAR(40) NOT NULL,
    description VARCHAR(512) DEFAULT NULL,
    name VARCHAR(128) DEFAULT NULL,
    version VARCHAR(64) NOT NULL,
    deleted BIT(1) DEFAULT NULL,
    required_migration_step BIT(1) DEFAULT NULL,
    ds_type BIGINT NOT NULL,
    valid TINYINT(1) DEFAULT NULL,
    locked TINYINT(1) NOT NULL DEFAULT '1',
    PRIMARY KEY (id)
);

CREATE TABLE sp_distribution_set_tag (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at BIGINT DEFAULT NULL,
    created_by VARCHAR(64) DEFAULT NULL,
    last_modified_at BIGINT DEFAULT NULL,
    last_modified_by VARCHAR(64) DEFAULT NULL,
    optlock_revision BIGINT DEFAULT NULL,
    tenant VARCHAR(40) NOT NULL,
    description VARCHAR(512) DEFAULT NULL,
    name VARCHAR(128) DEFAULT NULL,
    colour VARCHAR(16) DEFAULT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE sp_distribution_set_type (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at BIGINT DEFAULT NULL,
    created_by VARCHAR(64) DEFAULT NULL,
    last_modified_at BIGINT DEFAULT NULL,
    last_modified_by VARCHAR(64) DEFAULT NULL,
    optlock_revision BIGINT DEFAULT NULL,
    tenant VARCHAR(40) NOT NULL,
    description VARCHAR(512) DEFAULT NULL,
    name VARCHAR(128) DEFAULT NULL,
    colour VARCHAR(16) DEFAULT NULL,
    deleted BIT(1) DEFAULT NULL,
    type_key VARCHAR(64) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE sp_ds_metadata (
    meta_key VARCHAR(128) NOT NULL,
    meta_value VARCHAR(4000) DEFAULT NULL,
    ds BIGINT NOT NULL,
    PRIMARY KEY (ds, meta_key)
);

CREATE TABLE sp_ds_sm (
    ds_id BIGINT NOT NULL,
    sm_id BIGINT NOT NULL,
    PRIMARY KEY (ds_id, sm_id)
);

CREATE TABLE sp_ds_tag (
    ds BIGINT NOT NULL,
    tag BIGINT NOT NULL,
    PRIMARY KEY (ds, tag)
);

CREATE TABLE sp_ds_type_element (
    mandatory BIT(1) DEFAULT NULL,
    distribution_set_type BIGINT NOT NULL,
    software_module_type BIGINT NOT NULL,
    PRIMARY KEY (distribution_set_type, software_module_type)
);

CREATE TABLE sp_rollout (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at BIGINT DEFAULT NULL,
    created_by VARCHAR(64) DEFAULT NULL,
    last_modified_at BIGINT DEFAULT NULL,
    last_modified_by VARCHAR(64) DEFAULT NULL,
    optlock_revision BIGINT DEFAULT NULL,
    tenant VARCHAR(40) NOT NULL,
    description VARCHAR(512) DEFAULT NULL,
    name VARCHAR(128) DEFAULT NULL,
    last_check BIGINT DEFAULT NULL,
    group_theshold FLOAT DEFAULT NULL,
    status INT NOT NULL,
    distribution_set BIGINT NOT NULL,
    target_filter VARCHAR(1024) DEFAULT NULL,
    forced_time BIGINT DEFAULT NULL,
    total_targets BIGINT DEFAULT NULL,
    rollout_groups_created BIGINT DEFAULT NULL,
    start_at BIGINT DEFAULT NULL,
    deleted TINYINT(1) DEFAULT NULL,
    action_type INT NOT NULL,
    approval_decided_by VARCHAR(64) DEFAULT NULL,
    approval_remark VARCHAR(255) DEFAULT NULL,
    weight INT NOT NULL,
    access_control_context VARCHAR(4096) DEFAULT NULL,
    is_dynamic TINYINT(1) DEFAULT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE sp_rollout_group (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at BIGINT DEFAULT NULL,
    created_by VARCHAR(64) DEFAULT NULL,
    last_modified_at BIGINT DEFAULT NULL,
    last_modified_by VARCHAR(64) DEFAULT NULL,
    optlock_revision BIGINT DEFAULT NULL,
    tenant VARCHAR(40) NOT NULL,
    description VARCHAR(512) DEFAULT NULL,
    name VARCHAR(128) DEFAULT NULL,
    error_condition INT DEFAULT NULL,
    error_condition_exp VARCHAR(512) DEFAULT NULL,
    error_action INT DEFAULT NULL,
    error_action_exp VARCHAR(512) DEFAULT NULL,
    success_condition INT NOT NULL,
    success_condition_exp VARCHAR(512) NOT NULL,
    success_action INT NOT NULL,
    success_action_exp VARCHAR(512) DEFAULT NULL,
    status INT NOT NULL,
    parent BIGINT DEFAULT NULL,
    rollout BIGINT NOT NULL,
    total_targets BIGINT DEFAULT NULL,
    target_percentage FLOAT DEFAULT NULL,
    target_filter VARCHAR(1024) DEFAULT NULL,
    confirmation_required TINYINT(1) DEFAULT NULL,
    is_dynamic TINYINT(1) NOT NULL DEFAULT '0',
    PRIMARY KEY (id)
);

CREATE TABLE sp_rollout_target_group (
    target BIGINT NOT NULL,
    rollout_group BIGINT NOT NULL,
    PRIMARY KEY (rollout_group, target)
);

CREATE TABLE sp_sm_metadata (
    meta_key VARCHAR(128) NOT NULL,
    meta_value VARCHAR(4000) DEFAULT NULL,
    sm BIGINT NOT NULL,
    target_visible BIT(1) DEFAULT NULL,
    PRIMARY KEY (meta_key, sm)
);

CREATE TABLE sp_software_module (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at BIGINT DEFAULT NULL,
    created_by VARCHAR(64) DEFAULT NULL,
    last_modified_at BIGINT DEFAULT NULL,
    last_modified_by VARCHAR(64) DEFAULT NULL,
    optlock_revision BIGINT DEFAULT NULL,
    tenant VARCHAR(40) NOT NULL,
    description VARCHAR(512) DEFAULT NULL,
    name VARCHAR(128) DEFAULT NULL,
    version VARCHAR(64) NOT NULL,
    deleted BIT(1) DEFAULT NULL,
    vendor VARCHAR(256) DEFAULT NULL,
    sm_type BIGINT NOT NULL,
    encrypted TINYINT(1) DEFAULT NULL,
    locked TINYINT(1) NOT NULL DEFAULT '1',
    PRIMARY KEY (id)
);

CREATE TABLE sp_software_module_type (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at BIGINT DEFAULT NULL,
    created_by VARCHAR(64) DEFAULT NULL,
    last_modified_at BIGINT DEFAULT NULL,
    last_modified_by VARCHAR(64) DEFAULT NULL,
    optlock_revision BIGINT DEFAULT NULL,
    tenant VARCHAR(40) NOT NULL,
    description VARCHAR(512) DEFAULT NULL,
    name VARCHAR(128) DEFAULT NULL,
    colour VARCHAR(16) DEFAULT NULL,
    deleted BIT(1) DEFAULT NULL,
    type_key VARCHAR(64) NOT NULL,
    max_ds_assignments INT NOT NULL,
    min_artifacts INT NOT NULL DEFAULT '0',
    PRIMARY KEY (id)
);

CREATE TABLE sp_target (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at BIGINT DEFAULT NULL,
    created_by VARCHAR(64) DEFAULT NULL,
    last_modified_at BIGINT DEFAULT NULL,
    last_modified_by VARCHAR(64) DEFAULT NULL,
    optlock_revision BIGINT DEFAULT NULL,
    tenant VARCHAR(40) NOT NULL,
    description VARCHAR(512) DEFAULT NULL,
    name VARCHAR(128) DEFAULT NULL,
    controller_id VARCHAR(256) DEFAULT NULL,
    sec_token VARCHAR(128) NOT NULL,
    assigned_distribution_set BIGINT DEFAULT NULL,
    install_date BIGINT DEFAULT NULL,
    address VARCHAR(512) DEFAULT NULL,
    last_target_query BIGINT DEFAULT NULL,
    request_controller_attributes BIT(1) NOT NULL,
    installed_distribution_set BIGINT DEFAULT NULL,
    update_status INT NOT NULL,
    target_type BIGINT DEFAULT NULL,
    target_group VARCHAR(256) DEFAULT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE sp_target_attributes (
    target BIGINT NOT NULL,
    attribute_value VARCHAR(128) DEFAULT NULL,
    attribute_key VARCHAR(128) NOT NULL,
    PRIMARY KEY (target, attribute_key)
);

CREATE TABLE sp_target_conf_status (
    id BIGINT NOT NULL AUTO_INCREMENT,
    target BIGINT NOT NULL,
    initiator VARCHAR(64) DEFAULT NULL,
    remark VARCHAR(512) DEFAULT NULL,
    created_at BIGINT DEFAULT NULL,
    created_by VARCHAR(64) DEFAULT NULL,
    last_modified_at BIGINT DEFAULT NULL,
    last_modified_by VARCHAR(64) DEFAULT NULL,
    optlock_revision BIGINT DEFAULT NULL,
    tenant VARCHAR(40) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE sp_target_filter_query (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at BIGINT DEFAULT NULL,
    created_by VARCHAR(64) DEFAULT NULL,
    last_modified_at BIGINT DEFAULT NULL,
    last_modified_by VARCHAR(64) DEFAULT NULL,
    optlock_revision BIGINT DEFAULT NULL,
    tenant VARCHAR(40) NOT NULL,
    name VARCHAR(128) DEFAULT NULL,
    query VARCHAR(1024) NOT NULL,
    auto_assign_distribution_set BIGINT DEFAULT NULL,
    auto_assign_action_type INT DEFAULT NULL,
    auto_assign_weight INT NOT NULL,
    auto_assign_initiated_by VARCHAR(64) DEFAULT NULL,
    confirmation_required TINYINT(1) DEFAULT NULL,
    access_control_context VARCHAR(4096) DEFAULT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE sp_target_metadata (
    meta_key VARCHAR(128) NOT NULL,
    meta_value VARCHAR(4000) DEFAULT NULL,
    target BIGINT NOT NULL,
    PRIMARY KEY (target, meta_key)
);

CREATE TABLE sp_target_tag (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at BIGINT DEFAULT NULL,
    created_by VARCHAR(64) DEFAULT NULL,
    last_modified_at BIGINT DEFAULT NULL,
    last_modified_by VARCHAR(64) DEFAULT NULL,
    optlock_revision BIGINT DEFAULT NULL,
    tenant VARCHAR(40) NOT NULL,
    description VARCHAR(512) DEFAULT NULL,
    name VARCHAR(128) DEFAULT NULL,
    colour VARCHAR(16) DEFAULT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE sp_target_target_tag (
    target BIGINT NOT NULL,
    tag BIGINT NOT NULL,
    PRIMARY KEY (target, tag)
);

CREATE TABLE sp_target_type (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at BIGINT DEFAULT NULL,
    created_by VARCHAR(64) DEFAULT NULL,
    last_modified_at BIGINT DEFAULT NULL,
    last_modified_by VARCHAR(64) DEFAULT NULL,
    optlock_revision BIGINT DEFAULT NULL,
    tenant VARCHAR(40) NOT NULL,
    description VARCHAR(512) DEFAULT NULL,
    name VARCHAR(128) DEFAULT NULL,
    colour VARCHAR(16) DEFAULT NULL,
    type_key VARCHAR(64) NOT NULL DEFAULT (_utf8mb4'_'),
    PRIMARY KEY (id)
);

CREATE TABLE sp_target_type_ds_type (
    target_type BIGINT NOT NULL,
    distribution_set_type BIGINT NOT NULL,
    PRIMARY KEY (target_type, distribution_set_type)
);

CREATE TABLE sp_tenant (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at BIGINT DEFAULT NULL,
    created_by VARCHAR(64) DEFAULT NULL,
    last_modified_at BIGINT DEFAULT NULL,
    last_modified_by VARCHAR(64) DEFAULT NULL,
    optlock_revision BIGINT DEFAULT NULL,
    tenant VARCHAR(40) NOT NULL,
    default_ds_type BIGINT NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE sp_tenant_configuration (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at BIGINT DEFAULT NULL,
    created_by VARCHAR(64) DEFAULT NULL,
    last_modified_at BIGINT DEFAULT NULL,
    last_modified_by VARCHAR(64) DEFAULT NULL,
    optlock_revision BIGINT DEFAULT NULL,
    tenant VARCHAR(40) NOT NULL,
    conf_key VARCHAR(128) NOT NULL,
    conf_value VARCHAR(512) NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX sp_idx_action_01 ON sp_action (tenant, distribution_set);
CREATE INDEX sp_idx_action_02 ON sp_action (tenant, target, active);
CREATE INDEX sp_idx_action_prim ON sp_action (tenant, id);
CREATE INDEX fk_action_distribution_set ON sp_action (distribution_set);
CREATE INDEX fk_action_target ON sp_action (target);
CREATE INDEX fk_action_rollout ON sp_action (rollout);
CREATE INDEX fk_action_rollout_group ON sp_action (rollout_group);
CREATE INDEX sp_idx_action_external_ref ON sp_action (external_ref);
CREATE INDEX sp_idx_action_status_02 ON sp_action_status (tenant, action, status);
CREATE INDEX sp_idx_action_status_prim ON sp_action_status (tenant, id);
CREATE INDEX fk_action_status_action ON sp_action_status (action);
CREATE INDEX sp_idx_action_status_03 ON sp_action_status (tenant, code);
CREATE INDEX fk_action_status_messages_action_status ON sp_action_status_messages (action_status);
CREATE INDEX sp_idx_artifact_01 ON sp_artifact (tenant, software_module);
CREATE INDEX sp_idx_artifact_prim ON sp_artifact (tenant, id);
CREATE INDEX fk_artifact_software_module ON sp_artifact (software_module);
CREATE INDEX sp_idx_artifact_02 ON sp_artifact (tenant, sha1_hash);
CREATE UNIQUE INDEX uk_distribution_set ON sp_distribution_set (tenant, name, version, ds_type);
CREATE INDEX sp_idx_distribution_set_prim ON sp_distribution_set (tenant, id);
CREATE INDEX fk_distribution_set_ds_type ON sp_distribution_set (ds_type);
CREATE INDEX sp_idx_distribution_set_01 ON sp_distribution_set (tenant, deleted);
CREATE UNIQUE INDEX uk_distribution_set_tag ON sp_distribution_set_tag (name, tenant);
CREATE INDEX sp_idx_distribution_set_tag_prim ON sp_distribution_set_tag (tenant, id);
CREATE INDEX sp_idx_distribution_set_tag_01 ON sp_distribution_set_tag (tenant, name);
CREATE UNIQUE INDEX uk_distribution_set_type_type_key ON sp_distribution_set_type (type_key, tenant);
CREATE UNIQUE INDEX uk_distribution_set_type_name ON sp_distribution_set_type (name, tenant);
CREATE INDEX sp_idx_distribution_set_type_01 ON sp_distribution_set_type (tenant, deleted);
CREATE INDEX sp_idx_distribution_set_type_prim ON sp_distribution_set_type (tenant, id);
CREATE INDEX fk_ds_sm_sm_id ON sp_ds_sm (sm_id);
CREATE INDEX fk_ds_tag_tag ON sp_ds_tag (tag);
CREATE INDEX fk_ds_type_element_software_module_type ON sp_ds_type_element (software_module_type);
CREATE UNIQUE INDEX uk_rollout ON sp_rollout (name, tenant);
CREATE INDEX fk_rollout_distribution_set ON sp_rollout (distribution_set);
CREATE INDEX sp_idx_rollout_status_tenant ON sp_rollout (tenant, status);
CREATE UNIQUE INDEX uk_rollout_group ON sp_rollout_group (name, rollout, tenant);
CREATE INDEX fk_rollout_group_rollout ON sp_rollout_group (rollout);
CREATE INDEX sp_idx_rollout_group_parent ON sp_rollout_group (parent);
CREATE INDEX fk_rollout_target_group_target ON sp_rollout_target_group (target);
CREATE INDEX fk_sm_metadata_sm ON sp_sm_metadata (sm);
CREATE UNIQUE INDEX uk_software_module ON sp_software_module (sm_type, name, version, tenant);
CREATE INDEX sp_idx_software_module_01 ON sp_software_module (tenant, deleted, name, version);
CREATE INDEX sp_idx_software_module_02 ON sp_software_module (tenant, deleted, sm_type);
CREATE INDEX sp_idx_software_module_prim ON sp_software_module (tenant, id);
CREATE UNIQUE INDEX uk_software_module_type_type_key ON sp_software_module_type (type_key, tenant);
CREATE UNIQUE INDEX uk_software_module_type_name ON sp_software_module_type (name, tenant);
CREATE INDEX sp_idx_software_module_type_01 ON sp_software_module_type (tenant, deleted);
CREATE INDEX sp_idx_software_module_type_prim ON sp_software_module_type (tenant, id);
CREATE UNIQUE INDEX uk_target_controller_id ON sp_target (controller_id, tenant);
CREATE INDEX sp_idx_target_01 ON sp_target (tenant, name, assigned_distribution_set);
CREATE INDEX sp_idx_target_03 ON sp_target (tenant, controller_id, assigned_distribution_set);
CREATE INDEX sp_idx_target_04 ON sp_target (tenant, created_at);
CREATE INDEX sp_idx_target_prim ON sp_target (tenant, id);
CREATE INDEX fk_target_assign_ds ON sp_target (assigned_distribution_set);
CREATE INDEX fk_target_inst_ds ON sp_target (installed_distribution_set);
CREATE INDEX sp_idx_target_05 ON sp_target (tenant, last_modified_at);
CREATE INDEX fk_target_relation_target_type ON sp_target (target_type);
CREATE INDEX sp_idx_target_group ON sp_target (tenant, target_group);
CREATE INDEX fk_target_conf_status_target ON sp_target_conf_status (target);
CREATE UNIQUE INDEX uk_target_filter_query ON sp_target_filter_query (name, tenant);
CREATE INDEX fk_target_filter_query_auto_assign_distribution_set ON sp_target_filter_query (auto_assign_distribution_set);
CREATE UNIQUE INDEX uk_target_tag ON sp_target_tag (name, tenant);
CREATE INDEX sp_idx_target_tag_prim ON sp_target_tag (tenant, id);
CREATE INDEX sp_idx_target_tag_01 ON sp_target_tag (tenant, name);
CREATE INDEX fk_target_target_tag_tag ON sp_target_target_tag (tag);
CREATE UNIQUE INDEX uk_target_type_key ON sp_target_type (type_key, tenant);
CREATE UNIQUE INDEX uk_target_name ON sp_target_type (name, tenant);
CREATE INDEX sp_idx_target_type_prim ON sp_target_type (tenant, id);
CREATE INDEX fk_target_type_ds_type_distribution_set_type ON sp_target_type_ds_type (distribution_set_type);
CREATE UNIQUE INDEX uk_tenant ON sp_tenant (tenant);
CREATE INDEX sp_idx_tenant_prim ON sp_tenant (tenant, id);
CREATE INDEX fk_tenant_default_ds_type ON sp_tenant (default_ds_type);
CREATE UNIQUE INDEX uk_tenant_configuration ON sp_tenant_configuration (conf_key, tenant);

ALTER TABLE sp_action
    ADD CONSTRAINT fk_action_ds FOREIGN KEY (distribution_set) REFERENCES sp_distribution_set (id);
ALTER TABLE sp_action
    ADD CONSTRAINT fk_action_rollout FOREIGN KEY (rollout) REFERENCES sp_rollout (id);
ALTER TABLE sp_action
    ADD CONSTRAINT fk_action_rolloutgroup FOREIGN KEY (rollout_group) REFERENCES sp_rollout_group (id);
ALTER TABLE sp_action
    ADD CONSTRAINT fk_targ_act_hist_targ FOREIGN KEY (target) REFERENCES sp_target (id) ON DELETE CASCADE;
ALTER TABLE sp_action_status
    ADD CONSTRAINT fk_act_stat_action FOREIGN KEY (action) REFERENCES sp_action (id) ON DELETE CASCADE;
ALTER TABLE sp_action_status_messages
    ADD CONSTRAINT fk_stat_msg_act_stat FOREIGN KEY (action_status) REFERENCES sp_action_status (id) ON DELETE CASCADE;
ALTER TABLE sp_artifact
    ADD CONSTRAINT fk_assigned_sm FOREIGN KEY (software_module) REFERENCES sp_software_module (id) ON DELETE CASCADE;
ALTER TABLE sp_distribution_set
    ADD CONSTRAINT fk_ds_dstype_ds FOREIGN KEY (ds_type) REFERENCES sp_distribution_set_type (id);
ALTER TABLE sp_ds_metadata
    ADD CONSTRAINT fk_metadata_ds FOREIGN KEY (ds) REFERENCES sp_distribution_set (id) ON DELETE CASCADE;
ALTER TABLE sp_ds_sm
    ADD CONSTRAINT fk_ds_module_ds FOREIGN KEY (ds_id) REFERENCES sp_distribution_set (id) ON DELETE CASCADE;
ALTER TABLE sp_ds_sm
    ADD CONSTRAINT fk_ds_module_module FOREIGN KEY (sm_id) REFERENCES sp_software_module (id) ON DELETE CASCADE;
ALTER TABLE sp_ds_tag
    ADD CONSTRAINT fk_ds_dstag_ds FOREIGN KEY (ds) REFERENCES sp_distribution_set (id) ON DELETE CASCADE;
ALTER TABLE sp_ds_tag
    ADD CONSTRAINT fk_ds_dstag_tag FOREIGN KEY (tag) REFERENCES sp_distribution_set_tag (id) ON DELETE CASCADE;
ALTER TABLE sp_ds_type_element
    ADD CONSTRAINT fk_ds_type_element_element FOREIGN KEY (distribution_set_type) REFERENCES sp_distribution_set_type (id) ON DELETE CASCADE;
ALTER TABLE sp_ds_type_element
    ADD CONSTRAINT fk_ds_type_element_smtype FOREIGN KEY (software_module_type) REFERENCES sp_software_module_type (id) ON DELETE CASCADE;
ALTER TABLE sp_rollout
    ADD CONSTRAINT fk_rollout_ds FOREIGN KEY (distribution_set) REFERENCES sp_distribution_set (id);
ALTER TABLE sp_rollout_group
    ADD CONSTRAINT fk_rolloutgroup_rollout FOREIGN KEY (rollout) REFERENCES sp_rollout (id) ON DELETE CASCADE;
ALTER TABLE sp_rollout_target_group
    ADD CONSTRAINT fk_rollouttargetgroup_rolloutgroup FOREIGN KEY (rollout_group) REFERENCES sp_rollout_group (id) ON DELETE CASCADE;
ALTER TABLE sp_rollout_target_group
    ADD CONSTRAINT fk_rollouttargetgroup_target FOREIGN KEY (target) REFERENCES sp_target (id) ON DELETE CASCADE;
ALTER TABLE sp_sm_metadata
    ADD CONSTRAINT fk_metadata_sw FOREIGN KEY (sm) REFERENCES sp_software_module (id) ON DELETE CASCADE;
ALTER TABLE sp_software_module
    ADD CONSTRAINT fk_module_type FOREIGN KEY (sm_type) REFERENCES sp_software_module_type (id);
ALTER TABLE sp_target
    ADD CONSTRAINT fk_target_assign_ds FOREIGN KEY (assigned_distribution_set) REFERENCES sp_distribution_set (id);
ALTER TABLE sp_target
    ADD CONSTRAINT fk_target_inst_ds FOREIGN KEY (installed_distribution_set) REFERENCES sp_distribution_set (id);
ALTER TABLE sp_target
    ADD CONSTRAINT fk_target_relation_target_type FOREIGN KEY (target_type) REFERENCES sp_target_type (id) ON DELETE SET NULL;
ALTER TABLE sp_target_attributes
    ADD CONSTRAINT fk_targ_attrib_target FOREIGN KEY (target) REFERENCES sp_target (id) ON DELETE CASCADE;
ALTER TABLE sp_target_conf_status
    ADD CONSTRAINT fk_target_auto_conf FOREIGN KEY (target) REFERENCES sp_target (id) ON DELETE CASCADE;
ALTER TABLE sp_target_filter_query
    ADD CONSTRAINT fk_filter_auto_assign_ds FOREIGN KEY (auto_assign_distribution_set) REFERENCES sp_distribution_set (id) ON DELETE SET NULL;
ALTER TABLE sp_target_metadata
    ADD CONSTRAINT fk_metadata_target FOREIGN KEY (target) REFERENCES sp_target (id) ON DELETE CASCADE;
ALTER TABLE sp_target_target_tag
    ADD CONSTRAINT fk_targ_targtag_tag FOREIGN KEY (tag) REFERENCES sp_target_tag (id) ON DELETE CASCADE;
ALTER TABLE sp_target_target_tag
    ADD CONSTRAINT fk_targ_targtag_target FOREIGN KEY (target) REFERENCES sp_target (id) ON DELETE CASCADE;
ALTER TABLE sp_target_type_ds_type
    ADD CONSTRAINT fk_target_type_relation_ds_type FOREIGN KEY (distribution_set_type) REFERENCES sp_distribution_set_type (id) ON DELETE CASCADE;
ALTER TABLE sp_target_type_ds_type
    ADD CONSTRAINT fk_target_type_relation_target_type FOREIGN KEY (target_type) REFERENCES sp_target_type (id) ON DELETE CASCADE;
ALTER TABLE sp_tenant
    ADD CONSTRAINT fk_tenant_md_default_ds_type FOREIGN KEY (default_ds_type) REFERENCES sp_distribution_set_type (id);