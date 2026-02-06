CREATE TABLE nodes (
    id BINARY(16) NOT NULL,
    name VARCHAR(255) NOT NULL,
    region VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    last_seen_at DATETIME(6) NOT NULL,
    capacity INT NOT NULL,
    CONSTRAINT pk_nodes PRIMARY KEY (id),
    CONSTRAINT uq_nodes_name UNIQUE (name)
) ENGINE=InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
