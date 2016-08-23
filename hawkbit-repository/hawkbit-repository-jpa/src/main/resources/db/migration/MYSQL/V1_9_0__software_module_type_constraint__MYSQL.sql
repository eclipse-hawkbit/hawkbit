alter table sp_software_module_type 
        add constraint maxAssignmentCheck check (maxAssignments > 0);