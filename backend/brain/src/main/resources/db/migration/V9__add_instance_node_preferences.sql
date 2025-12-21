ALTER TABLE instances
    ADD COLUMN requested_region VARCHAR(255) NULL,
    ADD COLUMN requested_tags TEXT NULL,
    ADD COLUMN dev_mode_allowed TINYINT(1) NULL;
