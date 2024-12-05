CREATE TABLE sp_ds_type_element 
  ( 
     mandatory             SMALLINT DEFAULT 0, 
     distribution_set_type BIGINT NOT NULL, 
     software_module_type  BIGINT NOT NULL, 
     PRIMARY KEY (distribution_set_type, software_module_type) 
  ); 

CREATE TABLE sp_action 
  ( 
     id                         BIGINT GENERATED always AS IDENTITY NOT NULL, 
     tenant                     VARCHAR(40) NOT NULL, 
     action_type                INTEGER NOT NULL, 
     active                     SMALLINT DEFAULT 0, 
     created_at                 BIGINT NOT NULL, 
     created_by                 VARCHAR(40) NOT NULL, 
     forced_time                BIGINT, 
     last_modified_at           BIGINT NOT NULL, 
     last_modified_by           VARCHAR(40) NOT NULL, 
     optlock_revision           INTEGER, 
     status                     INTEGER NOT NULL, 
     distribution_set           BIGINT NOT NULL, 
     rollout                    BIGINT, 
     rolloutgroup               BIGINT, 
     target                     BIGINT NOT NULL, 
     maintenance_cron_schedule  VARCHAR(40), 
     maintenance_duration       VARCHAR(40), 
     maintenance_time_zone      VARCHAR(40), 
     PRIMARY KEY (id) 
  );

CREATE INDEX sp_idx_action_01 
  ON sp_action (tenant, distribution_set); 

CREATE INDEX sp_idx_action_02 
  ON sp_action (tenant, target, active); 

CREATE INDEX sp_idx_action_prim 
  ON sp_action (tenant, id); 

CREATE TABLE sp_action_status 
  ( 
     id                 BIGINT GENERATED always AS IDENTITY NOT NULL, 
     tenant             VARCHAR(40) NOT NULL, 
     created_at         BIGINT NOT NULL, 
     created_by         VARCHAR(40) NOT NULL, 
     last_modified_at   BIGINT NOT NULL, 
     last_modified_by   VARCHAR(40) NOT NULL, 
     target_occurred_at BIGINT NOT NULL, 
     optlock_revision   INTEGER, 
     status             INTEGER NOT NULL, 
     action             BIGINT NOT NULL, 
     PRIMARY KEY (id) 
  ); 

CREATE INDEX sp_idx_action_status_02 
  ON sp_action_status (tenant, action, status); 

CREATE INDEX sp_idx_action_status_prim 
  ON sp_action_status (tenant, id); 

CREATE TABLE sp_artifact 
  ( 
     id                 BIGINT GENERATED always AS IDENTITY NOT NULL, 
     tenant             VARCHAR(40) NOT NULL, 
     created_at         BIGINT NOT NULL, 
     created_by         VARCHAR(40) NOT NULL, 
     provided_file_name VARCHAR(256), 
     last_modified_at   BIGINT NOT NULL, 
     last_modified_by   VARCHAR(40) NOT NULL, 
     md5_hash           VARCHAR(32), 
     optlock_revision   INTEGER, 
     sha1_hash          VARCHAR(40) NOT NULL, 
     file_size          BIGINT, 
     software_module    BIGINT NOT NULL, 
     PRIMARY KEY (id) 
  ); 

CREATE INDEX sp_idx_artifact_01 
  ON sp_artifact (tenant, software_module); 

CREATE INDEX sp_idx_artifact_02 
  ON sp_artifact (tenant, sha1_hash); 

CREATE INDEX sp_idx_artifact_prim 
  ON sp_artifact (tenant, id); 

CREATE TABLE sp_distribution_set 
  ( 
     id                      BIGINT GENERATED always AS IDENTITY NOT NULL, 
     tenant                  VARCHAR(40) NOT NULL, 
     complete                SMALLINT DEFAULT 0, 
     created_at              BIGINT NOT NULL, 
     created_by              VARCHAR(40) NOT NULL, 
     deleted                 SMALLINT DEFAULT 0, 
     description             VARCHAR(512), 
     last_modified_at        BIGINT NOT NULL, 
     last_modified_by        VARCHAR(40) NOT NULL, 
     name                    VARCHAR(64) NOT NULL, 
     optlock_revision        INTEGER, 
     required_migration_step SMALLINT DEFAULT 0, 
     VERSION                 VARCHAR(64) NOT NULL, 
     ds_id                   BIGINT NOT NULL, 
     PRIMARY KEY (id) 
  ); 

CREATE INDEX sp_idx_distribution_set_01 
  ON sp_distribution_set (tenant, deleted, complete); 

CREATE INDEX sp_idx_distribution_set_prim 
  ON sp_distribution_set (tenant, id); 

CREATE TABLE sp_ds_metadata 
  ( 
     meta_key   VARCHAR(128) NOT NULL, 
     meta_value VARCHAR(4000), 
     ds_id      BIGINT NOT NULL, 
     PRIMARY KEY (meta_key, ds_id) 
  ); 

CREATE TABLE sp_distributionset_tag 
  ( 
     id               BIGINT GENERATED always AS IDENTITY NOT NULL, 
     tenant           VARCHAR(40) NOT NULL, 
     colour           VARCHAR(16), 
     created_at       BIGINT NOT NULL, 
     created_by       VARCHAR(40) NOT NULL, 
     description      VARCHAR(512), 
     last_modified_at BIGINT NOT NULL, 
     last_modified_by VARCHAR(40) NOT NULL, 
     name             VARCHAR(64) NOT NULL, 
     optlock_revision INTEGER, 
     PRIMARY KEY (id) 
  ); 

CREATE INDEX sp_idx_distribution_set_tag_prim 
  ON sp_distributionset_tag (tenant, id); 

CREATE TABLE sp_distribution_set_type 
  ( 
     id               BIGINT GENERATED always AS IDENTITY NOT NULL, 
     tenant           VARCHAR(40) NOT NULL, 
     colour           VARCHAR(16), 
     created_at       BIGINT NOT NULL, 
     created_by       VARCHAR(40) NOT NULL, 
     deleted          SMALLINT DEFAULT 0, 
     description      VARCHAR(512), 
     type_key         VARCHAR(64) NOT NULL, 
     last_modified_at BIGINT NOT NULL, 
     last_modified_by VARCHAR(40) NOT NULL, 
     name             VARCHAR(64) NOT NULL, 
     optlock_revision INTEGER, 
     PRIMARY KEY (id) 
  ); 

CREATE INDEX sp_idx_distribution_set_type_01 
  ON sp_distribution_set_type (tenant, deleted); 

CREATE INDEX sp_idx_distribution_set_type_prim 
  ON sp_distribution_set_type (tenant, id); 

CREATE TABLE sp_rollout 
  ( 
     id                     BIGINT GENERATED always AS IDENTITY NOT NULL, 
     tenant                 VARCHAR(40) NOT NULL, 
     action_type            INTEGER NOT NULL, 
     created_at             BIGINT NOT NULL, 
     created_by             VARCHAR(40) NOT NULL, 
     deleted                SMALLINT DEFAULT 0, 
     description            VARCHAR(512), 
     forced_time            BIGINT, 
     last_check             BIGINT, 
     last_modified_at       BIGINT NOT NULL, 
     last_modified_by       VARCHAR(40) NOT NULL, 
     name                   VARCHAR(64) NOT NULL, 
     optlock_revision       INTEGER, 
     rollout_groups_created INTEGER, 
     start_at               BIGINT, 
     status                 INTEGER NOT NULL, 
     target_filter          VARCHAR(1024) NOT NULL, 
     total_targets          BIGINT, 
     distribution_set       BIGINT NOT NULL, 
     PRIMARY KEY (id) 
  ); 

CREATE TABLE sp_rolloutgroup 
  ( 
     id                    BIGINT GENERATED always AS IDENTITY NOT NULL, 
     tenant                VARCHAR(40) NOT NULL, 
     created_at            BIGINT NOT NULL, 
     created_by            VARCHAR(40) NOT NULL, 
     description           VARCHAR(512), 
     error_action          INTEGER, 
     error_action_exp      VARCHAR(512), 
     error_condition       INTEGER, 
     error_condition_exp   VARCHAR(512), 
     last_modified_at      BIGINT NOT NULL, 
     last_modified_by      VARCHAR(40) NOT NULL, 
     name                  VARCHAR(64) NOT NULL, 
     optlock_revision      INTEGER, 
     status                INTEGER NOT NULL, 
     success_action        INTEGER NOT NULL, 
     success_action_exp    VARCHAR(512), 
     success_condition     INTEGER NOT NULL, 
     success_condition_exp VARCHAR(512) NOT NULL, 
     target_filter         VARCHAR(1024), 
     target_percentage     FLOAT, 
     total_targets         INTEGER, 
     parent_id             BIGINT, 
     rollout               BIGINT NOT NULL, 
     PRIMARY KEY (id) 
  ); 

CREATE TABLE sp_base_software_module 
  ( 
     id               BIGINT GENERATED always AS IDENTITY NOT NULL, 
     tenant           VARCHAR(40) NOT NULL, 
     created_at       BIGINT NOT NULL, 
     created_by       VARCHAR(40) NOT NULL, 
     deleted          SMALLINT DEFAULT 0, 
     description      VARCHAR(512), 
     last_modified_at BIGINT NOT NULL, 
     last_modified_by VARCHAR(40) NOT NULL, 
     name             VARCHAR(64) NOT NULL, 
     optlock_revision INTEGER, 
     vendor           VARCHAR(256), 
     VERSION          VARCHAR(64) NOT NULL, 
     module_type      BIGINT NOT NULL, 
     PRIMARY KEY (id) 
  ); 

CREATE INDEX sp_idx_base_sw_module_01 
  ON sp_base_software_module (tenant, deleted, name, VERSION); 

CREATE INDEX sp_idx_base_sw_module_02 
  ON sp_base_software_module (tenant, deleted, module_type); 

CREATE INDEX sp_idx_base_sw_module_prim 
  ON sp_base_software_module (tenant, id); 

CREATE TABLE sp_sw_metadata 
  ( 
     meta_key       VARCHAR(128) NOT NULL, 
     target_visible SMALLINT DEFAULT 0, 
     meta_value     VARCHAR(4000), 
     sw_id          BIGINT NOT NULL, 
     PRIMARY KEY (meta_key, sw_id) 
  ); 

CREATE TABLE sp_software_module_type 
  ( 
     id                 BIGINT GENERATED always AS IDENTITY NOT NULL, 
     tenant             VARCHAR(40) NOT NULL, 
     colour             VARCHAR(16), 
     created_at         BIGINT NOT NULL, 
     created_by         VARCHAR(40) NOT NULL, 
     deleted            SMALLINT DEFAULT 0, 
     description        VARCHAR(512), 
     type_key           VARCHAR(64) NOT NULL, 
     last_modified_at   BIGINT NOT NULL, 
     last_modified_by   VARCHAR(40) NOT NULL, 
     max_ds_assignments INTEGER NOT NULL, 
     name               VARCHAR(64) NOT NULL, 
     optlock_revision   INTEGER, 
     PRIMARY KEY (id) 
  ); 

CREATE INDEX sp_idx_software_module_type_01 
  ON sp_software_module_type (tenant, deleted); 

CREATE INDEX sp_idx_software_module_type_prim 
  ON sp_software_module_type (tenant, id); 

CREATE TABLE sp_target 
  ( 
     id                            BIGINT GENERATED always AS IDENTITY NOT NULL, 
     tenant                        VARCHAR(40) NOT NULL, 
     address                       VARCHAR(512), 
     controller_id                 VARCHAR(64) NOT NULL, 
     created_at                    BIGINT NOT NULL, 
     created_by                    VARCHAR(40) NOT NULL, 
     description                   VARCHAR(512), 
     install_date                  BIGINT, 
     last_modified_at              BIGINT NOT NULL, 
     last_modified_by              VARCHAR(40) NOT NULL, 
     last_target_query             BIGINT, 
     name                          VARCHAR(64) NOT NULL, 
     optlock_revision              INTEGER, 
     request_controller_attributes SMALLINT DEFAULT 0 NOT NULL, 
     sec_token                     VARCHAR(128) NOT NULL, 
     update_status                 INTEGER NOT NULL, 
     assigned_distribution_set     BIGINT, 
     installed_distribution_set    BIGINT, 
     PRIMARY KEY (id) 
  ); 

CREATE INDEX sp_idx_target_01 
  ON sp_target (tenant, name, assigned_distribution_set); 

CREATE INDEX sp_idx_target_03 
  ON sp_target (tenant, controller_id, assigned_distribution_set); 

CREATE INDEX sp_idx_target_04 
  ON sp_target (tenant, created_at); 

CREATE INDEX sp_idx_target_prim 
  ON sp_target (tenant, id); 

CREATE TABLE sp_target_filter_query 
  ( 
     id                           BIGINT GENERATED always AS IDENTITY NOT NULL, 
     tenant                       VARCHAR(40) NOT NULL, 
     created_at                   BIGINT NOT NULL, 
     created_by                   VARCHAR(40) NOT NULL, 
     last_modified_at             BIGINT NOT NULL, 
     last_modified_by             VARCHAR(40) NOT NULL, 
     name                         VARCHAR(64) NOT NULL, 
     optlock_revision             INTEGER, 
     QUERY                        VARCHAR(1024) NOT NULL, 
     auto_assign_distribution_set BIGINT, 
     PRIMARY KEY (id) 
  ); 

CREATE TABLE sp_target_tag 
  ( 
     id               BIGINT GENERATED always AS IDENTITY NOT NULL, 
     tenant           VARCHAR(40) NOT NULL, 
     colour           VARCHAR(16), 
     created_at       BIGINT NOT NULL, 
     created_by       VARCHAR(40) NOT NULL, 
     description      VARCHAR(512), 
     last_modified_at BIGINT NOT NULL, 
     last_modified_by VARCHAR(40) NOT NULL, 
     name             VARCHAR(64) NOT NULL, 
     optlock_revision INTEGER, 
     PRIMARY KEY (id) 
  ); 

CREATE INDEX sp_idx_target_tag_prim 
  ON sp_target_tag (tenant, id); 

CREATE TABLE sp_tenant_configuration 
  ( 
     id               BIGINT GENERATED always AS IDENTITY NOT NULL, 
     tenant           VARCHAR(40) NOT NULL, 
     created_at       BIGINT NOT NULL, 
     created_by       VARCHAR(40) NOT NULL, 
     conf_key         VARCHAR(128) NOT NULL, 
     last_modified_at BIGINT NOT NULL, 
     last_modified_by VARCHAR(40) NOT NULL, 
     optlock_revision INTEGER, 
     conf_value       VARCHAR(512) NOT NULL, 
     PRIMARY KEY (id) 
  ); 

CREATE TABLE sp_tenant 
  ( 
     id               BIGINT GENERATED always AS IDENTITY NOT NULL, 
     created_at       BIGINT NOT NULL, 
     created_by       VARCHAR(40) NOT NULL, 
     last_modified_at BIGINT NOT NULL, 
     last_modified_by VARCHAR(40) NOT NULL, 
     optlock_revision INTEGER, 
     tenant           VARCHAR(40) NOT NULL, 
     default_ds_type  BIGINT NOT NULL, 
     PRIMARY KEY (id) 
  ); 

CREATE INDEX sp_idx_tenant_prim 
  ON sp_tenant (tenant, id); 

CREATE TABLE sp_rollouttargetgroup 
  ( 
     rolloutgroup_id BIGINT NOT NULL, 
     target_id       BIGINT NOT NULL, 
     PRIMARY KEY (rolloutgroup_id, target_id) 
  ); 

CREATE TABLE sp_action_status_messages 
  ( 
     action_status_id BIGINT NOT NULL, 
     detail_message   VARCHAR(512) NOT NULL 
  ); 

CREATE INDEX sp_idx_action_status_msgs_01 
  ON sp_action_status_messages (action_status_id); 

CREATE TABLE sp_ds_module 
  ( 
     ds_id     BIGINT NOT NULL, 
     module_id BIGINT NOT NULL, 
     PRIMARY KEY (ds_id, module_id) 
  ); 

CREATE TABLE sp_ds_dstag 
  ( 
     ds  BIGINT NOT NULL, 
     tag BIGINT NOT NULL, 
     PRIMARY KEY (ds, tag) 
  ); 

CREATE TABLE sp_target_attributes 
  ( 
     target_id       BIGINT NOT NULL, 
     attribute_value VARCHAR(128), 
     attribute_key   VARCHAR(32) NOT NULL 
  ); 

CREATE TABLE sp_target_target_tag 
  ( 
     target BIGINT NOT NULL, 
     tag    BIGINT NOT NULL, 
     PRIMARY KEY (target, tag) 
  ); 

ALTER TABLE sp_distribution_set ADD CONSTRAINT uk_distrib_set UNIQUE (name, version, tenant);
ALTER TABLE sp_distributionset_tag ADD CONSTRAINT uk_ds_tag UNIQUE (name, tenant);
ALTER TABLE sp_distribution_set_type ADD CONSTRAINT uk_dst_name UNIQUE (name, tenant);
ALTER TABLE sp_distribution_set_type ADD CONSTRAINT uk_dst_key UNIQUE (type_key, tenant);
ALTER TABLE sp_rollout ADD CONSTRAINT uk_rollout UNIQUE (name, tenant);
ALTER TABLE sp_rolloutgroup ADD CONSTRAINT uk_rolloutgroup UNIQUE (name, rollout, tenant);
ALTER TABLE sp_base_software_module ADD CONSTRAINT uk_base_sw_mod UNIQUE (module_type, name, version, tenant);
ALTER TABLE sp_software_module_type ADD CONSTRAINT uk_smt_type_key UNIQUE (type_key, tenant);
ALTER TABLE sp_software_module_type ADD CONSTRAINT uk_smt_name UNIQUE (name, tenant);
ALTER TABLE sp_target ADD CONSTRAINT uk_tenant_controller_ UNIQUE (controller_id, tenant);
ALTER TABLE sp_target_filter_query ADD CONSTRAINT uk_tenant_custom_filt UNIQUE (name, tenant);
ALTER TABLE sp_target_tag ADD CONSTRAINT uk_targ_tag UNIQUE (name, tenant);
ALTER TABLE sp_tenant_configuration ADD CONSTRAINT uk_tenant_key UNIQUE (conf_key, tenant);
ALTER TABLE sp_tenant ADD CONSTRAINT uk_tenantmd_tenant UNIQUE (tenant);

ALTER TABLE sp_ds_type_element ADD CONSTRAINT fk_ds_type_element_element FOREIGN KEY (distribution_set_type) REFERENCES sp_distribution_set_type (id) ON DELETE CASCADE;
ALTER TABLE sp_ds_type_element ADD CONSTRAINT fk_ds_type_element_smtype FOREIGN KEY (software_module_type) REFERENCES  sp_software_module_type (id) ON DELETE CASCADE;
ALTER TABLE sp_action ADD CONSTRAINT fk_action_rolloutgroup FOREIGN KEY (rolloutgroup) REFERENCES sp_rolloutgroup (id);
ALTER TABLE sp_action ADD CONSTRAINT fk_action_rollout FOREIGN KEY (rolloutgroup) REFERENCES sp_rolloutgroup (id);
ALTER TABLE sp_action ADD CONSTRAINT fk_targ_act_hist_targ FOREIGN KEY (target) REFERENCES sp_target (id) ON DELETE CASCADE;
ALTER TABLE sp_action ADD CONSTRAINT fk_action_ds FOREIGN KEY (distribution_set) REFERENCES sp_distribution_set (id) ON DELETE CASCADE;
ALTER TABLE sp_action_status ADD CONSTRAINT fk_act_stat_action FOREIGN KEY (action) REFERENCES sp_action (id) ON DELETE CASCADE;
ALTER TABLE sp_artifact ADD CONSTRAINT fk_assigned_sm FOREIGN KEY (software_module) REFERENCES sp_base_software_module (id) ON DELETE CASCADE;
ALTER TABLE sp_distribution_set ADD CONSTRAINT fk_ds_dstype_ds FOREIGN KEY (ds_id) REFERENCES sp_distribution_set_type (id);
ALTER TABLE sp_ds_metadata ADD CONSTRAINT fk_metadata_ds FOREIGN KEY (ds_id) REFERENCES sp_distribution_set (id) ON DELETE CASCADE;
ALTER TABLE sp_rollout ADD CONSTRAINT fk_rollout_ds FOREIGN KEY (distribution_set) REFERENCES sp_distribution_set (id);
ALTER TABLE sp_rolloutgroup ADD CONSTRAINT fk_rolloutgroup_rollout FOREIGN KEY (rollout) REFERENCES sp_rollout (id) ON DELETE CASCADE;
ALTER TABLE sp_base_software_module ADD CONSTRAINT fk_module_type FOREIGN KEY (module_type) REFERENCES sp_software_module_type (id);
ALTER TABLE sp_sw_metadata ADD CONSTRAINT fk_metadata_sw FOREIGN KEY (sw_id) REFERENCES sp_base_software_module (id) ON DELETE CASCADE;
ALTER TABLE sp_target ADD CONSTRAINT fk_target_inst_ds FOREIGN KEY (installed_distribution_set) REFERENCES sp_distribution_set (id);
ALTER TABLE sp_target ADD CONSTRAINT fk_target_assign_ds FOREIGN KEY (assigned_distribution_set) REFERENCES sp_distribution_set (id);
ALTER TABLE sp_target_filter_query ADD CONSTRAINT fk_filter_auto_assign_ds FOREIGN KEY (auto_assign_distribution_set) REFERENCES sp_distribution_set (id) ON DELETE SET NULL;
ALTER TABLE sp_tenant ADD CONSTRAINT fk_tenant_md_default_ds_type FOREIGN KEY (default_ds_type) REFERENCES sp_distribution_set_type (id);
ALTER TABLE sp_rollouttargetgroup ADD CONSTRAINT fk_rollouttargetgroup_target FOREIGN KEY (target_id) REFERENCES sp_target (id) ON DELETE CASCADE;
ALTER TABLE sp_rollouttargetgroup ADD CONSTRAINT fk_rollouttargetgroup_group FOREIGN KEY (rolloutGroup_Id) REFERENCES sp_rolloutgroup (id) ON DELETE CASCADE;
ALTER TABLE sp_action_status_messages ADD CONSTRAINT fk_stat_msg_act_stat FOREIGN KEY (action_status_id) REFERENCES sp_action_status (id) ON DELETE CASCADE;
ALTER TABLE sp_ds_module ADD CONSTRAINT fk_ds_module_module FOREIGN KEY (module_id) REFERENCES sp_base_software_module (id) ON DELETE CASCADE;
ALTER TABLE sp_ds_module ADD CONSTRAINT fk_ds_module_ds FOREIGN KEY (ds_id) REFERENCES sp_distribution_set (id) ON DELETE CASCADE;
ALTER TABLE sp_ds_dstag ADD CONSTRAINT fk_ds_dstag_tag FOREIGN KEY (tag) REFERENCES sp_distributionset_tag (id) ON DELETE CASCADE;
ALTER TABLE sp_ds_dstag ADD CONSTRAINT fk_ds_dstag_ds FOREIGN KEY (ds) REFERENCES sp_distribution_set (id) ON DELETE CASCADE;
ALTER TABLE sp_target_attributes ADD CONSTRAINT fk_targ_attrib_target FOREIGN KEY (target_id) REFERENCES  sp_target (id) ON DELETE CASCADE;
ALTER TABLE sp_target_target_tag ADD CONSTRAINT fk_targ_targtag_tag FOREIGN KEY (tag) REFERENCES sp_target_tag (id) ON DELETE CASCADE;
ALTER TABLE sp_target_target_tag ADD CONSTRAINT fk_targ_targtag_target FOREIGN KEY (target) REFERENCES sp_target (id) ON DELETE CASCADE;
