ALTER TABLE sp_rollout ADD COLUMN deleted BOOLEAN;

UPDATE sp_rollout SET deleted = 0;

ALTER TABLE sp_action MODIFY target BIGINT NOT NULL;
ALTER TABLE sp_action MODIFY distribution_set BIGINT NOT NULL;
ALTER TABLE sp_action MODIFY status INTEGER NOT NULL;
ALTER TABLE sp_action_status MODIFY status INTEGER NOT NULL;
ALTER TABLE sp_rollout MODIFY status INTEGER NOT NULL;
ALTER TABLE sp_rollout MODIFY distribution_set BIGINT NOT NULL;
ALTER TABLE sp_rolloutgroup MODIFY rollout BIGINT NOT NULL;
ALTER TABLE sp_rolloutgroup MODIFY status INTEGER NOT NULL;

ALTER TABLE sp_ds_type_element DROP CONSTRAINT fk_ds_type_element_element;   
ALTER TABLE sp_ds_type_element 
        ADD CONSTRAINT fk_ds_type_element_element 
        FOREIGN KEY (distribution_set_type) 
        REFERENCES sp_distribution_set_type (id)
        ON DELETE CASCADE;

ALTER TABLE sp_ds_type_element DROP CONSTRAINT fk_ds_type_element_smtype;   
ALTER TABLE sp_ds_type_element 
        ADD CONSTRAINT fk_ds_type_element_smtype 
        FOREIGN KEY (software_module_type) 
        REFERENCES sp_software_module_type (id)
        ON DELETE CASCADE;
