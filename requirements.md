# Bridge — Complete Product & Engineering Requirements
## Codex implementation specification for the native Android application

**Version:** 1.0.0  
**Product:** Bridge  
**Platform:** Native Android application (Kotlin + Jetpack Compose); backend and cloud infrastructure as specified below  
**Audience:** Codex / AI coding agent, Android and backend engineers  
**Status:** Approved baseline requirements for implementation  
**Visual reference:** `SignalLoop Professional Networking Flow.png` supplied by the product owner. Use the flow and screen concepts as inspiration; the product name in the implementation must be **Bridge**.

---

## Version history

| Version | Date | Changes |
|---|---|---|
| 1.0.0 | 2026-11-04 | Baseline Bridge requirements aligned to a company-style SDLC with isolated `dev`, `stag`, and `prod` environments, controlled promotion, CI/CD gates, Terraform and production release approvals. |

# 0. Instructions to the coding agent

You are building **Bridge**, a native Android application for an exclusive, trust-oriented professional network. This file is the implementation source of truth.

## 0.1 Required working method

Before implementation:

1. Inspect the repository, existing code, project conventions, build configuration, and available assets.
2. Do not overwrite or remove working code without understanding it.
3. If the repository is empty, create the project structure in this document.
4. Create and maintain:
   - `docs/architecture.md`
   - `docs/database-schema.md`
   - `docs/api-contract.md`
   - `docs/implementation-plan.md`
   - `docs/assumptions-and-decisions.md`
   - `docs/provider-integration-limitations.md`
5. Break implementation into the phases in Section 24.
6. At the end of each phase, run builds, lint, tests, and report what is complete, incomplete, mocked, or blocked by external provider approval.

## 0.2 Non-negotiable implementation rules

- Build a **native Android app**, not a website, PWA, or WebView wrapper.
- Build real backend APIs and persistent storage. Do not implement the product as static screens or local-only mock interactions.
- Use demo/seed data only in development. Label demo accounts and demo values clearly.
- Enforce authorization, privacy, membership, quotas, and business rules on the backend. Client-side validation is additional UX only.
- Do not scrape LinkedIn or use unofficial scraping libraries. Use official, approved OAuth/API access. Implement a fallback when full profile access is unavailable.
- Do not claim that OAuth login verifies a person's employment, identity, or professional history.
- Keep Bridge Score calculation logic private. Do not expose formulas, weights, thresholds, internal dimensions, or source contributions in public/member APIs.
- Never show invented score percentiles or claims such as “Top 1%” without a valid, auditable population and calculation.
- Keep resumes and imported raw data private. Never make a resume public by default.
- Do not allow users to see private messages, private Pod content, private Cohort content, or hidden profile fields.
- Add loading, empty, offline, error, retry, and success states to all network-backed screens.
- Use accessible components, TalkBack labels, scalable text, and minimum 48dp touch targets.
- Keep secrets out of source control. Provide `.env.example`.
- If a third-party API cannot provide a field, do not silently fabricate it. Provide manual entry or a supported file-import fallback.
- Prefer a modular monolith for the first release. Do not introduce microservices without a demonstrated need.

## 0.3 Product terminology

Use these exact names throughout the app:

- **Bridge Score** — the member's private-to-them professional profile evidence score.
- **Bridge Passport** — the member's professional profile card/passport.
- **Bridge Discover** — people discovery using professional interests, topics, skills, and intents.
- **Bridge Connections** — accepted professional relationships.
- **Bridge Chat** — direct messages between accepted connections.
- **Bridge Cohorts** — larger topic-based professional communities.
- **Bridge Pods** — small, private, member-created groups; default maximum 10 people.
- **Ask for Referral** — referral request workflow with a maximum of 10 submitted requests per member account lifetime in MVP.

Do not use “Signal Score”, “Signal Pods”, or “SignalLoop” in user-facing UI or new code except when referring to the supplied visual reference or migration notes.

---

# 1. Product definition

Bridge is an exclusive professional networking and mentorship platform that helps people connect with relevant, credible professionals around a shared professional goal.

It should not feel like a generic social-media feed or a copy of LinkedIn. The core product loop is:

**Sign up → Import and review professional profile → Calculate Bridge Score → Set intents → Discover relevant professionals → Connect → Chat → Join Cohorts / create Pods → Ask for referrals**

## 1.1 Product promise

**Trusted professionals. Relevant connections. Meaningful progress.**

Bridge helps members:
- Present a professional profile supported by real evidence.
- Find people who share a current goal or have complementary expertise.
- Build one-to-one professional relationships.
- Participate in larger professional communities.
- Create focused private groups of up to 10 people.
- Ask relevant connections for referrals without enabling mass spam.

## 1.2 Product principles

1. **Trust over volume:** quality and relevance matter more than raw member counts.
2. **Intent over vanity:** connect around what people are trying to do now.
3. **Evidence, not assumptions:** imported data and member claims must be distinguishable.
4. **Privacy by default:** sensitive data and private spaces remain private.
5. **Human agency:** recommendations assist members; they do not make career decisions for them.
6. **No false guarantees:** a Bridge Score does not guarantee professional quality or a referral.
7. **Safety and accountability:** blocking, reporting, moderation, and audit trails are foundational.

## 1.3 Initial personas

| Persona | Typical goal | Main Bridge value |
|---|---|---|
| Engineer preparing for a switch | Interview peers, mentors, referrals | Discover, Chat, Pods, Referral |
| Senior engineer / mentor | Help peers, share expertise | Discover, Cohorts, Pods, Mentorship intent |
| Founder / builder | Find co-founders and collaborators | Discover, Cohorts, Pods |
| Product / design professional | Learn, collaborate, grow network | Discover, Cohorts, Pods |
| Hiring professional | Find relevant talent | Verified profiles and intent search |

The product must not assume that every member is a software engineer. The initial intent catalog may be technology-heavy, but the data model and UI must support other professions.

---

# 2. Scope and release boundaries

## 2.1 MVP scope

The MVP must include:

- Google sign-in and email authentication.
- GitHub OAuth connection and supported public profile import.
- LinkedIn connection only where official API access is approved; otherwise provide manual profile entry and supported LinkedIn data-export/PDF fallback.
- Resume upload and parsing for PDF/DOCX.
- Profile review, editing, privacy controls, and Bridge Passport.
- Server-side Bridge Score.
- Intent catalog and selection (maximum 3 active intents by default).
- Discover people search, filters, recommendations, and profile views.
- Connection request lifecycle.
- One-to-one chat after connection acceptance.
- Bridge Cohort directory and membership.
- Bridge Pod creation, invitations, private discussion/chat, and member management.
- Ask for Referral with a strict 10-request lifetime quota.
- In-app and Android push notifications.
- Blocking, reporting, moderation, and admin operations.
- Settings, data export request, account deletion request.

## 2.2 Explicitly out of scope for MVP

Unless the product owner later asks for them, do not build:
- iOS app.
- Public website or web app.
- Job board or ATS.
- Automated job applications.
- Automatic referral submission to an employer.
- Paid mentorship marketplace or payment processing.
- Subscription plans.
- Voice/video calling.
- Livestreaming.
- AI chatbot mentor.
- Public follower/following feed.
- Public leaderboard or unsubstantiated score percentile.
- LinkedIn scraping.
- Automatic private GitHub repository analysis without explicit scope and consent.
- Real-time collaborative whiteboard in the first MVP. Design the Pod architecture so it can be added later.

---

# 3. Technology stack

## 3.1 Android application

| Concern | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Design system | Material 3 |
| Architecture | MVVM with Clean Architecture boundaries |
| Navigation | Navigation Compose |
| Dependency injection | Hilt |
| Networking | Retrofit + OkHttp |
| Serialization | Kotlinx Serialization |
| Async/state | Kotlin Coroutines + Flow / StateFlow |
| Local database/cache | Room |
| Preferences | DataStore |
| Image loading | Coil |
| Background tasks | WorkManager |
| Push notifications | Firebase Cloud Messaging |
| Secure token storage | Android Keystore-backed storage |
| Unit tests | JUnit, MockK, Turbine |
| UI tests | Compose UI Test |
| Build | Gradle Kotlin DSL + version catalog |

Android requirements:
- Single Activity with Compose navigation.
- Minimum SDK: API 26 unless repository/device data justifies another choice.
- Target the current required Play Store API level at release time.
- Support light and dark themes.
- Use design tokens and reusable components.
- ViewModels expose immutable screen state.
- No business logic, network requests, or persistence logic inside Composables.
- Use WorkManager for retryable uploads/sync tasks.
- Cache only data that is safe to retain locally.
- Clear account-specific cache and tokens on logout/account switch.
- Support Android back gesture and system font scaling.
- Handle process death and configuration changes.

Suggested Android modules:

```text
android/
  app/
  core/
    common/
    designsystem/
    model/
    network/
    database/
    datastore/
    navigation/
    analytics/
  feature/
    auth/
    onboarding/
    profile/
    bridge-score/
    intents/
    discover/
    connections/
    chat/
    cohorts/
    pods/
    referrals/
    notifications/
    settings/
```

## 3.2 Backend

| Concern | Technology |
|---|---|
| Runtime | Python 3.12+ |
| Framework | FastAPI |
| API | REST `/api/v1` |
| API docs | FastAPI OpenAPI / Swagger UI |
| Primary database | PostgreSQL |
| ORM and migrations | SQLAlchemy 2.x + Alembic |
| Cache and rate limiting | PostgreSQL-backed limits initially; managed Redis only when justified |
| Background jobs | Managed queue (Amazon SQS) + Python worker in AWS deployment; lightweight DB-backed jobs for local/MVP where appropriate |
| Realtime messaging | FastAPI WebSockets; managed realtime service or API Gateway WebSocket at scale |
| File storage | Private S3-compatible object storage |
| Search (MVP) | PostgreSQL full-text search + indexed filters |
| Push | Firebase Cloud Messaging |
| Email | SES / Resend behind an adapter |
| Validation | Pydantic v2 at API boundaries |
| Tests | Pytest + HTTPX |
| Observability | Sentry, structured logs, OpenTelemetry where practical |
| Deployment | Docker image; managed container hosting initially, AWS App Runner or ECS Fargate for AWS production |
| CI/CD | GitHub Actions |

Use a **modular monolith** with separate Python packages and clear boundaries. Split into services only when traffic, ownership, or operational needs justify it.

Suggested backend modules:

```text
backend/
  app/
    main.py
    api/v1/
    core/                 # settings, security, logging, exceptions
    db/                   # SQLAlchemy session, models, repositories
    modules/
      auth/
      users/
      identities/
      profiles/
      provider_integrations/
      resume/
      verification/
      bridge_score/
      intents/
      discover/
      connections/
      conversations/
      messages/
      cohorts/
      pods/
      referrals/
      notifications/
      moderation/
      privacy/
      admin/
      analytics/
    workers/              # independently runnable async workers
  alembic/
  tests/
  pyproject.toml
  Dockerfile
```

## 3.3 Infrastructure and environments

- Use a managed PostgreSQL service for shared development/staging/production. For local development, use SQLite only for isolated unit tests or a local PostgreSQL container if needed; Redis is not required.
- Separate development, staging, and production environments.
- Private object storage for resumes and chat attachments.
- Firebase project for push notifications.
- Managed secrets in production.
- Database backups and tested restore process.
- Health/readiness endpoints.
- CI checks for format, lint, type checks, tests, migrations, and builds.


## 3.4 Production-ready backend and managed-service decisions

The implementation must be deployable without the product owner installing or operating database, cache, queue, or storage servers.

### Default MVP / first production deployment

Use this concrete, low-operations baseline unless the repository already has an approved provider:

| Capability | Default choice | Production requirement |
|---|---|---|
| Python API hosting | Managed container hosting (AWS App Runner is preferred for AWS-first deployment; an equivalent managed container host is acceptable) | Deploy immutable Docker images; HTTPS; health checks; separate staging and production |
| Relational database | Supabase managed PostgreSQL for quickest MVP, or Amazon RDS PostgreSQL if the project is AWS-first | Private credentials; automated backups; point-in-time recovery where available; TLS; migrations run as a controlled release step |
| Authentication | Supabase Auth for MVP, or Amazon Cognito for AWS-first deployments | Backend validates JWT signature, issuer, audience, expiry and claims; authorization remains in FastAPI |
| File storage | Private Amazon S3 bucket | Block public access; encryption; short-lived presigned URLs; malware/file validation; lifecycle and retention rules |
| Android push | Firebase Cloud Messaging | Store device tokens securely; handle token rotation and invalidation |
| Email | Amazon SES or a transactional email provider behind an adapter | Verified sender/domain, SPF, DKIM, DMARC, bounce/complaint handling |
| Background jobs | Amazon SQS + Python worker when async workloads are introduced | Retry policy, dead-letter queue, idempotent handlers, visibility timeout and monitoring |
| Cache | None initially | Add managed Redis only for measured caching, distributed throttling, presence, or fan-out requirements |
| Observability | Structured application logs + Sentry; CloudWatch if hosted on AWS | Correlation IDs, error alerts, health/readiness checks, no secrets or sensitive profile/message content in logs |
| CI/CD | GitHub Actions | Tests and security checks on pull requests; deploy staging automatically; production deploy gated by approval |

### Provider portability

- Define interfaces for authentication verification, object storage, email, push, and job enqueueing.
- Keep provider-specific SDK calls inside adapters; feature modules must depend on interfaces.
- Keep business authorization and all Bridge business rules in FastAPI, not in Supabase Row Level Security policies alone or in Android.
- Use standard PostgreSQL features and avoid vendor-specific database features unless documented.
- Store provider identifiers separately from Bridge user IDs.
- Document migration and export paths for each managed provider.
- Do not add Supabase Edge Functions, Supabase Realtime, or provider-specific business logic unless there is a documented reason. FastAPI remains the application API and source of truth.

### When to add optional infrastructure

- Add Redis only after measuring a need for shared cache, distributed rate limits, presence, or high-volume fan-out. Do not make Redis a local-development prerequisite.
- Add SQS and a separate worker for resume parsing, score recalculation, email/push fan-out, and other retryable jobs. Do not perform long-running parsing inside a request handler.
- Use PostgreSQL full-text search and indexes for MVP Discover. Consider OpenSearch only after search relevance, volume, or filtering requirements outgrow PostgreSQL.
- Do not use Kubernetes, microservices, Kafka, or a service mesh for the MVP.
- Use a modular monolith and one deployable API container plus an optional worker container.

### Environment separation

### SDLC environments and deployment lifecycle (mandatory)

Bridge must use three persistent, isolated application environments that mirror a conventional company SDLC:

| Environment | Purpose | Stability | Data | Deployment source |
|---|---|---|---|---|
| `dev` | Active engineering development and integration | May change frequently | Synthetic/seed data only; never production personal data | Feature branches / merges to `develop` |
| `stag` | QA, product acceptance, regression, security and release-candidate testing | Release-candidate quality; changes only through controlled deployments | Synthetic or explicitly approved masked test data | Successful build from `develop` or a release candidate |
| `prod` | Live customer-facing production application | Highest stability; controlled releases only | Real customer data | Approved release tag / approved promotion of a tested artifact |

These are the only three long-lived application environments. CI may create short-lived test containers or ephemeral test databases, but do not create a fourth persistent `test` application environment.

#### Environment isolation

Each environment must have its own:
- AWS deployment boundary. Prefer separate AWS accounts for `dev`, `stag`, and `prod` under AWS Organizations. If the owner initially uses one account, isolate with separate VPCs, resource names, IAM roles, secrets, buckets, databases, queues, and deployment permissions; document the temporary exception and migration plan.
- ECS services/tasks, ECR image tags or immutable image references, load balancer routing, security groups and environment configuration.
- RDS database/cluster and database credentials. No shared database across environments.
- S3 buckets or rigorously isolated bucket prefixes with separate IAM policies; separate buckets are preferred.
- SQS queues and dead-letter queues.
- Cognito user pool / app clients and OAuth callback URLs.
- Firebase/FCM configuration and APNs configuration where applicable. Never send dev/stag notifications to production users.
- Secrets Manager paths and KMS encryption context/keys as appropriate.
- CloudWatch log groups, dashboards, alarms and retention policies.
- DNS names and TLS certificates, for example `api-dev`, `api-stag`, and `api` under the chosen Bridge domain.
- Analytics projects/streams and crash-reporting environment labels.

Use environment-specific IAM roles and least privilege. The `dev` deployment identity must not be able to deploy to `prod`; production access must be restricted to explicitly authorized maintainers and protected CI/CD roles. Do not copy production data into `dev` or `stag`. If production-like data is required for testing, use an approved anonymization/masking process and obtain the required authorization.

#### Branching and promotion model

Use a simple, documented Git workflow:
- `feature/*` and `bugfix/*`: short-lived branches; pull request required.
- `develop`: integration branch; successful CI deploys to `dev`.
- `release/*` or a versioned release candidate: stabilization branch/tag; deploys to `stag`.
- `main`: protected production-ready branch; production deployment occurs only from an approved, versioned release tag or approved commit on `main`.
- `hotfix/*`: urgent production fix; must still pass CI and be deployed to `stag` for focused validation where practical before production.

Require pull-request review, passing automated checks, and protected branches. Do not commit directly to `main`. The exact number of required reviewers may be configured by the repository owner, but production changes must have an auditable approval.

#### CI/CD gates

Every pull request must run, as applicable:
1. Kotlin/Android formatting, lint, unit tests and build.
2. Python formatting, lint, type checks, unit tests and API contract checks.
3. Backend integration tests against an isolated test database.
4. Dependency and container vulnerability scanning.
5. Secret scanning and IaC scanning.
6. Terraform format and validation; plan generation for infrastructure changes.
7. Migration validation and backward-compatibility checks.
8. Build artifact creation and metadata/SBOM generation where supported.

Deployment pipeline:
- Merge to `develop` → build once → deploy to `dev` → smoke tests.
- Promote the same immutable artifact/image digest to `stag` → automated regression and integration tests → QA/product acceptance.
- Promote that exact tested artifact/image digest to `prod` only after explicit production approval and release checklist completion.
- Do not rebuild a different image for production from the same source commit; promote the already-tested immutable artifact.
- Use GitHub Actions with environment protection rules, scoped OIDC federation to AWS where supported, and no long-lived AWS access keys in repository secrets.
- Store deployment records: commit SHA, image digest, release version, environment, migration version, approver, timestamp and outcome.

#### Application configuration and naming

- Configuration must be injected at runtime; do not bake secrets or environment-specific endpoints into the application image.
- Maintain validated environment templates such as `.env.example` for local development only. Never commit actual `.env` files.
- Use explicit environment identifiers: `dev`, `stag`, `prod`.
- Android must use separate Gradle product flavors/build variants (`dev`, `stag`, `prod`) with distinct application IDs, app names or labels, API base URLs, Firebase configurations and signing arrangements. Production signing material must never be available to ordinary dev builds.
- API responses, logs and telemetry must carry the environment label.
- CORS, OAuth redirect URIs, callback URLs, allowed origins, push credentials and deep links must be environment-specific.
- Production must reject demo accounts, seed endpoints, debug menus, mock providers and development-only feature flags.

#### Database migration and release safety

- Run Alembic migrations as a controlled one-off release task, not independently from every API replica at startup.
- Use backward-compatible expand-and-contract migrations: add compatible schema first, deploy code that supports both states, backfill if required, then remove obsolete schema in a later release.
- Take/verify backups before risky production migrations and define a recovery plan.
- Never automatically downgrade production schema as a generic rollback. Prefer forward-fix migrations; document when a restore is required.
- Application rollback must remain compatible with the database schema during the rollback window.
- Run post-deployment health checks and smoke tests before marking a release successful.

#### Environment release gates

`dev` exit gate:
- CI passes; API and mobile builds install and launch; core integration smoke tests pass; no unresolved blocker-level defects.

`stag` exit gate:
- QA regression passes for authentication, onboarding, profile, Discover, connections, chat, Cohorts, Pods, referrals, notifications, privacy and moderation.
- No open release-blocking defects; test evidence and product acceptance are recorded.
- Security, migration, performance and operational checks relevant to the release are complete.

`prod` release gate:
- Explicit authorized approval.
- Tested artifact digest matches the staging-approved artifact.
- Production configuration, secrets, DNS/TLS, backups, alarms, dashboards, WAF, rate limits and rollback plan are verified.
- Database migration has been reviewed.
- Store submission/release checks and mobile signing are complete where applicable.
- Post-deployment smoke tests pass; release is monitored with a defined rollback/forward-fix decision owner.

#### Infrastructure as Code

- Provision and update AWS infrastructure through Terraform modules and reviewed pull requests.
- Keep environment-specific variable files separate; do not duplicate large Terraform modules per environment.
- Store Terraform state remotely in an encrypted, versioned S3 backend with locking enabled using the supported Terraform backend locking mechanism. Restrict state access because it can contain sensitive metadata.
- CI must run `terraform fmt -check`, `terraform validate`, security/IaC scans and generate a plan for review.
- `terraform apply` against `stag` and especially `prod` requires an authorized approval. Do not run destructive changes automatically.
- Document resource naming, tagging, ownership, environment, cost center and retention.
- Create AWS Budgets and cost anomaly alerts per environment. Keep dev resources right-sized and schedule nonessential dev resources to stop when appropriate; do not compromise production availability.

#### AWS deployment baseline

For the AWS-first production architecture, use ECS Fargate + ECR + RDS PostgreSQL + S3 + Cognito + SQS + CloudWatch, with VPC/private networking, IAM, Secrets Manager, KMS, Route 53, ACM, ALB and WAF as appropriate. Use AWS region `ap-south-1` (Mumbai) by default unless the product owner approves another region based on data residency, service availability, latency or cost. Redis/ElastiCache and Kafka/MSK remain optional and must not be provisioned by default for MVP.

The same architecture pattern must be deployable to all three environments, with production-grade sizing and availability controls applied to `prod`. Development and staging may use smaller managed resource sizes, but must retain equivalent security boundaries and deployment behavior.

Create isolated `dev`, `stag`, and `prod` hosted configurations. Local developer tooling may be used to run code and isolated tests, but it is not a shared application environment. Production must never use development OAuth clients, fake provider data, demo users, local file storage, or permissive CORS. Use separate provider credentials and storage buckets per environment.

---

# 4. Global user journey and navigation

## 4.1 First-time journey

1. User opens Bridge.
2. User sees the welcome/onboarding value proposition.
3. User signs in using Google, GitHub, LinkedIn (if approved), or email.
4. User accepts Terms and Privacy Policy.
5. User chooses profile sources to connect/import.
6. Bridge imports available data and parses the resume in the background.
7. User reviews and edits the imported profile.
8. Bridge calculates the Bridge Score.
9. User sees the score and Bridge Passport preview.
10. User selects up to three active intents.
11. User lands on Bridge Discover.
12. User views relevant professionals and sends connection requests.
13. Once a request is accepted, the members can chat.
14. Members can join Cohorts, create Pods, and ask for referrals.

Do not force the user to connect every source. Missing a source must not block account creation or basic participation.

## 4.2 Main bottom navigation

Use five primary destinations:

1. **Discover**
2. **Cohorts**
3. **Pods**
4. **Chats**
5. **Profile**

Notifications are accessible from a bell icon in the top app bar. Referral management is accessible from Profile and relevant member profiles.

## 4.3 Global UI states

Every remote screen must support:
- Initial loading.
- Pull-to-refresh where appropriate.
- Empty state with a useful next action.
- Offline state with cached content if available.
- Retryable error.
- Permission/authorization error.
- Success feedback.
- Pagination/loading-more state.

---

# 5. Authentication and account management

## 5.1 Sign-in options

- Continue with Google.
- Continue with GitHub.
- Continue with LinkedIn only if the relevant OAuth product and scopes are approved.
- Continue with email/password or passwordless email link (choose one and document the implementation).

Google sign-in should use the recommended native Android Google Identity / Credential Manager flow.

GitHub and LinkedIn are provider connections as well as possible sign-in methods only when supported OAuth configuration is implemented. Keep authentication identity separate from profile import consent.

**Gmail means Google sign-in. Do not create a separate Gmail provider.**

## 5.2 Account linking

- One Bridge account may link multiple providers.
- Prevent duplicate accounts using verified email and provider subject IDs.
- If an email is already used, provide a safe account-linking flow after reauthentication.
- Do not merge accounts automatically based only on an unverified email.
- Store provider access/refresh tokens encrypted server-side.
- Never send provider tokens to the Android app after exchange unless strictly required by the OAuth flow.
- Support disconnecting a provider.
- Disconnecting a provider revokes/deletes stored tokens where supported.

## 5.3 Session handling

- Access token + refresh token/session rotation.
- Revoke refresh sessions on logout.
- User can view active sessions/devices and revoke a session.
- Expired sessions route to sign-in without losing safe draft data.
- Rate-limit login, registration, email verification, and password reset.

## 5.4 Legal consent

Before importing profile data:
- Show Terms of Service and Privacy Policy.
- Record policy version, acceptance timestamp, and account ID.
- Obtain separate consent for LinkedIn, GitHub, and resume processing.
- Explain what data is imported, why, and how it is used.
- Allow provider disconnection and data deletion requests.

## 5.5 Acceptance criteria

- User can register and sign in with configured methods.
- User can link multiple providers to one Bridge account.
- Duplicate identities are handled safely.
- Provider login is not displayed as employment verification.
- User can revoke sessions and sign out.
- No credentials or tokens are stored in plaintext.

---

# 6. Profile import and onboarding

## 6.1 Profile analysis screen

After authentication, show a screen titled **“Build your Bridge profile”** or **“Analyzing your profile”**.

Show three source cards:

1. LinkedIn
2. GitHub
3. Resume

Each card shows:
- Not connected / Ready to connect.
- Connecting.
- Analyzing.
- Completed.
- Failed with reason and retry.
- Last synced date.
- Disconnect/remove action after connection.

A user may skip any source and continue with manual profile entry.

## 6.2 LinkedIn import

Only use official LinkedIn OAuth/API products and scopes for which the app has approval.

Potential fields, only if returned by approved APIs:
- Name.
- Profile image.
- Headline.
- Current/past positions.
- Company names.
- Employment dates.
- Education.
- Public profile URL.
- Other approved fields.

Important limitations:
- Standard LinkedIn API access generally does not grant unrestricted full profile or career-history access.
- Do not promise full work-history import until partner/API access is approved.
- Provide fallback options:
  - User manually enters experience.
  - User uploads a LinkedIn-exported PDF or data archive if supported.
  - User uploads a current resume.
- Do not scrape LinkedIn HTML or ask users for their LinkedIn password.

## 6.3 GitHub import

With explicit authorization, import permitted data:
- GitHub username and profile URL.
- Avatar and public bio.
- Public repositories.
- Repository descriptions and topics.
- Primary languages.
- Stars and forks.
- Public contribution/activity summaries where available.
- Public pull requests and contribution information where API permits.

Private repository data:
- Excluded by default.
- If ever supported, request separate explicit consent and least-privilege scopes.
- Let the user choose which projects to display.
- Never publish private code, repository names, or private activity.

Do not equate raw commit count or stars with engineering ability.

## 6.4 Resume upload and parsing

- Accept PDF and DOCX.
- Maximum file size: 5 MB for MVP (configurable).
- Validate file signature/MIME type, extension, and size.
- Upload through short-lived pre-signed URL.
- Store in private object storage.
- Parse asynchronously.
- Extract candidate fields:
  - Name and contact information.
  - Summary.
  - Work experience.
  - Skills.
  - Education.
  - Projects.
  - Achievements.
  - Certifications.
  - Patents/publications where present.
- Parsing results are suggestions, not verified facts.
- Show a review/edit screen before profile save.
- Keep original resume private.
- Allow replace and delete.
- Do not send resume content to analytics or application logs.

## 6.5 Profile review

User reviews and edits:
- Full name.
- Profile image.
- Professional headline.
- Current role and company.
- Work history.
- Years of experience (or range).
- Education.
- Skills.
- Professional summary.
- Projects.
- Achievements.
- Certifications.
- Public portfolio links.
- LinkedIn/GitHub URLs.
- Location/time zone (optional).

Show imported values with a subtle “Imported from …” source label. Manual edits must be possible.

## 6.6 Profile data provenance

For each profile field, store:
- Value.
- Source type: `LINKEDIN`, `GITHUB`, `RESUME`, `MANUAL`.
- Import timestamp.
- User-confirmed flag.
- Visibility.
- Optional source reference.

Do not mark a field verified simply because it was parsed from a resume.

## 6.7 Acceptance criteria

- User can import one, multiple, or zero external sources.
- Resume parsing is asynchronous and retryable.
- User can edit every parsed field.
- User can disconnect a source and request deletion of imported source data.
- Profile is not published until the user confirms it.
- Private documents are not exposed to other members.

---

# 7. Bridge Score and Bridge Passport

## 7.1 Purpose and wording

Bridge Score is a score from 0–100 representing the completeness and strength of the professional evidence available in a member's Bridge profile.

It is **not**:
- A measure of personal worth.
- A guarantee of job performance.
- A guarantee of trustworthiness.
- A guarantee of hiring success.
- A ranking of human value.

Use the name **Bridge Score** in all screens and APIs.

## 7.2 Internal source-specific rubric

The backend must maintain independent internal rubrics for:

### LinkedIn evidence
Possible internal signals:
- Profile completeness.
- Employment history completeness.
- Role/title information.
- Career history consistency.
- Skills and professional summary.
- Education/certification information when available.

### GitHub evidence
Possible internal signals:
- Profile completeness.
- Project/repository documentation.
- Technology relevance.
- Meaningful public project activity.
- Contribution patterns where supportable.
- Evidence of project ownership/collaboration where supportable.

### Resume evidence
Possible internal signals:
- Work history clarity and completeness.
- Skills and role consistency.
- Project and achievement descriptions.
- Evidence of impact.
- Certifications/education.
- Parsing confidence and data consistency.

Do not use raw counts as a sole measure. Do not reward only prestigious employers, elite universities, high GitHub activity, patents, or a particular career path.

## 7.3 Confidentiality of scoring logic

The following must remain server-side:
- Source weights.
- Formula.
- Thresholds.
- Internal feature list.
- Feature contributions.
- Ranking logic.
- Calibration data.
- Anti-gaming rules.

Never include these in:
- Member-facing API responses.
- Public profile responses.
- UI help text.
- Logs accessible to members.
- Public docs.
- Client-side source code.

The member-facing response may include:
- Final score.
- Score status.
- Last calculated timestamp.
- High-level improvement suggestions.
- A safe source status (e.g. connected, unavailable, needs review), if appropriate.

Do not expose per-source numeric scores or detailed dimension values in MVP. The product owner explicitly wants the calculation method hidden.

## 7.4 Calculation lifecycle

- Score calculation runs on the backend.
- Use a background queue for parsing and expensive evaluation.
- Score states:
  - `NOT_READY`
  - `PROCESSING`
  - `CALCULATED`
  - `NEEDS_REVIEW`
  - `FAILED`
- Do not show a numeric score until enough data exists.
- If no usable evidence exists, show a helpful empty state.
- Recalculate when relevant source/profile data changes.
- Store model version and calculation timestamp.
- Keep a score history.
- Avoid frequent score fluctuations; use a documented recalculation policy.
- User can request a score review if source data was misread.
- Admin score review actions must be audited.

## 7.5 Fairness and score safeguards

- Do not use protected characteristics (e.g. gender, age, ethnicity, religion) as scoring inputs.
- Do not use precise location as a score input.
- Missing GitHub must not automatically imply poor professional ability.
- Do not penalize a user for not having patents, public repositories, or a specific employer.
- Audit for systematic unfairness before public launch.
- Score should be described as evidence/profile strength, not professional merit in an absolute sense.
- Do not show “Top N%” in MVP. Add only after a defensible verified-member population and methodology exist.

## 7.6 Bridge Passport

The Bridge Passport is the member's shareable professional profile.

Member profile screen:
- Bridge Score ring at the top (owner always sees their own score).
- Score state and last-updated date.
- Verification badge/status shown separately.
- Name and professional headline.
- Current role/company if visible.
- Years of experience or range.
- Up to six selected skills.
- Professional summary.
- Work history.
- Education.
- Projects and achievements.
- Selected public links.
- Active intents if the member chooses to show them.

Privacy per field:
- `PUBLIC_TO_BRIDGE`
- `CONNECTIONS_ONLY`
- `PRIVATE`

The member controls score visibility separately.

## 7.7 Share Passport

- Android share sheet.
- Create a revocable public share link only after explicit user action.
- User can set expiry or revoke the link.
- Public link shows only fields explicitly marked shareable.
- Do not expose resume, email, phone, private notes, or hidden fields.
- Public link access must be rate-limited and auditable.

## 7.8 Acceptance criteria

- Score is calculated server-side.
- Calculation logic is not available in client or public API.
- Score state and history are persisted.
- User can request review.
- Bridge Passport respects field-level visibility.
- Sharing can be disabled immediately.

---

# 8. Intents and hashtag catalog

## 8.1 Purpose

An intent describes what a member currently wants to achieve or offer. Intents power Discover matching and help other members understand why a connection may be relevant.

## 8.2 Intent selection

- Default maximum: 3 active intents.
- The limit must be a backend-configurable policy.
- User may select from categories or search the catalog.
- Show selection counter, e.g. `2/3`.
- Save disabled if no intent is selected, unless the user explicitly skips.
- Intents expire after 30 days by default.
- Notify the user before expiry and allow renewal/change.
- User can pause or remove an intent.
- User can suggest a new intent; admin approves/rejects.

## 8.3 Initial catalog

Career:
- `#JobSwitch`
- `#CareerGrowth`
- `#Promotion`
- `#Leadership`
- `#CareerTransition`

Interview preparation:
- `#InterviewPrep`
- `#SystemDesign`
- `#DSA`
- `#FrontendInterview`
- `#BackendInterview`
- `#BehavioralInterview`

Learning:
- `#LearnNewTech`
- `#LearnReact`
- `#LearnAI`
- `#Cloud`
- `#ProductManagement`

Building:
- `#FoundersSeekingCTO`
- `#CoFounder`
- `#SideProject`
- `#OpenSource`
- `#Collaboration`

Mentorship:
- `#SeekingMentor`
- `#OfferingMentorship`
- `#PeerLearning`

Opportunities:
- `#Hiring`
- `#Freelance`
- `#SeekingReferral`
- `#AngelNetwork`

## 8.4 Intent direction

Each selection includes:
- `SEEKING`
- `OFFERING`
- `BOTH`

Optional context:
- Short description.
- Preferred seniority/experience.
- Expiry date.
- Visibility setting.

Example:
- `#SystemDesign — SEEKING`
- `#Mentorship — OFFERING`

This distinction is important for complementary matching.

## 8.5 Privacy

- Intents are private by default during onboarding.
- User can choose which intents are shown on Bridge Passport.
- Discover matching may use active intents even if not displayed publicly, but do not reveal private intent text or context. Show only a generic match reason when needed.
- User can pause Discover visibility.

## 8.6 Acceptance criteria

- Backend enforces maximum active intents.
- Expiry and renewal work.
- Admin can manage catalog and aliases.
- Matching respects direction and privacy.
- User can suggest new tags.

---

# 9. Bridge Discover — professional people discovery

## 9.1 Definition

Discover is where members find relevant professionals based on topics, hashtags, skills, roles, experience, and current professional goals.

Discover is **not a generic social feed**. It is a people-first discovery experience.

Examples:
- A member selecting `#SystemDesign` can discover peers preparing for interviews and experienced professionals offering guidance.
- A member selecting `#JobSwitch` can find peers also switching jobs and relevant people willing to help.
- A founder selecting `#FoundersSeekingCTO` can find engineers and potential collaborators.
- A member selecting `#SeekingMentor` can discover relevant mentors.

## 9.2 Discover home

Top app bar:
- Bridge branding.
- Search field: “Search people, skills, companies or topics”.
- Filter icon.
- Notification icon.

Content:
- `For You` tab.
- One tab/filter for each active intent.
- Recommended professionals.
- People with similar goals.
- Potential mentors / people offering relevant expertise (only where matching intent supports this).
- Recently active relevant members (optional; respect presence/privacy settings).

## 9.3 Professional member card

Each card may show:
- Avatar.
- Name.
- Professional headline.
- Current company/role if public.
- Experience range if public.
- Verification status.
- Bridge Score ring only if owner has enabled score visibility.
- Up to three public intent chips.
- Up to three relevant skills.
- Short match explanation.
- `View Profile`.
- `Connect`.

Match explanation examples:
- “You both selected #SystemDesign.”
- “Offers mentorship in a topic you are seeking.”
- “Shares three relevant skills.”
- “Works in a role related to your goal.”

Do not invent match explanations.

## 9.4 Search and filters

Search:
- Name.
- Company.
- Role/title.
- Skill.
- Hashtag/intent.
- Industry.

Filters:
- Intent.
- Skill.
- Experience range.
- Seniority.
- Company.
- Industry.
- Location/time zone only if the member has opted in.
- Availability / offering status.
- Verification state where meaningful.

Use cursor pagination/infinite scroll. Debounce search input. Preserve filters when returning from a profile.

## 9.5 Eligibility and ranking

Only show members who:
- Have an active account.
- Have enabled Discover visibility.
- Meet the platform's published verification eligibility policy.
- Are not blocked by or blocking the current user.
- Are not suspended/restricted.
- Are not the current user.

Important: Do not introduce an arbitrary undisclosed Bridge Score cutoff that silently excludes members. If a minimum eligibility threshold is used, document and audit it. Prefer verification and profile-quality eligibility rather than score-based exclusion.

Ranking may use:
- Shared active intents.
- Complementary `SEEKING` / `OFFERING` intent direction.
- Skill overlap.
- Role/industry relevance.
- Experience compatibility.
- Mutual connections.
- Recent activity.
- User-selected filters.

Do not use protected/sensitive characteristics.

## 9.6 Member actions

- View profile.
- Connect with optional note.
- Mark “Not interested” to hide a profile for 60 days.
- Block/report.
- Share profile through Android share sheet if allowed.

## 9.7 Acceptance criteria

- Search and filters query backend data.
- Results paginate.
- Match reason is grounded in actual matching data.
- Blocked/non-discoverable members are excluded.
- Private fields are never returned.
- Empty state suggests adding or changing intents.

---

# 10. Profile, verification, and trust indicators

## 10.1 Profile screen

Show:
- Bridge Score (owner view).
- Profile photo and name.
- Headline.
- Current role/company if public.
- Verification badge and scope.
- Professional summary.
- Skills.
- Experience.
- Education.
- Projects and achievements.
- Selected links.
- Active visible intents.
- Connect/message actions depending on relationship state.
- Ask for Referral action only when eligible.

## 10.2 Verification types

Keep these separate:

1. **Account verified** — email/account ownership verified.
2. **Provider connected** — user linked LinkedIn/GitHub.
3. **Professional evidence reviewed** — selected professional claims have been reviewed/validated.
4. **Employment verified** — only when an approved, reliable verification process supports this claim.

OAuth sign-in alone must never produce a “Verified professional” badge.

## 10.3 Verification states

- `NOT_STARTED`
- `IN_REVIEW`
- `NEEDS_INFORMATION`
- `PARTIALLY_VERIFIED`
- `VERIFIED`
- `REJECTED`
- `RECHECK_REQUIRED`

Admin can request additional evidence, approve, or reject with reason. Keep audit history.

## 10.4 Acceptance criteria

- Badge meaning is clear.
- Provider connection is not confused with professional verification.
- Admin actions are audited.
- Profile privacy applies consistently in Discover, chat, and share links.

---

# 11. Connections

## 11.1 Connection request flow

From Discover or a profile:
1. User taps `Connect`.
2. Optional note, maximum 300 characters.
3. Confirm recipient and send.
4. Recipient receives in-app/push notification.
5. Recipient accepts or declines.
6. On acceptance, both become connections and direct chat is enabled.

States:
- `PENDING`
- `ACCEPTED`
- `DECLINED`
- `WITHDRAWN`
- `CANCELLED`

Declines must not send an explicit decline notification to the requester. The requester may see the request as no longer pending without being told the recipient's reason.

## 11.2 Rules

- Prevent duplicate pending requests.
- A member can withdraw a pending request.
- Apply configurable daily outgoing request cap (initial default 20).
- Prevent repeated unwanted requests after decline.
- Blocked users cannot send or receive connection requests.
- User can view incoming, outgoing, and accepted connections.
- Show last interaction only if permitted.

## 11.3 Acceptance criteria

- Request lifecycle works end-to-end.
- Daily cap is enforced server-side.
- Duplicate and blocked requests are rejected.
- Chat becomes available only after acceptance.

---

# 12. Bridge Chat — direct messaging

## 12.1 Access rule

One-to-one chat is available only between accepted connections.

Every REST request and WebSocket subscription must verify:
- Authenticated user.
- Conversation membership.
- Accepted connection state.
- No active block/restriction.

## 12.2 MVP capabilities

- Text messages.
- Emoji reactions.
- Link sharing with safe link preview.
- Message timestamps.
- Conversation list.
- Unread count.
- Read state/receipts (user-configurable).
- Typing indicator (user-configurable).
- Online status (user-configurable; visible only to connections).
- Message pagination.
- Push notifications.
- Report and block.
- Delete message with a clear policy.

Attachments:
- Optional MVP feature if secure upload is implemented.
- Maximum 10 MB per file.
- Allow safe document/image types only.
- Virus/malware scanning where available.
- Private storage and signed download URLs.
- Do not support executable files.

## 12.3 Quick Actions

A Quick Actions panel may contain:

1. **Schedule a 15-minute chat**
   - MVP: share a suggested time or calendar link.
   - Google Calendar integration only after OAuth scopes and consent are configured.
   - Do not create calendar events without explicit confirmation.

2. **Share resources**
   - Share a link or supported attachment.
   - Use the same private attachment rules.

3. **Create a Pod**
   - Start Pod creation with the current connection preselected as an invitee.
   - User can edit invitees before creation.

## 12.4 Message deletion and retention

- Define whether delete means delete-for-me or delete-for-everyone.
- For MVP, support delete-for-everyone only within a documented short window, or use delete-for-me if reliable global deletion is not implemented.
- Keep audit/security records where legally required, without exposing deleted content to members.
- Messages encrypted in transit and at rest.
- Do not claim end-to-end encryption unless implemented and independently reviewed.

## 12.5 Acceptance criteria

- Non-connections cannot start direct messages.
- Message history persists.
- WebSocket joins are authorized.
- Blocking revokes future messaging access.
- Push payloads do not include sensitive message contents.

---

# 13. Bridge Cohorts — larger professional communities

## 13.1 Definition

A Bridge Cohort is a larger community around a professional domain, role, skill, industry, or goal.

Cohorts are intended for broader discovery, discussions, shared learning, and professional community participation. They are distinct from Pods.

Examples:
- Frontend Engineering.
- AI Builders.
- Product Managers.
- Startup Founders.
- System Design.
- Engineering Leadership.
- Career Switchers.

## 13.2 Cohort types and visibility

- `OPEN_TO_BRIDGE_MEMBERS`
- `REQUEST_TO_JOIN`
- `INVITE_ONLY`

Cohorts may be:
- Platform-created.
- Admin-created.
- Member-created, if enabled by admin policy.

## 13.3 Cohort details

- Name.
- Description.
- Category/topic.
- Cover image.
- Rules.
- Visibility.
- Member count.
- Creator.
- Moderators.
- Created date.
- Status: `DRAFT`, `ACTIVE`, `ARCHIVED`.

## 13.4 Member features

- Browse/search/filter Cohorts.
- View details, rules, moderators, and member count.
- Join, request to join, accept invite, or leave.
- Create text posts.
- Comment/reply.
- React to posts.
- Pin/save useful content (personal save).
- View pinned announcements and resources.
- Report posts, comments, or members.

## 13.5 Moderation

Cohort moderators can:
- Approve/decline requests if configured.
- Pin/unpin announcements.
- Hide/remove posts and comments.
- Remove members from that Cohort.
- Review reports scoped to the Cohort.
- Publish announcements.

Moderators cannot manage unrelated Cohorts or access private DMs.

## 13.6 Acceptance criteria

- Membership and access policy enforced server-side.
- Private Cohort posts are visible only to authorized members.
- Leaving/removal revokes access.
- Moderator privileges are scoped to assigned Cohorts.

---

# 14. Bridge Pods — private groups of up to 10

## 14.1 Definition

A Bridge Pod is a small private group created by a member for focused discussion, learning, accountability, interview preparation, collaboration, or mentorship.

A Pod is different from a Cohort:
- Cohort = larger, topic-based community.
- Pod = small, private, focused group.

Default maximum is **10 members including the creator**.

## 14.2 Pod creation

User taps `Create Pod` and enters:
- Pod name.
- Purpose/description.
- Topic/hashtags.
- Optional cover image.
- Maximum members (default 10; platform policy controls maximum).
- Invite-only or discoverable/request-to-join.
- Optional start/end date.
- Pod guidelines.

Creator becomes Pod admin.

By default, Pods are private and invite-only. If discoverable, only basic public metadata is shown; messages/resources/member details remain private.

## 14.3 Pod membership

- Invite existing connections.
- Invite multiple connections.
- Accept/decline invitation.
- Request to join only if enabled.
- Creator/admin approves join requests.
- Capacity cannot be exceeded.
- Member can leave at any time.
- Admin can remove a member with a reason.
- Pod continues if creator leaves; transfer admin role or archive based on policy.

## 14.4 Pod lifecycle

The supplied reference uses a 4-week Pod cycle. Implement:
- Default duration: 4 weeks.
- Status: `ACTIVE`, `ENDING`, `ARCHIVED`.
- Display time remaining.
- Notify members before expiry.
- At expiry, Pod becomes read-only/archived unless admin extends it.
- Members can export their own permitted content before archival.
- Archived Pods are read-only.
- Do not delete content immediately on archive.

## 14.5 Pod home

Header:
- Pod name.
- Topic.
- Status and time remaining.
- Member avatars and `+N`.
- Member list action.
- Pod settings (admins only).

Tabs:
1. Chat
2. Discussions
3. Resources

Whiteboard is a future enhancement; do not block MVP on real-time collaborative canvas.

## 14.6 Pod chat and discussions

MVP:
- Group text chat.
- Replies/threads (if implemented reliably).
- Emoji reactions.
- Mentions.
- Pinned messages.
- Discussion prompts/questions.
- Post/comment/reaction.
- Message history and pagination.

## 14.7 Resources

- Share links.
- Upload supported documents/images (private).
- Resource title and description.
- Posted by and timestamp.
- Pin/unpin.
- Delete own resource; admin can moderate.
- Enforce file size/type and membership access.

## 14.8 Pod safety and permissions

- Only members can read Pod chat, posts, resources, and member details.
- Verify membership on every REST and WebSocket operation.
- Removing/leaving a Pod revokes access immediately.
- Only creator/admin can manage membership and settings.
- Members can report content and members.
- Block rules apply inside Pods; define safe handling for members who share a Pod but have blocked one another.

## 14.9 Acceptance criteria

- Pod creation and invitation flow works.
- Maximum 10 members including creator is enforced transactionally.
- Private content cannot be read by non-members.
- Pod lifecycle and archive behavior work.
- Admin-only actions are enforced server-side.

---

# 15. Ask for Referral — 10-request lifetime limit

## 15.1 Product behavior and quota definition

Ask for Referral lets a member request a referral from a relevant professional connection.

**MVP quota: each member can submit at most 10 referral requests over the lifetime of their Bridge account.**

This is the interpretation of the product owner's requirement “restrict a user to ask for 10 referrals only.” It is not 10 open slots and not 10 per month.

Rules:
- Each successfully submitted referral request consumes one of the 10.
- Declined, withdrawn, rejected, expired, or completed requests do not restore quota.
- Drafts and failed submissions do not consume quota.
- Quota does not reset automatically.
- Store the limit as a backend policy for future configurability, but keep MVP default at 10.
- Do not permit client-side quota manipulation.
- All quota checks and consumption must be atomic and transaction-safe.

Display:
- `3 of 10 referral requests used`
- `7 requests remaining`
- When exhausted: `You have used all 10 referral requests available on your account.`

Do not implement the alternative “10 open requests at once” interpretation unless the product owner explicitly changes this rule.

## 15.2 Entry points

- `Ask for Referral` button on an eligible connection's profile.
- Action on an accepted connection card.
- `My Referrals` screen under Profile.

Show the action only if:
- Members are accepted connections.
- Neither has blocked the other.
- Recipient accepts referral requests.
- Requester has remaining quota.
- No duplicate active request exists for the same job and recipient.

## 15.3 Referral request form

Required:
- Target company.
- Role/job title.
- Short message (maximum 500 characters).

Optional:
- Job posting URL.
- Job/requisition ID.
- Relevant skills/experience.
- Resume attachment selected from the member's private documents.

Before sharing a resume:
- Show the recipient.
- Show the selected file.
- Explain that the file will be shared with this recipient.
- Require explicit confirmation.

## 15.4 Referrer selection

MVP:
- Requester selects one recipient per referral request.
- Recipient must be an accepted connection.
- Recipient should have a verified employment signal for the target company, where such verification is available.
- If employment is not verified, show a clear “company affiliation not verified” state and do not represent the person as an employee.
- Do not automatically send one request to multiple people.
- User can create another request to another connection only if lifetime quota remains.

The PDF reference suggested selecting up to three referrers per request. For the MVP, use one recipient per request so the 10-request limit is meaningful, prevents mass outreach, and provides clear accountability. This is an explicit product decision.

## 15.5 Referral request states

- `SUBMITTED`
- `VIEWED`
- `NEEDS_INFORMATION`
- `WILL_REFER`
- `REFERRED`
- `DECLINED`
- `WITHDRAWN`
- `EXPIRED`

Suggested lifecycle:
1. Requester submits.
2. Backend validates eligibility and atomically consumes one quota unit.
3. Recipient is notified.
4. Recipient views request.
5. Recipient chooses:
   - Will refer.
   - Decline.
   - Ask for more information.
6. If more information is requested, conversation can continue through Bridge Chat.
7. Referrer marks `REFERRED` after submitting the referral externally.
8. Requester can acknowledge outcome.
9. Both parties see status history.

Referral requests expire after 14 days without a response. Expiry does **not** restore the lifetime quota.

## 15.6 Referral eligibility and abuse controls

- Only accepted connections may receive referral requests.
- Recipient can opt out of referral requests.
- Recipient can set an optional monthly inbound cap.
- Prevent duplicate active requests for the same recipient/job.
- Rate-limit submissions in addition to lifetime quota.
- Blocked users cannot send referral requests.
- Recipient can report spam or inappropriate requests.
- Do not guarantee referral outcomes.
- Referrals are voluntary and unpaid through Bridge.
- Do not automatically submit a referral to an employer.
- Do not expose private referral portal links or employer-only information.

## 15.7 Referral screens

`My Referrals`:
- Quota used and remaining.
- Tabs: Sent / Received.
- Request cards with company, role, recipient, date, status.
- Filter by status.
- Open request details and status history.

`Referral detail`:
- Job/company details.
- Message.
- Resume shared indicator.
- Recipient.
- Status timeline.
- Available actions based on role and status.

## 15.8 Acceptance criteria

- A member can never submit an 11th lifetime request.
- Concurrent requests cannot bypass the quota.
- Failed validation/delivery does not consume quota.
- Declined/withdrawn/expired requests do not restore quota.
- Only accepted connections can receive a request.
- Resume sharing requires explicit consent.
- Recipient can opt out.
- Request history is auditable.

---

# 16. Notifications

## 16.1 Channels

- In-app notifications.
- Android push notifications via Firebase Cloud Messaging.
- Email notifications for selected account/security events.

## 16.2 Events

- Connection request received.
- Connection accepted.
- New direct message.
- Cohort invitation.
- Cohort join request/approval.
- Cohort announcement.
- Pod invitation.
- Pod join request/approval.
- Pod message/activity.
- Pod nearing expiry.
- Referral request received.
- Referral status changed.
- Verification status changed.
- Profile import failed/completed.
- Bridge Score recalculated.
- Intent expiring soon.
- Security/session event.

## 16.3 Notification preferences

- Per-event preferences where practical.
- Push on/off.
- Email on/off.
- Quiet hours.
- Respect device time zone.
- Notification list with read/unread states.
- Keep last 30 days in the default notification screen; older records may remain in backend according to retention policy.

Do not put resume content, private message body, or sensitive profile information in push payloads.

## 16.4 Acceptance criteria

- Device token registration and refresh work.
- Logout revokes device association.
- Deep links re-check authorization.
- User preferences are respected.

---

# 17. Settings, privacy, and account controls

Settings sections:

1. Account and security.
2. Connected accounts.
3. Profile visibility.
4. Bridge Score visibility.
5. Discover visibility.
6. Intent visibility.
7. Chat privacy (read receipts, typing, online status).
8. Referral preferences (accept/decline inbound requests, monthly cap).
9. Notifications and quiet hours.
10. Data export.
11. Account deletion.
12. Terms and Privacy Policy.
13. Sign out.

## 17.1 Data export

- User can request an export of their personal profile data.
- Export generated asynchronously.
- Short-lived secure download link.
- Do not include other users' private messages or private data.
- Document what is included and excluded.

## 17.2 Account deletion

- Require reauthentication/confirmation.
- Revoke sessions and provider tokens.
- Delete or anonymize personal data according to legal/retention requirements.
- Remove private resume files and imported source data where required.
- Anonymize shared content where deletion would otherwise break community context, subject to applicable law.
- Record deletion request and completion status.
- Communicate expected processing time.
- Do not claim immediate full deletion if backups or legally required records remain.

---

# 18. Trust, safety, moderation, and admin

## 18.1 Member safety

Reporting available on:
- Profile.
- Direct message.
- Cohort post/comment.
- Pod message/resource/member.
- Referral request.

Block:
- Blocked members cannot discover one another.
- Cannot send connection requests.
- Cannot start direct chats.
- Cannot send referral requests.
- Cannot invite one another to private Pods.
- Existing conversations are restricted according to a documented block policy.
- If both members are in the same Cohort/Pod, do not expose private contact information; provide moderation/reporting options.

## 18.2 Admin roles

- Super admin.
- Verification reviewer.
- Community moderator.
- Support operator.

Use least privilege and scoped permissions.

## 18.3 Admin capabilities

- Review verification submissions.
- Review score appeals.
- Review user reports.
- Warn/restrict/suspend/ban accounts.
- Manage intent catalog.
- Manage Cohorts.
- Review Pod reports.
- Review referral abuse.
- View operational dashboards.
- View audit logs.

Admin actions require:
- Reason.
- Actor ID.
- Timestamp.
- Target resource.
- Before/after state where relevant.

Do not build an unrestricted admin backdoor.

---

# 19. Backend architecture and data flow

## 19.1 High-level architecture

```text
Android App (Kotlin + Compose)
        |
        | HTTPS REST / WebSocket
        v
FastAPI Modular Monolith
        |
        +-- Auth & Identity
        +-- Profile & Import
        +-- Resume Processing
        +-- Verification
        +-- Bridge Score
        +-- Intent & Discover
        +-- Connections
        +-- Chat
        +-- Cohorts
        +-- Pods
        +-- Referrals
        +-- Notifications
        +-- Moderation / Admin
        |
        +---------- Managed PostgreSQL (Supabase or Amazon RDS)
        +---------- Optional SQS queue + Python worker
        +---------- Optional managed Redis (only when justified)
        +---------- Private S3 object storage
        +---------- Firebase Cloud Messaging
        +---------- OAuth providers / Email provider
```

## 19.2 Profile import flow

1. Android requests provider connection.
2. Backend creates OAuth state/PKCE as appropriate.
3. Provider redirects/returns authorization.
4. Backend validates state and exchanges code.
5. Backend encrypts provider tokens.
6. Backend queues permitted data import.
7. Worker fetches approved provider fields.
8. Resume upload is stored privately and queued for parsing.
9. Parsed/imported fields are stored with source provenance.
10. Android shows review screen.
11. User confirms/edits profile.
12. Backend queues Bridge Score calculation.

## 19.3 Bridge Score flow

1. Profile is confirmed or relevant source changes.
2. Backend queues scoring job.
3. Scoring service reads authorized evidence.
4. Private rubric is applied server-side.
5. Score result and model version are stored.
6. Member-facing API returns only permitted output.
7. User sees Bridge Score ring and safe suggestions.
8. Score history records the change.

## 19.4 Discover flow

1. Android sends active filters/search/cursor.
2. Backend loads current member intents and visibility.
3. Matching service finds eligible members.
4. Privacy/block/verification filters are applied.
5. Ranking is calculated.
6. Match reasons are generated from actual overlap.
7. API returns paginated public profile cards.
8. Android renders cards and actions.

## 19.5 Connection and chat flow

1. Member sends connection request.
2. Backend validates rate limit, duplicate, block state.
3. Recipient receives notification.
4. Recipient accepts.
5. Backend creates/activates connection.
6. Chat conversation is created or made available.
7. WebSocket authorization checks connection state.
8. Message is persisted before broadcast.
9. Recipient receives push if offline.

## 19.6 Cohort flow

1. Member browses Cohorts.
2. Backend filters by visibility and eligibility.
3. Member joins or requests access.
4. Backend enforces membership policy.
5. Member reads/posts/comments according to membership.
6. Moderator actions are scoped to that Cohort.

## 19.7 Pod flow

1. Member creates Pod.
2. Backend creates Pod and creator membership in one transaction.
3. Member invites accepted connections.
4. Invitees accept.
5. Backend enforces max capacity of 10 including creator.
6. Members access private chat/discussions/resources.
7. Pod expires after default four weeks.
8. Pod is archived/read-only unless extended.

## 19.8 Referral flow

1. Member opens eligible connection profile.
2. Android checks/display quota from backend.
3. Member submits request.
4. Backend validates accepted connection, recipient settings, duplicate request, and remaining lifetime quota.
5. In a single transaction, backend creates request and increments quota ledger.
6. Recipient is notified.
7. Recipient responds.
8. Status changes and notifications are persisted.
9. Request history is visible to both parties with role-appropriate fields.

---

# 20. Database model requirements

Use PostgreSQL, SQLAlchemy 2.x, Alembic migrations, UUID primary keys, foreign keys, indexes, timestamps, and transaction-safe constraints.

Minimum entities:

## Identity and authentication
- `User`
- `AuthIdentity`
- `Session`
- `DeviceToken`
- `UserConsent`
- `ConnectedProvider`

## Profile and evidence
- `MemberProfile`
- `ProfileFieldSource`
- `EmploymentHistory`
- `Education`
- `Skill`
- `MemberSkill`
- `Project`
- `Achievement`
- `ResumeDocument`
- `ImportedProfileSnapshot`
- `VerificationRecord`

## Bridge Score
- `BridgeScoreSnapshot`
- `BridgeScoreModelVersion`
- `ScoreReviewRequest`
- Private score feature/config tables, restricted to scoring/admin service only.

Do not expose private scoring tables through general member repositories/controllers.

## Intents and Discover
- `IntentCategory`
- `Intent`
- `MemberIntent`
- `HiddenMember`
- `DiscoverPreference`

## Connections and Chat
- `Connection`
- `ConnectionRequest`
- `Block`
- `Conversation`
- `ConversationMember`
- `Message`
- `MessageReaction`
- `MessageAttachment`
- `MessageReadState`

## Cohorts
- `Cohort`
- `CohortMembership`
- `CohortInvitation`
- `CohortPost`
- `CohortComment`
- `CohortReaction`
- `CohortResource`

## Pods
- `Pod`
- `PodMembership`
- `PodInvitation`
- `PodJoinRequest`
- `PodMessage`
- `PodDiscussion`
- `PodComment`
- `PodReaction`
- `PodResource`
- `PodCheckIn`

## Referrals
- `ReferralRequest`
- `ReferralStatusHistory`
- `ReferralQuotaUsage`
- `ReferralRecipientPreference`
- `ReferralAttachmentConsent`

## Platform operations
- `Notification`
- `NotificationPreference`
- `Report`
- `AdminRole`
- `AdminRoleAssignment`
- `AuditLog`
- `FeaturePolicy`
- `AnalyticsEvent` or analytics integration.

## 20.1 Important data constraints

- Unique provider subject per provider.
- Unique normalized email where verified.
- Unique active connection request per ordered member pair.
- No self-connection.
- Pod member count cannot exceed configured maximum; enforce in transaction.
- Cohort membership unique per user/cohort.
- Referral quota usage count must be derived from server-side ledger.
- Referral lifetime quota must be transaction-safe under concurrent requests.
- Avoid duplicate active referral request for same requester/recipient/job.
- Foreign keys and deletion policies must be explicit.
- Add indexes for Discover filters, conversation pagination, cohort membership, Pod membership, and referral history.

---

# 21. API contract requirements

All REST APIs use `/api/v1`. Generate and maintain Swagger/OpenAPI documentation.

Use consistent response and error structures. Validate all request bodies, query parameters, and path IDs. Use cursor pagination for feeds/lists.

Suggested endpoint groups:

## Auth
- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/oauth/:provider/start`
- `POST /auth/oauth/:provider/callback`
- `POST /auth/refresh`
- `POST /auth/logout`
- `POST /auth/forgot-password`
- `POST /auth/reset-password`
- `GET /auth/sessions`
- `DELETE /auth/sessions/:id`

## Providers and profile import
- `GET /providers`
- `POST /providers/:provider/connect`
- `DELETE /providers/:provider/disconnect`
- `POST /profile-import/:provider`
- `GET /profile-import/:jobId`
- `POST /resume/upload-url`
- `POST /resume/complete`
- `GET /resume/:id/status`
- `DELETE /resume/:id`

## Profile and verification
- `GET /members/me`
- `PATCH /members/me`
- `GET /members/:memberId`
- `PATCH /members/me/visibility`
- `GET /verification/me`
- `POST /verification/submit`
- `POST /verification/review-request`

## Bridge Score
- `GET /bridge-score/me`
- `GET /bridge-score/me/history`
- `POST /bridge-score/me/review-request`
- Internal/admin-only recalculation endpoint; never expose to normal users.

## Intents and Discover
- `GET /intents`
- `POST /intents/suggestions`
- `PUT /members/me/intents`
- `GET /discover`
- `GET /discover/search`
- `POST /discover/:memberId/not-interested`

## Connections
- `POST /connections/requests`
- `GET /connections/requests/incoming`
- `GET /connections/requests/outgoing`
- `POST /connections/requests/:id/accept`
- `POST /connections/requests/:id/decline`
- `POST /connections/requests/:id/withdraw`
- `GET /connections`
- `DELETE /connections/:memberId`

## Chat
- `GET /conversations`
- `GET /conversations/:id/messages`
- `POST /conversations/:id/messages`
- `POST /messages/:id/reactions`
- `DELETE /messages/:id`
- WebSocket events for authorized message delivery, typing, read state, and presence.

## Cohorts
- `GET /cohorts`
- `POST /cohorts` (if member-created Cohorts enabled)
- `GET /cohorts/:id`
- `POST /cohorts/:id/join`
- `POST /cohorts/:id/leave`
- `GET /cohorts/:id/members`
- `GET /cohorts/:id/posts`
- `POST /cohorts/:id/posts`
- `POST /cohorts/:id/posts/:postId/comments`
- `POST /cohorts/:id/reports`

## Pods
- `GET /pods`
- `POST /pods`
- `GET /pods/:id`
- `PATCH /pods/:id`
- `POST /pods/:id/invitations`
- `POST /pods/:id/invitations/:inviteId/accept`
- `POST /pods/:id/join-requests`
- `POST /pods/:id/leave`
- `GET /pods/:id/members`
- `GET /pods/:id/messages`
- `POST /pods/:id/messages`
- `GET /pods/:id/resources`
- `POST /pods/:id/resources`
- `POST /pods/:id/archive` (authorized admin/system)

## Referrals
- `GET /referrals/quota`
- `GET /referrals`
- `POST /referrals`
- `GET /referrals/:id`
- `POST /referrals/:id/view`
- `POST /referrals/:id/request-information`
- `POST /referrals/:id/will-refer`
- `POST /referrals/:id/decline`
- `POST /referrals/:id/mark-referred`
- `POST /referrals/:id/withdraw`
- `GET /members/:memberId/referral-eligibility`

## Notifications and safety
- `GET /notifications`
- `PATCH /notifications/:id/read`
- `POST /device-tokens`
- `PATCH /notification-preferences`
- `POST /blocks`
- `DELETE /blocks/:memberId`
- `POST /reports`

## Admin
- Admin endpoints for verification, score review, member moderation, Cohort moderation, Pod reports, referral abuse, intent catalog, feature policies, and audit logs.

API security requirements:
- Do not allow a member to update their own role, score, verification state, referral quota, or moderation state.
- Never return private score rubric.
- Never return raw provider tokens.
- Never return private resume URL without authorization.
- Check access to each Cohort/Pod/Conversation resource.
- Validate WebSocket authorization at connection and subscription time.
- Use idempotency keys for operations that may be retried, especially referral creation and payment-like future actions.

---

# 22. Android screen inventory and UI requirements

## 22.1 Authentication
- Splash.
- Welcome.
- Sign in.
- Sign up.
- OAuth loading/callback/error.
- Email verification.
- Forgot/reset password.
- Terms/Privacy consent.

## 22.2 Onboarding
- Profile source selection.
- LinkedIn connect/status.
- GitHub connect/status.
- Resume upload.
- Import progress.
- Import failure/retry.
- Review imported fields.
- Edit profile.
- Profile preview.
- Bridge Score processing.
- Bridge Score result.
- Intent selection.
- Onboarding complete.

## 22.3 Discover
- Discover home.
- Search.
- Filter sheet.
- Results.
- Member profile.
- Connect note composer.
- Not-interested confirmation.
- Report/block.

## 22.4 Connections and Chats
- Incoming requests.
- Outgoing requests.
- Connections list.
- Conversation list.
- Direct chat.
- Attachment preview (if attachments included).
- Chat quick actions.
- Report/block.

## 22.5 Cohorts
- Cohort directory.
- Cohort search/filter.
- Cohort detail.
- Cohort member list.
- Cohort post list.
- Post detail/comments.
- Create/edit post.
- Join/request state.
- Cohort moderation/report.

## 22.6 Pods
- My Pods.
- Discover Pods (only discoverable Pods).
- Create Pod.
- Invite connections.
- Pod detail.
- Pod member list.
- Pod chat.
- Pod discussions.
- Pod resources.
- Pod settings.
- Pod archive/expiry.

## 22.7 Profile and referrals
- My Bridge Passport.
- Bridge Score detail.
- Score improvement suggestions.
- Edit profile.
- Edit skills/experience/projects.
- Intent management.
- Verification status.
- My Referrals.
- Referral form.
- Referrer profile/eligibility.
- Referral detail/status history.
- Privacy settings.
- Connected accounts.
- Notification settings.
- Session management.
- Data export.
- Account deletion.

## 22.8 Notifications
- Notification inbox.
- Notification preferences.
- Quiet hours.

## 22.9 Visual direction

Use the supplied SignalLoop flow image as reference for:
- Step-by-step onboarding.
- Source analysis status.
- Large circular score treatment.
- Intent chips.
- Professional discovery cards.
- Direct chat.
- Small group Pod experience.
- Backend flow and source-based analysis concept.

Apply Bridge branding:
- Premium, clean, modern, professional.
- Deep navy/indigo base.
- Restrained blue/purple accent.
- White/off-white surfaces.
- Neutral gray supporting text.
- Rounded cards and chips.
- Clear typographic hierarchy.
- Generous spacing.
- Minimal visual noise.
- No public vanity leaderboard.

Create reusable components:
- `BridgeTopBar`
- `BridgeScoreRing`
- `VerificationBadge`
- `BridgePassportCard`
- `ProfessionalProfileCard`
- `IntentChip`
- `SearchFilterBar`
- `ConnectionButton`
- `CohortCard`
- `PodCard`
- `ChatMessageBubble`
- `ReferralRequestCard`
- `EmptyState`
- `LoadingState`
- `ErrorState`

Do not copy the reference image's iPhone frame or web-style layout. Reinterpret it as native Android screens.

---

# 23. Security, privacy, and non-functional requirements

## 23.1 Security

- HTTPS/TLS for all deployed traffic.
- Secure OAuth state/PKCE.
- Encrypt provider tokens at rest.
- Use Android Keystore-backed secure storage for app credentials.
- Backend authorization on every resource.
- Private object storage and short-lived signed URLs.
- File type/size validation and malware scanning where available.
- Secrets in managed secret store/environment variables.
- Do not log passwords, tokens, resume contents, or private message bodies.
- Rate-limit authentication, provider import, uploads, connections, chat, referrals, and reports.
- Audit admin actions.
- No sensitive/protected attributes in scoring or ranking.
- Do not claim end-to-end encryption unless implemented and reviewed.
- Certificate pinning is optional and must include a safe rotation strategy; do not brick app connectivity through hard-coded pins.

## 23.2 Privacy and compliance

Design for applicable laws and provider terms, including:
- India's DPDP Act.
- GDPR where EU users are served.
- CCPA/CPRA where applicable.
- Google Play user-data policies.
- LinkedIn/GitHub developer terms.

Requirements:
- Data minimization.
- Consent per source.
- Purpose limitation.
- Field-level profile visibility.
- Provider disconnection.
- Data export.
- Account deletion.
- Retention schedule.
- Regional data handling if required.
- Document legal review requirements before production.

## 23.3 Performance targets

Initial planning targets; validate with load tests:
- Discover first page p95 under 2 seconds under expected MVP load.
- Chat message delivery p95 under 1 second under expected load.
- Common API reads p95 under 500 ms where practical.
- Cold start target under 2 seconds on representative mid-range devices; measure rather than assume.
- Paginate all unbounded lists.
- Load images asynchronously.
- Avoid large profile payloads.
- Cache safe, non-sensitive data.
- Use background jobs for parsing, scoring, notifications, and sync.

## 23.4 Scalability

Design data access and background jobs to scale beyond initial MVP. Do not claim 100K users or 1M messages/day is supported until verified by load tests.

## 23.5 Availability and observability

- Structured logs with request IDs.
- Crash reporting in Android.
- Backend error monitoring.
- Metrics for API latency, job failures, OAuth failures, message delivery, notification failures, and referral abuse.
- Health/readiness endpoints.
- Alerting for critical workflows.
- Database backups and restore tests.

## 23.6 Accessibility and localization

- TalkBack labels for all controls.
- Score ring has accessible textual value/state.
- 48dp minimum touch targets.
- Support system font size.
- Color contrast.
- English at launch.
- Use localization-ready string resources.
- Respect device locale/time zone for date/time display.

---

# 24. Analytics and success events

Track product events without recording private message contents, resumes, provider tokens, or hidden profile fields.

Events:
- `signup_started`
- `signup_completed`
- `consent_accepted`
- `provider_connect_started`
- `provider_connect_completed`
- `provider_connect_failed`
- `profile_import_started`
- `profile_import_completed`
- `resume_uploaded`
- `profile_review_completed`
- `profile_completed`
- `bridge_score_generated`
- `bridge_score_viewed`
- `score_review_requested`
- `intent_selected`
- `intent_renewed`
- `discover_opened`
- `discover_search_performed`
- `discover_filter_applied`
- `member_profile_viewed`
- `connection_requested`
- `connection_accepted`
- `message_sent` (metadata only)
- `cohort_joined`
- `cohort_post_created`
- `pod_created`
- `pod_joined`
- `referral_request_submitted`
- `referral_request_viewed`
- `referral_request_will_refer`
- `referral_request_referred`
- `referral_quota_exhausted`
- `report_submitted`

Dashboards:
- Signup-to-profile completion.
- Provider connection success/failure.
- Profile import completion and failure reasons.
- Time to Bridge Score.
- Discover-to-profile-view conversion.
- Connection acceptance rate.
- Time to first accepted connection.
- Chat engagement.
- Cohort and Pod participation/retention.
- Referral submission and response outcomes.
- Referral abuse/report rate.
- D7/D30 retention.
- Moderation response time.

Do not use analytics events to store message text, resume content, or private user data.

---

# 25. Testing requirements

## 25.1 Android unit/UI tests

- Authentication success/failure.
- Expired session and refresh.
- Provider connection loading/error/success.
- Profile import and manual fallback.
- Resume upload/parse/review.
- Profile field visibility.
- Bridge Score processing/calculated/failed states.
- Intent maximum and expiry.
- Discover search/filter/pagination/empty state.
- Connection request lifecycle.
- Chat connection gating.
- Cohort membership and private states.
- Pod creation/invites/capacity.
- Referral quota and exhausted state.
- Referral form validation and status rendering.
- Logout clears account cache.
- Accessibility labels for score ring, chips, and actions.

## 25.2 Backend tests

- OAuth callback state validation.
- Account linking and duplicate identity prevention.
- Provider token encryption.
- Resume upload authorization and MIME validation.
- Score calculation private-data serialization tests.
- Intent maximum enforcement and expiry.
- Discover ranking and privacy exclusions.
- Not-interested hiding.
- Connection duplicate/rate-limit/block rules.
- Direct chat authorization.
- WebSocket membership checks.
- Cohort membership authorization.
- Pod capacity under concurrent invitations/acceptances.
- Pod private-resource authorization.
- Referral quota under concurrent submissions.
- Referral eligibility and duplicate active request checks.
- Resume share consent.
- Block/report enforcement.
- Admin scope and audit logging.

## 25.3 End-to-end scenarios

1. Register using Google or email test provider.
2. Accept Terms and Privacy Policy.
3. Connect GitHub or use manual fallback.
4. Attempt LinkedIn connection; if API access is unavailable, use documented fallback.
5. Upload resume.
6. Review and correct parsed profile fields.
7. Save Bridge Passport.
8. Generate Bridge Score and confirm no scoring formula is exposed.
9. Select three intents and verify a fourth is rejected.
10. Discover a relevant professional and inspect the match reason.
11. Send connection request.
12. Recipient accepts.
13. Exchange direct messages.
14. Join a Cohort and create a post.
15. Create a Pod and invite connections.
16. Verify Pod cannot exceed 10 members including creator.
17. Exchange Pod messages and share a resource.
18. Verify non-members cannot access Pod content.
19. Submit a referral request to an accepted connection.
20. Verify quota changes from 10 remaining to 9 remaining.
21. Submit requests until all 10 are used.
22. Verify an 11th request is rejected by backend.
23. Run concurrent referral submissions and verify quota is not exceeded.
24. Verify declined/withdrawn/expired requests do not restore quota.
25. Verify a non-connection cannot receive a referral request.
26. Verify resume is shared only after explicit consent.
27. Verify recipient opt-out works.
28. Block a member and verify Discover, connection, chat, and referral restrictions.
29. Submit a report and verify admin queue/audit.
30. Request data export and account deletion.

---

# 26. Seed data and local development

Seed development with clearly fictional data:
- One admin account.
- At least 12 fictional professional members across roles and industries.
- At least 25 intents.
- Three Cohorts with different visibility policies.
- Two Pods, including one with 10 members.
- Sample connection requests and accepted connections.
- Sample direct conversations.
- Sample referral requests in different states.
- Demo Bridge Scores explicitly marked as demo.

Provide:
- `.env.example` and Alembic migrations.
- Optional `docker-compose.yml` for local PostgreSQL only; Redis must not be a required dependency.
- Seed script.
- Android Studio setup instructions.
- Backend setup instructions.
- Swagger URL.
- Test commands.
- Firebase setup guide.
- Object storage setup guide.
- OAuth setup guide.
- Mock OAuth adapters for local development only, clearly disabled in production.

---

# 26A. Production readiness requirements by capability

These requirements apply to every feature before it can be marked production-ready. A visually complete screen is not sufficient.

## 26A.1 Authentication and account lifecycle
- Use OAuth 2.0 Authorization Code with PKCE for native Android where supported. Never embed client secrets in the APK.
- Validate provider tokens and backend-issued access tokens server-side. Rotate refresh tokens where supported; revoke sessions on logout, password reset, account suspension, and account deletion.
- Store tokens using Android Keystore-backed encrypted storage. Never log tokens.
- Implement session/device management, account recovery, email verification where applicable, consent versioning, and suspicious-login controls.
- Apply rate limits to login, OTP, password reset, OAuth callback, and account creation endpoints.
- Do not equate Google/GitHub/LinkedIn login with employment or identity verification.

## 26A.2 Profile import, resume and file handling
- Use official provider APIs only; request least-privilege scopes and document fields unavailable without provider approval.
- Encrypt provider refresh/access tokens at rest; support disconnect and token revocation.
- Validate MIME type, extension, file signature, file size and page limits. Reject executable and unsupported files.
- Upload directly to private object storage using short-lived, scoped presigned URLs. Verify ownership before issuing a URL.
- Scan uploaded files for malware before parsing or making them available to downstream processing.
- Parse resumes asynchronously with timeouts, bounded memory, retry limits and a dead-letter path.
- Store source provenance and import timestamps; require user review before imported data becomes the member's profile.
- Define retention and deletion behavior for original resumes, parsed text, extracted fields and provider snapshots.

## 26A.3 Bridge Score and verification
- Calculate scores only on the backend using versioned, access-controlled rubric configuration.
- Do not expose weights, formula, thresholds, feature contributions, raw source evidence or internal confidence values in member APIs or client bundles.
- Store score model version, calculation timestamp, input snapshot reference and audit metadata.
- Make recalculation idempotent and safe to retry. Avoid score changes caused by duplicate events.
- Clearly distinguish verified, provider-imported, resume-extracted and user-entered fields.
- Provide an appeal/correction path and record administrative changes with actor, reason and timestamp.
- Do not show rankings or percentiles unless statistically valid, explainable and approved.

## 26A.4 Discover, search and recommendations
- Enforce profile visibility, blocks, mutes, age/region restrictions if configured, and account status on the server before returning results.
- Use cursor pagination with stable ordering; cap page size and query complexity.
- Prevent enumeration of member IDs, emails, phone numbers and hidden profile fields.
- Explain recommendations only using real overlapping intents, skills or declared goals.
- Add request-level rate limits and abuse monitoring. Log recommendation version and safe aggregate metrics, not sensitive query text.
- Provide user controls for discoverability, hidden members and recommendation preferences.

## 26A.5 Connections, chat and realtime
- Enforce connection state and block status on every REST request and WebSocket event.
- Persist messages before broadcasting. Use client-generated idempotency keys to prevent duplicate sends after retries.
- Implement message pagination, delivery/read state semantics, attachment authorization, message deletion policy, and abuse reporting.
- Validate WebSocket authentication at handshake and authorization for every conversation subscription/action.
- Apply per-user and per-conversation message rate limits, payload limits, connection limits and heartbeat/reconnect handling.
- Use push notifications for offline recipients without including sensitive message content by default.
- Define retention, user deletion, reporting evidence and legal hold behavior before launch.
- Do not claim end-to-end encryption unless it is actually implemented and independently reviewed.

## 26A.6 Cohorts and Pods
- Enforce membership, visibility and moderator roles on every read/write operation.
- Perform Pod capacity checks and membership writes in one database transaction; enforce the 10-member maximum including the creator with a database-safe concurrency strategy.
- Validate invite expiry, duplicate invitations, join policy, removal, leave, archive and extension transitions server-side.
- Record moderator actions and provide report/block flows.
- Define Cohort owner/moderator succession and orphaned-community behavior.
- Use cursor pagination and limits for posts, comments, reactions and member lists.

## 26A.7 Ask for Referral
- Enforce the lifetime limit of 10 submitted requests in the backend, using an atomic transaction and immutable quota ledger.
- Define “submitted” precisely: a request consumes quota only after it is accepted by the API and persisted; rejected validation attempts do not consume quota.
- Declined, withdrawn, expired or unanswered requests do not restore quota unless a documented product policy explicitly changes this.
- Prevent duplicate active requests to the same recipient/opportunity where applicable.
- Enforce accepted-connection eligibility, recipient opt-out, block status, abuse limits and consent before sharing a resume/profile.
- Make request creation idempotent and test concurrent submissions. The 11th valid request must fail even under race conditions.
- Keep referral requests distinct from actual employer referrals; never imply that Bridge submits a referral to an employer automatically.

## 26A.8 Notifications and user communication
- Store notification preferences and respect opt-outs by channel and category.
- Use an outbox pattern or equivalent durable mechanism so committed business events are not lost before notification dispatch.
- Make notification dispatch retryable and idempotent; handle invalid FCM tokens and email bounces.
- Deep links must authenticate and re-check authorization before showing protected content.
- Avoid exposing sensitive message, referral or profile details on lock screens unless the member explicitly opts in.

## 26A.9 API, data and database
- Version public APIs under `/api/v1`; publish OpenAPI and maintain backward-compatible changes.
- Use Pydantic request/response schemas; never serialize ORM objects directly as public responses.
- Use SQLAlchemy 2.x with explicit transactions and Alembic migrations. Do not run schema migrations automatically on every application startup.
- Use UTC timestamps, UUID identifiers, foreign keys, unique constraints, check constraints and indexes based on actual query patterns.
- Use connection pooling with safe limits for the selected managed PostgreSQL plan.
- Add query timeouts, request timeouts, pagination and bounded payload sizes.
- Use optimistic concurrency or row locking for quota, capacity and state-transition operations.
- Provide database backup, restore and migration rollback/forward-fix procedures. Test restore before production launch.

## 26A.10 Security, privacy and compliance
- Apply OWASP API Security guidance: object-level authorization, function-level authorization, input validation, rate limits, secure defaults and inventory of endpoints.
- Use TLS for all network traffic and encryption at rest for database, object storage and backups.
- Store secrets in the hosting provider's secret manager; rotate credentials and use separate credentials per environment.
- Apply least-privilege IAM/service roles. Do not use AWS root credentials or broad administrator keys in application runtime.
- Configure CORS narrowly for any admin tooling; native Android API access does not require wildcard CORS.
- Protect against SSRF in URL imports, path traversal, mass assignment, injection, insecure direct object references and malicious uploads.
- Minimize personal data collection. Define purpose, access, retention, export, correction and deletion processes.
- Provide account deletion and data export workflows. Deletion must cover database records, object storage, search indexes, queues and provider tokens, subject to documented legal retention.
- Complete a privacy/legal review for applicable Indian data protection requirements and any other launch jurisdictions. Do not represent this document as legal advice.
- Perform dependency vulnerability scanning and secret scanning in CI. Pin and regularly update dependencies.

## 26A.11 Reliability, performance and operations
- Expose `/health/live` and `/health/ready`; readiness must verify required dependencies without leaking details.
- Use structured JSON logs with request/correlation IDs. Never log credentials, raw resumes, private message bodies or unnecessary personal data.
- Track API latency, error rate, database pool saturation, queue depth/age, WebSocket connections, notification failures and storage failures.
- Configure alerts for elevated 5xx rates, failed deployments, database capacity, backup failures and queue dead-letter growth.
- Set explicit service timeouts, bounded retries with exponential backoff and jitter, and circuit-breaker behavior for external providers.
- Define service objectives and capacity assumptions before public launch; load-test key journeys and database contention points.
- Use rolling/blue-green deployment where supported; maintain a rollback procedure and database migration compatibility.
- Define RPO/RTO targets with the hosting provider and product owner; verify backup restoration through a scheduled drill.
- Use feature flags for risky launches and provider integrations.

## 26A.12 Android production quality
- Keep access tokens out of logs, analytics, screenshots and crash reports.
- Use network security configuration, TLS, safe deep-link validation and secure local storage.
- Handle offline, slow network, retry, duplicate taps, process death and expired sessions.
- Request only necessary Android permissions and explain them contextually.
- Add TalkBack semantics, font scaling, contrast checks and touch target validation.
- Use release signing, Play App Signing, R8/minification where appropriate, crash reporting and staged rollout.
- Do not store resumes or private message attachments in publicly accessible app directories.
- Add privacy-safe analytics with consent and an opt-out where required.

## 26A.13 Testing and release gates
- Unit tests for all domain rules and scoring boundaries.
- Integration tests against a real PostgreSQL-compatible test database; SQLite-only tests are insufficient for transaction, constraint and locking behavior.
- API tests for authorization failures, object-level access, pagination, validation and rate limits.
- Concurrency tests for referral quota and Pod capacity.
- WebSocket tests for authentication, membership revocation and reconnect behavior.
- Android UI tests for onboarding, profile review, Discover, connections, chat, Pods and referral flows.
- Security tests for IDOR, privilege escalation, malicious files, token leakage and abuse limits.
- Load tests for Discover, message send/delivery and notification fan-out.
- Release checklist must confirm backups, restore test, monitoring, alerts, secrets, provider approvals, privacy/legal review and rollback plan.

# 27. Implementation phases and exit criteria
## 27.0 SDLC and environment setup (must precede feature rollout)

Before feature development is treated as production-bound:
- Establish protected Git branches and pull-request checks.
- Create isolated `dev`, `stag`, and `prod` AWS environments (separate accounts preferred).
- Provision the baseline infrastructure using Terraform.
- Configure environment-specific Cognito, Firebase/FCM, S3, SQS, RDS, Secrets Manager and CloudWatch resources.
- Configure GitHub Actions with AWS OIDC roles and environment protection rules.
- Implement Android `dev`, `stag`, and `prod` build variants.
- Implement backend runtime configuration validation and environment labels.
- Create deployment, migration, smoke-test, rollback/forward-fix and incident runbooks.
- Demonstrate a release promotion from `dev` to `stag` to `prod` using the same immutable artifact.
- Verify that dev/stag identities, notifications, files, databases and credentials cannot access production resources.

Exit criteria: all three environments are independently deployable; CI/CD gates work; production deployment is approval-gated; no production secrets or customer data are present in dev/stag.


## Phase 0 — Repository inspection and foundation

Tasks:
- Inspect repository.
- Set up Android Kotlin/Compose app.
- Set up FastAPI backend.
- Configure managed PostgreSQL and Alembic. Redis is not required for MVP.
- Add CI, linting, Python formatting, linting, type checking, and environment validation.
- Create design system and navigation shell.
- Add architecture/API/database docs.

Exit:
- Android app runs on emulator.
- Backend starts.
- Database migration and seed succeed.
- CI passes.

## Phase 1 — Authentication and onboarding

Tasks:
- Google and email authentication.
- GitHub OAuth.
- LinkedIn OAuth adapter only if approved.
- Manual profile fallback.
- Resume upload and parsing job.
- Profile review/edit/preview.
- Connected account management.
- Consent recording.

Exit:
- User can sign in, import or manually enter profile information, review, and save a Bridge Passport.

## Phase 2 — Bridge Score and verification

Tasks:
- Private source-specific scoring rubrics.
- Server-side score worker.
- Score state/history.
- Score ring UI.
- High-level improvement suggestions.
- Verification states.
- Admin review/appeal workflow.

Exit:
- Score is generated and shown without exposing internal calculation logic.

## Phase 3 — Intents, Discover, and Connections

Tasks:
- Intent taxonomy and selection.
- Expiry/renewal.
- Discover recommendation/search/filter.
- Match explanations.
- Profile details.
- Not-interested.
- Connection lifecycle.
- Block/report foundation.

Exit:
- Members can discover relevant professionals and establish accepted connections.

## Phase 4 — Direct Chat and Notifications

Tasks:
- One-to-one chat.
- WebSocket delivery.
- Persistent history.
- Read/unread states.
- Push notifications.
- Quick Actions foundation.

Exit:
- Accepted connections can exchange messages reliably.

## Phase 5 — Bridge Cohorts

Tasks:
- Cohort directory.
- Join/request/invite policies.
- Cohort posts/comments/reactions.
- Member list.
- Moderator permissions.
- Reporting.

Exit:
- Members can join and participate in Cohorts with correct access control.

## Phase 6 — Bridge Pods

Tasks:
- Pod creation.
- Invitations and join requests.
- Capacity enforcement.
- Private Pod chat.
- Discussions and resources.
- Four-week lifecycle and archive.
- Pod admin/moderation.

Exit:
- Members can create and use private Pods of up to 10 members.

## Phase 7 — Ask for Referral

Tasks:
- Lifetime quota ledger.
- Referral form and status lifecycle.
- Recipient preferences.
- Referral notifications.
- Request history.
- Anti-abuse controls.
- Concurrency tests.

Exit:
- Each account can submit at most 10 lifetime referral requests, enforced by backend under concurrency.

## Phase 8 — Production hardening

Tasks:
- Security/privacy review.
- Accessibility review.
- Performance/load testing.
- Backup/restore validation.
- Monitoring and alerting.
- Store release preparation.
- Production deployment and rollback runbooks.

Exit:
- Critical user journeys pass; security, release, and operational checklists are complete.

---

# 28. Definition of done
A feature is not production-complete until it has been validated in `dev`, promoted and tested in `stag`, and released to `prod` through the approved pipeline. Feature branches and local runs are not substitutes for environment validation. All environment-specific configuration, permissions, telemetry, migration behavior and rollback/forward-fix considerations must be documented.


A feature is done only when:
- Native Android UI is implemented.
- Backend API and persistence are implemented.
- Server-side validation and authorization are present.
- Loading, empty, error, offline, and success states exist.
- Unit/integration/UI tests cover critical behavior.
- Analytics events are implemented where applicable.
- Swagger/API docs and README are updated.
- Android build, lint, and tests pass.
- Backend lint, typecheck, tests, and container build pass.
- No secrets or private user data are committed.
- Provider limitations and fallbacks are documented.
- No critical security/privacy TODO remains hidden.

---

# 29. Final acceptance checklist

The MVP is acceptable only if all statements below are true:

- [ ] Bridge is a native Android application built with Kotlin and Jetpack Compose.
- [ ] User can register and sign in.
- [ ] User can connect supported providers and use manual fallbacks.
- [ ] User can upload a resume and review parsed data.
- [ ] User can edit and publish a Bridge Passport.
- [ ] Bridge Score is calculated server-side and internal logic is private.
- [ ] User can set up to three active intents.
- [ ] Discover finds relevant professionals using intents/topics/skills.
- [ ] User can view profiles and send connection requests.
- [ ] Direct chat is available only after connection acceptance.
- [ ] User can join and participate in Bridge Cohorts.
- [ ] User can create a private Bridge Pod.
- [ ] Pod capacity is 10 members including creator by default.
- [ ] Pod content is private and authorization is enforced.
- [ ] User can submit a maximum of 10 referral requests over account lifetime.
- [ ] Referral quota is enforced transactionally on the backend.
- [ ] Referral request recipient must be an eligible accepted connection.
- [ ] Resume sharing requires explicit consent.
- [ ] Notifications work for key events.
- [ ] Block/report/moderation flows work.
- [ ] Profile privacy and account controls work.
- [ ] Automated tests cover all critical business rules.

---

# 30. Canonical end-to-end Bridge flow

1. **Sign up:** Google, GitHub, LinkedIn where approved, or email.
2. **Consent:** accept Terms/Privacy and consent to each data source separately.
3. **Import:** connect permitted LinkedIn/GitHub data and/or upload resume.
4. **Analyze:** show import progress and retryable failures.
5. **Review:** user edits and confirms imported profile information.
6. **Bridge Score:** backend calculates the score using private rubrics; UI shows the final score and safe improvement guidance.
7. **Bridge Passport:** show the score at the top and the member's professional profile below it.
8. **Set intents:** select up to three active hashtags/goals.
9. **Discover:** find relevant professionals based on intent, skill, role, and professional goal.
10. **Connect:** send/accept connection requests.
11. **Chat:** communicate one-to-one after acceptance.
12. **Cohorts:** join larger communities around professional topics.
13. **Pods:** create focused private groups of up to 10 people and collaborate.
14. **Ask for Referral:** request referrals from eligible connections, using a maximum of 10 lifetime requests per account.
15. **Stay safe:** manage privacy, notifications, blocks, reports, and account data.

**Build Bridge as a complete, coherent Android product around this flow. Trust, relevance, privacy, and a polished native experience are more important than shipping every optional enhancement at once.**
