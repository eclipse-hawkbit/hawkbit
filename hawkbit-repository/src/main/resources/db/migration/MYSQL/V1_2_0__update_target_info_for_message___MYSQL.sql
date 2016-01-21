DROP INDEX sp_idx_target_info_01 ON sp_target_info;

ALTER TABLE sp_target_info
CHANGE ip_address address VARCHAR(512);

UPDATE sp_target_info t1, sp_target_info t2
SET t1.address = CONCAT('http://',t2.address)
WHERE t1.target_id = t2.target_id AND t2.address is not null