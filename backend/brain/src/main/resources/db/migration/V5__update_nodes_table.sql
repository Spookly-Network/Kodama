ALTER TABLE nodes
    RENAME COLUMN last_seen_at TO last_heartbeat_at;

ALTER TABLE nodes
    RENAME COLUMN capacity TO capacity_slots;

ALTER TABLE nodes
    ADD COLUMN dev_mode TINYINT(1) NOT NULL DEFAULT 0 AFTER status,
    ADD COLUMN used_slots INT NOT NULL DEFAULT 0 AFTER capacity_slots,
    ADD COLUMN node_version VARCHAR(64) NOT NULL DEFAULT 'unknown' AFTER last_heartbeat_at,
    ADD COLUMN tags TEXT NULL AFTER node_version,
    ADD COLUMN base_url VARCHAR(512) NULL AFTER tags;
