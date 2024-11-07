 alter table sp_target_filter_query 
        add constraint uk_tenant_custom_filter_name  unique (name, tenant);
