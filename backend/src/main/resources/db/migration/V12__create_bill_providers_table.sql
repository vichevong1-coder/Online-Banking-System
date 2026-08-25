-- Bill providers, bill payments, and recurring bill payments (US-038, US-039, US-041, US-042)
CREATE TABLE bill_providers (
    id                     UUID PRIMARY KEY,
    name                   VARCHAR(100) NOT NULL UNIQUE,
    category               VARCHAR(50) NOT NULL,
    account_number_pattern VARCHAR(255),
    is_active              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE bill_payments (
    id                  UUID PRIMARY KEY,
    user_id             UUID NOT NULL REFERENCES users(id),
    provider_id         UUID NOT NULL REFERENCES bill_providers(id),
    account_id          UUID NOT NULL REFERENCES accounts(id),
    bill_account_number VARCHAR(100) NOT NULL,
    amount              NUMERIC(19, 4) NOT NULL,
    currency            VARCHAR(3) NOT NULL,
    transfer_id         UUID NOT NULL REFERENCES transfers(id),
    reference           VARCHAR(35) NOT NULL UNIQUE,
    status              VARCHAR(20) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE recurring_bill_payments (
    id                  UUID PRIMARY KEY,
    user_id             UUID NOT NULL REFERENCES users(id),
    provider_id         UUID NOT NULL REFERENCES bill_providers(id),
    account_id          UUID NOT NULL REFERENCES accounts(id),
    bill_account_number VARCHAR(100) NOT NULL,
    amount              NUMERIC(19, 4) NOT NULL,
    currency            VARCHAR(3) NOT NULL,
    frequency           VARCHAR(20) NOT NULL,
    next_payment_date   DATE NOT NULL,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_bill_payments_user_id ON bill_payments (user_id);
CREATE INDEX idx_bill_payments_provider_id ON bill_payments (provider_id);
CREATE INDEX idx_bill_payments_account_id ON bill_payments (account_id);
CREATE INDEX idx_recurring_bill_payments_user_id ON recurring_bill_payments (user_id);
