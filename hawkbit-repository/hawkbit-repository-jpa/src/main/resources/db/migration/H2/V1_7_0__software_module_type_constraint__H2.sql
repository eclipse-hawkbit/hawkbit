alter table sp_software_module_type 
        add constraint maxAssignmentCheck check (max_ds_assignments > 0);