ALTER TABLE sp_tenant_configuration MODIFY conf_value TEXT NOT NULL;
ALTER TABLE sp_target_filter_query MODIFY access_control_context TEXT;
ALTER TABLE sp_rollout MODIFY access_control_context TEXT;
