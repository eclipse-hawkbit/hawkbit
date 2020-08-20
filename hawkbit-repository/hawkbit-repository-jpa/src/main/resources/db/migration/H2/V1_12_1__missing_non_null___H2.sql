ALTER TABLE sp_action_status_messages ALTER COLUMN detail_message varchar(512) not null;
ALTER TABLE sp_action ALTER COLUMN distribution_set bigint not null;
ALTER TABLE sp_action ALTER COLUMN target bigint not null;
ALTER TABLE sp_action ALTER COLUMN status integer not null;
ALTER TABLE sp_action_status ALTER COLUMN target_occurred_at bigint not null;
ALTER TABLE sp_action_status ALTER COLUMN status integer not null;
ALTER TABLE sp_rollout ALTER COLUMN distribution_set bigint not null;
ALTER TABLE sp_rollout ALTER COLUMN status integer not null;
ALTER TABLE sp_rolloutgroup ALTER COLUMN rollout bigint not null;
ALTER TABLE sp_rolloutgroup ALTER COLUMN status integer not null;
ALTER TABLE sp_artifact ALTER COLUMN sha1_hash varchar(40) not null;
ALTER TABLE sp_target ALTER COLUMN controller_id varchar(64) not null;