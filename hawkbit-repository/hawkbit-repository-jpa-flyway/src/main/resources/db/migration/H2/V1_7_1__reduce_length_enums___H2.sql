ALTER TABLE sp_target_info ALTER update_status VARCHAR(16) not null;
ALTER TABLE sp_action ALTER action_type VARCHAR(16) not null;
ALTER TABLE sp_rollout ALTER action_type VARCHAR(16) not null;
ALTER TABLE sp_tenant_configuration ALTER conf_key VARCHAR(128) not null;
ALTER TABLE sp_tenant_configuration ALTER conf_value VARCHAR(512) not null;
