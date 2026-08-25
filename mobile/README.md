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

## Running against the backend on a physical Android phone

The default base URL (`http://10.0.2.2:8080`) is the **emulator's** alias for the host machine and
means nothing on a real handset. On a phone, point the app at the dev machine's LAN address:

```bash
# 1. find your machine's LAN IP (phone and machine must be on the same network)
hostname -I | awk '{print $1}'          # e.g. 192.168.1.20

# 2. start the backend so it listens on that interface (Spring binds 0.0.0.0 by default)
cd ../backend && ./mvnw spring-boot:run

# 3. run the app with that address compiled in
cd ../mobile
flutter run --dart-define=API_BASE_URL=http://192.168.1.20:8080
```

Notes:

- **Debug builds only** are allowed to use plain HTTP — `android/app/src/debug/` carries a network
  security config for it. Release builds keep Android's HTTPS-only default, so a release APK will
  not reach the local backend, by design.
- If requests time out, it is almost always the host firewall rather than the app: allow inbound
  8080 on the dev machine, and confirm from the phone's browser that
  `http://<LAN-IP>:8080/swagger-ui.html` loads.
- US-033's QR scanning needs the camera permission, which Android prompts for the first time the
  scanner screen opens. Denying it leaves the screen's "type the code instead" fallback.
