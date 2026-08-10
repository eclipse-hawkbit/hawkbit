CREATE INDEX sp_idx_target_update_status ON sp_target (tenant, update_status);
CREATE INDEX sp_idx_target_attributes_key_value ON sp_target_attributes (attribute_key, attribute_value);