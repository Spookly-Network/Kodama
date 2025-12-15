CREATE TABLE instances (
    id BINARY(16) NOT NULL,
    name VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    state VARCHAR(32) NOT NULL,
    requested_by_user_id BINARY(16) NOT NULL,
    node_id BINARY(16) NULL,
    ports_json TEXT NULL,
    variables_json TEXT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    started_at DATETIME(6) NULL,
    stopped_at DATETIME(6) NULL,
    failure_reason TEXT NULL,
    CONSTRAINT pk_instances PRIMARY KEY (id),
    CONSTRAINT fk_instances_node FOREIGN KEY (node_id) REFERENCES nodes (id) ON DELETE SET NULL
) ENGINE=InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_instances_node_id ON instances (node_id);

CREATE TABLE instance_events (
    id BINARY(16) NOT NULL,
    instance_id BINARY(16) NOT NULL,
    timestamp DATETIME(6) NOT NULL,
    type VARCHAR(32) NOT NULL,
    payload_json TEXT NULL,
    CONSTRAINT pk_instance_events PRIMARY KEY (id),
    CONSTRAINT fk_instance_events_instance FOREIGN KEY (instance_id) REFERENCES instances (id) ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_instance_events_instance_id ON instance_events (instance_id);
CREATE INDEX idx_instance_events_instance_timestamp ON instance_events (instance_id, timestamp);
