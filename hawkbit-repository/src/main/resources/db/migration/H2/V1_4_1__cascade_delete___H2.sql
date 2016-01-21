alter table sp_target_attributes drop constraint fk_targ_attrib_target;
alter table sp_target_attributes 
        add constraint fk_targ_attrib_target 
        foreign key (target_id) 
        references sp_target_info
        on delete cascade;