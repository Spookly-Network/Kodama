ALTER TABLE nodes
    MODIFY name VARCHAR(255) NOT NULL,
    MODIFY region VARCHAR(255) NOT NULL,
    MODIFY status VARCHAR(32) NOT NULL,
    MODIFY dev_mode TINYINT(1) NOT NULL DEFAULT 0,
    MODIFY capacity_slots INT NOT NULL,
    MODIFY used_slots INT NOT NULL DEFAULT 0,
    MODIFY last_heartbeat_at DATETIME(6) NOT NULL,
    MODIFY node_version VARCHAR(64) NOT NULL DEFAULT 'unknown',
    MODIFY tags TEXT NULL;

ALTER TABLE nodes
    ADD CONSTRAINT chk_nodes_status CHECK (status IN ('ONLINE', 'OFFLINE', 'UNKNOWN'));

CREATE INDEX idx_nodes_status ON nodes (status);
CREATE INDEX idx_nodes_region ON nodes (region);
