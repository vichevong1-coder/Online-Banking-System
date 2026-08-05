CREATE TABLE users (
    id              UUID PRIMARY KEY,
    first_name      VARCHAR(255) NOT NULL,
    last_name       VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    email           VARCHAR(255) UNIQUE,
    nid_number      VARCHAR(100) NOT NULL,
    nid_expiry_date DATE NOT NULL,
    date_of_birth   DATE NOT NULL,
    gender          VARCHAR(20) NOT NULL,
    phone           VARCHAR(30) NOT NULL UNIQUE,
    role            VARCHAR(20) NOT NULL,
    status          VARCHAR(20) NOT NULL,
    phone_verified  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
