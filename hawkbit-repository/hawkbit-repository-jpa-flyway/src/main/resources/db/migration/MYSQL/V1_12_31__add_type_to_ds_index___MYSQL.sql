alter table sp_distribution_set drop constraint uk_distrib_set;
alter table sp_distribution_set add constraint uk_distrib_set unique (tenant, name, version, ds_id);