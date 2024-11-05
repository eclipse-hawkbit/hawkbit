ALTER TABLE sp_target_filter_query
    ADD access_control_context VARCHAR(4096);
ALTER TABLE sp_rollout
    ADD access_control_context VARCHAR(4096);