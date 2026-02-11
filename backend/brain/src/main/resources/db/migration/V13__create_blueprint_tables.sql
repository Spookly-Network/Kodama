CREATE TABLE blueprints (
    id BINARY(16) NOT NULL,
    name VARCHAR(255) NOT NULL,
    permanent TINYINT(1) NOT NULL,
    slots_required INT NOT NULL,
    container_image VARCHAR(255) NOT NULL,
    install_script TEXT NULL,
    start_command_json TEXT NOT NULL,
    variables_json TEXT NULL,
    deleted_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_blueprints PRIMARY KEY (id),
    CONSTRAINT uq_blueprints_name UNIQUE (name)
) ENGINE=InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE blueprint_template_assignments (
    id BINARY(16) NOT NULL,
    blueprint_id BINARY(16) NOT NULL,
    template_id BINARY(16) NOT NULL,
    template_version_id BINARY(16) NULL,
    priority INT NOT NULL,
    CONSTRAINT pk_blueprint_template_assignments PRIMARY KEY (id),
    CONSTRAINT fk_blueprint_template_assignments_blueprint FOREIGN KEY (blueprint_id) REFERENCES blueprints (id) ON DELETE CASCADE,
    CONSTRAINT fk_blueprint_template_assignments_template FOREIGN KEY (template_id) REFERENCES templates (id),
    CONSTRAINT fk_blueprint_template_assignments_template_version FOREIGN KEY (template_version_id) REFERENCES template_versions (id)
) ENGINE=InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_blueprint_template_assignments_blueprint_id
    ON blueprint_template_assignments (blueprint_id);
CREATE INDEX idx_blueprint_template_assignments_template_id
    ON blueprint_template_assignments (template_id);
CREATE INDEX idx_blueprint_template_assignments_template_version_id
    ON blueprint_template_assignments (template_version_id);

CREATE TABLE blueprint_port_definitions (
    id BINARY(16) NOT NULL,
    blueprint_id BINARY(16) NOT NULL,
    name VARCHAR(255) NOT NULL,
    protocol VARCHAR(8) NOT NULL,
    container_port INT NOT NULL,
    host_range_min INT NOT NULL,
    host_range_max INT NOT NULL,
    host_range_step INT NOT NULL,
    CONSTRAINT pk_blueprint_port_definitions PRIMARY KEY (id),
    CONSTRAINT fk_blueprint_port_definitions_blueprint FOREIGN KEY (blueprint_id) REFERENCES blueprints (id) ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_blueprint_port_definitions_blueprint_id
    ON blueprint_port_definitions (blueprint_id);

CREATE TABLE blueprint_group_links (
    blueprint_id BINARY(16) NOT NULL,
    group_id BINARY(16) NOT NULL,
    CONSTRAINT pk_blueprint_group_links PRIMARY KEY (blueprint_id, group_id),
    CONSTRAINT fk_blueprint_group_links_blueprint FOREIGN KEY (blueprint_id) REFERENCES blueprints (id) ON DELETE CASCADE,
    CONSTRAINT fk_blueprint_group_links_group FOREIGN KEY (group_id) REFERENCES instance_groups (id) ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_blueprint_group_links_group_id
    ON blueprint_group_links (group_id);

ALTER TABLE instances
    ADD COLUMN blueprint_id BINARY(16) NULL,
    ADD COLUMN permanent_override TINYINT(1) NULL,
    ADD COLUMN slots_required_override INT NULL,
    ADD COLUMN container_image_override VARCHAR(255) NULL,
    ADD COLUMN install_script_override TEXT NULL,
    ADD COLUMN start_command_override_json TEXT NULL,
    ADD COLUMN variables_override_json TEXT NULL,
    ADD COLUMN port_definitions_override_json TEXT NULL,
    ADD CONSTRAINT fk_instances_blueprint FOREIGN KEY (blueprint_id) REFERENCES blueprints (id);

CREATE INDEX idx_instances_blueprint_id ON instances (blueprint_id);
