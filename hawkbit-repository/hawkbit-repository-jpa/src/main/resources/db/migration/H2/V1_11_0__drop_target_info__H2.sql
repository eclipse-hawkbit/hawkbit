ALTER TABLE sp_target ADD COLUMN install_date bigint;
ALTER TABLE sp_target ADD COLUMN address varchar(512);
ALTER TABLE sp_target ADD COLUMN last_target_query bigint;
ALTER TABLE sp_target ADD COLUMN request_controller_attributes bit not null;
ALTER TABLE sp_target ADD COLUMN update_status varchar(16) not null;
ALTER TABLE sp_target ADD COLUMN installed_distribution_set bigint;
	
UPDATE sp_target t SET install_date=(SELECT i.install_date FROM sp_target_info i WHERE t.id = i.target_id);
UPDATE sp_target t SET address=(SELECT i.address FROM sp_target_info i WHERE t.id = i.target_id);
UPDATE sp_target t SET last_target_query=(SELECT i.last_target_query FROM sp_target_info i WHERE t.id = i.target_id);
UPDATE sp_target t SET request_controller_attributes=(SELECT i.request_controller_attributes FROM sp_target_info i WHERE t.id = i.target_id);
UPDATE sp_target t SET update_status=(SELECT i.update_status FROM sp_target_info i WHERE t.id = i.target_id);
UPDATE sp_target t SET installed_distribution_set=(SELECT i.installed_distribution_set FROM sp_target_info i WHERE t.id = i.target_id);

ALTER TABLE sp_target_attributes DROP CONSTRAINT fk_targ_attrib_target;
ALTER TABLE sp_target_attributes 
        ADD CONSTRAINT fk_targ_attrib_target 
        FOREIGN KEY (target_id) 
        REFERENCES sp_target (id)
        ON DELETE cascade;

ALTER TABLE sp_target_info DROP CONSTRAINT fk_target_inst_ds;     
ALTER TABLE sp_target 
        ADD CONSTRAINT fk_target_inst_ds 
        FOREIGN KEY (installed_distribution_set) 
        REFERENCES sp_distribution_set (id);

ALTER TABLE sp_target_info DROP CONSTRAINT fk_targ_stat_targ;
ALTER TABLE sp_target_info DROP INDEX sp_idx_target_info_02;

DROP TABLE sp_target_info;