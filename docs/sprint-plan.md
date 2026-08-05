# Online Banking System — Agile Sprint Plan

Scoped for Solo Local Demo
6 Sprints / 12 Weeks | Local demo only, no production deployment

Tech stack: **Java (Spring Boot)** backend API | **React** staff / admin web portal | **Flutter** customer mobile app

Each user story is tagged with the layer(s) it touches: `[BE]` Java backend, `[React]` React admin portal, `[Flutter]` Flutter customer app.

> **Rev 2** — rebalanced after the review in [`sprint-plan-review.md`](./sprint-plan-review.md), which
> records the six decisions behind this version and the reasoning for each. Read it before changing
> anything structural here.

## Scope

The original 20-feature scope was cut down to what one person can build in 12 weeks as a local demo.

**Removed:** Loan application (form + doc upload + eligibility calculator + approval workflow), ATM locator (real map SDK), Chat / ticketing support, Reports with CSV / PDF / Excel export, Fixed deposit interest engine with maturity / auto-renewal, admin approval workflows for loans and account freeze, **transaction fraud / risk check (US-040)**, and **admin broadcast announcements (US-051)**.

This plan covers the original feature set minus the removals listed above, across **58 user stories**.

> The previous revision claimed "15 functionalities," carried over from before US-040 and US-051 were
> cut. That count needs re-deriving against the original 20-feature list (which isn't in this repo)
> before it goes back in — see [`sprint-plan-review.md`](./sprint-plan-review.md).

## Sprint Status

| Sprint | Weeks | Theme | Stories | Status |
|---|---|---|---|---|
| 1 | 1–2 | Authentication & Onboarding | 12 | In progress |
| 2 | 3–4 | Accounts & Balances | 12 | Not started |
| 3 | 5–6 | Transfers & Payments | 11 | Not started |
| 4 | 7–8 | Bill Payment, Cards & Account Recovery | 9 | Not started |
| 5 | 9–10 | Admin Portal | 8 | Not started |
| 6 | 11–12 | Audit, Security Hardening & Testing | 6 | Not started |

## Two structural rules this plan follows

1. **Backend halves ship where the data is needed; React halves ship in Sprint 5.** Three stories were split on this rule — US-005, US-006, and US-038 — because each was really two stories wearing one ID. The `a` half is backend and lands early; the `b` half is a screen and lands with the rest of the portal.
2. **The scattered dashboard stories collapse into one build pass.** US-049, US-050, US-053, US-054 and US-051 were previously spread across Sprints 2, 3 and 5 with no shared spec — the portal would have been assembled in disconnected slices. They are now built together in Sprint 5, against APIs that already exist and are tested.

   Two pieces of React work necessarily sit outside that pass and always will: the **auth shell** (US-002, US-010, US-012) has to come first, because you cannot build screens behind a login that does not exist yet; and the **audit log viewer** (US-056) has to come after US-055 captures the logs. That is dependency, not scatter. See the screen inventory in Sprint 5 for which screen is built when.

---

## I. Sprint 1 (Week 1–2)

### Goal
Stand up the project foundation across all three layers and deliver secure registration, login, and two-factor authentication.

### User Stories — in dependency order

Build them in this sequence. Token handling and the role/status model come *before* the login stories that depend on them.

- **US-001** Backend project setup (Spring Boot, DB schema, migrations, `docker-compose.yml`, CI) `[BE]`
- **US-002** React admin portal project setup & design system `[React]`
- **US-003** Flutter mobile app project setup & design system `[Flutter]`
- **US-004** JWT / session token handling `[BE]`
- **US-005** Role model & JWT role claims `[BE]`
- **US-006** Account status model & login-path enforcement (active / suspended / locked) `[BE]`
- **US-007** Customer registration – KYC form & manual ID entry `[Flutter]` `[BE]`
- **US-008** Phone verification (OTP via SMS) `[BE]` `[Flutter]`
- **US-009** Customer login (mobile) `[Flutter]` `[BE]`
- **US-010** Staff / admin login (web) `[React]` `[BE]`
- **US-011** Two-factor authentication – OTP via SMS `[BE]` `[Flutter]`
- **US-012** Two-factor authentication enforcement for admin accounts `[BE]` `[React]`

### US-001 acceptance criteria — note

Includes a `docker-compose.yml` with **two** services: `postgres:17-alpine` (matching `backend-ci.yml`) and `axllent/mailpit`. Migration tooling (Flyway or Liquibase) is named here, not improvised later. Mailpit stays in the stack for US-020's statement email in Sprint 2 even though OTP no longer uses email — see below.

### US-007 acceptance criteria — note

The KYC form collects exactly: **first name, last name, Cambodian NID number, NID expiry date, date of birth, gender**, plus the **phone number** used for OTP delivery (US-008). **No email field.** Customers are not identified or contacted by email anywhere in the registration or verification flow.

### US-010 acceptance criteria — note

Admin/staff accounts log in with **email**, not phone. This is the one place email exists in the system: the `users` table's `email` column is populated only for `ADMIN` rows and stays `NULL` for customers, who never have one (US-007). Admin rows still carry a `phone` too, but only as the destination for SMS-based 2FA (US-012, same `OtpSender` channel as everyone else) — it plays no part in sign-in.

2FA is mandatory for every account, not just admin — `POST /auth/login` never returns real tokens directly; it always returns a short-lived challenge token that must be redeemed at `/auth/2fa/verify` (US-011 for customers, US-012 for admin). There is no per-user toggle to skip it. This settles the deliverable line below in favor of "enforced" for both roles, not just admin.

There is no self-registration for staff/admin accounts, and the real admin-provisions-staff flow is US-047 in Sprint 5 — so Sprint 1 needs a way for an admin account to exist before that flow is built. It's seeded: a Flyway migration (`V2__seed_bootstrap_admin.sql`) inserts one `ADMIN` row with credentials sourced from `ADMIN_BOOTSTRAP_PASSWORD_HASH` / `ADMIN_BOOTSTRAP_EMAIL` / `ADMIN_BOOTSTRAP_PHONE` env vars (throwaway local-demo defaults documented in `backend/.env.example`, same convention as `JWT_SECRET`), never hardcoded in the migration. Because the KYC columns (`nid_number`, `nid_expiry_date`, `date_of_birth`, `gender`) only apply to customers, that migration also relaxes them to nullable so the seeded admin row doesn't need placeholder ID data.

### OTP & SMS delivery

Registration and login OTPs go out over **SMS to the phone number on file** — email is never the delivery channel, for either customers or admin (see US-010's note: admin's email is a sign-in identifier, not an OTP destination). There is no real SMS provider wired up yet for local dev, so delivery is a **logging stub**: the code is written to the backend log (and can be surfaced in a dev-only response/console for demoing) rather than sent to a real handset.

```java
public interface OtpSender { void send(String phoneNumber, String code); }

@Component @Profile("dev") class LoggingSmsSender implements OtpSender { /* phone: log only, no real provider yet */ }
```

US-060 in Sprint 6 swaps `LoggingSmsSender` for a live SMS provider (e.g. Twilio) behind the same `OtpSender` interface — same swap shape as previously planned, just SMS instead of email. Email/SMTP + Mailpit is retained solely for US-020 (statement email), which is unrelated to OTP.

### Deliverable
Authentication & Onboarding Module — register → OTP verify → customer login → 2FA → admin login with enforced 2FA.

### Deliberately *not* in Sprint 1
Change password (US-036/046 → Sprints 4/5), forgot password (US-037 → Sprint 4), and the admin screens for roles and account status (US-047 / US-048 → Sprint 5). None of them are needed for a demoable auth module, and Sprint 1 is already scaffolding three stacks from zero.

---

## II. Sprint 2 (Week 3–4)

### Goal
Deliver core account features: linked accounts, balances, transaction history, and statements.

### User Stories
- **US-013** Account management – view linked accounts `[Flutter]` `[BE]`
- **US-014** Account management – open new account request `[Flutter]` `[BE]`
- **US-015** Balance inquiry – real-time balance `[Flutter]` `[BE]`
- **US-016** Balance inquiry – multi-currency support `[BE]` `[Flutter]`
- **US-017** Transaction history – list & filter `[Flutter]` `[BE]`
- **US-018** Transaction history – search by date / type / amount `[Flutter]` `[BE]`
- **US-019** Statement download – generate PDF statement `[BE]`
- **US-020** Statement download – email statement `[BE]`
- **US-021** Statement download – in-app viewer `[Flutter]`
- **US-022** Notifications – push notification infrastructure `[BE]` `[Flutter]`
- **US-023** Notifications – balance change alerts `[BE]` `[Flutter]`
- **US-024** Account dashboard / home screen (mobile) `[Flutter]`

### Notes
- **US-016** is now tagged `[Flutter]` as well. A backend that holds multi-currency balances needs a currency selector or per-currency rows in the app, or there is nothing to demo.
- **US-020** works end-to-end because Mailpit exists from Sprint 1 — the statement email arrives in the inbox at `localhost:8025` with the PDF attached.
- **US-022** should define a single `NotificationService` with pluggable channels (push, email, SMS). US-023, US-035 and US-060 all attach to it rather than each inventing their own path.

### Deliverable
Account Management Module.

---

## III. Sprint 3 (Week 5–6)

### Goal
Enable money movement: fund transfers, beneficiary management, and QR payments (interbank and merchant flows simulated for the demo).

### User Stories
- **US-025** Fund transfer – between own accounts `[Flutter]` `[BE]`
- **US-026** Fund transfer – to other bank accounts (interbank, simulated) `[Flutter]` `[BE]`
- **US-027** Fund transfer – limits & validation rules `[BE]`
- **US-028** Fund transfer – confirmation & receipt `[Flutter]` `[BE]`
- **US-029** Beneficiary management – add beneficiary `[Flutter]` `[BE]`
- **US-030** Beneficiary management – edit / delete beneficiary `[Flutter]` `[BE]`
- **US-031** Beneficiary management – favorites / quick transfer `[Flutter]`
- **US-032** QR payment – generate personal QR code `[Flutter]` `[BE]`
- **US-033** QR payment – scan & pay `[Flutter]` `[BE]`
- **US-034** QR payment – merchant payment (simulated response) `[BE]`
- **US-035** Transfer notifications (push) `[BE]` `[Flutter]`

### Notes
- **US-027 carries the transfer-protection story on its own** now that US-040 is cut. It is a hard block: over-limit transfers are rejected with a `400`, not flagged.
- **US-034 still needs a written spec before it starts.** Minimum viable: seed three demo merchants; scanning a merchant QR settles instantly; one seeded merchant always declines so the failure path is demoable.
- **Schedule risk lives here.** QR scanning on a physical device is the most likely thing in this plan to eat an unplanned day. Sprint 3 is deliberately the lightest of the first three.

### Deliverable
Fund Transfer & Payments Module.

---

## IV. Sprint 4 (Week 7–8)

### Goal
Build bill payment, debit card management, and the account-recovery flows deferred out of Sprint 1.

### User Stories
- **US-036** Change password (customer) `[Flutter]` `[BE]`
- **US-037** Forgot password / reset flow `[BE]` `[Flutter]` `[React]`
- **US-038** Bill payment – utility provider model & seed data `[BE]`
- **US-039** Bill payment – pay electricity / water / internet `[Flutter]` `[BE]`
- **US-041** Bill payment – scheduled / recurring payments `[BE]` `[Flutter]`
- **US-042** Bill payment – payment history `[Flutter]` `[BE]`
- **US-043** Debit card management – request new card `[Flutter]` `[BE]`
- **US-044** Debit card management – block / unblock card `[Flutter]` `[BE]`
- **US-045** Debit card management – set PIN / spending limits `[Flutter]` `[BE]`

### Notes
- **US-038** seeds providers so US-039 is unblocked. The admin CRUD screen for them is US-052 in Sprint 5 — paying a bill does not require an admin UI to exist first.
- **US-037's React half** attaches to the admin login screen built in Sprint 1, so the shell it needs already exists.

### Deliverable
Bill Payment, Card Management & Account Recovery Module.

---

## V. Sprint 5 (Week 9–10)

### Goal
Build the entire staff-facing React portal in one pass, against backend APIs that are already built and tested.

### User Stories
- **US-046** Change password (admin) `[React]` `[BE]`
- **US-047** Roles & permissions management screen `[React]`
- **US-048** Account status actions – suspend / lock / reactivate `[React]`
- **US-049** Customer account overview (accounts + balances) `[React]` `[BE]`
- **US-050** Transfer monitoring feed `[React]` `[BE]`
- **US-052** Bill provider CRUD screen `[React]`
- **US-053** Dashboard overview KPIs `[React]` `[BE]`
- **US-054** Customer account management `[React]` `[BE]`

### Screen inventory — Tier B (7 screens + shell)

| # | Screen | Stories | Contents |
|---|---|---|---|
| 0 | App shell | US-002 *(S1)* | Sidebar nav, header with logged-in admin, design system, protected routes |
| 1 | Login + 2FA | US-010, US-012 *(S1)*, US-037 *(S4)* | Email/password → mandatory 2FA challenge → forgot-password link |
| 2 | KPI home | US-053 | Total customers, total accounts, today's transfer count & volume, failed-login count |
| 3 | Customers | US-054, US-048, US-049 | Searchable customer table → detail drawer showing their accounts and balances, with suspend / lock / reactivate actions |
| 4 | Transaction monitor | US-050 | Filterable transfer feed (date, amount, status, account) — read-only monitoring |
| 5 | Roles & permissions | US-047 | Role list, permission checkboxes per role, assign role to staff user |
| 6 | Bill providers | US-052 | CRUD table (name, category, account-number format) |
| 7 | Audit log | US-056 *(S6)* | Read-only log table with filters (actor, action type, date range) |
| — | Admin settings | US-046 | Change own password |

Screens 0 and 1 are built in Sprint 1 (you need to log in before you can build anything behind auth). Screen 7 lands in Sprint 6 because it depends on US-055 capturing the logs first — **build screens 3 and 4 with reusable table and filter components so US-056 is assembly, not a rebuild.**

### Deliverable
Admin Portal.

---

## VI. Sprint 6 (Week 11–12)

### Goal
Security hardening, audit logging, quality assurance, and local demo readiness.

### User Stories
- **US-055** Audit logs – capture all critical actions `[BE]`
- **US-056** Audit logs – admin log viewer & filters `[React]` `[BE]`
- **US-057** Security hardening – encryption at rest & in transit `[BE]`
- **US-058** Security hardening – rate limiting & brute-force protection `[BE]`
- **US-059** Two-factor authentication – review & edge cases `[BE]`
- **US-060** Notifications – email / SMS provider integration `[BE]`

### US-058 is a retrofit, not greenfield — budget accordingly

Rate limiting was deliberately kept out of Sprint 1, which means US-058 adds protection to endpoints already written without it. Acceptance criteria must name them:

`POST /auth/login` · `POST /auth/otp/verify` · `POST /auth/otp/resend` · `POST /auth/forgot-password` · `POST /auth/register` · `POST /auth/refresh`

Each needs a limit, a lockout policy that agrees with US-006's `LOCKED` status, and a regression test proving the existing happy path still passes.

### US-060 is a swap, not a build
`LoggingSmsSender` already exists from Sprint 1 behind the `OtpSender` interface. This story replaces the logging stub with a live SMS provider — roughly an hour, not a sprint's worth of work.

### Additional Activities
- System Integration Testing (SIT) — note that unit tests are written per-story throughout; Sprint 6 is *integration* testing, not "the sprint where testing happens." `backend-ci.yml` and `mobile-ci.yml` already run tests on every PR.
- User Acceptance Testing (UAT)
- Bug Fixing
- Basic performance & security checks (no penetration testing required for local demo)
- Project Documentation
- Sprint Review & Retrospective
- Final Presentation / live walkthrough on localhost

### Deliverable
Demo-ready Online Banking System: Java backend API, React admin portal, and Flutter mobile app, all running locally.

---

## Agile Timeline Summary

| Sprint | Weeks | Main Deliverables | Stories |
|---|---|---|---|
| Sprint 1 | 1–2 | Authentication, Two-Factor Security, Onboarding | 12 |
| Sprint 2 | 3–4 | Account Management, Balance, History, Statements | 12 |
| Sprint 3 | 5–6 | Fund Transfer, Beneficiaries, QR Payments | 11 |
| Sprint 4 | 7–8 | Bill Payment, Card Management, Account Recovery | 9 |
| Sprint 5 | 9–10 | Admin Portal (7 screens) | 8 |
| Sprint 6 | 11–12 | Audit, Security Hardening, Testing, Local Demo | 6 |

Story count is a rough signal, not a measurement — US-007 (KYC form + manual ID entry, two layers) is not the same size as US-031 (favorites list, one layer). The distribution above is intended to keep the two hardest weeks (1–2, scaffolding three stacks from zero) from also being the fullest.

The architecture splits across three layers: a Java / Spring Boot REST API as the single source of truth, a React portal for staff and administrators, and a Flutter mobile app for customers — all run locally for demo purposes rather than deployed to production infrastructure.

## Still to be written

- **US-034 merchant QR spec** — seeded merchants, settlement behaviour, failure path.
- **Notifications architecture note** — one `NotificationService` shared by US-022, US-023, US-035 and US-060, written before Sprint 2 starts.
- **SMS provider choice for US-060** — no vendor picked yet for the live swap behind `OtpSender` (Twilio or similar); needed before Sprint 6 planning.
- **US-020 email-address source** — statement email now has no KYC-collected email to send to, since registration dropped the email field. Needs a decision before Sprint 2: collect email separately (e.g. optional profile field) or drop US-020's email delivery.
