-- hawkbit 1.0.0 MySQL database migration script baseline --

CREATE TABLE SP_LOCK ( -- spring table --
    LOCK_KEY CHAR(36) NOT NULL,
    REGION VARCHAR(100) NOT NULL,
    CLIENT_ID CHAR(36),
    CREATED_DATE DATETIME(6) NOT NULL,
    PRIMARY KEY (LOCK_KEY, REGION)
);

CREATE TABLE sp_distribution_set_tag (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at BIGINT,
    created_by VARCHAR(64),
    last_modified_at BIGINT,
    last_modified_by VARCHAR(64),
    optlock_revision BIGINT,
    tenant VARCHAR(40) NOT NULL,
    description VARCHAR(512),
    name VARCHAR(128) NOT NULL,
    colour VARCHAR(16),
    PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_distribution_set_tag ON sp_distribution_set_tag (name, tenant);
CREATE INDEX sp_idx_distribution_set_tag_prim ON sp_distribution_set_tag (tenant, id);
CREATE INDEX sp_idx_distribution_set_tag_01 ON sp_distribution_set_tag (tenant, name);

CREATE TABLE sp_distribution_set_type (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at BIGINT,
    created_by VARCHAR(64),
    last_modified_at BIGINT,
    last_modified_by VARCHAR(64),
    optlock_revision BIGINT,
    tenant VARCHAR(40) NOT NULL,
    description VARCHAR(512),
    name VARCHAR(128) NOT NULL,
    colour VARCHAR(16),
    deleted BOOLEAN,
    type_key VARCHAR(64) NOT NULL,
    PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_distribution_set_type_type_key ON sp_distribution_set_type (type_key, tenant);
CREATE UNIQUE INDEX uk_distribution_set_type_name ON sp_distribution_set_type (name, tenant);
CREATE INDEX sp_idx_distribution_set_type_prim ON sp_distribution_set_type (tenant, id);
CREATE INDEX sp_idx_distribution_set_type_01 ON sp_distribution_set_type (tenant, deleted);

CREATE TABLE sp_software_module_type (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at BIGINT,
    created_by VARCHAR(64),
    last_modified_at BIGINT,
    last_modified_by VARCHAR(64),
    optlock_revision BIGINT,
    tenant VARCHAR(40) NOT NULL,
    description VARCHAR(512),
    name VARCHAR(128) NOT NULL,
    colour VARCHAR(16),
    deleted BOOLEAN,
    type_key VARCHAR(64) NOT NULL,
    max_ds_assignments INTEGER NOT NULL,
    min_artifacts INTEGER DEFAULT 0 NOT NULL,
    PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_software_module_type_type_key ON sp_software_module_type (type_key, tenant);
CREATE UNIQUE INDEX uk_software_module_type_name ON sp_software_module_type (name, tenant);
CREATE INDEX sp_idx_software_module_type_prim ON sp_software_module_type (tenant, id);
CREATE INDEX sp_idx_software_module_type_01 ON sp_software_module_type (tenant, deleted);

CREATE TABLE sp_target_tag (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at BIGINT,
    created_by VARCHAR(64),
    last_modified_at BIGINT,
    last_modified_by VARCHAR(64),
    optlock_revision BIGINT,
    tenant VARCHAR(40) NOT NULL,
    description VARCHAR(512),
    name VARCHAR(128) NOT NULL,
    colour VARCHAR(16),
    PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_target_tag ON sp_target_tag (name, tenant);
CREATE INDEX sp_idx_target_tag_prim ON sp_target_tag (tenant, id);
CREATE INDEX sp_idx_target_tag_01 ON sp_target_tag (tenant, name);

CREATE TABLE sp_target_type (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at BIGINT,
    created_by VARCHAR(64),
    last_modified_at BIGINT,
    last_modified_by VARCHAR(64),
    optlock_revision BIGINT,
    tenant VARCHAR(40) NOT NULL,
    description VARCHAR(512),
    name VARCHAR(128) NOT NULL,
    colour VARCHAR(16),
    type_key VARCHAR(64) NOT NULL,
    PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_target_type_key ON sp_target_type (type_key, tenant);
CREATE UNIQUE INDEX uk_target_name ON sp_target_type (name, tenant);
CREATE INDEX sp_idx_target_type_prim ON sp_target_type (tenant, id);

CREATE TABLE sp_tenant_configuration (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at BIGINT,
    created_by VARCHAR(64),
    last_modified_at BIGINT,
    last_modified_by VARCHAR(64),
    optlock_revision BIGINT,
    tenant VARCHAR(40) NOT NULL,
    conf_key VARCHAR(128) NOT NULL,
    conf_value VARCHAR(512) NOT NULL,
    PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_tenant_configuration ON sp_tenant_configuration (conf_key, tenant);

CREATE TABLE sp_distribution_set (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at BIGINT,
    created_by VARCHAR(64),
    last_modified_at BIGINT,
    last_modified_by VARCHAR(64),
    optlock_revision BIGINT,
    tenant VARCHAR(40) NOT NULL,
    description VARCHAR(512),
    name VARCHAR(128) NOT NULL,
    version VARCHAR(64) NOT NULL,
    deleted BOOLEAN,
    required_migration_step BOOLEAN,
    ds_type BIGINT NOT NULL,
    valid BOOLEAN,
    locked BOOLEAN DEFAULT TRUE NOT NULL,
    PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_distribution_set ON sp_distribution_set (tenant, name, version, ds_type);
CREATE INDEX sp_idx_distribution_set_prim ON sp_distribution_set (tenant, id);
CREATE INDEX sp_idx_distribution_set_01 ON sp_distribution_set (tenant, deleted);
ALTER TABLE sp_distribution_set
    ADD CONSTRAINT fk_distribution_set_ds_type FOREIGN KEY (ds_type) REFERENCES sp_distribution_set_type (id);

CREATE TABLE sp_tenant (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at BIGINT,
    created_by VARCHAR(64),
    last_modified_at BIGINT,
    last_modified_by VARCHAR(64),
    optlock_revision BIGINT,
    tenant VARCHAR(40) NOT NULL,
    default_ds_type BIGINT NOT NULL,
    PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_tenant ON sp_tenant (tenant);
CREATE INDEX sp_idx_tenant_prim ON sp_tenant (tenant, id);
ALTER TABLE sp_tenant
    ADD CONSTRAINT fk_sp_tenant_default_ds_type FOREIGN KEY (default_ds_type) REFERENCES sp_distribution_set_type (id);

CREATE TABLE sp_ds_type_element (
    mandatory BOOLEAN,
    distribution_set_type BIGINT NOT NULL,
    software_module_type BIGINT NOT NULL,
    PRIMARY KEY (distribution_set_type, software_module_type)
);
ALTER TABLE sp_ds_type_element
    ADD CONSTRAINT fk_ds_type_element_distribution_set_type FOREIGN KEY (distribution_set_type) REFERENCES sp_distribution_set_type (id) ON DELETE CASCADE;
ALTER TABLE sp_ds_type_element
    ADD CONSTRAINT fk_ds_type_element_software_module_type FOREIGN KEY (software_module_type) REFERENCES sp_software_module_type (id) ON DELETE CASCADE;

CREATE TABLE sp_software_module (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at BIGINT,
    created_by VARCHAR(64),
    last_modified_at BIGINT,
    last_modified_by VARCHAR(64),
    optlock_revision BIGINT,
    tenant VARCHAR(40) NOT NULL,
    description VARCHAR(512),
    name VARCHAR(128) NOT NULL,
    version VARCHAR(64) NOT NULL,
    deleted BOOLEAN,
    vendor VARCHAR(256),
    sm_type BIGINT NOT NULL,
    encrypted BOOLEAN,
    locked BOOLEAN DEFAULT TRUE NOT NULL,
    PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_software_module ON sp_software_module (sm_type, name, version, tenant);
CREATE INDEX sp_idx_software_module_01 ON sp_software_module (tenant, deleted, name, version);
CREATE INDEX sp_idx_software_module_02 ON sp_software_module (tenant, deleted, sm_type);
CREATE INDEX sp_idx_software_module_prim ON sp_software_module (tenant, id);
ALTER TABLE sp_software_module
    ADD CONSTRAINT fk_software_module_sm_type FOREIGN KEY (sm_type) REFERENCES sp_software_module_type (id);

CREATE TABLE sp_target_type_ds_type (
    target_type BIGINT NOT NULL,
    distribution_set_type BIGINT NOT NULL,
    PRIMARY KEY (target_type, distribution_set_type)
);
ALTER TABLE sp_target_type_ds_type
    ADD CONSTRAINT fk_target_type_ds_type_distribution_set_type FOREIGN KEY (distribution_set_type) REFERENCES sp_distribution_set_type (id) ON DELETE CASCADE;
ALTER TABLE sp_target_type_ds_type
    ADD CONSTRAINT fk_target_type_ds_type_target_type FOREIGN KEY (target_type) REFERENCES sp_target_type (id) ON DELETE CASCADE;

CREATE TABLE sp_ds_metadata (
    meta_key VARCHAR(128) NOT NULL,
    meta_value VARCHAR(4000),
    ds BIGINT NOT NULL,
    PRIMARY KEY (ds, meta_key)
);
ALTER TABLE sp_ds_metadata
    ADD CONSTRAINT fk_ds_metadata_ds FOREIGN KEY (ds) REFERENCES sp_distribution_set (id) ON DELETE CASCADE;

CREATE TABLE sp_ds_tag (
    ds BIGINT NOT NULL,
    tag BIGINT NOT NULL,
    PRIMARY KEY (ds, tag)
);
ALTER TABLE sp_ds_tag
    ADD CONSTRAINT fk_ds_tag_ds FOREIGN KEY (ds) REFERENCES sp_distribution_set (id) ON DELETE CASCADE;
ALTER TABLE sp_ds_tag
    ADD CONSTRAINT fk_ds_tag_tag FOREIGN KEY (tag) REFERENCES sp_distribution_set_tag (id) ON DELETE CASCADE;

CREATE TABLE sp_rollout (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at BIGINT,
    created_by VARCHAR(64),
    last_modified_at BIGINT,
    last_modified_by VARCHAR(64),
    optlock_revision BIGINT,
    tenant VARCHAR(40) NOT NULL,
    description VARCHAR(512),
    name VARCHAR(128) NOT NULL,
    last_check BIGINT,
    status INTEGER NOT NULL,
    distribution_set BIGINT NOT NULL,
    target_filter VARCHAR(1024),
    forced_time BIGINT,
    total_targets BIGINT,
    rollout_groups_created BIGINT,
    start_at BIGINT,
    deleted BOOLEAN,
    action_type INTEGER NOT NULL,
    approval_decided_by VARCHAR(64),
    approval_remark VARCHAR(255),
    weight INTEGER NOT NULL,
    access_control_context VARCHAR(4096),
    is_dynamic BOOLEAN,
    PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_rollout ON sp_rollout (name, tenant);
CREATE INDEX sp_idx_rollout_status_tenant ON sp_rollout (tenant, status);
ALTER TABLE sp_rollout
    ADD CONSTRAINT fk_rollout_distribution_set FOREIGN KEY (distribution_set) REFERENCES sp_distribution_set (id);

CREATE TABLE sp_target (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at BIGINT,
    created_by VARCHAR(64),
    last_modified_at BIGINT,
    last_modified_by VARCHAR(64),
    optlock_revision BIGINT,
    tenant VARCHAR(40) NOT NULL,
    description VARCHAR(512),
    name VARCHAR(128) NOT NULL,
    controller_id VARCHAR(256) NOT NULL,
    sec_token VARCHAR(128) NOT NULL,
    assigned_distribution_set BIGINT,
    install_date BIGINT,
    address VARCHAR(512),
    last_target_query BIGINT,
    request_controller_attributes BOOLEAN NOT NULL,
    installed_distribution_set BIGINT,
    update_status INTEGER NOT NULL,
    target_type BIGINT,
    target_group VARCHAR(256),
    PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_target_controller_id ON sp_target (controller_id, tenant);
CREATE INDEX sp_idx_target_01 ON sp_target (tenant, name, assigned_distribution_set);
CREATE INDEX sp_idx_target_03 ON sp_target (tenant, controller_id, assigned_distribution_set);
CREATE INDEX sp_idx_target_04 ON sp_target (tenant, created_at);
CREATE INDEX sp_idx_target_prim ON sp_target (tenant, id);
CREATE INDEX sp_idx_target_05 ON sp_target (tenant, last_modified_at);
CREATE INDEX sp_idx_target_group ON sp_target (tenant, target_group);
ALTER TABLE sp_target
    ADD CONSTRAINT fk_target_assign_ds FOREIGN KEY (assigned_distribution_set) REFERENCES sp_distribution_set (id);
ALTER TABLE sp_target
    ADD CONSTRAINT fk_target_inst_ds FOREIGN KEY (installed_distribution_set) REFERENCES sp_distribution_set (id);
ALTER TABLE sp_target
    ADD CONSTRAINT fk_target_relation_target_type FOREIGN KEY (target_type) REFERENCES sp_target_type (id) ON DELETE SET NULL;

CREATE TABLE sp_target_filter_query (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at BIGINT,
    created_by VARCHAR(64),
    last_modified_at BIGINT,
    last_modified_by VARCHAR(64),
    optlock_revision BIGINT,
    tenant VARCHAR(40) NOT NULL,
    name VARCHAR(128) NOT NULL,
    query VARCHAR(1024) NOT NULL,
    auto_assign_distribution_set BIGINT,
    auto_assign_action_type INTEGER,
    auto_assign_weight INTEGER NOT NULL,
    auto_assign_initiated_by VARCHAR(64),
    confirmation_required BOOLEAN,
    access_control_context VARCHAR(4096),
    PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_target_filter_query ON sp_target_filter_query (name, tenant);
ALTER TABLE sp_target_filter_query
    ADD CONSTRAINT fk_target_filter_query_auto_assign_distribution_set FOREIGN KEY (auto_assign_distribution_set) REFERENCES sp_distribution_set (id) ON DELETE SET NULL;

CREATE TABLE sp_artifact (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at BIGINT,
    created_by VARCHAR(64),
    last_modified_at BIGINT,
    last_modified_by VARCHAR(64),
    optlock_revision BIGINT,
    tenant VARCHAR(40) NOT NULL,
    md5_hash VARCHAR(32),
    file_size BIGINT,
    provided_file_name VARCHAR(256),
    sha1_hash VARCHAR(40) NOT NULL,
    software_module BIGINT NOT NULL,
    sha256_hash CHAR(64),
    PRIMARY KEY (id)
);
CREATE INDEX sp_idx_artifact_prim ON sp_artifact (tenant, id);
CREATE INDEX sp_idx_artifact_01 ON sp_artifact (tenant, software_module);
CREATE INDEX sp_idx_artifact_02 ON sp_artifact (tenant, sha1_hash);
ALTER TABLE sp_artifact
    ADD CONSTRAINT fk_artifact_software_module FOREIGN KEY (software_module) REFERENCES sp_software_module (id) ON DELETE CASCADE;

CREATE TABLE sp_ds_sm (
    ds_id BIGINT NOT NULL,
    sm_id BIGINT NOT NULL,
    PRIMARY KEY (ds_id, sm_id)
);
ALTER TABLE sp_ds_sm
    ADD CONSTRAINT fk_ds_sm_ds_id FOREIGN KEY (ds_id) REFERENCES sp_distribution_set (id) ON DELETE CASCADE;
ALTER TABLE sp_ds_sm
    ADD CONSTRAINT fk_ds_sm_sm_id FOREIGN KEY (sm_id) REFERENCES sp_software_module (id) ON DELETE CASCADE;

CREATE TABLE sp_sm_metadata (
    meta_key VARCHAR(128) NOT NULL,
    meta_value VARCHAR(4000),
    sm BIGINT NOT NULL,
    target_visible BOOLEAN,
    PRIMARY KEY (meta_key, sm)
);
ALTER TABLE sp_sm_metadata
    ADD CONSTRAINT fk_sm_metadata_sm FOREIGN KEY (sm) REFERENCES sp_software_module (id) ON DELETE CASCADE;

CREATE TABLE sp_rollout_group (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at BIGINT,
    created_by VARCHAR(64),
    last_modified_at BIGINT,
    last_modified_by VARCHAR(64),
    optlock_revision BIGINT,
    tenant VARCHAR(40) NOT NULL,
    description VARCHAR(512),
    name VARCHAR(128) NOT NULL,
    error_condition INTEGER,
    error_condition_exp VARCHAR(512),
    error_action INTEGER,
    error_action_exp VARCHAR(512),
    success_condition INTEGER NOT NULL,
    success_condition_exp VARCHAR(512) NOT NULL,
    success_action INTEGER NOT NULL,
    success_action_exp VARCHAR(512),
    status INTEGER NOT NULL,
    parent BIGINT,
    rollout BIGINT NOT NULL,
    total_targets BIGINT,
    target_percentage FLOAT,
    target_filter VARCHAR(1024),
    confirmation_required BOOLEAN,
    is_dynamic BOOLEAN DEFAULT FALSE NOT NULL,
    PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_rollout_group ON sp_rollout_group (name, rollout, tenant);
ALTER TABLE sp_rollout_group
    ADD CONSTRAINT fk_rollout_group_rollout FOREIGN KEY (rollout) REFERENCES sp_rollout (id) ON DELETE CASCADE;

CREATE TABLE sp_target_attributes (
    target BIGINT NOT NULL,
    attribute_value VARCHAR(128),
    attribute_key VARCHAR(128) NOT NULL,
    PRIMARY KEY (target, attribute_key)
);
ALTER TABLE sp_target_attributes
    ADD CONSTRAINT fk_target_attributes_target FOREIGN KEY (target) REFERENCES sp_target (id) ON DELETE CASCADE;

CREATE TABLE sp_target_conf_status (
    id BIGINT NOT NULL AUTO_INCREMENT,
    target BIGINT NOT NULL,
    initiator VARCHAR(64),
    remark VARCHAR(512),
    created_at BIGINT,
    created_by VARCHAR(64),
    last_modified_at BIGINT,
    last_modified_by VARCHAR(64),
    optlock_revision BIGINT,
    tenant VARCHAR(40) NOT NULL,
    PRIMARY KEY (id)
);
ALTER TABLE sp_target_conf_status
    ADD CONSTRAINT fk_target_conf_status_target FOREIGN KEY (target) REFERENCES sp_target (id) ON DELETE CASCADE;

CREATE TABLE sp_target_metadata (
    meta_key VARCHAR(128) NOT NULL,
    meta_value VARCHAR(4000),
    target BIGINT NOT NULL,
    PRIMARY KEY (target, meta_key)
);
ALTER TABLE sp_target_metadata
    ADD CONSTRAINT fk_target_metadata_target FOREIGN KEY (target) REFERENCES sp_target (id) ON DELETE CASCADE;

CREATE TABLE sp_target_target_tag (
    target BIGINT NOT NULL,
    tag BIGINT NOT NULL,
    PRIMARY KEY (target, tag)
);
ALTER TABLE sp_target_target_tag
    ADD CONSTRAINT fk_target_target_tag_target FOREIGN KEY (target) REFERENCES sp_target (id) ON DELETE CASCADE;
ALTER TABLE sp_target_target_tag
    ADD CONSTRAINT fk_target_target_tag_tag FOREIGN KEY (tag) REFERENCES sp_target_tag (id) ON DELETE CASCADE;

CREATE TABLE sp_action (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at BIGINT,
    created_by VARCHAR(64),
    last_modified_at BIGINT,
    last_modified_by VARCHAR(64),
    optlock_revision BIGINT,
    tenant VARCHAR(40) NOT NULL,
    active BOOLEAN,
    forced_time BIGINT,
    status INTEGER NOT NULL,
    distribution_set BIGINT NOT NULL,
    target BIGINT NOT NULL,
    rollout BIGINT,
    rollout_group BIGINT,
    action_type INTEGER NOT NULL,
    maintenance_cron_schedule VARCHAR(40),
    maintenance_duration VARCHAR(40),
    maintenance_time_zone VARCHAR(40),
    external_ref VARCHAR(512),
    weight INTEGER NOT NULL,
    initiated_by VARCHAR(64) NOT NULL,
    last_action_status_code INTEGER,
    PRIMARY KEY (id)
);
CREATE INDEX sp_idx_action_prim ON sp_action (tenant, id);
CREATE INDEX sp_idx_action_01 ON sp_action (tenant, distribution_set);
CREATE INDEX sp_idx_action_02 ON sp_action (tenant, target, active);
CREATE INDEX sp_idx_action_external_ref ON sp_action (external_ref);
ALTER TABLE sp_action
    ADD CONSTRAINT fk_action_distribution_set FOREIGN KEY (distribution_set) REFERENCES sp_distribution_set (id);
ALTER TABLE sp_action
    ADD CONSTRAINT fk_action_rollout_group FOREIGN KEY (rollout_group) REFERENCES sp_rollout_group (id);
ALTER TABLE sp_action
    ADD CONSTRAINT fk_action_target FOREIGN KEY (target) REFERENCES sp_target (id) ON DELETE CASCADE;
ALTER TABLE sp_action
    ADD CONSTRAINT fk_action_rollout FOREIGN KEY (rollout) REFERENCES sp_rollout (id);

CREATE TABLE sp_rollout_target_group (
    target BIGINT NOT NULL,
    rollout_group BIGINT NOT NULL,
    PRIMARY KEY (rollout_group, target)
);
ALTER TABLE sp_rollout_target_group
    ADD CONSTRAINT fk_rollout_target_group_rollout_group FOREIGN KEY (rollout_group) REFERENCES sp_rollout_group (id) ON DELETE CASCADE;
ALTER TABLE sp_rollout_target_group
    ADD CONSTRAINT fk_rollout_target_group_target FOREIGN KEY (target) REFERENCES sp_target (id) ON DELETE CASCADE;

CREATE TABLE sp_action_status (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at BIGINT,
    created_by VARCHAR(64),
    last_modified_at BIGINT,
    last_modified_by VARCHAR(64),
    optlock_revision BIGINT,
    tenant VARCHAR(40) NOT NULL,
    target_occurred_at BIGINT NOT NULL,
    status INTEGER NOT NULL,
    action BIGINT NOT NULL,
    code INTEGER,
    PRIMARY KEY (id)
);
CREATE INDEX sp_idx_action_status_prim ON sp_action_status (tenant, id);
CREATE INDEX sp_idx_action_status_02 ON sp_action_status (tenant, action, status);
CREATE INDEX sp_idx_action_status_03 ON sp_action_status (tenant, code);
ALTER TABLE sp_action_status
    ADD CONSTRAINT fk_action_status_action FOREIGN KEY (action) REFERENCES sp_action (id) ON DELETE CASCADE;

CREATE TABLE sp_action_status_messages (
    action_status BIGINT NOT NULL,
    detail_message VARCHAR(512) NOT NULL
);
CREATE INDEX fk_action_status_messages_action_status ON sp_action_status_messages (action_status);
ALTER TABLE sp_action_status_messages
    ADD CONSTRAINT fk_action_status_messages_action_status FOREIGN KEY (action_status) REFERENCES sp_action_status (id) ON DELETE CASCADE;