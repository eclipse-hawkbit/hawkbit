-- ------------ Write CREATE-SEQUENCE-stage scripts -----------

CREATE SEQUENCE IF NOT EXISTS sp_target_type_seq
INCREMENT BY 1
START WITH 1
NO CYCLE;

-- ------------ Write CREATE-TABLE-stage scripts -----------

CREATE TABLE sp_target_type(
    id BIGINT NOT NULL DEFAULT nextval('sp_target_type_seq'),
    created_at       BIGINT,
    created_by       VARCHAR(64),
    last_modified_at BIGINT,
    last_modified_by VARCHAR(64),
    optlock_revision BIGINT,
    tenant           VARCHAR(40) NOT NULL,
    description      VARCHAR(512),
    name             VARCHAR(64),
    colour           VARCHAR(16)
)
        WITH (
        OIDS=FALSE
        );

CREATE TABLE sp_target_type_ds_type_relation(
    target_type           BIGINT NOT NULL,
    distribution_set_type BIGINT NOT NULL
)
        WITH (
        OIDS=FALSE
        );

-- ------------ Alter Table and Write INDEX scripts -----------

ALTER TABLE sp_target_type
ADD CONSTRAINT pk_sp_target_type PRIMARY KEY (id);

ALTER TABLE sp_target_type
ADD CONSTRAINT uk_target_type_name UNIQUE (name, tenant);

CREATE INDEX sp_idx_target_type_prim
ON sp_target_type
USING BTREE (tenant, id);

ALTER TABLE sp_target
ADD COLUMN target_type BIGINT;

ALTER TABLE sp_target
ADD CONSTRAINT fk_target_relation_target_type FOREIGN KEY (target_type)
REFERENCES sp_target_type (id)
ON UPDATE RESTRICT
ON DELETE SET NULL;

ALTER TABLE sp_target_type_ds_type_relation
ADD CONSTRAINT fk_target_type_relation_target_type FOREIGN KEY (target_type)
REFERENCES sp_target_type (id)
ON UPDATE RESTRICT
ON DELETE CASCADE;

ALTER TABLE sp_target_type_ds_type_relation
ADD CONSTRAINT fk_target_type_relation_ds_type FOREIGN KEY (distribution_set_type)
REFERENCES sp_distribution_set_type (id)
ON UPDATE RESTRICT
ON DELETE CASCADE;