alter table sp_ds_module drop constraint fk_ds_module_ds;
alter table sp_ds_module 
        add constraint fk_ds_module_ds 
        foreign key (ds_id) 
        references sp_distribution_set (id)
        on delete cascade;

alter table sp_ds_module drop constraint fk_ds_module_module;
alter table sp_ds_module 
        add constraint fk_ds_module_module 
        foreign key (module_id) 
        references sp_base_software_module (id)
        on delete cascade;

alter table sp_external_artifact drop constraint fk_external_assigned_sm;
alter table sp_external_artifact 
        add constraint fk_external_assigned_sm 
        foreign key (software_module) 
        references sp_base_software_module (id)
        on delete cascade;

alter table sp_artifact drop constraint fk_assigned_sm;
alter table sp_artifact 
        add constraint fk_assigned_sm 
        foreign key (software_module) 
        references sp_base_software_module (id)
        on delete cascade;