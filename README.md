# Online Banking System

A monorepo containing three coordinated codebases that together deliver a full online banking product. Built as a 6-sprint, 12-week capstone project. See `.claude/CLAUDE.md` for the current sprint scope and conventions, and `docs/` for the full sprint plan, architecture notes, and ADRs.

## Structure

```
.
├── backend/     # Java / Spring Boot REST API
├── web-admin/   # React admin portal
├── mobile/      # Flutter customer app
├── docs/        # sprint plan, architecture diagrams, ADRs
└── .github/     # CI workflows, issue/PR templates
```

## Setup

### Local services (start these first)

Postgres and a local mail server run in Docker. Start them before the backend:

```bash
docker compose up -d        # postgres:17-alpine + mailpit
docker compose ps           # check both are running
docker compose down         # stop (database survives)
docker compose down -v      # stop and wipe the database
```

| Service | Address | Purpose |
|---|---|---|
| Postgres | `localhost:5432` — db/user/password all `obs` | Application database |
| Mailpit SMTP | `localhost:1025` | Where the backend sends mail |
| **Mailpit inbox** | **http://localhost:8025** | **Read OTP and statement emails here** |
| - Swagger UI: http://localhost:8080/swagger-ui.html (redirects to /swagger-ui/index.html)
| - Raw OpenAPI spec (JSON): http://localhost:8080/v3/api-docs
Credentials match `.github/workflows/backend-ci.yml` so local runs and CI behave the same. Override
them by copying `.env.example` to `.env` at the repo root.

> **Email is real; SMS is not.** OTP and statement emails are genuinely delivered over SMTP and
> readable in the Mailpit inbox. The SMS channel is a logging stub for the whole project — phone
> OTPs are written to the backend log, never sent. See `docs/sprint-plan-review.md`.

### Backend (`backend/`)
```bash
cd backend
./mvnw spring-boot:run     # run locally
./mvnw test                 # run tests (starts a throwaway Postgres container; needs Docker)
./mvnw clean package        # build jar
```
Requires a `.env` (gitignored) — see `backend/README.md` once created for required variables.

### Web Admin (`web-admin/`)
```bash
cd web-admin
npm install
npm run dev                 # local dev server
npm run build
npm test
```
Requires a `.env` (gitignored) — see `web-admin/README.md` once created for required variables.

### Mobile (`mobile/`)
```bash
cd mobile
flutter pub get
flutter run
flutter test
```
Requires a `.env` (gitignored) — see `mobile/README.md` once created for required variables.

## Contributing

- Keep PRs scoped to one directory (`backend/`, `web-admin/`, or `mobile/`) so the path-filtered CI workflows stay meaningful.
- Reference user story IDs (e.g. `US-004`) in commit messages and PR titles.
- Never commit secrets or API keys — use gitignored `.env` files.
