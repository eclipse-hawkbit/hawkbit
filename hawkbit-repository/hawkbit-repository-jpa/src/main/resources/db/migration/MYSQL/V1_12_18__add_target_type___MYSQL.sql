create table sp_target_type
(
    id               bigint      not null auto_increment,
    created_at       bigint,
    created_by       varchar(64),
    last_modified_at bigint,
    last_modified_by varchar(64),
    optlock_revision bigint,
    tenant           varchar(40) not null,
    description      varchar(512),
    name             varchar(64) not null,
    colour           varchar(16),
    primary key (id)
);

create table sp_target_type_element
(
    target_type           bigint not null,
    distribution_set_type bigint not null,
    primary key (target_type, distribution_set_type)
);

alter table sp_target_type
    add constraint uk_target_type_name unique (name, tenant);

alter table sp_target_type
    add constraint uk_target_type_id unique (id, tenant);

create index sp_idx_target_type_prim on sp_target_type (tenant, id);

alter table sp_target
    add column target_type bigint;

alter table sp_target
    add constraint fk_target_target_type_target
        foreign key (target_type)
            references sp_target_type (id);

alter table sp_target_type_element
    add constraint fk_target_type_element_element
        foreign key (target_type)
            references sp_target_type (id);

alter table sp_target_type_element
    add constraint fk_target_type_element_dstype
        foreign key (distribution_set_type)
            references sp_distribution_set_type (id);
