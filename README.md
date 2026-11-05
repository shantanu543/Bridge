# Bridge

Bridge is a native Android professional networking app, backed by a Python API. The working tree includes account/onboarding and private Passport flows, plus the first persistent implementations of Discover, intents, connections, chat, Cohorts, Pods, referrals, and member reports. Product feature coverage is still incomplete: real-time/push notifications, richer moderation and group management, provider imports, legal-policy configuration, and release hardening remain open. The current app continues to use its existing account gate; sign-in changes are deferred.

## Repository layout

- `android/` — Kotlin and Jetpack Compose application.
- `backend/` — FastAPI modular-monolith API.
- `docs/` — architecture, data, API, SDLC, and provider notes.
- `assets/reference/` — supplied visual references; these are design references, not production UI screens.
- `infra/` — reserved for reviewed infrastructure-as-code work after the cloud setup gate.

## Local development

Requirements: JDK 17 or newer, Android SDK (API 35 baseline), Gradle 8.10.2+, Python 3.12+, Docker Compose.

1. Copy `.env.example` to `.env` and fill local-only values. Never put real secrets in Git.
2. Start local PostgreSQL and the development-only Mailpit inbox with `docker compose up -d db mailpit` (Mailpit UI: `http://localhost:8025`).
3. Create a Python 3.12 virtual environment, install the project (`pip install -e '.[dev]'`), and run `alembic upgrade head`.
4. Run the API with `uvicorn app.main:app --reload --app-dir backend --port 8001` (or build `docker build -f backend/Dockerfile .`).
5. Open `android/` in Android Studio and run the `devDebug` variant on an API 26+ emulator.

Google sign-in requires a Google OAuth **Web client ID** in `GOOGLE_SERVER_CLIENT_ID` for the API and the matching Gradle property `bridge.google.serverClientId` (or `BRIDGE_GOOGLE_SERVER_CLIENT_ID`) for Android. Use `android/gradle.properties.example` as a template; keep the filled local file uncommitted. Generate a local-only `EMAIL_TOKEN_PEPPER` before email registration; do not reuse it outside development. The backend issues opaque access and rotating refresh tokens and stores only their hashes. Android encrypts session values with an Android Keystore key.

For local UI work, set `DEV_AUTH_BYPASS_ENABLED=true` in the ignored local `.env` and run the `devDebug` app flavor. The welcome screen then offers **Explore local demo**, which creates a clearly labeled synthetic member and a normal expiring Bridge session. The setting defaults to false and the API refuses it unless `BRIDGE_ENV=dev`; staging and production app variants do not contain the bypass button.

The API provides liveness and database readiness checks at `/health/live` and `/health/ready`. Interactive OpenAPI documentation is available at `/docs` in local development. The dev Android build targets port 8001. Current local migrations include the social graph, group spaces, messages, referral requests, and reports.

## Scope and release controls

The requirements and implementation status are in [docs/implementation-plan.md](docs/implementation-plan.md). Product feature implementation is authorized for local development. No production services are provisioned. Production deployment and any Git commit require separate explicit approval.

## Product and security rules

- Android is native Compose. Tailwind CSS is a web styling framework and does not apply to this app.
- Business rules and access control belong in the backend.
- Use only official, approved OAuth/API paths; provide manual or supported file-import fallbacks.
- Never expose Bridge Score internals, publish resumes by default, or claim a score percentile without audited evidence.
- Use synthetic/seed data outside production; keep credentials out of source control.
- See [docs/provider-integration-limitations.md](docs/provider-integration-limitations.md) for dependencies on external access and approvals.
