ALTER TABLE sp_rolloutgroup ADD column confirmation_required BOOLEAN;
UPDATE sp_rolloutgroup SET confirmation_required = 0;

ALTER TABLE sp_target_filter_query ADD column confirmation_required BOOLEAN;
UPDATE sp_target_filter_query SET confirmation_required = 0;

create table sp_target_conf_status
(
    id                  bigint not null auto_increment,
    target_id           bigint not null,
    initiator           varchar(64),
    remark              VARCHAR(512),
    created_at          bigint,
    created_by          varchar(64),
    last_modified_at    bigint,
    last_modified_by    varchar(64),
    optlock_revision    bigint,
    tenant              varchar(40) not null,
    primary key (id)
);
ALTER TABLE sp_target_conf_status
    ADD CONSTRAINT fk_target_auto_conf FOREIGN KEY (target_id) REFERENCES sp_target (id) ON DELETE CASCADE;