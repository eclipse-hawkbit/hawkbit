CREATE TABLE sp_target_type
(
    id               BIGINT GENERATED always AS IDENTITY NOT NULL,
    tenant           VARCHAR(40) NOT NULL,
    colour           VARCHAR(16),
    created_at       BIGINT NOT NULL,
    created_by       VARCHAR(64) NOT NULL,
    description      VARCHAR(512),
    last_modified_at BIGINT NOT NULL,
    last_modified_by VARCHAR(64) NOT NULL,
    name             VARCHAR(64) NOT NULL,
    optlock_revision INTEGER,
    PRIMARY KEY (id)
);

CREATE INDEX sp_idx_target_type_prim
    ON sp_target_type (tenant, id);

CREATE TABLE sp_target_type_ds_type_relation
(
    target_type BIGINT NOT NULL,
    distribution_set_type  BIGINT NOT NULL,
    PRIMARY KEY (target_type, distribution_set_type)
);

ALTER TABLE sp_target_type ADD CONSTRAINT uk_target_type_name UNIQUE (name, tenant);

ALTER TABLE sp_target ADD COLUMN target_type BIGINT;
ALTER TABLE sp_target ADD CONSTRAINT fk_target_relation_target_type FOREIGN KEY (target_type) REFERENCES sp_target_type (id) ON DELETE SET NULL;

ALTER TABLE sp_target_type_ds_type_relation ADD CONSTRAINT fk_target_type_relation_target_type FOREIGN KEY (target_type) REFERENCES sp_target_type (id) ON DELETE CASCADE;
ALTER TABLE sp_target_type_ds_type_relation ADD CONSTRAINT fk_target_type_relation_ds_type FOREIGN KEY (distribution_set_type) REFERENCES  sp_distribution_set_type (id) ON DELETE CASCADE;