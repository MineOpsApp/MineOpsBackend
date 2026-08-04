-- A worker logging shift production can name the co-workers who mined the same total
-- alongside them, so the pay cycle split credits everyone in the group, not just whoever
-- happened to submit the log. volume_extracted on the shift log itself stays a single,
-- un-duplicated figure — group membership only affects who shares in the resulting pay
-- cycle split, not how much total volume/revenue that cycle counts.
CREATE TABLE shift_log_group_members (
    id BIGSERIAL PRIMARY KEY,
    shift_log_id BIGINT NOT NULL REFERENCES shift_logs(id),
    worker_email VARCHAR(255) NOT NULL,
    worker_name VARCHAR(255) NOT NULL
);

CREATE INDEX idx_shift_log_group_members_shift_log_id ON shift_log_group_members(shift_log_id);
