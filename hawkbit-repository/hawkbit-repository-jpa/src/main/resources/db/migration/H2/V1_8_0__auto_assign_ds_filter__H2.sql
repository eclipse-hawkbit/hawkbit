ALTER TABLE sp_target_filter_query
  ADD column auto_assign_distribution_set BIGINT;

ALTER TABLE sp_target_filter_query
  ADD CONSTRAINT fk_filter_auto_assign_ds
FOREIGN KEY (auto_assign_distribution_set)
REFERENCES sp_distribution_set
ON DELETE SET NULL;
