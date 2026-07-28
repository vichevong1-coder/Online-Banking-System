# Mobile

Flutter customer app (org `com.obs.mobile`). See root `.claude/CLAUDE.md` for Sprint 1 scope and conventions.

## Structure

Feature-first under `lib/`:

- `lib/main.dart` — entry point
- `lib/app.dart` — root `MaterialApp` widget
- `lib/core/` — shared theme, utilities, cross-cutting concerns
- `lib/features/` — one folder per feature (added as auth screens land in US-004+); no business logic in widgets

## Local setup

```bash
flutter pub get
flutter run
flutter test
```

Copy `.env.example` to `.env` once backend API endpoints are wired up (US-006+).
