-- ailepro Phase 1 identity

CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL UNIQUE,
    email_verified_at TIMESTAMPTZ,
    password_hash VARCHAR(255),
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    locale VARCHAR(16) NOT NULL DEFAULT 'en',
    timezone VARCHAR(64) NOT NULL DEFAULT 'UTC',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE auth_identities (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    provider VARCHAR(64) NOT NULL,
    provider_subject VARCHAR(255) NOT NULL,
    UNIQUE (provider, provider_subject)
);

CREATE TABLE roles (
    code VARCHAR(64) PRIMARY KEY
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_code VARCHAR(64) NOT NULL REFERENCES roles (code),
    PRIMARY KEY (user_id, role_code)
);

CREATE TABLE permissions (
    code VARCHAR(128) PRIMARY KEY
);

CREATE TABLE role_permissions (
    role_code VARCHAR(64) NOT NULL REFERENCES roles (code),
    permission_code VARCHAR(128) NOT NULL REFERENCES permissions (code),
    PRIMARY KEY (role_code, permission_code)
);

CREATE TABLE email_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    purpose VARCHAR(32) NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    actor_id UUID REFERENCES users (id),
    action VARCHAR(128) NOT NULL,
    resource_type VARCHAR(64),
    resource_id VARCHAR(64),
    ip VARCHAR(64),
    meta JSONB,
    at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_logs_at ON audit_logs (at DESC);
CREATE INDEX idx_email_tokens_user ON email_tokens (user_id);

INSERT INTO roles (code) VALUES ('learner'), ('admin');
INSERT INTO permissions (code) VALUES ('admin.ping'), ('learner.read');
INSERT INTO role_permissions (role_code, permission_code) VALUES
    ('admin', 'admin.ping'),
    ('admin', 'learner.read'),
    ('learner', 'learner.read');
