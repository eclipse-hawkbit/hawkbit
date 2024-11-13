ALTER TABLE sp_action ADD initiated_by VARCHAR(64) NOT NULL CONSTRAINT DF_SpAction_InitiatedBy DEFAULT '';
ALTER TABLE sp_action DROP CONSTRAINT DF_SpAction_InitiatedBy;
ALTER TABLE sp_target_filter_query ADD auto_assign_initiated_by VARCHAR(64);
