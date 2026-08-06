CREATE TABLE accounts (
    id             UUID PRIMARY KEY,
    user_id        UUID NOT NULL REFERENCES users(id),
    account_number VARCHAR(20) NOT NULL UNIQUE,
    account_type   VARCHAR(20) NOT NULL,
    currency       VARCHAR(3) NOT NULL,
    balance        NUMERIC(19, 4) NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_accounts_user_id ON accounts (user_id);
