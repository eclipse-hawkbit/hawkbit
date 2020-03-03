-- ------------ Write CREATE-SEQUENCE-stage scripts -----------

CREATE SEQUENCE IF NOT EXISTS sp_action_seq
INCREMENT BY 1
START WITH 1
NO CYCLE;

CREATE SEQUENCE IF NOT EXISTS sp_action_status_seq
INCREMENT BY 1
START WITH 1
NO CYCLE;

CREATE SEQUENCE IF NOT EXISTS sp_artifact_seq
INCREMENT BY 1
START WITH 1
NO CYCLE;

CREATE SEQUENCE IF NOT EXISTS sp_base_software_module_seq
INCREMENT BY 1
START WITH 1
NO CYCLE;

CREATE SEQUENCE IF NOT EXISTS sp_distribution_set_seq
INCREMENT BY 1
START WITH 1
NO CYCLE;

CREATE SEQUENCE IF NOT EXISTS sp_distribution_set_type_seq
INCREMENT BY 1
START WITH 1
NO CYCLE;

CREATE SEQUENCE IF NOT EXISTS sp_distributionset_tag_seq
INCREMENT BY 1
START WITH 1
NO CYCLE;

CREATE SEQUENCE IF NOT EXISTS sp_rollout_seq
INCREMENT BY 1
START WITH 1
NO CYCLE;

CREATE SEQUENCE IF NOT EXISTS sp_rolloutgroup_seq
INCREMENT BY 1
START WITH 1
NO CYCLE;

CREATE SEQUENCE IF NOT EXISTS sp_software_module_type_seq
INCREMENT BY 1
START WITH 1
NO CYCLE;

CREATE SEQUENCE IF NOT EXISTS sp_target_filter_query_seq
INCREMENT BY 1
START WITH 1
NO CYCLE;

CREATE SEQUENCE IF NOT EXISTS sp_target_seq
INCREMENT BY 1
START WITH 1
NO CYCLE;

CREATE SEQUENCE IF NOT EXISTS sp_target_tag_seq
INCREMENT BY 1
START WITH 1
NO CYCLE;

CREATE SEQUENCE IF NOT EXISTS sp_tenant_configuration_seq
INCREMENT BY 1
START WITH 1
NO CYCLE;

CREATE SEQUENCE IF NOT EXISTS sp_tenant_seq
INCREMENT BY 1
START WITH 1
NO CYCLE;

-- ------------ Write CREATE-TABLE-stage scripts -----------

CREATE TABLE sp_action(
    id BIGINT NOT NULL DEFAULT nextval('sp_action_seq'),
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
    rolloutgroup BIGINT,
    action_type INTEGER NOT NULL,
    maintenance_cron_schedule VARCHAR(40),
    maintenance_duration VARCHAR(40),
    maintenance_time_zone VARCHAR(40),
    external_ref VARCHAR(512),
    weight INTEGER
)
        WITH (
        OIDS=FALSE
        );

CREATE TABLE sp_action_status(
    id BIGINT NOT NULL DEFAULT nextval('sp_action_status_seq'),
    created_at BIGINT,
    created_by VARCHAR(64),
    last_modified_at BIGINT,
    last_modified_by VARCHAR(64),
    optlock_revision BIGINT,
    tenant VARCHAR(40) NOT NULL,
    target_occurred_at BIGINT NOT NULL,
    status INTEGER NOT NULL,
    action BIGINT NOT NULL
)
        WITH (
        OIDS=FALSE
        );

CREATE TABLE sp_action_status_messages(
    action_status_id BIGINT NOT NULL,
    detail_message VARCHAR(512) NOT NULL
)
        WITH (
        OIDS=FALSE
        );

CREATE TABLE sp_artifact(
    id BIGINT NOT NULL DEFAULT nextval('sp_artifact_seq'),
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
    sha256_hash CHAR(64)
)
        WITH (
        OIDS=FALSE
        );

CREATE TABLE sp_base_software_module(
    id BIGINT NOT NULL DEFAULT nextval('sp_base_software_module_seq'),
    created_at BIGINT,
    created_by VARCHAR(64),
    last_modified_at BIGINT,
    last_modified_by VARCHAR(64),
    optlock_revision BIGINT,
    tenant VARCHAR(40) NOT NULL,
    description VARCHAR(512),
    name VARCHAR(128),
    version VARCHAR(64) NOT NULL,
    deleted BOOLEAN,
    vendor VARCHAR(256),
    module_type BIGINT NOT NULL
)
        WITH (
        OIDS=FALSE
        );

CREATE TABLE sp_distribution_set(
    id BIGINT NOT NULL DEFAULT nextval('sp_distribution_set_seq'),
    created_at BIGINT,
    created_by VARCHAR(64),
    last_modified_at BIGINT,
    last_modified_by VARCHAR(64),
    optlock_revision BIGINT,
    tenant VARCHAR(40) NOT NULL,
    description VARCHAR(512),
    name VARCHAR(128),
    version VARCHAR(64) NOT NULL,
    complete BOOLEAN,
    deleted BOOLEAN,
    required_migration_step BOOLEAN,
    ds_id BIGINT NOT NULL
)
        WITH (
        OIDS=FALSE
        );

CREATE TABLE sp_distribution_set_type(
    id BIGINT NOT NULL DEFAULT nextval('sp_distribution_set_type_seq'),
    created_at BIGINT,
    created_by VARCHAR(64),
    last_modified_at BIGINT,
    last_modified_by VARCHAR(64),
    optlock_revision BIGINT,
    tenant VARCHAR(40) NOT NULL,
    description VARCHAR(512),
    name VARCHAR(128),
    colour VARCHAR(16),
    deleted BOOLEAN,
    type_key VARCHAR(64) NOT NULL
)
        WITH (
        OIDS=FALSE
        );

CREATE TABLE sp_distributionset_tag(
    id BIGINT NOT NULL DEFAULT nextval('sp_distributionset_tag_seq'),
    created_at BIGINT,
    created_by VARCHAR(64),
    last_modified_at BIGINT,
    last_modified_by VARCHAR(64),
    optlock_revision BIGINT,
    tenant VARCHAR(40) NOT NULL,
    description VARCHAR(512),
    name VARCHAR(128),
    colour VARCHAR(16)
)
        WITH (
        OIDS=FALSE
        );

CREATE TABLE sp_ds_dstag(
    ds BIGINT NOT NULL,
    tag BIGINT NOT NULL
)
        WITH (
        OIDS=FALSE
        );

CREATE TABLE sp_ds_metadata(
    meta_key VARCHAR(128) NOT NULL,
    meta_value VARCHAR(4000),
    ds_id BIGINT NOT NULL
)
        WITH (
        OIDS=FALSE
        );

CREATE TABLE sp_ds_module(
    ds_id BIGINT NOT NULL,
    module_id BIGINT NOT NULL
)
        WITH (
        OIDS=FALSE
        );

CREATE TABLE sp_ds_type_element(
    mandatory BOOLEAN,
    distribution_set_type BIGINT NOT NULL,
    software_module_type BIGINT NOT NULL
)
        WITH (
        OIDS=FALSE
        );

CREATE TABLE sp_rollout(
    id BIGINT NOT NULL DEFAULT nextval('sp_rollout_seq'),
    created_at BIGINT,
    created_by VARCHAR(64),
    last_modified_at BIGINT,
    last_modified_by VARCHAR(64),
    optlock_revision BIGINT,
    tenant VARCHAR(40) NOT NULL,
    description VARCHAR(512),
    name VARCHAR(128),
    last_check BIGINT,
    group_theshold REAL,
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
    weight INTEGER
)
        WITH (
        OIDS=FALSE
        );

CREATE TABLE sp_rolloutgroup(
    id BIGINT NOT NULL DEFAULT nextval('sp_rolloutgroup_seq'),
    created_at BIGINT,
    created_by VARCHAR(64),
    last_modified_at BIGINT,
    last_modified_by VARCHAR(64),
    optlock_revision BIGINT,
    tenant VARCHAR(40) NOT NULL,
    description VARCHAR(512),
    name VARCHAR(128),
    error_condition INTEGER,
    error_condition_exp VARCHAR(512),
    error_action INTEGER,
    error_action_exp VARCHAR(512),
    success_condition INTEGER NOT NULL,
    success_condition_exp VARCHAR(512) NOT NULL,
    success_action INTEGER NOT NULL,
    success_action_exp VARCHAR(512),
    status INTEGER NOT NULL,
    parent_id BIGINT,
    rollout BIGINT NOT NULL,
    total_targets BIGINT,
    target_percentage REAL,
    target_filter VARCHAR(1024)
)
        WITH (
        OIDS=FALSE
        );

CREATE TABLE sp_rollouttargetgroup(
    target_id BIGINT NOT NULL,
    rolloutgroup_id BIGINT NOT NULL
)
        WITH (
        OIDS=FALSE
        );

CREATE TABLE sp_software_module_type(
    id BIGINT NOT NULL DEFAULT nextval('sp_software_module_type_seq'),
    created_at BIGINT,
    created_by VARCHAR(64),
    last_modified_at BIGINT,
    last_modified_by VARCHAR(64),
    optlock_revision BIGINT,
    tenant VARCHAR(40) NOT NULL,
    description VARCHAR(512),
    name VARCHAR(128),
    colour VARCHAR(16),
    deleted BOOLEAN,
    type_key VARCHAR(64) NOT NULL,
    max_ds_assignments INTEGER NOT NULL
)
        WITH (
        OIDS=FALSE
        );

CREATE TABLE sp_sw_metadata(
    meta_key VARCHAR(128) NOT NULL,
    meta_value VARCHAR(4000),
    sw_id BIGINT NOT NULL,
    target_visible BOOLEAN
)
        WITH (
        OIDS=FALSE
        );

CREATE TABLE sp_target(
    id BIGINT NOT NULL DEFAULT nextval('sp_target_seq'),
    created_at BIGINT,
    created_by VARCHAR(64),
    last_modified_at BIGINT,
    last_modified_by VARCHAR(64),
    optlock_revision BIGINT,
    tenant VARCHAR(40) NOT NULL,
    description VARCHAR(512),
    name VARCHAR(128),
    controller_id VARCHAR(256),
    sec_token VARCHAR(128) NOT NULL,
    assigned_distribution_set BIGINT,
    install_date BIGINT,
    address VARCHAR(512),
    last_target_query BIGINT,
    request_controller_attributes BOOLEAN NOT NULL,
    installed_distribution_set BIGINT,
    update_status INTEGER NOT NULL
)
        WITH (
        OIDS=FALSE
        );

CREATE TABLE sp_target_attributes(
    target_id BIGINT NOT NULL,
    attribute_value VARCHAR(128),
    attribute_key VARCHAR(128) NOT NULL
)
        WITH (
        OIDS=FALSE
        );

CREATE TABLE sp_target_filter_query(
    id BIGINT NOT NULL DEFAULT nextval('sp_target_filter_query_seq'),
    created_at BIGINT,
    created_by VARCHAR(64),
    last_modified_at BIGINT,
    last_modified_by VARCHAR(64),
    optlock_revision BIGINT,
    tenant VARCHAR(40) NOT NULL,
    name VARCHAR(128),
    query VARCHAR(1024) NOT NULL,
    auto_assign_distribution_set BIGINT,
    auto_assign_action_type INTEGER,
    auto_assign_weight INTEGER
)
        WITH (
        OIDS=FALSE
        );

CREATE TABLE sp_target_metadata(
    meta_key VARCHAR(128) NOT NULL,
    meta_value VARCHAR(4000),
    target_id BIGINT NOT NULL
)
        WITH (
        OIDS=FALSE
        );

CREATE TABLE sp_target_tag(
    id BIGINT NOT NULL DEFAULT nextval('sp_target_tag_seq'),
    created_at BIGINT,
    created_by VARCHAR(64),
    last_modified_at BIGINT,
    last_modified_by VARCHAR(64),
    optlock_revision BIGINT,
    tenant VARCHAR(40) NOT NULL,
    description VARCHAR(512),
    name VARCHAR(128),
    colour VARCHAR(16)
)
        WITH (
        OIDS=FALSE
        );

CREATE TABLE sp_target_target_tag(
    target BIGINT NOT NULL,
    tag BIGINT NOT NULL
)
        WITH (
        OIDS=FALSE
        );

CREATE TABLE sp_tenant(
    id BIGINT NOT NULL DEFAULT nextval('sp_tenant_seq'),
    created_at BIGINT,
    created_by VARCHAR(64),
    last_modified_at BIGINT,
    last_modified_by VARCHAR(64),
    optlock_revision BIGINT,
    tenant VARCHAR(40) NOT NULL,
    default_ds_type BIGINT NOT NULL
)
        WITH (
        OIDS=FALSE
        );

CREATE TABLE sp_tenant_configuration(
    id BIGINT NOT NULL DEFAULT nextval('sp_tenant_configuration_seq'),
    created_at BIGINT,
    created_by VARCHAR(64),
    last_modified_at BIGINT,
    last_modified_by VARCHAR(64),
    optlock_revision BIGINT,
    tenant VARCHAR(40) NOT NULL,
    conf_key VARCHAR(128) NOT NULL,
    conf_value VARCHAR(512) NOT NULL
)
        WITH (
        OIDS=FALSE
        );

-- ------------ Write CREATE-INDEX-stage scripts -----------

CREATE INDEX sp_idx_action_01_sp_action
ON sp_action
USING BTREE (tenant, distribution_set);

CREATE INDEX sp_idx_action_02_sp_action
ON sp_action
USING BTREE (tenant, target, active);

CREATE INDEX sp_idx_action_external_ref_sp_action
ON sp_action
USING BTREE (external_ref);

CREATE INDEX sp_idx_action_prim_sp_action
ON sp_action
USING BTREE (tenant, id);

CREATE INDEX sp_idx_action_status_02_sp_action_status
ON sp_action_status
USING BTREE (tenant, action, status);

CREATE INDEX sp_idx_action_status_prim_sp_action_status
ON sp_action_status
USING BTREE (tenant, id);

CREATE INDEX sp_idx_action_status_msgs_01_sp_action_status_messages
ON sp_action_status_messages
USING BTREE (action_status_id);

CREATE INDEX sp_idx_artifact_01_sp_artifact
ON sp_artifact
USING BTREE (tenant, software_module);

CREATE INDEX sp_idx_artifact_02_sp_artifact
ON sp_artifact
USING BTREE (tenant, sha1_hash);

CREATE INDEX sp_idx_artifact_prim_sp_artifact
ON sp_artifact
USING BTREE (tenant, id);

CREATE INDEX sp_idx_base_sw_module_01_sp_base_software_module
ON sp_base_software_module
USING BTREE (tenant, deleted, name, version);

CREATE INDEX sp_idx_base_sw_module_02_sp_base_software_module
ON sp_base_software_module
USING BTREE (tenant, deleted, module_type);

CREATE INDEX sp_idx_base_sw_module_prim_sp_base_software_module
ON sp_base_software_module
USING BTREE (tenant, id);

CREATE INDEX sp_idx_distribution_set_01_sp_distribution_set
ON sp_distribution_set
USING BTREE (tenant, deleted, complete);

CREATE INDEX sp_idx_distribution_set_prim_sp_distribution_set
ON sp_distribution_set
USING BTREE (tenant, id);

CREATE INDEX sp_idx_distribution_set_type_01_sp_distribution_set_type
ON sp_distribution_set_type
USING BTREE (tenant, deleted);

CREATE INDEX sp_idx_distribution_set_type_prim_sp_distribution_set_type
ON sp_distribution_set_type
USING BTREE (tenant, id);

CREATE INDEX sp_idx_distribution_set_tag_01_sp_distributionset_tag
ON sp_distributionset_tag
USING BTREE (tenant, name);

CREATE INDEX sp_idx_distribution_set_tag_prim_sp_distributionset_tag
ON sp_distributionset_tag
USING BTREE (tenant, id);

CREATE INDEX fk_rolloutgroup_rolloutgroup_sp_rolloutgroup
ON sp_rolloutgroup
USING BTREE (parent_id);

CREATE INDEX sp_idx_software_module_type_01_sp_software_module_type
ON sp_software_module_type
USING BTREE (tenant, deleted);

CREATE INDEX sp_idx_software_module_type_prim_sp_software_module_type
ON sp_software_module_type
USING BTREE (tenant, id);

CREATE INDEX sp_idx_target_01_sp_target
ON sp_target
USING BTREE (tenant, name, assigned_distribution_set);

CREATE INDEX sp_idx_target_03_sp_target
ON sp_target
USING BTREE (tenant, controller_id, assigned_distribution_set);

CREATE INDEX sp_idx_target_04_sp_target
ON sp_target
USING BTREE (tenant, created_at);

CREATE INDEX sp_idx_target_prim_sp_target
ON sp_target
USING BTREE (tenant, id);

CREATE INDEX sp_idx_target_tag_01_sp_target_tag
ON sp_target_tag
USING BTREE (tenant, name);

CREATE INDEX sp_idx_target_tag_prim_sp_target_tag
ON sp_target_tag
USING BTREE (tenant, id);

CREATE INDEX sp_idx_tenant_prim_sp_tenant
ON sp_tenant
USING BTREE (tenant, id);

-- ------------ Write CREATE-CONSTRAINT-stage scripts -----------

ALTER TABLE sp_action
ADD CONSTRAINT pk_sp_action PRIMARY KEY (id);

ALTER TABLE sp_action_status
ADD CONSTRAINT pk_sp_action_status PRIMARY KEY (id);

ALTER TABLE sp_artifact
ADD CONSTRAINT pk_sp_artifact PRIMARY KEY (id);

ALTER TABLE sp_base_software_module
ADD CONSTRAINT pk_sp_base_software_module PRIMARY KEY (id);

ALTER TABLE sp_base_software_module
ADD CONSTRAINT uk_base_sw_mod_sp_base_software_module UNIQUE (module_type, name, version, tenant);

ALTER TABLE sp_distribution_set
ADD CONSTRAINT pk_sp_distribution_set PRIMARY KEY (id);

ALTER TABLE sp_distribution_set
ADD CONSTRAINT uk_distrib_set_sp_distribution_set UNIQUE (name, version, tenant);

ALTER TABLE sp_distribution_set_type
ADD CONSTRAINT pk_sp_distribution_set_type PRIMARY KEY (id);

ALTER TABLE sp_distribution_set_type
ADD CONSTRAINT uk_dst_key_sp_distribution_set_type UNIQUE (type_key, tenant);

ALTER TABLE sp_distribution_set_type
ADD CONSTRAINT uk_dst_name_sp_distribution_set_type UNIQUE (name, tenant);

ALTER TABLE sp_distributionset_tag
ADD CONSTRAINT pk_sp_distributionset_tag PRIMARY KEY (id);

ALTER TABLE sp_distributionset_tag
ADD CONSTRAINT uk_ds_tag_sp_distributionset_tag UNIQUE (name, tenant);

ALTER TABLE sp_ds_dstag
ADD CONSTRAINT pk_sp_ds_dstag PRIMARY KEY (ds, tag);

ALTER TABLE sp_ds_metadata
ADD CONSTRAINT pk_sp_ds_metadata PRIMARY KEY (ds_id, meta_key);

ALTER TABLE sp_ds_module
ADD CONSTRAINT pk_sp_ds_module PRIMARY KEY (ds_id, module_id);

ALTER TABLE sp_ds_type_element
ADD CONSTRAINT pk_sp_ds_type_element PRIMARY KEY (distribution_set_type, software_module_type);

ALTER TABLE sp_rollout
ADD CONSTRAINT pk_sp_rollout PRIMARY KEY (id);

ALTER TABLE sp_rollout
ADD CONSTRAINT uk_rollout_sp_rollout UNIQUE (name, tenant);

ALTER TABLE sp_rolloutgroup
ADD CONSTRAINT pk_sp_rolloutgroup PRIMARY KEY (id);

ALTER TABLE sp_rolloutgroup
ADD CONSTRAINT uk_rolloutgroup_sp_rolloutgroup UNIQUE (name, rollout, tenant);

ALTER TABLE sp_rollouttargetgroup
ADD CONSTRAINT pk_sp_rollouttargetgroup PRIMARY KEY (rolloutgroup_id, target_id);

ALTER TABLE sp_software_module_type
ADD CONSTRAINT pk_sp_software_module_type PRIMARY KEY (id);

ALTER TABLE sp_software_module_type
ADD CONSTRAINT uk_smt_name_sp_software_module_type UNIQUE (name, tenant);

ALTER TABLE sp_software_module_type
ADD CONSTRAINT uk_smt_type_key_sp_software_module_type UNIQUE (type_key, tenant);

ALTER TABLE sp_sw_metadata
ADD CONSTRAINT pk_sp_sw_metadata PRIMARY KEY (meta_key, sw_id);

ALTER TABLE sp_target
ADD CONSTRAINT pk_sp_target PRIMARY KEY (id);

ALTER TABLE sp_target
ADD CONSTRAINT uk_tenant_controller_id_sp_target UNIQUE (controller_id, tenant);

ALTER TABLE sp_target_attributes
ADD CONSTRAINT pk_sp_target_attributes PRIMARY KEY (target_id, attribute_key);

ALTER TABLE sp_target_filter_query
ADD CONSTRAINT pk_sp_target_filter_query PRIMARY KEY (id);

ALTER TABLE sp_target_filter_query
ADD CONSTRAINT uk_tenant_custom_filter_name_sp_target_filter_query UNIQUE (name, tenant);

ALTER TABLE sp_target_metadata
ADD CONSTRAINT pk_sp_target_metadata PRIMARY KEY (target_id, meta_key);

ALTER TABLE sp_target_tag
ADD CONSTRAINT pk_sp_target_tag PRIMARY KEY (id);

ALTER TABLE sp_target_tag
ADD CONSTRAINT uk_targ_tag_sp_target_tag UNIQUE (name, tenant);

ALTER TABLE sp_target_target_tag
ADD CONSTRAINT pk_sp_target_target_tag PRIMARY KEY (target, tag);

ALTER TABLE sp_tenant
ADD CONSTRAINT pk_sp_tenant PRIMARY KEY (id);

ALTER TABLE sp_tenant
ADD CONSTRAINT uk_tenantmd_tenant_sp_tenant UNIQUE (tenant);

ALTER TABLE sp_tenant_configuration
ADD CONSTRAINT pk_sp_tenant_configuration PRIMARY KEY (id);

ALTER TABLE sp_tenant_configuration
ADD CONSTRAINT uk_tenant_key_sp_tenant_configuration UNIQUE (conf_key, tenant);

-- ------------ Write CREATE-FOREIGN-KEY-CONSTRAINT-stage scripts -----------

ALTER TABLE sp_action
ADD CONSTRAINT fk_action_ds FOREIGN KEY (distribution_set)
REFERENCES sp_distribution_set (id)
ON UPDATE RESTRICT
ON DELETE RESTRICT;

ALTER TABLE sp_action
ADD CONSTRAINT fk_action_rollout FOREIGN KEY (rollout)
REFERENCES sp_rollout (id)
ON UPDATE RESTRICT
ON DELETE RESTRICT;

ALTER TABLE sp_action
ADD CONSTRAINT fk_action_rolloutgroup FOREIGN KEY (rolloutgroup)
REFERENCES sp_rolloutgroup (id)
ON UPDATE RESTRICT
ON DELETE RESTRICT;

ALTER TABLE sp_action
ADD CONSTRAINT fk_targ_act_hist_targ FOREIGN KEY (target)
REFERENCES sp_target (id)
ON UPDATE RESTRICT
ON DELETE CASCADE;

ALTER TABLE sp_action_status
ADD CONSTRAINT fk_act_stat_action FOREIGN KEY (action)
REFERENCES sp_action (id)
ON UPDATE RESTRICT
ON DELETE CASCADE;

ALTER TABLE sp_action_status_messages
ADD CONSTRAINT fk_stat_msg_act_stat FOREIGN KEY (action_status_id)
REFERENCES sp_action_status (id)
ON UPDATE RESTRICT
ON DELETE CASCADE;

ALTER TABLE sp_artifact
ADD CONSTRAINT fk_assigned_sm FOREIGN KEY (software_module)
REFERENCES sp_base_software_module (id)
ON UPDATE RESTRICT
ON DELETE CASCADE;

ALTER TABLE sp_base_software_module
ADD CONSTRAINT fk_module_type FOREIGN KEY (module_type)
REFERENCES sp_software_module_type (id)
ON UPDATE RESTRICT
ON DELETE RESTRICT;

ALTER TABLE sp_distribution_set
ADD CONSTRAINT fk_ds_dstype_ds FOREIGN KEY (ds_id)
REFERENCES sp_distribution_set_type (id)
ON UPDATE RESTRICT
ON DELETE RESTRICT;

ALTER TABLE sp_ds_dstag
ADD CONSTRAINT fk_ds_dstag_ds FOREIGN KEY (ds)
REFERENCES sp_distribution_set (id)
ON UPDATE RESTRICT
ON DELETE CASCADE;

ALTER TABLE sp_ds_dstag
ADD CONSTRAINT fk_ds_dstag_tag FOREIGN KEY (tag)
REFERENCES sp_distributionset_tag (id)
ON UPDATE RESTRICT
ON DELETE CASCADE;

ALTER TABLE sp_ds_metadata
ADD CONSTRAINT fk_metadata_ds FOREIGN KEY (ds_id)
REFERENCES sp_distribution_set (id)
ON UPDATE RESTRICT
ON DELETE CASCADE;

ALTER TABLE sp_ds_module
ADD CONSTRAINT fk_ds_module_ds FOREIGN KEY (ds_id)
REFERENCES sp_distribution_set (id)
ON UPDATE RESTRICT
ON DELETE CASCADE;

ALTER TABLE sp_ds_module
ADD CONSTRAINT fk_ds_module_module FOREIGN KEY (module_id)
REFERENCES sp_base_software_module (id)
ON UPDATE RESTRICT
ON DELETE CASCADE;

ALTER TABLE sp_ds_type_element
ADD CONSTRAINT fk_ds_type_element_element FOREIGN KEY (distribution_set_type)
REFERENCES sp_distribution_set_type (id)
ON UPDATE RESTRICT
ON DELETE CASCADE;

ALTER TABLE sp_ds_type_element
ADD CONSTRAINT fk_ds_type_element_smtype FOREIGN KEY (software_module_type)
REFERENCES sp_software_module_type (id)
ON UPDATE RESTRICT
ON DELETE CASCADE;

ALTER TABLE sp_rollout
ADD CONSTRAINT fk_rollout_ds FOREIGN KEY (distribution_set)
REFERENCES sp_distribution_set (id)
ON UPDATE RESTRICT
ON DELETE RESTRICT;

ALTER TABLE sp_rolloutgroup
ADD CONSTRAINT fk_rolloutgroup_rollout FOREIGN KEY (rollout)
REFERENCES sp_rollout (id)
ON UPDATE RESTRICT
ON DELETE CASCADE;

ALTER TABLE sp_rollouttargetgroup
ADD CONSTRAINT fk_rollouttargetgroup_rolloutgroup FOREIGN KEY (rolloutgroup_id)
REFERENCES sp_rolloutgroup (id)
ON UPDATE RESTRICT
ON DELETE CASCADE;

ALTER TABLE sp_rollouttargetgroup
ADD CONSTRAINT fk_rollouttargetgroup_target FOREIGN KEY (target_id)
REFERENCES sp_target (id)
ON UPDATE RESTRICT
ON DELETE CASCADE;

ALTER TABLE sp_sw_metadata
ADD CONSTRAINT fk_metadata_sw FOREIGN KEY (sw_id)
REFERENCES sp_base_software_module (id)
ON UPDATE RESTRICT
ON DELETE CASCADE;

ALTER TABLE sp_target
ADD CONSTRAINT fk_target_assign_ds FOREIGN KEY (assigned_distribution_set)
REFERENCES sp_distribution_set (id)
ON UPDATE RESTRICT
ON DELETE RESTRICT;

ALTER TABLE sp_target
ADD CONSTRAINT fk_target_inst_ds FOREIGN KEY (installed_distribution_set)
REFERENCES sp_distribution_set (id)
ON UPDATE RESTRICT
ON DELETE RESTRICT;

ALTER TABLE sp_target_attributes
ADD CONSTRAINT fk_targ_attrib_target FOREIGN KEY (target_id)
REFERENCES sp_target (id)
ON UPDATE RESTRICT
ON DELETE CASCADE;

ALTER TABLE sp_target_filter_query
ADD CONSTRAINT fk_filter_auto_assign_ds FOREIGN KEY (auto_assign_distribution_set)
REFERENCES sp_distribution_set (id)
ON UPDATE RESTRICT
ON DELETE SET NULL;

ALTER TABLE sp_target_metadata
ADD CONSTRAINT fk_metadata_target FOREIGN KEY (target_id)
REFERENCES sp_target (id)
ON UPDATE RESTRICT
ON DELETE CASCADE;

ALTER TABLE sp_target_target_tag
ADD CONSTRAINT fk_targ_targtag_tag FOREIGN KEY (tag)
REFERENCES sp_target_tag (id)
ON UPDATE RESTRICT
ON DELETE CASCADE;

ALTER TABLE sp_target_target_tag
ADD CONSTRAINT fk_targ_targtag_target FOREIGN KEY (target)
REFERENCES sp_target (id)
ON UPDATE RESTRICT
ON DELETE CASCADE;

ALTER TABLE sp_tenant
ADD CONSTRAINT fk_tenant_md_default_ds_type FOREIGN KEY (default_ds_type)
REFERENCES sp_distribution_set_type (id)
ON UPDATE RESTRICT
ON DELETE RESTRICT;
