create table sp_target_metadata (
	meta_key varchar(128) not null,
	meta_value varchar(4000),
	target_id bigint not null,
	primary key (target_id, meta_key)
);
     
alter table sp_target_metadata 
	add constraint fk_metadata_target 
	foreign key (target_id) 
	references sp_target (id)
	on delete cascade;