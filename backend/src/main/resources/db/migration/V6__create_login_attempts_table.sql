CREATE TABLE login_attempts (
    id              UUID PRIMARY KEY,
    identifier      VARCHAR(100) NOT NULL,
    success         BOOLEAN NOT NULL,
    ip_address      VARCHAR(45),
    user_agent      VARCHAR(255),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_login_attempts_created_at ON login_attempts (created_at DESC);
CREATE INDEX idx_login_attempts_identifier ON login_attempts (identifier);
