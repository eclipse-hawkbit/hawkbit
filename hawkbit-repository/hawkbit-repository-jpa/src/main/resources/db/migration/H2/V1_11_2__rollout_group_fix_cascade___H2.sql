alter table sp_rolloutgroup drop constraint fk_rolloutgroup_rolloutgroup;
alter table sp_rolloutgroup 
        add constraint fk_rolloutgroup_rolloutgroup 
        foreign key (parent_id) 
        references sp_rolloutgroup (id);
