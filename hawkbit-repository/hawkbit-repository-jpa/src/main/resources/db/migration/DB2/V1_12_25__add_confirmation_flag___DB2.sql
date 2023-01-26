ALTER TABLE sp_rolloutgroup ADD COLUMN confirmation_required BOOLEAN;
UPDATE sp_rolloutgroup SET confirmation_required = 0;

ALTER TABLE sp_target_filter_query ADD COLUMN confirmation_required BOOLEAN;
UPDATE sp_target_filter_query SET confirmation_required = 0;

CREATE TABLE sp_target_conf_status
(
    id                  BIGINT GENERATED always AS IDENTITY NOT NULL,
    target_id           bigint not null,
    initiator           VARCHAR(64),
    remark              VARCHAR(512),
    created_at          BIGINT,
    created_by          VARCHAR(64),
    last_modified_at    BIGINT,
    last_modified_by    VARCHAR(64),
    optlock_revision    BIGINT,
    tenant              VARCHAR(40) not null,
    primary key (id)
);
ALTER TABLE sp_target_conf_status
    ADD CONSTRAINT fk_target_auto_conf FOREIGN KEY (target_id) REFERENCES sp_target (id) ON DELETE CASCADE;
