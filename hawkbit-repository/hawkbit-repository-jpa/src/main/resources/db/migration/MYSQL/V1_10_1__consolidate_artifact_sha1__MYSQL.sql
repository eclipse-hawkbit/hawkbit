ALTER TABLE sp_artifact DROP COLUMN sha1_hash;
ALTER TABLE sp_artifact CHANGE gridfs_file_name sha1_hash varchar(40) not null;
CREATE INDEX sp_idx_artifact_02 ON sp_artifact (tenant, sha1_hash);