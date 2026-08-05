-- KYC columns only apply to customers (US-007). Admin/staff rows have no ID
-- document to satisfy them, so they must be nullable rather than filled with
-- placeholder data.
ALTER TABLE users
    ALTER COLUMN nid_number DROP NOT NULL,
    ALTER COLUMN nid_expiry_date DROP NOT NULL,
    ALTER COLUMN date_of_birth DROP NOT NULL,
    ALTER COLUMN gender DROP NOT NULL;

-- Bootstrap admin account so staff/admin login (US-010) has something to log
-- into before the real admin-provisions-staff flow (US-047, Sprint 5) exists.
-- Admin logs in with email (US-010); phone is still required so the same
-- SMS-based 2FA (US-012) that customers use has somewhere to send the code.
-- Password hash, email and phone come from Flyway placeholders resolved
-- from env vars (see application.properties) — never hardcoded here.
INSERT INTO users (id, first_name, last_name, password_hash, email, phone, role, status, phone_verified)
VALUES (gen_random_uuid(), 'System', 'Admin', '${adminPasswordHash}', '${adminEmail}', '${adminPhone}', 'ADMIN', 'ACTIVE', TRUE);
