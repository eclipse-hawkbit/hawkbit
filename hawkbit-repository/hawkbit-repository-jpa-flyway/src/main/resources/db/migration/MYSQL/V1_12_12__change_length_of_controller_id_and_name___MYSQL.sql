ALTER TABLE sp_distribution_set MODIFY name VARCHAR(128);
ALTER TABLE sp_distribution_set_type MODIFY name VARCHAR(128);
ALTER TABLE sp_distributionset_tag MODIFY name VARCHAR(128);
ALTER TABLE sp_base_software_module MODIFY name VARCHAR(128);
ALTER TABLE sp_rollout MODIFY name VARCHAR(128);
ALTER TABLE sp_rolloutgroup MODIFY name VARCHAR(128);
ALTER TABLE sp_software_module_type MODIFY name VARCHAR(128);
ALTER TABLE sp_target MODIFY name VARCHAR(128);
ALTER TABLE sp_target_filter_query MODIFY name VARCHAR(128);
ALTER TABLE sp_target_tag MODIFY name VARCHAR(128);


ALTER TABLE sp_target MODIFY controller_id VARCHAR(256);
