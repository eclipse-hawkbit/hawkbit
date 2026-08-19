ALTER TABLE sp_target
    ADD COLUMN last_controller_attributes_update BIGINT,
    ADD COLUMN last_controller_attributes_update_requested BOOLEAN;
