    create table sp_rolloutgroup (
        id bigint not null auto_increment,
        created_at bigint,
        created_by varchar(40),
        last_modified_at bigint,
        last_modified_by varchar(40),
        optlock_revision bigint,
        tenant varchar(40) not null,
        description varchar(512),
        name varchar(64) not null,
        error_condition integer,
        error_condition_exp varchar(512),
		error_action integer,
        error_action_exp varchar(512),
        success_condition integer not null,
        success_condition_exp varchar(512) not null,
		success_action integer not null,
        success_action_exp varchar(512),
        status integer,
        parent_id bigint,
        rollout bigint,
        total_targets bigint,
        primary key (id)
    );
	
	create table sp_rollout (
        id bigint not null auto_increment,
        created_at bigint,
        created_by varchar(40),
        last_modified_at bigint,
        last_modified_by varchar(40),
        optlock_revision bigint,
        tenant varchar(40) not null,
        description varchar(512),
        name varchar(64) not null,
		last_check bigint,
        group_theshold float,
        status integer,
        distribution_set bigint,
        target_filter varchar(1024),
		action_type varchar(255) not null,
        forced_time bigint,
        total_targets bigint,
        primary key (id)
    );
	
    create table sp_rollouttargetgroup (
        target_Id bigint not null,
        rolloutGroup_Id bigint not null,
        primary key (rolloutGroup_Id, target_Id)
    );
	
	create index sp_idx_rollout_01 on sp_rollout (tenant, name);
	
	create index sp_idx_rolloutgroup_01 on sp_rolloutgroup (tenant, name);
	
	ALTER TABLE sp_action ADD COLUMN rollout bigint;
    ALTER TABLE sp_action ADD COLUMN rolloutgroup bigint;
	
	alter table sp_rollout 
        add constraint uk_rollout unique (name, tenant);
		
	alter table sp_rolloutgroup 
        add constraint uk_rolloutgroup  unique (name, rollout, tenant);
	
	alter table sp_action 
        add constraint fk_action_rollout 
        foreign key (rollout) 
        references sp_rollout (id);
		
	alter table sp_action 
        add constraint fk_action_rolloutgroup 
        foreign key (rolloutgroup) 
        references sp_rolloutgroup (id);
		
	alter table sp_rollout 
        add constraint fk_rollout_ds 
        foreign key (distribution_set) 
        references sp_distribution_set (id);

    alter table sp_rolloutgroup 
        add constraint fk_rolloutgroup_rollout 
        foreign key (rollout) 
        references sp_rollout (id)
		on delete cascade;
		
	alter table sp_rolloutgroup 
        add constraint fk_rolloutgroup_rolloutgroup 
        foreign key (parent_id) 
        references sp_rolloutgroup (id)
		on delete cascade;
		
	alter table sp_rollouttargetgroup 
        add constraint fk_rollouttargetgroup_target 
        foreign key (target_id) 
        references sp_target (id)
		on delete cascade;

    alter table sp_rollouttargetgroup 
        add constraint fk_rollouttargetgroup_rolloutgroup 
        foreign key (rolloutgroup_id) 
        references sp_rolloutgroup (id)
		on delete cascade;