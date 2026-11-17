CREATE TABLE vadmin_users (
    id CHAR(36) NOT NULL PRIMARY KEY,
    username VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL,
    auth_version BIGINT NOT NULL
);
CREATE TABLE vadmin_roles (
    id CHAR(36) NOT NULL PRIMARY KEY,
    code VARCHAR(120) NOT NULL UNIQUE
);
CREATE TABLE vadmin_permissions (
    id CHAR(36) NOT NULL PRIMARY KEY,
    code VARCHAR(120) NOT NULL UNIQUE,
    system_managed BOOLEAN NOT NULL
);
CREATE TABLE vadmin_user_roles (
    user_id CHAR(36) NOT NULL,
    role_id CHAR(36) NOT NULL,
    PRIMARY KEY (user_id, role_id)
);
CREATE TABLE vadmin_role_permissions (
    role_id CHAR(36) NOT NULL,
    permission_id CHAR(36) NOT NULL,
    PRIMARY KEY (role_id, permission_id)
);
CREATE TABLE vadmin_audit_entries (
    id CHAR(36) NOT NULL PRIMARY KEY,
    actor_user_id CHAR(36) NOT NULL,
    action_code VARCHAR(120) NOT NULL,
    target_type VARCHAR(120) NOT NULL,
    target_id VARCHAR(255) NOT NULL,
    outcome VARCHAR(16) NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    correlation_id VARCHAR(64),
    metadata VARCHAR(4000)
);
