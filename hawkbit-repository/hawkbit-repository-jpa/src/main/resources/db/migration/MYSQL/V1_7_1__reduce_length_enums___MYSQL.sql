ALTER TABLE sp_target_info MODIFY update_status VARCHAR(16) not null;
ALTER TABLE sp_action MODIFY action_type VARCHAR(16) not null;
ALTER TABLE sp_rollout MODIFY action_type VARCHAR(16) not null;
ALTER TABLE sp_tenant_configuration MODIFY conf_key VARCHAR(128) not null;
ALTER TABLE sp_tenant_configuration MODIFY conf_value VARCHAR(512) not null;