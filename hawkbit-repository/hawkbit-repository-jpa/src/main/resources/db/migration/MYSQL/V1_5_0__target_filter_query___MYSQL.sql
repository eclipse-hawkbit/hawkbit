    create table sp_target_filter_query (
        id bigint not null auto_increment,
        created_at bigint,
        created_by varchar(40),
        last_modified_at bigint,
        last_modified_by varchar(40),
        optlock_revision bigint,
        tenant varchar(40) not null,
        name varchar(64) not null,
		query varchar(1024) not null,
        primary key (id)
	);
	
	
	create index sp_idx_target_filter_query_01 on sp_target_filter_query (tenant, name);
