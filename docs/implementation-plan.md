# Implementation plan and SDLC gates

**Status:** Foundation, Phase 1, and Phase 2 were approved. On 2026-11-06 the product owner directed completion of the remaining local application feature areas and requested that sign-in changes be deferred. This authorizes local feature development through Phases 3–7; it does not authorize a production release or Git commit. Phase 8 and production operations remain separately gated.

## Foundation stage — requirements Phase 0 (approved)

- Inspect repository and references; preserve existing work.
- Add the native Android and FastAPI skeletons, modular boundaries, design-system/navigation shell, local PostgreSQL setup, environment templates, CI scaffold, and required engineering documentation.
- Keep cloud accounts, provider approvals, actual credentials, deployments, and production data outside this phase.
- Exit review: source tree is coherent; configuration and ownership boundaries are documented; dependency/runtime limitations are reported. No Git commit is implied.

## Phase 1 — Authentication and onboarding (approved)

Google Credential Manager, email/password with verification/reset, Bridge sessions and account linking, consent, approved GitHub/LinkedIn integrations and fallbacks, resume upload/parsing, profile review/edit/preview, and import provenance. Implemented in the working tree: native Google sign-in exchange, Bridge-managed email/password registration/verification/reset, opaque access and rotating refresh sessions, Android encrypted session storage, Terms/Privacy acceptance UI driven by configured policy URLs, private manual profile editing/confirmation, a local profile preview, privacy audience preference, and field provenance. The Android source-selection screen explains that imports are unavailable until configured.

Still needed to complete all Phase 1 requirements: product-approved Terms/Privacy/import-consent text and versions; Google OAuth client IDs and verified email delivery; approved GitHub OAuth application; LinkedIn API approval and credentials; private S3 upload configuration, malware scanning/parser worker, and retention policy; import review UI and end-to-end provider/import verification. No credentials or production secrets are included in the repository.

The local environment now has Java 17, Android SDK, Python virtual environment, and Docker. The Android dev-debug build and local database migration have been run for the current working tree. Phase 1 provider/legal/storage dependencies remain open as described above.

## Phase 2 — Bridge Score and Passport (approved; partial implementation)

Implemented: private server-side evidence-completeness scoring, versioned score snapshots/history, not-ready states until a confirmed profile has sufficient evidence, member-safe improvement suggestions, a separate verification state, owner-only Passport API, and Android score ring/Passport view. The internal rubric is persisted separately and omitted from member responses. No provider source contributions, public percentile, public link, or member-visible sharing have been implemented.

Remaining: an authorized reviewer role and audited verification decisions; field-level visibility controls; score visibility controls; explicit share/revoke/expiry flows and share-sheet integration; and member-requested score review. These require product privacy/reviewer policy before they can be safely exposed.

## Phase 3 — Intents, Discover, Connections, and safety foundation (in progress)

Implemented locally: seeded intent catalog, up to three member intents, discoverable confirmed-profile search with skill/name/headline filters at the API, request/accept/decline lifecycle, accepted-connection conversation creation, blocking and report submission APIs, and a native Discover/Connections UI. Remaining: filters/search UI, full member profile view, safer undo/dismiss controls, report/block UI, and richer authorization regression review.

## Phase 4 — Chat and notifications (in progress)

Implemented locally: persistent direct messages, accepted-connection checks on read/send, in-app notification persistence for connection/message/referral/group activity, and a native conversation list/message composer/notification inbox. Remaining: WebSocket delivery/reconnect, FCM push, notification preferences, attachment handling, unread synchronization, and messaging rate limits.

## Phase 5 — Cohorts (in progress)

Implemented locally: public directory, create/join, persistent member posts, and membership checks for discussions. Remaining: comments/reactions, member directory, private cohorts, moderator actions, and scoped moderation review.

## Phase 6 — Pods (in progress)

Implemented locally: private-by-default creation, optional discoverable join, transactional 10-member join cap, expiry, read-only behavior after expiry, member-only persistent chat, and native create/list/chat screens. Remaining: invitation acceptance, member management/admin transfer, resources, archival controls, and shared-block handling inside Pods.

## Phase 7 — Ask for Referral (in progress)

Implemented locally: accepted-connection eligibility, request state/history and recipient response, account-lifetime quota of 10 serialized by a per-account database lock, and native request/history UI. Remaining: recipient preferences, notifications, idempotency/rate limits, withdrawal rules, and additional anti-abuse review.

## Phase 8 — Production hardening and release (separate approval required)

Security/privacy/accessibility/performance reviews, backups/restore, monitoring, store preparation, operational runbooks, and staged production release. Production deployment always requires explicit authorized approval.

## Company SDLC

### Branch and review flow

- `feature/*`, `bugfix/*`, `hotfix/*` short-lived branches; pull request and passing gates required.
- `develop` integrates changes and promotes to `dev`.
- `release/*` or a versioned release candidate promotes to `stag`.
- Protected `main` accepts reviewed, CI-passing changes; production only from an approved version tag/commit.
- Do not commit directly to `main`. Remote branch protections and reviewer settings require repository-owner access; they have not been configured here.

### Environments and promotion

Persistent environments are only `dev`, `stag`, and `prod`, isolated across database, identity, storage, queues, credentials, logs, and deployment roles. CI may create ephemeral test resources but not a fourth persistent application environment. Use synthetic/seed data in non-production; never copy production personal data.

Build once, deploy to `dev`, promote the same immutable image/artifact to `stag`, complete regression/product acceptance, then promote that same digest to `prod` after explicit approval. No hosted environment has been provisioned by this foundation.

### CI gates

Pull requests should run Android formatting/lint/unit tests/build; backend format/lint/type checks/tests/API contract checks; isolated PostgreSQL integration checks; dependency/container/secret/IaC scans; Terraform validation/plan review; and migration compatibility checks as those pieces are introduced. CI should publish artifact identity/SBOM where supported. The initial workflow is a scaffold; validate and harden it after the required toolchain and repository settings are available.

### Infrastructure and operations

Terraform modules and environment-specific configuration go through reviewed pull requests. Store state remotely, encrypted, versioned, and locked; restrict state access. Production applies need approval. Add per-environment budgets, cost alerts, backups/restore drills, dashboards, alarms, health checks, release records, and forward-fix/rollback runbooks before release.

## Per-phase exit checklist

For each phase, review scope and open dependencies; implement; run relevant build, lint, unit, integration, UI, security, migration, and accessibility checks; record pass/fail and limitations; update API/DB/docs. Production promotion and external provider use still require their own approvals. Never mark provider-dependent functionality complete without real approved credentials and a successful integration path.
