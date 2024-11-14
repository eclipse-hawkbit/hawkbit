CREATE TABLE sp_target_metadata
(
	meta_key VARCHAR(128) NOT NULL,
	meta_value VARCHAR(4000) NULL,
	target_id NUMERIC(19) NOT NULL,
	PRIMARY KEY (meta_key, target_id)
);

ALTER TABLE sp_target_metadata ADD CONSTRAINT fk_metadata_target FOREIGN KEY (target_id) REFERENCES sp_target (id) ON DELETE CASCADE;
