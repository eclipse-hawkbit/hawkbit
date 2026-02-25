ALTER TABLE sp_tenant_configuration ALTER COLUMN conf_value TYPE TEXT;
ALTER TABLE sp_target_filter_query ALTER COLUMN access_control_context TYPE TEXT;
ALTER TABLE sp_rollout ALTER COLUMN access_control_context TYPE TEXT;
