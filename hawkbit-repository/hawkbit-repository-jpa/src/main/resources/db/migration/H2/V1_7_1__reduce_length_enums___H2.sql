ALTER TABLE sp_target_info ALTER COLUMN update_status VARCHAR(16) not null;
ALTER TABLE sp_action ALTER COLUMN action_type VARCHAR(16) not null;
ALTER TABLE sp_rollout ALTER COLUMN action_type VARCHAR(16) not null;
ALTER TABLE sp_tenant_configuration ALTER COLUMN conf_key VARCHAR(128) not null;
ALTER TABLE sp_tenant_configuration ALTER COLUMN conf_value VARCHAR(512) not null;