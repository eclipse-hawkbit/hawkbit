ALTER TABLE sp_action MODIFY created_by VARCHAR(64), MODIFY last_modified_by VARCHAR(64);

ALTER TABLE sp_action_status MODIFY created_by VARCHAR(64), MODIFY last_modified_by VARCHAR(64);

ALTER TABLE sp_artifact MODIFY created_by VARCHAR(64), MODIFY last_modified_by VARCHAR(64);

ALTER TABLE sp_base_software_module MODIFY created_by VARCHAR(64), MODIFY last_modified_by VARCHAR(64);

ALTER TABLE sp_distributionset_tag MODIFY created_by VARCHAR(64), MODIFY last_modified_by VARCHAR(64);

ALTER TABLE sp_distribution_set MODIFY created_by VARCHAR(64), MODIFY last_modified_by VARCHAR(64);

ALTER TABLE sp_distribution_set_type MODIFY created_by VARCHAR(64), MODIFY last_modified_by VARCHAR(64);

ALTER TABLE sp_rollout MODIFY created_by VARCHAR(64), MODIFY last_modified_by VARCHAR(64), MODIFY approval_decided_by VARCHAR(64);

ALTER TABLE sp_rolloutgroup MODIFY created_by VARCHAR(64), MODIFY last_modified_by VARCHAR(64);

ALTER TABLE sp_software_module_type MODIFY created_by VARCHAR(64), MODIFY last_modified_by VARCHAR(64);

ALTER TABLE sp_target MODIFY created_by VARCHAR(64), MODIFY last_modified_by VARCHAR(64);

ALTER TABLE sp_target_filter_query MODIFY created_by VARCHAR(64), MODIFY last_modified_by VARCHAR(64);

ALTER TABLE sp_target_tag MODIFY created_by VARCHAR(64), MODIFY last_modified_by VARCHAR(64);

ALTER TABLE sp_tenant MODIFY created_by VARCHAR(64), MODIFY last_modified_by VARCHAR(64);

ALTER TABLE sp_tenant_configuration MODIFY created_by VARCHAR(64), MODIFY last_modified_by VARCHAR(64);