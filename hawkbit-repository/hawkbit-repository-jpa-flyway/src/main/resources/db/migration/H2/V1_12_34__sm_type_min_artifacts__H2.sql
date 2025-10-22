ALTER TABLE sp_software_module_type ADD COLUMN min_artifacts integer default 0 NOT NULL;
DROP INDEX sp_idx_distribution_set_01;
CREATE INDEX sp_idx_distribution_set_01 ON sp_distribution_set (tenant, deleted);
ALTER TABLE sp_distribution_set DROP COLUMN complete;