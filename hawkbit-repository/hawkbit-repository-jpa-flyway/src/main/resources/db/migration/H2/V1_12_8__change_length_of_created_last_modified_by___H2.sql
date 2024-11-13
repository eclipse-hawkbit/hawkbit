ALTER TABLE sp_action ALTER COLUMN created_by VARCHAR(64);
ALTER TABLE sp_action ALTER COLUMN last_modified_by VARCHAR(64);

ALTER TABLE sp_action_status ALTER COLUMN created_by VARCHAR(64);
ALTER TABLE sp_action_status ALTER COLUMN last_modified_by VARCHAR(64);

ALTER TABLE sp_artifact ALTER COLUMN created_by VARCHAR(64);
ALTER TABLE sp_artifact ALTER COLUMN last_modified_by VARCHAR(64);

ALTER TABLE sp_base_software_module ALTER COLUMN created_by VARCHAR(64);
ALTER TABLE sp_base_software_module ALTER COLUMN last_modified_by VARCHAR(64);

ALTER TABLE sp_distributionset_tag ALTER COLUMN created_by VARCHAR(64);
ALTER TABLE sp_distributionset_tag ALTER COLUMN last_modified_by VARCHAR(64);

ALTER TABLE sp_distribution_set ALTER COLUMN created_by VARCHAR(64);
ALTER TABLE sp_distribution_set ALTER COLUMN last_modified_by VARCHAR(64);

ALTER TABLE sp_distribution_set_type ALTER COLUMN created_by VARCHAR(64);
ALTER TABLE sp_distribution_set_type ALTER COLUMN last_modified_by VARCHAR(64);

ALTER TABLE sp_rollout ALTER COLUMN created_by VARCHAR(64);
ALTER TABLE sp_rollout ALTER COLUMN last_modified_by VARCHAR(64);
ALTER TABLE sp_rollout ALTER COLUMN approval_decided_by VARCHAR(64);

ALTER TABLE sp_rolloutgroup ALTER COLUMN created_by VARCHAR(64);
ALTER TABLE sp_rolloutgroup ALTER COLUMN last_modified_by VARCHAR(64);

ALTER TABLE sp_software_module_type ALTER COLUMN created_by VARCHAR(64);
ALTER TABLE sp_software_module_type ALTER COLUMN last_modified_by VARCHAR(64);

ALTER TABLE sp_target ALTER COLUMN created_by VARCHAR(64);
ALTER TABLE sp_target ALTER COLUMN last_modified_by VARCHAR(64);

ALTER TABLE sp_target_filter_query ALTER COLUMN created_by VARCHAR(64);
ALTER TABLE sp_target_filter_query ALTER COLUMN last_modified_by VARCHAR(64);

ALTER TABLE sp_target_tag ALTER COLUMN created_by VARCHAR(64);
ALTER TABLE sp_target_tag ALTER COLUMN last_modified_by VARCHAR(64);

ALTER TABLE sp_tenant ALTER COLUMN created_by VARCHAR(64);
ALTER TABLE sp_tenant ALTER COLUMN last_modified_by VARCHAR(64);

ALTER TABLE sp_tenant_configuration ALTER COLUMN created_by VARCHAR(64);
ALTER TABLE sp_tenant_configuration ALTER COLUMN last_modified_by VARCHAR(64);