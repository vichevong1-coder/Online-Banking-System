# Online Banking System — Agile Sprint Plan

Scoped for Solo Local Demo
6 Sprints / 12 Weeks | Local demo only, no production deployment

Tech stack: **Java (Spring Boot)** backend API | **React** staff / admin web portal | **Flutter** customer mobile app

Each user story is tagged with the layer(s) it touches: `[BE]` Java backend, `[React]` React admin portal, `[Flutter]` Flutter customer app.

> **Rev 2** — rebalanced down from the original 20-feature scope. The reasoning behind each
> structural decision is recorded inline, in the notes attached to the stories it affects, rather
> than in a separate review document.

## Scope

The original 20-feature scope was cut down to what one person can build in 12 weeks as a local demo.

**Removed:** Loan application (form + doc upload + eligibility calculator + approval workflow), ATM locator (real map SDK), Chat / ticketing support, Reports with CSV / PDF / Excel export, Fixed deposit interest engine with maturity / auto-renewal, admin approval workflows for loans and account freeze, **transaction fraud / risk check (US-040)**, and **admin broadcast announcements (US-051)**.

This plan covers the original feature set minus the removals listed above, across **58 user stories**.

> **Open item.** The previous revision claimed "15 functionalities," carried over from before US-040
> and US-051 were cut. That count needs re-deriving against the original 20-feature list (which isn't
> in this repo) before it goes back in.

## Sprint Status

| Sprint | Weeks | Theme | Status |
|---|---|---|---|
| 1 | 1–2 | Authentication & Onboarding | **Done** |
| 2 | 3–4 | Accounts & Balances | **Done except US-020, US-022, US-023** |
| 3 | 5–6 | Admin Portal I — customers, KPIs, roles, status | **Done** |
| 4 | 7–8 | Money Movement (backend) & Transfer Monitoring | **Done except US-020** |
| 5 | 9–10 | Bills & Cards (backend), Admin CRUD, Mobile Money Movement | Not started |
| 6 | 11–12 | Mobile Bills & Cards, Audit, Hardening & Testing | Not started |

> **Rev 3 — resequenced.** The admin portal moved from Sprint 5 to Sprints 3–5, and the remaining
> mobile work moved behind it. Rationale and the evidence for what was already shipped are in
> "Where the project actually stands" below. Sprints 1 and 2 are recorded as built, not re-planned.

## Where the project actually stands

Verified against the git history and the working tree, not from memory:

| Shipped | Evidence |
|---|---|
| US-001, US-002, US-003 — all three stacks scaffolded | `7dbb829`, `062e15e`, `e03182f` |
| US-004 – US-012 — backend auth, JWT, roles, status model, 2FA | `2ac36e2` |
| US-002, US-010, US-012 — admin login, 2FA challenge, app shell `[React]` | `3aa095d` |
| US-003, US-007 – US-011 — registration, OTP, login, 2FA screens `[Flutter]` | `d1a813b` |
| US-013 – US-019 — accounts, balances, history, PDF statements `[BE]` | `8930a57` |
| US-013 – US-019, US-021, US-024 — mobile account screens | `5446444` |

**Still open from Sprint 2:** US-020 (statement email — blocked on the email-address decision),
US-022 and US-023 (notifications — no `feature/notification` package exists yet).

This matters for the resequencing below: **the mobile app is already built through the whole of
account management.** What remains on Flutter is money movement (US-025 – US-035), bills and cards
(US-036 – US-045) and notifications — roughly 20 stories, not the 33 the `[Flutter]` tag count
suggests. Deferring mobile means deferring *those*; the shipped screens stay shipped.

## Two structural rules this plan follows

1. **Backend models ship where the data is needed; the staff screen over each one follows.** Three capabilities are split across two story IDs each on this rule:

   | Backend model | Ships | Admin screen | Ships |
   |---|---|---|---|
   | US-005 Role model & JWT role claims | Sprint 1 ✓ | US-047 Roles screen | Sprint 3 |
   | US-006 Account status model & login-path enforcement | Sprint 1 ✓ | US-048 Suspend / lock / reactivate actions | Sprint 3 |
   | US-038 Bill provider model & seed data | Sprint 5 | US-052 Bill provider CRUD screen | Sprint 5 |

   These are distinct story IDs, not `a`/`b` halves of a single one.
2. **A screen is built in the sprint where its data becomes available — never earlier.** This is the rule that drives the Rev 3 resequencing. The admin portal moved forward to Sprint 3 because Sprint 2 shipped the account backend it reads; US-050 stayed back to Sprint 4 because the `transfers` table it monitors does not exist until then; US-052 waits for US-038, and US-056 waits for US-055 to capture logs.

   The corollary is the resequencing itself: **the client that ships first is the one whose data already exists.** Mobile is now built one sprint behind the backend it calls, rather than alongside it — see the screen inventory in Sprint 3 and the mobile blocks in Sprints 5 and 6.

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

There is no self-registration for staff/admin accounts, and the real admin-provisions-staff flow is US-047 in Sprint 3 — so Sprint 1 needs a way for an admin account to exist before that flow is built. It's seeded: a Flyway migration (`V2__seed_bootstrap_admin.sql`) inserts one `ADMIN` row with credentials sourced from `ADMIN_BOOTSTRAP_PASSWORD_HASH` / `ADMIN_BOOTSTRAP_EMAIL` / `ADMIN_BOOTSTRAP_PHONE` env vars (throwaway local-demo defaults documented in `backend/.env.example`, same convention as `JWT_SECRET`), never hardcoded in the migration. Because the KYC columns (`nid_number`, `nid_expiry_date`, `date_of_birth`, `gender`) only apply to customers, that migration also relaxes them to nullable so the seeded admin row doesn't need placeholder ID data.

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
- **US-022** Notifications – push notification infrastructure `[BE]` `[Flutter]` → *carried to Sprint 3*
- **US-023** Notifications – balance change alerts `[BE]` `[Flutter]` → *carried to Sprint 3*
- **US-024** Account dashboard / home screen (mobile) `[Flutter]`

### Notes
- **US-016** is now tagged `[Flutter]` as well. A backend that holds multi-currency balances needs a currency selector or per-currency rows in the app, or there is nothing to demo.
- **US-020** works end-to-end because Mailpit exists from Sprint 1 — the statement email arrives in the inbox at `localhost:8025` with the PDF attached.
- **US-022** should define a single `NotificationService` with pluggable channels (push, email, SMS). US-023, US-035 and US-060 all attach to it rather than each inventing their own path. **The push channel itself is an unmade decision and blocks this story** — there is no Firebase project for a local demo. Default to a `notifications` table plus in-app polling, with FCM as a later swap behind the same interface; that is the same stub-behind-an-interface shape `OtpSender` already uses. Decide before Sprint 2 planning.
- **US-020's mail configuration is deferred behind its own open question.** `application.properties` has no `spring.mail.*` and deliberately will not get any until the email-address question at the bottom of this plan is settled. If the answer is "collect email separately," add `spring.mail.host` / `spring.mail.port` pointing at Mailpit as part of US-020's acceptance criteria. If the answer is "drop email delivery," US-020 is cut and Mailpit leaves `docker-compose.yml` with it. Do not configure mail before that decision.
- **US-024 owns mobile session persistence.** The Flutter `ApiClient` currently holds the access token in memory only, and `pubspec.yaml` has no secure-storage dependency — so closing the app logs the customer out, and the refresh token the backend issues has nowhere to live. US-024 is the first screen that must survive an app restart, so it carries the fix: add `flutter_secure_storage`, persist the **refresh token only** (mirroring the React admin's choice), and re-exchange it for an access token on launch. Sprint 4's US-036 / US-037 assume a durable session exists.

### Deliverable
Account Management Module.

---

## III. Sprint 3 (Week 5–6) — Admin Portal I

### Goal
Build the staff-facing screens that read data the backend already serves. Account management
(US-013 – US-019) shipped in Sprint 2, so customers, accounts, balances and transaction history are
all queryable today — the portal has something real to display without any further backend work.

### User Stories
- **US-046** Change password (admin) `[React]` `[BE]`
- **US-047** Roles & permissions management screen `[React]` `[BE]`
- **US-048** Account status actions – suspend / lock / reactivate `[React]` `[BE]`
- **US-049** Customer account overview (accounts + balances) `[React]` `[BE]`
- **US-053** Dashboard overview KPIs `[React]` `[BE]`
- **US-054** Customer account management `[React]` `[BE]`
- **US-022** Notifications – push notification infrastructure `[BE]`
- **US-023** Notifications – balance change alerts `[BE]`

### Notes

- **Why this sprint moved earlier.** The portal was originally Sprint 5 because its screens display
  customers, accounts and transfers that the mobile app produces. Sprint 2 shipped all of that
  backend, and `backend/src/main/resources/db/demo/seed_demo_data.sql` seeds four customers, six
  multi-currency accounts and eighteen transactions — so every screen in this sprint has realistic
  data to render against with no further dependency. **Run the seed before starting.**
- **Two portal screens deliberately are *not* here**, because their data genuinely does not exist
  yet: US-050 (transfer monitoring) needs the `transfers` table built in Sprint 4, and US-052 (bill
  provider CRUD) needs US-038's provider model from Sprint 5. Promising a "finished portal" this
  sprint would be false — this is the portal minus those two screens.
- **US-047, US-048 and US-052 are not React-only.** Each needs backend that no other story creates: US-047 needs staff-account creation and role assignment, US-048 needs a status-mutation endpoint (`AccountStatusPolicy` only *reads* status today — nothing writes it), and US-052 needs provider CRUD on top of US-038's seed data. All three are now tagged `[BE]` as well.
- **US-047 is scoped to single-role assignment, deliberately.** `users.role` is one scalar column, and there are no `roles` / `permissions` / `role_permissions` tables. A real permission matrix is a sprint of work on its own and is not in the 58-story budget. US-047 therefore assigns one of the existing `Role` values to a staff user and creates staff accounts — no per-permission checkboxes. (`JwtService` already emits a `roles[]` array claim while `User.role` is scalar; that is forward-compatibility, not evidence multi-role is supported.)
- **A `login_attempts` table lands here.** Nothing records failed logins today, which leaves US-053's failed-login KPI with no data source, and US-058's lockout policy (Sprint 6) with no counter. One small table serves both, and it has to precede the earlier of the two — which is now this sprint.
- **US-053's transfer-count and volume tiles have no data until Sprint 4.** Transfers do not exist
  yet. Build the KPI home with the customer/account tiles live and the transfer tiles stubbed, then
  wire them in Sprint 4 when `transfers` lands. The story must also name the single display currency
  its volume figure is reported in — see Sprint 4's cross-currency decision.
- **US-022 and US-023 are Sprint 2 carry-over** and stay attached to the backend/notification work
  rather than moving with the mobile app; the `NotificationService` they define is a dependency of
  US-035 and US-060.
- **Build screens 3 and 4 with reusable table and filter components.** US-050 (Sprint 4) and US-056
  (Sprint 6) are then assembly, not a rebuild.

### Screen inventory — 7 screens + shell, now spread across Sprints 3–6

| # | Screen | Stories | Lands | Contents |
|---|---|---|---|---|
| 0 | App shell | US-002 | **Done** *(S1)* | Sidebar nav, header with logged-in admin, design system, protected routes |
| 1 | Login + 2FA | US-010, US-012 | **Done** *(S1)*, + US-037 in S5 | Email/password → mandatory 2FA challenge → forgot-password link |
| 2 | KPI home | US-053 | **S3** (transfer tiles wired in S4) | Total customers, total accounts, today's transfer count & volume, failed-login count |
| 3 | Customers | US-054, US-048, US-049 | **S3** | Searchable customer table → detail drawer showing their accounts and balances, with suspend / lock / reactivate actions |
| 5 | Roles | US-047 | **S3** | Role list, assign one role to a staff user, create staff accounts |
| — | Admin settings | US-046 | **S3** | Change own password |
| 4 | Transaction monitor | US-050 | **S4** — needs `transfers` | Filterable transfer feed (date, amount, status, account) — read-only monitoring |
| 6 | Bill providers | US-052 | **S5** — needs US-038 | CRUD table (name, category, account-number format) |
| 7 | Audit log | US-056 | **S6** — needs US-055 | Read-only log table with filters (actor, action type, date range) |

Screens 0 and 1 already exist. The portal is *usable* at the end of Sprint 3, *complete except for
audit* at the end of Sprint 5. **Build screens 2 and 3 with reusable table and filter components so
screens 4, 6 and 7 are assembly, not a rebuild.**

### Deliverable
Admin Portal I — login, KPI home, customers with status actions, roles, admin settings.

---

## IV. Sprint 4 (Week 7–8) — Money Movement (backend) & Transfer Monitoring

### Goal
Build the money-movement engine and the staff screen that watches it. Backend and admin only —
the mobile screens for these flows follow in Sprint 5.

### User Stories
- **US-025** Fund transfer – between own accounts `[BE]`
- **US-026** Fund transfer – to other bank accounts (interbank, simulated) `[BE]`
- **US-027** Fund transfer – limits & validation rules `[BE]`
- **US-028** Fund transfer – confirmation & receipt `[BE]`
- **US-029** Beneficiary management – add beneficiary `[BE]`
- **US-030** Beneficiary management – edit / delete beneficiary `[BE]`
- **US-032** QR payment – generate personal QR code `[BE]`
- **US-033** QR payment – scan & pay `[BE]`
- **US-034** QR payment – merchant payment (simulated response) `[BE]`
- **US-035** Transfer notifications (push) `[BE]`
- **US-050** Transfer monitoring feed `[React]` `[BE]`

### Notes

- **US-050 is the payoff for doing backend-first.** The transfer monitoring feed is built in the
  same sprint as the transfers it monitors, against real rows rather than seeded stand-ins — and it
  becomes the tool you use to verify the transfer engine while building it.
- **Finish US-053's stubbed transfer tiles here**, now that `transfers` exists.
- **A transfer needs to be a first-class row before US-025 starts.** The `transactions` table is a per-account ledger (`account_id`, `amount`, `balance_after`) with no `transfer_id`, no counterparty and no status — so a transfer is currently two unlinked rows. US-028 (receipt), US-050 (transfer monitoring feed) and US-053 (today's transfer count & volume, which would double-count) all need the transfer itself. Add a migration creating `transfers (id, from_account_id, to_account_id NULL, external_ref, amount, currency, status, reference UNIQUE, created_at)` plus `transactions.transfer_id`, and have one `@Transactional` service method write the transfer and both legs together. This is a prerequisite for US-025, not part of it.
- **US-027 must add optimistic locking on `accounts.balance`.** The `Account` entity has no `@Version` field and nothing takes a row lock, so two concurrent debits read the same balance and both write their own result — a lost update that lets the demo create money. Acceptance criteria: a `@Version` column plus migration, a `409` (or a bounded retry) on `OptimisticLockingFailureException`, and a test that fires two simultaneous transfers at one account and proves it cannot overdraw.
- **Cross-currency transfers are rejected, not converted.** `accounts.currency` exists and US-016 shows per-currency balances, but there is no rate table and no conversion service, and building an FX engine is out of budget for a 12-week solo project. Decision: US-027 rejects a transfer whose source and destination currencies differ with `400 CURRENCY_MISMATCH`; customers move money between same-currency accounts only. Multi-currency stays a display feature. US-053's volume KPI is therefore reported in a single declared display currency — name it in that story.
- **US-027 carries the transfer-protection story on its own** now that US-040 is cut. It is a hard block: over-limit transfers are rejected with a `400`, not flagged.
- **US-034's spec is written** — see [`qr-payments-spec.md`](./qr-payments-spec.md), which covers US-032/US-033/US-034 together because the payload format is shared. Three seeded demo merchants, instant settlement, and one merchant that always declines so the failure path is demoable.
- **The QR schedule risk moved to Sprint 5 with the mobile half.** Generating and parsing QR payloads is backend work and lands here; *scanning* with a physical device camera (US-033) is the part most likely to eat an unplanned day, and that now sits in Sprint 5. Keep the payload format simple enough that the mobile half is genuinely just a camera plus a POST.

### Deliverable
Money Movement Engine + Transfer Monitoring screen.

---

## V. Sprint 5 (Week 9–10) — Bills & Cards (backend), Admin CRUD, Mobile Money Movement

### Goal
Build the bill-payment and card backends plus the last admin screen, and bring the mobile app back
up to the backend by delivering the money-movement screens whose APIs shipped in Sprint 4.

### User Stories — backend & admin
- **US-036** Change password (customer) `[BE]`
- **US-037** Forgot password / reset flow `[BE]` `[React]`
- **US-038** Bill payment – utility provider model & seed data `[BE]`
- **US-039** Bill payment – pay electricity / water / internet `[BE]`
- **US-041** Bill payment – scheduled / recurring payments `[BE]`
- **US-042** Bill payment – payment history `[BE]`
- **US-043** Debit card management – request new card `[BE]`
- **US-044** Debit card management – block / unblock card `[BE]`
- **US-045** Debit card management – set PIN / spending limits `[BE]`
- **US-052** Bill provider CRUD screen `[React]` `[BE]`

### User Stories — mobile (money movement, one sprint behind its backend)
- **US-025** Fund transfer – between own accounts `[Flutter]`
- **US-026** Fund transfer – to other bank accounts `[Flutter]`
- **US-028** Fund transfer – confirmation & receipt `[Flutter]`
- **US-029** Beneficiary management – add beneficiary `[Flutter]`
- **US-030** Beneficiary management – edit / delete beneficiary `[Flutter]`
- **US-031** Beneficiary management – favorites / quick transfer `[Flutter]`
- **US-032** QR payment – generate personal QR code `[Flutter]`
- **US-033** QR payment – scan & pay `[Flutter]`
- **US-035** Transfer notifications (push) `[Flutter]`
- **US-022 / US-023** Notifications – in-app notification list & balance alerts `[Flutter]` *(backend shipped in Sprint 3)*

### Notes
- **This is the heaviest sprint in the plan** — 10 backend/admin stories plus 9 mobile ones. It is heavy *by construction*: deferring mobile stacks it behind the backend rather than removing it. If something slips, US-041 (scheduled / recurring payments) is the most droppable story here — it is the only one with no demo dependency on anything else.
- **US-038** seeds providers so US-039 is unblocked, and US-052 gives them a CRUD screen in the same sprint — the ordering constraint is only that US-038 comes first, not that it comes a sprint earlier.
- **US-037's React half** attaches to the admin login screen built in Sprint 1, so the shell it needs already exists.
- **US-033's camera work is the schedule risk.** It is the one story in the plan that needs a physical device and cannot be fully verified in an emulator. Start it early in the sprint, not late.
- With US-052 the portal is complete except for the audit log (US-056, Sprint 6).

### Deliverable
Bill Payment & Card backends, complete Admin Portal, and the mobile money-movement screens.

---

## VI. Sprint 6 (Week 11–12)

### Goal
Security hardening, audit logging, quality assurance, and local demo readiness.

### User Stories — mobile (bills, cards & recovery, one sprint behind its backend)
- **US-036** Change password (customer) `[Flutter]`
- **US-037** Forgot password / reset flow `[Flutter]`
- **US-039** Bill payment – pay electricity / water / internet `[Flutter]`
- **US-041** Bill payment – scheduled / recurring payments `[Flutter]`
- **US-042** Bill payment – payment history `[Flutter]`
- **US-043** Debit card management – request new card `[Flutter]`
- **US-044** Debit card management – block / unblock card `[Flutter]`
- **US-045** Debit card management – set PIN / spending limits `[Flutter]`

### User Stories — audit, hardening & release
- **US-055** Audit logs – capture all critical actions `[BE]`
- **US-056** Audit logs – admin log viewer & filters `[React]` `[BE]`
- **US-057** Security hardening – encryption at rest & in transit `[BE]`
- **US-058** Security hardening – rate limiting & brute-force protection `[BE]`
- **US-059** Two-factor authentication – review & edge cases `[BE]`
- **US-060** Notifications – email / SMS provider integration `[BE]`

> **This sprint is overloaded and you should plan to cut.** Eight mobile stories on top of six
> hardening stories, in the same two weeks as SIT, UAT and the final presentation, is not a realistic
> load — and it is the direct, predictable cost of putting mobile last. The mobile bill-payment and
> card screens (US-039, US-041 – US-045) are the honest cut: their backends still ship, the admin
> portal still demonstrates them, and the mobile app still demos auth, accounts, statements and the
> full money-movement flow from Sprint 5. Decide at the **start** of Sprint 6, not in week 12.

### US-058 is a retrofit, not greenfield — budget accordingly

Rate limiting was deliberately kept out of Sprint 1, which means US-058 adds protection to endpoints already written without it. Acceptance criteria must name them:

`POST /auth/login` · `POST /auth/otp/verify` · `POST /auth/otp/resend` · `POST /auth/2fa/verify` · `POST /auth/2fa/resend` · `POST /auth/forgot-password` · `POST /auth/register` · `POST /auth/refresh`

`/auth/2fa/verify` and `/auth/2fa/resend` were missing from the earlier revision of this list. `SecurityConfig` permits both unauthenticated, and they are the two endpoints that consume a 6-digit code — they belong here more than most of the others.

Each needs a limit, a lockout policy that agrees with US-006's `LOCKED` status, and a regression test proving the existing happy path still passes.

### US-059 has three specific holes to close, one of them a schema change

"Review & edge cases" is too vague to plan against. The three known gaps:

1. **The OTP has no attempt counter.** `otp_codes` has `expires_at` and `consumed_at` but no `attempts` column, and `OtpServiceImpl` has no limit logic — so a 6-digit code with a 5-minute window accepts unlimited guesses. This is a *migration*, not just rate limiting, and US-058 does not cover it: add `attempts INT NOT NULL DEFAULT 0`, increment on each failed verify, hard-fail and consume the row at 5.
2. **Challenge tokens are replayable for their full 5-minute TTL.** `resolveChallengeSubject` checks signature and expiry only — there is no `jti` and no record of a challenge being spent. Combined with (1), one successful password gives a 5-minute unlimited-guess window against the second factor. Fix both together: put a `jti` in the challenge token, store it on the `otp_codes` row, and invalidate the pair on success or at the attempt limit.
3. **There is no logout and no revocation.** Refresh tokens are stateless JWTs with no `jti` and no server-side store, and there is no `POST /auth/logout`. Severity is moderate rather than critical because `/auth/refresh` re-runs `AccountStatusPolicy`, so US-048's suspend cuts a session off within one access-token lifetime (≤15 min) rather than the 7-day refresh window. What it does *not* cover: a password change (US-036) cannot invalidate a stolen refresh token, and "log out" on either client only forgets the token locally. Fix: a `revoked_tokens (jti, user_id, revoked_at)` table checked in `/auth/refresh`, plus `POST /auth/logout`. Add "changing the password revokes existing refresh tokens" as an acceptance criterion on US-036.

Items 1 and 2 are one afternoon's work done together and two separate reworks done apart.

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

| Sprint | Weeks | Main Deliverables | Primary stack |
|---|---|---|---|
| Sprint 1 | 1–2 | Authentication, Two-Factor Security, Onboarding — **done** | BE + both clients |
| Sprint 2 | 3–4 | Account Management, Balance, History, Statements — **done** | BE + Flutter |
| Sprint 3 | 5–6 | Admin Portal I: KPI home, customers, roles, status actions; notifications | React + BE |
| Sprint 4 | 7–8 | Money-movement engine; transfer monitoring screen | BE + React |
| Sprint 5 | 9–10 | Bill & card backends; bill provider CRUD; mobile money movement | BE + React + Flutter |
| Sprint 6 | 11–12 | Mobile bills & cards; audit, hardening, testing, demo | Flutter + BE |

Story count is a rough signal, not a measurement — US-007 (KYC form + manual ID entry, two layers) is not the same size as US-031 (favorites list, one layer). The distribution above is intended to keep the two hardest weeks (1–2, scaffolding three stacks from zero) from also being the fullest.

The architecture splits across three layers: a Java / Spring Boot REST API as the single source of truth, a React portal for staff and administrators, and a Flutter mobile app for customers — all run locally for demo purposes rather than deployed to production infrastructure.

## Still to be written

- **Notifications architecture note** — one `NotificationService` shared by US-022, US-023, US-035 and US-060, written before Sprint 2 starts.
- **SMS provider choice for US-060** — no vendor picked yet for the live swap behind `OtpSender` (Twilio or similar); needed before Sprint 6 planning.
- **US-020 email-address source** — statement email now has no KYC-collected email to send to, since registration dropped the email field. Needs a decision before Sprint 2: collect email separately (e.g. optional profile field) or drop US-020's email delivery. Whichever way it lands also decides whether `spring.mail.*` gets configured and whether Mailpit stays in `docker-compose.yml`.
- **Push notification channel for US-022** — no Firebase project exists for a local demo. Decide before Sprint 2 planning; see the Sprint 2 note for the recommended default.
- **Feature count** — the "15 functionalities" figure needs re-deriving against the original 20-feature list before it goes back into the Scope section.
