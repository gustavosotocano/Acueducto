CREATE TABLE app_user (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(64),
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role_code VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_app_user_email UNIQUE (email),
    CONSTRAINT fk_app_user_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
    CONSTRAINT chk_app_user_role CHECK (role_code IN ('ADMIN', 'PRESIDENT', 'TREASURY')),
    CONSTRAINT chk_app_user_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_app_user_tenant ON app_user (tenant_id);
CREATE INDEX idx_app_user_status ON app_user (status);

CREATE TABLE password_reset_token (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_password_reset_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_password_reset_user ON password_reset_token (user_id);
