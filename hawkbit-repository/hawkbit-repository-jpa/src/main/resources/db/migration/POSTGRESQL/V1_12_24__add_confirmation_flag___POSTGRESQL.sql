ALTER TABLE sp_rolloutgroup
    ADD COLUMN confirmation_required BOOLEAN;
ALTER TABLE sp_target_filter_query
    ADD COLUMN confirmation_required BOOLEAN;

UPDATE sp_base_software_module SET confirmation_required = 0;
UPDATE sp_target_filter_query SET confirmation_required = 0;

CREATE TABLE sp_target_conf_status
(
    id               BIGINT      NOT NULL DEFAULT nextval('sp_target_conf_status_seq'),
    target_id        BIGINT      NOT NULL,
    initiator        VARCHAR(64),
    remark           VARCHAR(512),
    tenant           VARCHAR(40) NOT NULL,
    created_at       BIGINT      NOT NULL,
    created_by       VARCHAR(64) NOT NULL,
    last_modified_at BIGINT      NOT NULL,
    last_modified_by VARCHAR(64) NOT NULL,
    optlock_revision BIGINT NULL
)
    WITH (
        OIDS = FALSE
        );


ALTER TABLE sp_target_conf_status
    ADD CONSTRAINT pk_sp_target_conf_status PRIMARY KEY (id);

ALTER TABLE sp_sw_metadata
    ADD CONSTRAINT fk_target_auto_conf FOREIGN KEY (target_id)
        REFERENCES REFERENCES (id)
        ON UPDATE RESTRICT
        ON DELETE CASCADE;