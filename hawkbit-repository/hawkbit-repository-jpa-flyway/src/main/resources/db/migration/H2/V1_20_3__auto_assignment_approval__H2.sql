ALTER TABLE sp_target_filter_query ADD COLUMN auto_assign_status INTEGER;
ALTER TABLE sp_target_filter_query ADD COLUMN start_at BIGINT;
ALTER TABLE sp_target_filter_query ADD COLUMN approval_decided_by VARCHAR(64);
ALTER TABLE sp_target_filter_query ADD COLUMN approval_remark VARCHAR(255);
UPDATE sp_target_filter_query SET auto_assign_status = 4, start_at = last_modified_at WHERE auto_assign_distribution_set IS NOT NULL;
