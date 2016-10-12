ALTER TABLE sp_target_filter_query
  ADD COLUMN auto_assign_distribution_set BIGINT;

ALTER TABLE sp_target_filter_query
  ADD CONSTRAINT fk_filter_auto_assign_ds
FOREIGN KEY (auto_assign_distribution_set)
REFERENCES sp_distribution_set (id)
ON DELETE SET NULL;
