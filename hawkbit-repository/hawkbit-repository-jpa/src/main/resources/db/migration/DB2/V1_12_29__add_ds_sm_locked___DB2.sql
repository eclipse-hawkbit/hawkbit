ALTER TABLE sp_base_software_module
    ADD COLUMN locked BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE sp_distribution_set
    ADD COLUMN locked BOOLEAN NOT NULL DEFAULT true;