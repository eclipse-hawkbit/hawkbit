ALTER TABLE sp_rolloutgroup ADD confirmation_required BIT DEFAULT 0;
ALTER TABLE sp_target_filter_query ADD confirmation_required BIT DEFAULT 0;

CREATE TABLE sp_target_conf_status
(
    id                  NUMERIC(19) IDENTITY NOT NULL,
    target_id           NUMERIC(19) NOT NULL,
    initiator           VARCHAR(64),
    remark              VARCHAR(512),
    tenant              VARCHAR(40) NOT NULL,
    created_at          NUMERIC(19) NOT NULL,
    created_by          VARCHAR(64) NOT NULL,
    last_modified_at    NUMERIC(19) NOT NULL,
    last_modified_by    VARCHAR(64) NOT NULL,
    optlock_revision    INTEGER NULL,
    PRIMARY KEY (id)
);
ALTER TABLE sp_target_conf_status
    ADD CONSTRAINT fk_target_auto_conf FOREIGN KEY (target_id) REFERENCES sp_target (id) ON DELETE CASCADE;