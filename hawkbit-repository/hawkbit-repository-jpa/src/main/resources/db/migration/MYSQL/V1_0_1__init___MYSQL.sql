
    create table sp_action (
        id bigint not null auto_increment,
        created_at bigint,
        created_by varchar(40),
        last_modified_at bigint,
        last_modified_by varchar(40),
        optlock_revision bigint,
        tenant varchar(40) not null,
        action_type varchar(255) not null,
        active bit,
        forced_time bigint,
        status integer,
        distribution_set bigint,
        target bigint,
        primary key (id)
    );

    create table sp_action_status (
        id bigint not null auto_increment,
        created_at bigint,
        created_by varchar(40),
        last_modified_at bigint,
        last_modified_by varchar(40),
        optlock_revision bigint,
        tenant varchar(40) not null,
        target_occurred_at bigint,
        status integer,
        action bigint not null,
        primary key (id)
    );

    create table sp_action_status_messages (
        action_status_id bigint not null,
        detail_message varchar(512)
    );

    create table sp_artifact (
        id bigint not null auto_increment,
        created_at bigint,
        created_by varchar(40),
        last_modified_at bigint,
        last_modified_by varchar(40),
        optlock_revision bigint,
        tenant varchar(40) not null,
        md5_hash varchar(32),
        sha1_hash varchar(40),
        file_size bigint,
        provided_file_name varchar(256),
        gridfs_file_name varchar(40),
        software_module bigint not null,
        primary key (id)
    );

    create table sp_base_software_module (
        id bigint not null auto_increment,
        created_at bigint,
        created_by varchar(40),
        last_modified_at bigint,
        last_modified_by varchar(40),
        optlock_revision bigint,
        tenant varchar(40) not null,
        description varchar(512),
        name varchar(64) not null,
        version varchar(64) not null,
        deleted bit,
        vendor varchar(256),
        module_type bigint not null,
        primary key (id)
    );

    create table sp_distribution_set (
        id bigint not null auto_increment,
        created_at bigint,
        created_by varchar(40),
        last_modified_at bigint,
        last_modified_by varchar(40),
        optlock_revision bigint,
        tenant varchar(40) not null,
        description varchar(512),
        name varchar(64) not null,
        version varchar(64) not null,
        complete bit,
        deleted bit,
        required_migration_step bit,
        ds_id bigint not null,
        primary key (id)
    );

    create table sp_distribution_set_type (
        id bigint not null auto_increment,
        created_at bigint,
        created_by varchar(40),
        last_modified_at bigint,
        last_modified_by varchar(40),
        optlock_revision bigint,
        tenant varchar(40) not null,
        description varchar(512),
        name varchar(64) not null,
        colour varchar(16),
        deleted bit,
        type_key varchar(64) not null,
        primary key (id)
    );

    create table sp_distributionset_tag (
        id bigint not null auto_increment,
        created_at bigint,
        created_by varchar(40),
        last_modified_at bigint,
        last_modified_by varchar(40),
        optlock_revision bigint,
        tenant varchar(40) not null,
        description varchar(512),
        name varchar(64) not null,
        colour varchar(16),
        primary key (id)
    );

    create table sp_ds_dstag (
        ds bigint not null,
        TAG bigint not null,
        primary key (ds, TAG)
    );

    create table sp_ds_metadata (
        meta_key varchar(128) not null,
        meta_value varchar(4000),
        ds_id bigint not null,
        primary key (ds_id, meta_key)
    );

    create table sp_ds_module (
        ds_id bigint not null,
        module_id bigint not null,
        primary key (ds_id, module_id)
    );

    create table sp_ds_type_element (
        mandatory bit,
        distribution_set_type bigint not null,
        software_module_type bigint not null,
        primary key (distribution_set_type, software_module_type)
    );

    create table sp_external_artifact (
        id bigint not null auto_increment,
        created_at bigint,
        created_by varchar(40),
        last_modified_at bigint,
        last_modified_by varchar(40),
        optlock_revision bigint,
        tenant varchar(40) not null,
        md5_hash varchar(32),
        sha1_hash varchar(40),
        file_size bigint,
        url_suffix varchar(512),
        provider bigint not null,
        software_module bigint not null,
        primary key (id)
    );

    create table sp_external_provider (
        id bigint not null auto_increment,
        created_at bigint,
        created_by varchar(40),
        last_modified_at bigint,
        last_modified_by varchar(40),
        optlock_revision bigint,
        tenant varchar(40) not null,
        description varchar(512),
        name varchar(64) not null,
        base_url varchar(512) not null,
        default_url_suffix varchar(512),
        primary key (id)
    );

    create table sp_software_module_type (
        id bigint not null auto_increment,
        created_at bigint,
        created_by varchar(40),
        last_modified_at bigint,
        last_modified_by varchar(40),
        optlock_revision bigint,
        tenant varchar(40) not null,
        description varchar(512),
        name varchar(64) not null,
        colour varchar(16),
        deleted bit,
        type_key varchar(64) not null,
        max_ds_assignments integer not null,
        primary key (id)
    );

    create table sp_sw_metadata (
        meta_key varchar(128) not null,
        meta_value varchar(4000),
        sw_id bigint not null,
        primary key (meta_key, sw_id)
    );

    create table sp_target (
        id bigint not null auto_increment,
        created_at bigint,
        created_by varchar(40),
        last_modified_at bigint,
        last_modified_by varchar(40),
        optlock_revision bigint,
        tenant varchar(40) not null,
        description varchar(512),
        name varchar(64) not null,
        controller_id varchar(64),
        sec_token varchar(128) not null,
        assigned_distribution_set bigint,
        primary key (id)
    );

    create table sp_target_attributes (
        target_id bigint not null,
        attribute_value varchar(128),
        attribute_key varchar(32) not null,
        primary key (target_id, attribute_key)
    );

    create table sp_target_info (
        target_id bigint not null,
        install_date bigint,
        ip_address varchar(46),
        last_target_query bigint,
        request_controller_attributes bit not null,
        update_status varchar(255) not null,
        installed_distribution_set bigint,
        primary key (target_id)
    );

    create table sp_target_tag (
        id bigint not null auto_increment,
        created_at bigint,
        created_by varchar(40),
        last_modified_at bigint,
        last_modified_by varchar(40),
        optlock_revision bigint,
        tenant varchar(40) not null,
        description varchar(512),
        name varchar(64) not null,
        colour varchar(16),
        primary key (id)
    );

    create table sp_target_target_tag (
        target bigint not null,
        tag bigint not null,
        primary key (target, tag)
    );

    create table sp_tenant (
        id bigint not null auto_increment,
        created_at bigint,
        created_by varchar(40),
        last_modified_at bigint,
        last_modified_by varchar(40),
        optlock_revision bigint,
        tenant varchar(40) not null,
        default_ds_type bigint not null,
        primary key (id)
    );

    create table sp_tenant_configuration (
        id bigint not null auto_increment,
        created_at bigint,
        created_by varchar(40),
        last_modified_at bigint,
        last_modified_by varchar(40),
        optlock_revision bigint,
        tenant varchar(40) not null,
        conf_key varchar(128),
        conf_value varchar(512),
        primary key (id)
    );

    create index sp_idx_action_01 on sp_action (tenant, distribution_set);

    create index sp_idx_action_02 on sp_action (tenant, target, active);

    create index sp_idx_action_prim on sp_action (tenant, id);

    create index sp_idx_action_status_01 on sp_action_status (tenant, action);

    create index sp_idx_action_status_02 on sp_action_status (tenant, action, status);

    create index sp_idx_action_status_prim on sp_action_status (tenant, id);

    create index sp_idx_action_status_msgs_01 on sp_action_status_messages (action_status_id);

    create index sp_idx_artifact_01 on sp_artifact (tenant, software_module);

    create index sp_idx_artifact_prim on sp_artifact (tenant, id);

    alter table sp_base_software_module 
        add constraint uk_base_sw_mod  unique (module_type, name, version, tenant);

    create index sp_idx_base_sw_module_01 on sp_base_software_module (tenant, deleted, name, version);

    create index sp_idx_base_sw_module_02 on sp_base_software_module (tenant, deleted, module_type);

    create index sp_idx_base_sw_module_prim on sp_base_software_module (tenant, id);

    alter table sp_distribution_set 
        add constraint uk_distrib_set  unique (name, version, tenant);

    create index sp_idx_distribution_set_01 on sp_distribution_set (tenant, deleted, name, complete);

    create index sp_idx_distribution_set_02 on sp_distribution_set (tenant, required_migration_step);

    create index sp_idx_distribution_set_prim on sp_distribution_set (tenant, id);

    alter table sp_distribution_set_type 
        add constraint uk_dst_name  unique (name, tenant);

    alter table sp_distribution_set_type 
        add constraint uk_dst_key  unique (type_key, tenant);

    create index sp_idx_distribution_set_type_01 on sp_distribution_set_type (tenant, deleted);

    create index sp_idx_distribution_set_type_prim on sp_distribution_set_type (tenant, id);

    alter table sp_distributionset_tag 
        add constraint uk_ds_tag  unique (name, tenant);

    create index sp_idx_distribution_set_tag_prim on sp_distributionset_tag (tenant, id);

    create index sp_idx_external_artifact_prim on sp_external_artifact (id, tenant);

    create index sp_idx_external_provider_prim on sp_external_provider (tenant, id);

    alter table sp_software_module_type 
        add constraint uk_smt_type_key  unique (type_key, tenant);

    alter table sp_software_module_type 
        add constraint uk_smt_name  unique (name, tenant);

    create index sp_idx_software_module_type_01 on sp_software_module_type (tenant, deleted);

    create index sp_idx_software_module_type_prim on sp_software_module_type (tenant, id);

    alter table sp_target 
        add constraint uk_tenant_controller_id  unique (controller_id, tenant);

    create index sp_idx_target_01 on sp_target (tenant, name, assigned_distribution_set);

    create index sp_idx_target_02 on sp_target (tenant, name);

    create index sp_idx_target_03 on sp_target (tenant, controller_id, assigned_distribution_set);
	
	create index sp_idx_target_04 on sp_target (tenant, created_at);

    create index sp_idx_target_prim on sp_target (tenant, id);

    create index sp_idx_target_info_01 on sp_target_info (ip_address);

    create index sp_idx_target_info_02 on sp_target_info (target_id, update_status);

    alter table sp_target_tag 
        add constraint uk_targ_tag  unique (name, tenant);

    create index sp_idx_target_tag_prim on sp_target_tag (tenant, id);

    alter table sp_tenant 
        add constraint uk_tenantmd_tenant  unique (tenant);

    create index sp_idx_tenant_prim on sp_tenant (tenant, id);

    alter table sp_tenant_configuration 
        add constraint uk_tenant_key  unique (conf_key, tenant);

    alter table sp_action 
        add constraint fk_action_ds 
        foreign key (distribution_set) 
        references sp_distribution_set (id);

    alter table sp_action 
        add constraint fk_targ_act_hist_targ 
        foreign key (target) 
        references sp_target (id);

    alter table sp_action_status 
        add constraint fk_act_stat_action 
        foreign key (action) 
        references sp_action (id);

    alter table sp_action_status_messages 
        add constraint fk_stat_msg_act_stat 
        foreign key (action_status_id) 
        references sp_action_status (id);

    alter table sp_artifact 
        add constraint fk_assigned_sm 
        foreign key (software_module) 
        references sp_base_software_module (id);

    alter table sp_base_software_module 
        add constraint fk_module_type 
        foreign key (module_type) 
        references sp_software_module_type (id);

    alter table sp_distribution_set 
        add constraint fk_ds_dstype_ds 
        foreign key (ds_id) 
        references sp_distribution_set_type (id);

    alter table sp_ds_dstag 
        add constraint fk_ds_dstag_tag 
        foreign key (TAG) 
        references sp_distributionset_tag (id);

    alter table sp_ds_dstag 
        add constraint fk_ds_dstag_ds 
        foreign key (ds) 
        references sp_distribution_set (id);

    alter table sp_ds_metadata 
        add constraint fk_metadata_ds 
        foreign key (ds_id) 
        references sp_distribution_set (id);

    alter table sp_ds_module 
        add constraint fk_ds_module_module 
        foreign key (module_id) 
        references sp_base_software_module (id);

    alter table sp_ds_module 
        add constraint fk_ds_module_ds 
        foreign key (ds_id) 
        references sp_distribution_set (id);

    alter table sp_ds_type_element 
        add constraint fk_ds_type_element_element 
        foreign key (distribution_set_type) 
        references sp_distribution_set_type (id);

    alter table sp_ds_type_element 
        add constraint fk_ds_type_element_smtype 
        foreign key (software_module_type) 
        references sp_software_module_type (id);

    alter table sp_external_artifact 
        add constraint fk_art_to_ext_provider 
        foreign key (provider) 
        references sp_external_provider (id);

    alter table sp_external_artifact 
        add constraint fk_external_assigned_sm 
        foreign key (software_module) 
        references sp_base_software_module (id);

    alter table sp_sw_metadata 
        add constraint fk_metadata_sw 
        foreign key (sw_id) 
        references sp_base_software_module (id);

    alter table sp_target 
        add constraint fk_target_assign_ds 
        foreign key (assigned_distribution_set) 
        references sp_distribution_set (id);

    alter table sp_target_attributes 
        add constraint fk_targ_attrib_target 
        foreign key (target_id) 
        references sp_target_info (target_id);

    alter table sp_target_info 
        add constraint fk_target_inst_ds 
        foreign key (installed_distribution_set) 
        references sp_distribution_set (id);

    alter table sp_target_info 
        add constraint fk_targ_stat_targ 
        foreign key (target_id) 
        references sp_target (id);

    alter table sp_target_target_tag 
        add constraint fk_targ_targtag_tag 
        foreign key (tag) 
        references sp_target_tag (id);

    alter table sp_target_target_tag 
        add constraint fk_targ_targtag_target 
        foreign key (target) 
        references sp_target (id);

    alter table sp_tenant 
        add constraint fk_tenant_md_default_ds_type 
        foreign key (default_ds_type) 
        references sp_distribution_set_type (id);
