DROP INDEX sp_idx_target_info_01;

ALTER TABLE sp_target_info ALTER COLUMN ip_address RENAME TO address;
ALTER TABLE sp_target_info ALTER COLUMN address VARCHAR(512);

UPDATE sp_target_info
SET address = CONCAT('http://',(SELECT address 
                        FROM  sp_target_info i
                        WHERE sp_target_info.target_id = i.target_id))
WHERE EXISTS(SELECT target_id
                        FROM  sp_target_info i
                        WHERE sp_target_info.target_id = i.target_id)
AND address != null;