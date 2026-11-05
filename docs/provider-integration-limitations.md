# Provider integration limitations

This foundation creates provider-neutral boundaries and records dependencies. It does not register provider applications, request credentials, contact providers, or claim that any account has been approved.

## Authentication decisions

The product owner selected Android Credential Manager for Google sign-in. The app obtains a Google ID token, adds a server-issued nonce, and sends it to FastAPI over HTTPS. FastAPI verifies signature, issuer, audience, expiry, verified email, subject, and nonce using Google's supported verification library. It then issues Bridge access/refresh tokens whose hashes are stored in PostgreSQL. Refresh tokens rotate; reuse revokes the session family. Google tokens are never returned by Bridge APIs or stored as account session tokens.

The MVP no longer uses Cognito as the Bridge session issuer. Email/password verification and password reset use short-lived one-time codes delivered via SES SMTP in hosted environments and a local Mailpit service in development. Requires the Google web OAuth client ID, email sender verification, and environment-specific secret configuration before sign-in can run end to end.

| Provider/service | Planned use | Limit / fallback |
|---|---|---|
| Google | Sign-in | Requires an owner-controlled OAuth project, consent screen, verified redirect URIs, and environment-specific client IDs. Email sign-in remains the fallback. |
| GitHub | Sign-in/connection and supported public profile import | Requires approved OAuth app/scopes and user consent. Import only fields available through the approved official API; no private repositories without explicit scope. Manual entry remains available. |
| LinkedIn | Connection/import only where official product API access is approved | No scraping or unofficial libraries. Public profile API access is limited and may require LinkedIn program approval. Manual entry and user-provided LinkedIn export/PDF remain fallbacks. OAuth login alone does not verify identity, employment, or history. |
| Resume parser | PDF/DOCX parsing | Requires file validation, malware checks, privacy/retention decision, and parser selection. Parsing is asynchronous; user reviews and confirms fields. Raw resume remains private. |
| AWS | Hosting, PostgreSQL, private objects, queue, secrets, observability | Needs authorized accounts, IAM/OIDC role setup, billing/cost controls, domain and region approval. No infrastructure deployed here. |
| Cognito | Not used as the MVP session issuer | Reconsider only through a separately approved architecture change. |
| Firebase FCM | Android push | Requires owned Firebase project(s), Android app registration, environment-specific config, notification consent and privacy review. |
| Email provider | Transactional notifications | SES needs sender/domain verification, SPF/DKIM/DMARC and bounce/complaint handling; no sender configured. |
| GitHub Actions | CI and later deployments | Requires Actions enabled, branch/environment protections, AWS OIDC roles, security scanning decisions and environment secrets; CI files alone do not create these controls. |

Never fabricate unavailable provider fields, score evidence, verification states, recommendations, or percentile claims. Document each integration's approved scopes and fallback behavior before implementing its feature phase.
