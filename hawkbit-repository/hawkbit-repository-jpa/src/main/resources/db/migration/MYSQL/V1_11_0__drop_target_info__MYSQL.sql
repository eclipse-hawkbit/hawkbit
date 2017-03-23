ALTER TABLE sp_target ADD COLUMN install_date bigint;
ALTER TABLE sp_target ADD COLUMN address varchar(512);
ALTER TABLE sp_target ADD COLUMN last_target_query bigint;
ALTER TABLE sp_target ADD COLUMN request_controller_attributes bit not null;
ALTER TABLE sp_target ADD COLUMN update_status varchar(16) not null;
ALTER TABLE sp_target ADD COLUMN installed_distribution_set bigint;

UPDATE sp_target AS t INNER JOIN sp_target_info AS i
    ON t.id = i.target_id
SET t.install_date = i.install_date, t.address = i.address,t.last_target_query = i.last_target_query,
	t.request_controller_attributes = i.request_controller_attributes,t.update_status = i.update_status,
	t.installed_distribution_set = i.installed_distribution_set;

ALTER TABLE sp_target_info DROP INDEX sp_idx_target_info_02;

ALTER TABLE sp_target_attributes DROP FOREIGN KEY fk_targ_attrib_target;
ALTER TABLE sp_target_attributes 
        ADD CONSTRAINT fk_targ_attrib_target 
        FOREIGN KEY (target_id) 
        REFERENCES sp_target (id)
        ON DELETE cascade;

ALTER TABLE sp_target_info DROP FOREIGN KEY fk_target_inst_ds;     
ALTER TABLE sp_target 
        ADD CONSTRAINT fk_target_inst_ds 
        FOREIGN KEY (installed_distribution_set) 
        REFERENCES sp_distribution_set (id);

ALTER TABLE sp_target_info DROP FOREIGN KEY fk_targ_stat_targ;

DROP TABLE sp_target_info;