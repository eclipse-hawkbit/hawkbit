ALTER TABLE sp_tenant_configuration ALTER COLUMN conf_value CLOB NOT NULL;
ALTER TABLE sp_target_filter_query ALTER COLUMN access_control_context CLOB;
ALTER TABLE sp_rollout ALTER COLUMN access_control_context CLOB;
