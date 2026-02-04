-- remove unnecessary / faulty default for a tenant's scope unique key
ALTER TABLE sp_target_type ALTER COLUMN type_key DROP DEFAULT;

-- remove unused column
ALTER TABLE sp_rollout DROP COLUMN group_theshold;