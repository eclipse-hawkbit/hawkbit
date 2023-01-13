ALTER TABLE sp_distribution_set ADD COLUMN valid BOOLEAN;

UPDATE sp_distribution_set SET valid = TRUE;