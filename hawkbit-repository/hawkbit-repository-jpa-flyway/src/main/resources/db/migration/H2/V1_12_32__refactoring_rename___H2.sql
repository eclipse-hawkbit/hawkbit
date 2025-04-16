-- rename tables
alter table sp_base_software_module rename to sp_software_module;
alter table sp_distributionset_tag rename to sp_distribution_set_tag;
alter table sp_ds_dstag rename to sp_ds_tag;
alter table sp_rolloutgroup rename to sp_rollout_group;
alter table sp_rollouttargetgroup rename to sp_rollout_target_group;
alter table sp_sw_metadata rename to sp_sm_metadata;
-- rename columns
alter table sp_distribution_set rename column ds_id to ds_type;
alter table sp_software_module rename column module_type to sm_type;
-- rename fks
alter table sp_ds_tag rename constraint fk_ds_dstag_tag to fk_ds_ds_tag;
alter table sp_ds_tag rename constraint fk_ds_dstag_ds to fk_ds_tag_ds;
alter table sp_rollout_group rename constraint fk_rolloutgroup_rolloutgroup to fk_rollout_group_parent_id;
alter table sp_rollout_target_group rename constraint fk_rollouttargetgroup_rolloutgroup to fk_rollout_target_group_rollout_group_id;
alter table sp_rollout_target_group rename constraint fk_rollouttargetgroup_target to fk_rollout_target_group_target_id;
alter table sp_software_module rename constraint fk_module_type to fk_sm_type;
alter table sp_sm_metadata rename constraint pk_sp_sw_metadata to pk_sp_sm_metadata;
