CREATE TABLE templates (
    id BINARY(16) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(255) NOT NULL,
    type VARCHAR(32) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NOT NULL,
    CONSTRAINT pk_templates PRIMARY KEY (id),
    CONSTRAINT uq_templates_name UNIQUE (name)
) ENGINE=InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE template_versions (
    id BINARY(16) NOT NULL,
    template_id BINARY(16) NOT NULL,
    version VARCHAR(255) NOT NULL,
    checksum VARCHAR(255) NOT NULL,
    s3_key VARCHAR(255) NOT NULL,
    metadata_json TEXT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_template_versions PRIMARY KEY (id),
    CONSTRAINT uq_template_versions_template_version UNIQUE (template_id, version),
    CONSTRAINT fk_template_versions_template FOREIGN KEY (template_id) REFERENCES templates (id) ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_template_versions_template_id ON template_versions (template_id);
