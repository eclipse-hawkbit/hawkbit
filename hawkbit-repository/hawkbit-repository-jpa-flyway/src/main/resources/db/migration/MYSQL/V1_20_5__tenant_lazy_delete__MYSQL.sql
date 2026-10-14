ALTER TABLE sp_tenant ADD COLUMN deleted_at BIGINT;
ALTER TABLE sp_tenant ADD COLUMN deleted_by VARCHAR(64);
ALTER TABLE sp_tenant ADD COLUMN hard_delete_at BIGINT;
