ALTER TABLE sp_action ADD COLUMN action_type_new integer not null;
UPDATE sp_action SET action_type_new =
CASE  WHEN (action_type = 'SOFT') THEN 1
      WHEN (action_type = 'TIMEFORCED') THEN 2
      ELSE 0 END;
ALTER TABLE sp_action DROP COLUMN action_type;
ALTER TABLE sp_action CHANGE COLUMN action_type_new action_type integer;

ALTER TABLE sp_rollout ADD COLUMN action_type_new integer not null;
UPDATE sp_rollout SET action_type_new =
CASE  WHEN (action_type = 'SOFT') THEN 1
      WHEN (action_type = 'TIMEFORCED') THEN 2
      ELSE 0 END;
ALTER TABLE sp_rollout DROP COLUMN action_type;
ALTER TABLE sp_rollout CHANGE COLUMN action_type_new action_type integer;

ALTER TABLE sp_target ADD COLUMN update_status_new integer not null;
UPDATE sp_target SET update_status_new =
CASE  WHEN (update_status = 'IN_SYNC') THEN 1
      WHEN (update_status = 'PENDING') THEN 2
      WHEN (update_status = 'ERROR') THEN 3
      WHEN (update_status = 'REGISTERED') THEN 4
      ELSE 0 END;     
ALTER TABLE sp_target DROP COLUMN update_status;
ALTER TABLE sp_target CHANGE COLUMN update_status_new update_status integer;