ALTER TABLE sp_action_status_messages CHANGE COLUMN detail_message detail_message VARCHAR(512) NOT NULL;

ALTER TABLE sp_action CHANGE COLUMN created_at created_at BIGINT NOT NULL,
        CHANGE COLUMN created_by created_by VARCHAR(40) NOT NULL,
        CHANGE COLUMN last_modified_at last_modified_at BIGINT NOT NULL,
        CHANGE COLUMN last_modified_by last_modified_by VARCHAR(40) NOT NULL;
        
ALTER TABLE sp_action_status CHANGE COLUMN created_at created_at BIGINT NOT NULL,
        CHANGE COLUMN created_by created_by VARCHAR(40) NOT NULL,
        CHANGE COLUMN last_modified_at last_modified_at BIGINT NOT NULL,
        CHANGE COLUMN last_modified_by last_modified_by VARCHAR(40) NOT NULL;
        
ALTER TABLE sp_artifact CHANGE COLUMN created_at created_at BIGINT NOT NULL,
        CHANGE COLUMN created_by created_by VARCHAR(40) NOT NULL,
        CHANGE COLUMN last_modified_at last_modified_at BIGINT NOT NULL,
        CHANGE COLUMN last_modified_by last_modified_by VARCHAR(40) NOT NULL;
        
ALTER TABLE sp_distribution_set CHANGE COLUMN created_at created_at BIGINT NOT NULL,
        CHANGE COLUMN created_by created_by VARCHAR(40) NOT NULL,
        CHANGE COLUMN last_modified_at last_modified_at BIGINT NOT NULL,
        CHANGE COLUMN last_modified_by last_modified_by VARCHAR(40) NOT NULL;
        
ALTER TABLE sp_distributionset_tag CHANGE COLUMN created_at created_at BIGINT NOT NULL,
        CHANGE COLUMN created_by created_by VARCHAR(40) NOT NULL,
        CHANGE COLUMN last_modified_at last_modified_at BIGINT NOT NULL,
        CHANGE COLUMN last_modified_by last_modified_by VARCHAR(40) NOT NULL;
        
ALTER TABLE sp_distribution_set_type CHANGE COLUMN created_at created_at BIGINT NOT NULL,
        CHANGE COLUMN created_by created_by VARCHAR(40) NOT NULL,
        CHANGE COLUMN last_modified_at last_modified_at BIGINT NOT NULL,
        CHANGE COLUMN last_modified_by last_modified_by VARCHAR(40) NOT NULL;
        
ALTER TABLE sp_rollout CHANGE COLUMN created_at created_at BIGINT NOT NULL,
        CHANGE COLUMN created_by created_by VARCHAR(40) NOT NULL,
        CHANGE COLUMN last_modified_at last_modified_at BIGINT NOT NULL,
        CHANGE COLUMN last_modified_by last_modified_by VARCHAR(40) NOT NULL;
        
ALTER TABLE sp_rolloutgroup CHANGE COLUMN created_at created_at BIGINT NOT NULL,
        CHANGE COLUMN created_by created_by VARCHAR(40) NOT NULL,
        CHANGE COLUMN last_modified_at last_modified_at BIGINT NOT NULL,
        CHANGE COLUMN last_modified_by last_modified_by VARCHAR(40) NOT NULL;
        
ALTER TABLE sp_base_software_module CHANGE COLUMN created_at created_at BIGINT NOT NULL,
        CHANGE COLUMN created_by created_by VARCHAR(40) NOT NULL,
        CHANGE COLUMN last_modified_at last_modified_at BIGINT NOT NULL,
        CHANGE COLUMN last_modified_by last_modified_by VARCHAR(40) NOT NULL;
        
ALTER TABLE sp_software_module_type CHANGE COLUMN created_at created_at BIGINT NOT NULL,
        CHANGE COLUMN created_by created_by VARCHAR(40) NOT NULL,
        CHANGE COLUMN last_modified_at last_modified_at BIGINT NOT NULL,
        CHANGE COLUMN last_modified_by last_modified_by VARCHAR(40) NOT NULL;
        
ALTER TABLE sp_target CHANGE COLUMN created_at created_at BIGINT NOT NULL,
        CHANGE COLUMN created_by created_by VARCHAR(40) NOT NULL,
        CHANGE COLUMN last_modified_at last_modified_at BIGINT NOT NULL,
        CHANGE COLUMN last_modified_by last_modified_by VARCHAR(40) NOT NULL;
        
ALTER TABLE sp_target_filter_query CHANGE COLUMN created_at created_at BIGINT NOT NULL,
        CHANGE COLUMN created_by created_by VARCHAR(40) NOT NULL,
        CHANGE COLUMN last_modified_at last_modified_at BIGINT NOT NULL,
        CHANGE COLUMN last_modified_by last_modified_by VARCHAR(40) NOT NULL;
        
ALTER TABLE sp_target_tag CHANGE COLUMN created_at created_at BIGINT NOT NULL,
        CHANGE COLUMN created_by created_by VARCHAR(40) NOT NULL,
        CHANGE COLUMN last_modified_at last_modified_at BIGINT NOT NULL,
        CHANGE COLUMN last_modified_by last_modified_by VARCHAR(40) NOT NULL;
        
ALTER TABLE sp_tenant_configuration CHANGE COLUMN created_at created_at BIGINT NOT NULL,
        CHANGE COLUMN created_by created_by VARCHAR(40) NOT NULL,
        CHANGE COLUMN last_modified_at last_modified_at BIGINT NOT NULL,
        CHANGE COLUMN last_modified_by last_modified_by VARCHAR(40) NOT NULL;

ALTER TABLE sp_tenant CHANGE COLUMN created_at created_at BIGINT NOT NULL,
        CHANGE COLUMN created_by created_by VARCHAR(40) NOT NULL,
        CHANGE COLUMN last_modified_at last_modified_at BIGINT NOT NULL,
        CHANGE COLUMN last_modified_by last_modified_by VARCHAR(40) NOT NULL;