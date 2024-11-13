alter table sp_target_info drop FOREIGN KEY fk_targ_stat_targ;
alter table sp_target_info 
        add constraint fk_targ_stat_targ 
        foreign key (target_id) 
        references sp_target (id)
        on delete cascade;

alter table sp_action drop FOREIGN KEY fk_targ_act_hist_targ;
alter table sp_action drop index fk_targ_act_hist_targ;
alter table sp_action 
        add constraint fk_targ_act_hist_targ 
        foreign key (target) 
        references sp_target (id)
        on delete cascade;

alter table sp_action_status drop FOREIGN KEY fk_act_stat_action;
alter table sp_action_status drop index fk_act_stat_action;      
alter table sp_action_status 
        add constraint fk_act_stat_action 
        foreign key (action) 
        references sp_action (id)
        on delete cascade;

alter table sp_sw_metadata drop FOREIGN KEY fk_metadata_sw;
alter table sp_sw_metadata drop index fk_metadata_sw;
alter table sp_sw_metadata 
        add constraint fk_metadata_sw 
        foreign key (sw_id) 
        references sp_base_software_module (id)
        on delete cascade;

alter table sp_ds_metadata drop FOREIGN KEY fk_metadata_ds;       
alter table sp_ds_metadata 
        add constraint fk_metadata_ds 
        foreign key (ds_id) 
        references sp_distribution_set (id)
        on delete cascade;

alter table sp_action_status_messages drop FOREIGN KEY fk_stat_msg_act_stat;  
alter table sp_action_status_messages 
        add constraint fk_stat_msg_act_stat 
        foreign key (action_status_id) 
        references sp_action_status (id)
        on delete cascade;

alter table sp_ds_dstag drop FOREIGN KEY fk_ds_dstag_tag;
alter table sp_ds_dstag drop index fk_ds_dstag_tag;
alter table sp_ds_dstag 
        add constraint fk_ds_dstag_tag 
        foreign key (TAG) 
        references sp_distributionset_tag (id)
        on delete cascade;

alter table sp_ds_dstag drop FOREIGN KEY fk_ds_dstag_ds;
alter table sp_ds_dstag 
        add constraint fk_ds_dstag_ds 
        foreign key (ds) 
        references sp_distribution_set (id)
        on delete cascade;

alter table sp_target_target_tag drop FOREIGN KEY fk_targ_targtag_tag;
alter table sp_target_target_tag 
        add constraint fk_targ_targtag_tag 
        foreign key (tag) 
        references sp_target_tag (id)
        on delete cascade;

alter table sp_target_target_tag drop FOREIGN KEY fk_targ_targtag_target;
alter table sp_target_target_tag 
        add constraint fk_targ_targtag_target 
        foreign key (target) 
        references sp_target (id)
        on delete cascade;