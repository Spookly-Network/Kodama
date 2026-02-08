CREATE TABLE instance_groups (
    id BINARY(16) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_instance_groups PRIMARY KEY (id),
    CONSTRAINT uq_instance_groups_name UNIQUE (name)
) ENGINE=InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE instance_group_memberships (
    instance_id BINARY(16) NOT NULL,
    group_id BINARY(16) NOT NULL,
    CONSTRAINT pk_instance_group_memberships PRIMARY KEY (instance_id, group_id),
    CONSTRAINT fk_instance_group_memberships_instance FOREIGN KEY (instance_id) REFERENCES instances (id) ON DELETE CASCADE,
    CONSTRAINT fk_instance_group_memberships_group FOREIGN KEY (group_id) REFERENCES instance_groups (id) ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_instance_group_memberships_instance_id ON instance_group_memberships (instance_id);
CREATE INDEX idx_instance_group_memberships_group_id ON instance_group_memberships (group_id);

CREATE TABLE instance_template_assignments (
    id BINARY(16) NOT NULL,
    instance_id BINARY(16) NOT NULL,
    template_id BINARY(16) NOT NULL,
    template_version_id BINARY(16) NULL,
    priority INT NOT NULL,
    CONSTRAINT pk_instance_template_assignments PRIMARY KEY (id),
    CONSTRAINT fk_instance_template_assignments_instance FOREIGN KEY (instance_id) REFERENCES instances (id) ON DELETE CASCADE,
    CONSTRAINT fk_instance_template_assignments_template FOREIGN KEY (template_id) REFERENCES templates (id),
    CONSTRAINT fk_instance_template_assignments_template_version FOREIGN KEY (template_version_id) REFERENCES template_versions (id)
) ENGINE=InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_instance_template_assignments_instance_id ON instance_template_assignments (instance_id);
CREATE INDEX idx_instance_template_assignments_template_id ON instance_template_assignments (template_id);
CREATE INDEX idx_instance_template_assignments_template_version_id ON instance_template_assignments (template_version_id);

CREATE TABLE group_template_assignments (
    id BINARY(16) NOT NULL,
    group_id BINARY(16) NOT NULL,
    template_id BINARY(16) NOT NULL,
    template_version_id BINARY(16) NULL,
    priority INT NOT NULL,
    CONSTRAINT pk_group_template_assignments PRIMARY KEY (id),
    CONSTRAINT fk_group_template_assignments_group FOREIGN KEY (group_id) REFERENCES instance_groups (id) ON DELETE CASCADE,
    CONSTRAINT fk_group_template_assignments_template FOREIGN KEY (template_id) REFERENCES templates (id),
    CONSTRAINT fk_group_template_assignments_template_version FOREIGN KEY (template_version_id) REFERENCES template_versions (id)
) ENGINE=InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_group_template_assignments_group_id ON group_template_assignments (group_id);
CREATE INDEX idx_group_template_assignments_template_id ON group_template_assignments (template_id);
CREATE INDEX idx_group_template_assignments_template_version_id ON group_template_assignments (template_version_id);

INSERT INTO instance_template_assignments (id, instance_id, template_id, template_version_id, priority)
SELECT UNHEX(REPLACE(UUID(), '-', '')),
       itl.instance_id,
       tv.template_id,
       itl.template_version_id,
       itl.order_index
FROM instance_template_layers itl
JOIN template_versions tv ON tv.id = itl.template_version_id;
