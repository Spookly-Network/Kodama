CREATE TABLE users (
    id BINARY(16) NOT NULL,
    username VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    auth_provider VARCHAR(64) NOT NULL,
    external_id VARCHAR(255) NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email UNIQUE (email)
) ENGINE=InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE roles (
    id BINARY(16) NOT NULL,
    name VARCHAR(32) NOT NULL,
    CONSTRAINT pk_roles PRIMARY KEY (id),
    CONSTRAINT uq_roles_name UNIQUE (name),
    CONSTRAINT chk_roles_name CHECK (name IN ('ADMIN', 'OPERATOR', 'VIEWER'))
) ENGINE=InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE user_roles (
    user_id BINARY(16) NOT NULL,
    role_id BINARY(16) NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id)
) ENGINE=InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_user_roles_role_id ON user_roles (role_id);
