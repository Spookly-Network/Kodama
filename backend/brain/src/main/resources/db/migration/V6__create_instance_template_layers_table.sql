CREATE TABLE instance_template_layers (
    id BINARY(16) NOT NULL,
    instance_id BINARY(16) NOT NULL,
    template_version_id BINARY(16) NOT NULL,
    order_index INT NOT NULL,
    CONSTRAINT pk_instance_template_layers PRIMARY KEY (id),
    CONSTRAINT uq_instance_template_layers_instance_order UNIQUE (instance_id, order_index),
    CONSTRAINT fk_instance_template_layers_instance FOREIGN KEY (instance_id) REFERENCES instances (id) ON DELETE CASCADE,
    CONSTRAINT fk_instance_template_layers_template_version FOREIGN KEY (template_version_id) REFERENCES template_versions (id)
) ENGINE=InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_instance_template_layers_instance_id ON instance_template_layers (instance_id);
CREATE INDEX idx_instance_template_layers_template_version_id ON instance_template_layers (template_version_id);
