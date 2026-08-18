# Sprint 1 To-Do List — Authentication & Onboarding

Weeks 1–2. Details, acceptance criteria, and field lists live in [`sprint-plan.md`](./sprint-plan.md) —
this file just tracks what's built vs. not, as API endpoints and screens.

Legend: `[x]` done · `[~]` in progress · `[ ]` not started

---

## API endpoints (`backend/`)

- [x] `POST /auth/register` — customer registration (US-007)
- [x] `POST /auth/otp/verify` — phone OTP verification (US-008)
- [x] `POST /auth/otp/resend` — resend OTP (US-008)
- [x] `POST /auth/login` — login, customer + admin (US-009, US-010) — single endpoint; customer keyed by phone, admin keyed by email; always returns a challengeToken (2FA is mandatory for every role, not just admin)
- [x] `POST /auth/refresh` — token refresh (US-004)
- [x] `POST /auth/2fa/verify` — 2FA challenge, customer + admin (US-011, US-012)
- [x] `POST /auth/2fa/resend` — fresh OTP + challenge token for a login already in progress (US-011, US-012)

## Web pages (`web-admin/`)

- [x] Project setup & design system (US-002)
- [x] Login page (US-010) — email-based (admin is the only role with an email; phone is admin-only too, but just for SMS 2FA delivery, not sign-in); wired to `/auth/login`
- [x] 2FA challenge screen (US-012) — wired to `/auth/2fa/verify`, with countdown (decoded from the challenge token's JWT `exp`) + resend wired to `/auth/2fa/resend`
- [x] App shell — sidebar, header, protected routes — real session state (access token + user); survives a page refresh via sessionStorage (refresh token + user profile only, not the access token) and a silent `/auth/refresh` call on load
h
## Mobile pages (`mobile/`)

- [x] Project setup & design system (US-003)
- [x] Registration / KYC form (US-007) — wired to `/auth/register`; server-side field errors mapped onto individual form fields
- [x] OTP verification screen (US-008) — wired to `/auth/otp/verify` + `/auth/otp/resend` (no countdown here — no token to read an expiry from; resend is what actually matters)
- [x] Login screen (US-009) — wired to `/auth/login`; routes a `PHONE_NOT_VERIFIED` response straight to the OTP screen instead of a dead end
- [x] 2FA challenge screen (US-011) — wired to `/auth/2fa/verify` + `/auth/2fa/resend`, same countdown+resend pattern as web-admin's US-012 screen

Mobile verification note: `flutter analyze` and `flutter test` are clean, and the full register → otp/verify → login → 2fa/verify pipeline was walked end-to-end with curl against a live backend to confirm the exact request/response contract the Dart code targets. The screens themselves were not exercised in an emulator or on a device — no GUI available in this environment.

## Out of scope for Sprint 1

Change password, forgot password, admin role/status screens — see sprint-plan.md.
