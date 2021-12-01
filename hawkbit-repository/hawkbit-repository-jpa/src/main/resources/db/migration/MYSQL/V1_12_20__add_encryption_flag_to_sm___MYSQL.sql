ALTER TABLE sp_base_software_module ADD COLUMN encrypted BOOLEAN;

UPDATE sp_base_software_module SET encrypted = 0;