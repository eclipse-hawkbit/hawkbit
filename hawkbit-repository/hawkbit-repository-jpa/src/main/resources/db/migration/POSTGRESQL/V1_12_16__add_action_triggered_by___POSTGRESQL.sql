ALTER TABLE sp_action
ADD COLUMN triggered_by VARCHAR (64) NOT NULL;

ALTER TABLE sp_target_filter_query
ADD COLUMN auto_assign_triggered_by VARCHAR (64);
