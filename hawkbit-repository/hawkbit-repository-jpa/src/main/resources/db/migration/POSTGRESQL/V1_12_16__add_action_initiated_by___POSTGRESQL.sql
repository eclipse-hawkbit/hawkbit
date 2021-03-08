ALTER TABLE sp_action
ADD COLUMN initiated_by VARCHAR (64) NOT NULL DEFAULT '';
ALTER TABLE sp_action
ALTER COLUMN initiated_by DROP DEFAULT;

ALTER TABLE sp_target_filter_query
ADD COLUMN auto_assign_initiated_by VARCHAR (64);