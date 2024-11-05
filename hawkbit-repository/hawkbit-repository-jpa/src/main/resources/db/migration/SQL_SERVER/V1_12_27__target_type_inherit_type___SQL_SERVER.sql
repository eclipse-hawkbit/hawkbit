ALTER TABLE sp_target_type
    ADD type_key VARCHAR(64) NOT NULL DEFAULT ('_');
UPDATE sp_target_type
SET type_key = name;
ALTER TABLE sp_target_type
    ADD CONSTRAINT uk_target_type_key UNIQUE (type_key, tenant);