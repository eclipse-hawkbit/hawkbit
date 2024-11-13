ALTER TABLE sp_rolloutgroup
    ADD COLUMN confirmation_required BOOLEAN;
ALTER TABLE sp_target_filter_query
    ADD COLUMN confirmation_required BOOLEAN;

UPDATE sp_rolloutgroup SET confirmation_required = FALSE;
UPDATE sp_target_filter_query SET confirmation_required = FALSE;

CREATE TABLE sp_target_conf_status
(
    id               BIGSERIAL,
    target_id        BIGINT      NOT NULL,
    initiator        VARCHAR(64),
    remark           VARCHAR(512),
    tenant           VARCHAR(40) NOT NULL,
    created_at       BIGINT      NOT NULL,
    created_by       VARCHAR(64) NOT NULL,
    last_modified_at BIGINT      NOT NULL,
    last_modified_by VARCHAR(64) NOT NULL,
    optlock_revision BIGINT NULL
);


ALTER TABLE sp_target_conf_status
    ADD CONSTRAINT pk_sp_target_conf_status PRIMARY KEY (id);

