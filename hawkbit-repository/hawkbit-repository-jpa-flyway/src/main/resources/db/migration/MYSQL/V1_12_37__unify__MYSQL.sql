-- fix NOT NULL disappeared in V1_12_12__change_length_of_controller_id_and_name___MYSQL.sql and V1_12_22__change_target_type_name_length___MYSQL.sql
UPDATE sp_software_module SET name = '' WHERE name IS NULL;
ALTER TABLE sp_software_module MODIFY name VARCHAR(128) NOT NULL;
UPDATE sp_software_module_type SET name = '' WHERE name IS NULL;
ALTER TABLE sp_software_module_type MODIFY name VARCHAR(128) NOT NULL;

UPDATE sp_distribution_set SET name = '' WHERE name IS NULL;
ALTER TABLE sp_distribution_set MODIFY name VARCHAR(128) NOT NULL;
UPDATE sp_distribution_set_type SET name = '' WHERE name IS NULL;
ALTER TABLE sp_distribution_set_type MODIFY name VARCHAR(128) NOT NULL;
UPDATE sp_distribution_set_tag SET name = '' WHERE name IS NULL;
ALTER TABLE sp_distribution_set_tag MODIFY name VARCHAR(128) NOT NULL;

UPDATE sp_target SET name = '' WHERE name IS NULL;
ALTER TABLE sp_target MODIFY name VARCHAR(128) NOT NULL;
UPDATE sp_target_type SET name = '' WHERE name IS NULL;
ALTER TABLE sp_target_type MODIFY name VARCHAR(128) NOT NULL;
UPDATE sp_target_tag SET name = '' WHERE name IS NULL;
ALTER TABLE sp_target_tag MODIFY name VARCHAR(128) NOT NULL;
UPDATE sp_target_filter_query SET name = '' WHERE name IS NULL;
ALTER TABLE sp_target_filter_query MODIFY name VARCHAR(128) NOT NULL;

UPDATE sp_rollout SET name = '' WHERE name IS NULL;
ALTER TABLE sp_rollout MODIFY name VARCHAR(128) NOT NULL;
UPDATE sp_rollout_group SET name = '' WHERE name IS NULL;
ALTER TABLE sp_rollout_group MODIFY name VARCHAR(128) NOT NULL;

UPDATE sp_target SET controller_id = '' WHERE controller_id IS NULL;
ALTER TABLE sp_target MODIFY controller_id VARCHAR(256) NOT NULL;

-- remove unnecessary / faulty default for a tenant's scope unique key
ALTER TABLE sp_target_type ALTER COLUMN type_key DROP DEFAULT;

-- remove unused column
ALTER TABLE sp_rollout DROP COLUMN group_theshold;