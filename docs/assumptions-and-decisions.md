# Assumptions and decisions

**Status:** Architecture and foundation approved by product owner before implementation. Decisions below do not authorize later phases, cloud spending, releases, or commits.

| ID | Decision | Rationale / boundary | State |
|---|---|---|---|
| D-001 | Build a native Android app with Kotlin, Jetpack Compose and Material 3 | Required by product specification; Tailwind is CSS for web apps and cannot style Compose | Approved |
| D-002 | Use Android MVVM with Clean Architecture boundaries and feature-oriented modules | Testable separation and clear feature ownership; no business rules in composables | Approved |
| D-003 | Use a FastAPI modular monolith with PostgreSQL | Keeps first release deployable and operationally simple; APIs remain the policy source of truth | Approved |
| D-004 | Use AWS-first deployment pattern in `ap-south-1` | Product specification provides ECS/RDS/S3/SQS baseline and Mumbai default | Approved as architecture; no provisioning yet |
| D-005 | Isolate persistent `dev`, `stag`, `prod` environments | Release promotion and customer-data separation require distinct resources and identities | Approved as policy; provider/account setup pending |
| D-006 | Treat provided flow and branding sheet as visual references; keep originals in repository | Avoid relying on generated reference content as implemented functionality; validate claims against product requirements | Approved |
| D-007 | Tailwind CSS is not part of native Android UI | Using it would require a web front end/WebView and would conflict with native-only scope | Accepted consequence of D-001 |
| D-008 | Feature work advances only after product-owner approval of each phase; commits require separate approval | Explicit user workflow instruction | Approved |
| D-009 | Google sign-in uses Android Credential Manager; FastAPI verifies the Google ID token and issues Bridge opaque sessions. Email/password uses the same session service. Cognito is not used as the MVP session issuer. | Product owner selected native Credential Manager after reviewing the Cognito hosted-login tradeoff | Approved |

## Pending external decisions

- AWS account boundaries, domain/DNS ownership, cost limits, and production approvers.
- Google server OAuth client registration and app signing fingerprints; GitHub/LinkedIn provider app registration and approved API scopes.
- Email verification/reset delivery via SES SMTP and a verified sender/domain; Mailpit is used for local development.
- Email sender/domain, Firebase project ownership, and privacy/retention/legal review.
- Android application ID, signing ownership, Play Console access, and final target SDK at release.
- Whether GitHub branch protections and Actions environments can be configured by the repository owner.
- Final font and precise color tokens/logo export from the supplied branding sheet.
