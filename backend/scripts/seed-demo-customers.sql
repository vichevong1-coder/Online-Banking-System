-- Seeds 5 login-ready CUSTOMER rows for local demo/testing.
--
-- Run against the local dev database:
--   docker exec -i obs-postgres psql -U obs -d obs < backend/scripts/seed-demo-customers.sql
--
-- All 5 share the password "Passw0rd1!" (hashes below are bcrypt of that string, same
-- BCryptPasswordEncoder the app uses). phone_verified is TRUE and status is ACTIVE, so each
-- can go through the normal POST /auth/login -> POST /auth/2fa/verify flow immediately —
-- the OTP code for 2FA still goes through the real flow (logged by LoggingSmsSender in dev).
--
-- Re-runnable: ON CONFLICT (phone) DO NOTHING skips rows that already exist.

INSERT INTO users (
    id, first_name, last_name, password_hash, nid_number, nid_expiry_date, date_of_birth,
    gender, phone, role, status, phone_verified
) VALUES
    (gen_random_uuid(), 'Sophal', 'Chan', '$2a$10$sxNapSvKDlcVk/3NMo4OQOe3g/mi1OAVJTsUf9i3hKWP8lQ4t2QIO',
     '100000001', '2032-01-01', '1990-05-14', 'MALE', '+855-12-000-001', 'CUSTOMER', 'ACTIVE', TRUE),
    (gen_random_uuid(), 'Sreymom', 'Pich', '$2a$10$gd3.6ltHdtD77XyqaGH4A.2EkDnuklIB1Js44rLutFNO7HRp/33zu',
     '100000002', '2032-01-01', '1995-11-02', 'FEMALE', '+855-12-000-002', 'CUSTOMER', 'ACTIVE', TRUE),
    (gen_random_uuid(), 'Dara', 'Long', '$2a$10$7yfPeWt1ZsuuDiqSPJyGVe.mpNxLmkzVfRPhT3qx5PG/zCTm4.AX.',
     '100000003', '2032-01-01', '1988-03-21', 'MALE', '+855-12-000-003', 'CUSTOMER', 'ACTIVE', TRUE),
    (gen_random_uuid(), 'Kanya', 'Sok', '$2a$10$dRvc3k4J7BFs5UBl6jGb/.1F.Yd2ki4ZS1TNRqpSmmtLMr2XGuRP6',
     '100000004', '2032-01-01', '1992-07-09', 'FEMALE', '+855-12-000-004', 'CUSTOMER', 'ACTIVE', TRUE),
    (gen_random_uuid(), 'Vibol', 'Meas', '$2a$10$/rjKYLcC8vLGhuJlO6AwouO3eEVtU3PbvDKPt1lMTQWX5syeg/KS6',
     '100000005', '2032-01-01', '1985-01-30', 'MALE', '+855-12-000-005', 'CUSTOMER', 'ACTIVE', TRUE)
ON CONFLICT (phone) DO NOTHING;
