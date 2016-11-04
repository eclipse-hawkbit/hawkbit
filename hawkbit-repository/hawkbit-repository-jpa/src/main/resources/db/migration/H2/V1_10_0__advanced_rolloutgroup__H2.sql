ALTER TABLE sp_rolloutgroup
  ADD COLUMN target_percentage FLOAT;
ALTER TABLE sp_rolloutgroup
  ADD COLUMN target_filter VARCHAR (1024);
