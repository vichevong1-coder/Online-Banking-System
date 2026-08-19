-- Payees a customer pays by scanning a merchant QR code (US-034).
--
-- Merchants are not users: they never log in, and giving them user rows would
-- drag KYC, passwords and the whole auth surface into what the demo needs to be
-- a payee (qr-payments-spec.md).
--
-- settlement_account_id is an ordinary accounts row, so a merchant payment is an
-- ordinary internal transfer with a merchant on one end — no second ledger and
-- no special-casing in the balance code.
CREATE TABLE merchants (
    id                    UUID PRIMARY KEY,
    merchant_code         VARCHAR(32) NOT NULL UNIQUE,
    display_name          VARCHAR(100) NOT NULL,
    settlement_account_id UUID NOT NULL REFERENCES accounts(id),

    -- ACTIVE | ALWAYS_DECLINES. The failure path is data rather than a hardcoded
    -- merchant code in the service, so a reviewer can add a second declining
    -- merchant without touching Java.
    status                VARCHAR(20) NOT NULL,

    created_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- The only read path is "resolve the code scanned off a QR" (US-033). The UNIQUE
-- constraint above already indexes merchant_code, so nothing more is needed.

-- The three demo merchants live in db/demo/seed_demo_data.sql, not here: that
-- script stays outside spring.flyway.locations so demo rows never load in tests
-- or CI.
