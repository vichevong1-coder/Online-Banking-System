# Sprint 6 To-Do List — Mobile Bills & Cards, Audit, Hardening & Testing

Weeks 11–12. Details, acceptance criteria, and field lists live in [`sprint-plan.md`](./sprint-plan.md) —
this file just tracks what's built vs. not, as API endpoints and screens.

Legend: `[x]` done · `[~]` in progress · `[ ]` not started

> **This sprint is overloaded and you should plan to cut.** Eight mobile stories on top of six
> hardening stories, in the same two weeks as SIT, UAT and the final presentation, is not a realistic
> load — and it is the direct, predictable cost of putting mobile last. Decide at the **start** of
> Sprint 6 which mobile screens to drop if schedule slips.

---

## Before you start

- **Check the seed data.** `backend/src/main/resources/db/demo/seed_demo_data.sql` handles seeding customers, accounts, providers, and transfers. Make sure it isn't overwriting or conflicting with the new `audit_logs` table migrations.
- **The latest migration is `V14`.** Sprint 6 hardening and token revocation features will require migrations starting from `V15` (e.g., for `login_attempts`, OTP attempt limits, and `revoked_tokens`).
- **Tests.** Remember that tests run on Testcontainers. Hardening measures (like rate limiting) might break existing auth integration tests; expect to update test configurations as you implement US-058.

## Decisions already made — do not relitigate mid-implementation

- **US-058 Rate Limiting is a retrofit.** You are adding protection to endpoints already written without it (`POST /auth/login`, `POST /auth/otp/verify`, `/auth/2fa/verify`, etc.). Do not rebuild the endpoints; wrap them.
- **OTP Guesses & Token Replay (US-059).** The OTP has no attempt counter, and challenge tokens are replayable for their full 5-minute TTL. Fix both together: add an `attempts` column to `otp_codes`, put a `jti` in the challenge token, and invalidate the pair on success or at the 5-attempt limit.
- **Refresh Token Revocation.** Changing a password (US-036) *must* revoke existing refresh tokens. Implement a `revoked_tokens` table and `POST /auth/logout`.
- **Live SMS Swap (US-060) is just a swap.** You are replacing `LoggingSmsSender` with a live provider (like Twilio) behind the same `OtpSender` interface. This is an hour of work, not a sprint's worth.

## Audit & Web Admin

- [x] **US-055** Audit logs – capture all critical actions `[BE]`
  - *Completed via Agent:* `audit_logs` table (`V14`), `AuditLog` entity, and `AuditService` hooked into `AuthController` and `AdminCustomerController`.
- [x] **US-056** Audit logs – admin log viewer & filters `[React]` `[BE]`
  - *Completed via Agent:* `GET /admin/audit-logs` endpoint and the React `AuditLogsPage` with `DataTable` and `FilterBar` in `web-admin`.

## Security Hardening (Backend)

- [x] **US-057** Security hardening – encryption at rest & in transit `[BE]`
- [x] **US-058** Security hardening – rate limiting & brute-force protection `[BE]`
  - Requires limiting `POST /auth/login`, `/auth/otp/*`, `/auth/2fa/*`, `/auth/forgot-password`, `/auth/register`, `/auth/refresh`.
- [x] **US-059** Two-factor authentication – review & edge cases `[BE]`
  - [x] Add `attempts` column to `otp_codes` (fail at 5).
  - [x] Add `jti` to challenge token and store it; invalidate on success.
  - [x] Implement `revoked_tokens` table and check in `/auth/refresh`.
  - [x] Implement `POST /auth/logout`.
  - [x] Revoke tokens upon password change (US-036).
- [x] **US-060** Notifications – email / SMS provider integration `[BE]`
  - Swap `LoggingSmsSender` for a live provider.

## Mobile: Bills, Cards & Recovery (`mobile/`)

*(Note: The mobile bill-payment and card screens are the primary cut candidates if time runs short, as their backends still ship and demo via the admin portal).*

- [x] **US-036** Change password (customer) `[Flutter]`
- [x] **US-037** Forgot password / reset flow `[Flutter]`
- [x] **US-039** Bill payment – pay electricity / water / internet `[Flutter]`
- [x] **US-041** Bill payment – scheduled / recurring payments `[Flutter]`
- [x] **US-042** Bill payment – payment history `[Flutter]`
- [x] **US-043** Debit card management – request new card `[Flutter]`
- [x] **US-044** Debit card management – block / unblock card `[Flutter]`
- [x] **US-045** Debit card management – set PIN / spending limits `[Flutter]`

## Final Activities

- [ ] System Integration Testing (SIT)
- [ ] User Acceptance Testing (UAT)
- [ ] Final bug fixing and edge-case resolution
- [ ] Local Demo prep and final presentation walkthrough
