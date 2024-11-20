ALTER TABLE sp_distribution_set ALTER COLUMN name VARCHAR(128);
ALTER TABLE sp_distribution_set_type ALTER COLUMN name VARCHAR(128);
ALTER TABLE sp_distributionset_tag ALTER COLUMN name VARCHAR(128);
ALTER TABLE sp_base_software_module ALTER COLUMN name VARCHAR(128);
ALTER TABLE sp_rollout ALTER COLUMN name VARCHAR(128);
ALTER TABLE sp_rolloutgroup ALTER COLUMN name VARCHAR(128);
ALTER TABLE sp_software_module_type ALTER COLUMN name VARCHAR(128);
ALTER TABLE sp_target ALTER COLUMN name VARCHAR(128);
ALTER TABLE sp_target_filter_query ALTER COLUMN name VARCHAR(128);
ALTER TABLE sp_target_tag ALTER COLUMN name VARCHAR(128);


ALTER TABLE sp_target ALTER COLUMN controller_id VARCHAR(256);
