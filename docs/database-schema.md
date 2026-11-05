# Database schema plan

**Status:** Local migrations create identity, consent, draft-profile, provenance, resume metadata, import jobs, private score snapshots, verification review state, intents, connection requests/blocks, direct conversations/messages, Cohorts/memberships/posts, Pods/memberships/messages, referral requests, and member reports. The local development database has been migrated. No production data has been created.

## Database conventions

- PostgreSQL 16 baseline for local development; managed PostgreSQL in hosted environments.
- UUID primary keys, UTC timestamps, explicit foreign keys, and indexes for common lookup paths.
- Keep provider subject IDs in a separate identity table; never use email or provider IDs as Bridge's primary key.
- Store provenance, consent, visibility, and retention metadata where user-provided/imported data is persisted.
- Schema migrations are forward-compatible expand-and-contract changes. Run migrations once as a controlled release task, never during each API replica startup.
- Do not write production rows or copy production data into `dev`/`stag`.

## Planned entity groups

| Feature phase | Core entities | Key constraints |
|---|---|---|
| Authentication/profile | `bridge_users`, `linked_identities`, `password_credentials`, `user_sessions`, `refresh_token_history`, `auth_challenges`, `rate_limit_buckets`, `consent_records`, `profiles`, `profile_field_provenance`, `resume_assets`, `profile_import_jobs` | `(issuer, subject)` unique; passwords are Argon2id hashes; access/refresh tokens and one-time codes are stored as hashes; raw resume objects private; consent versioned and timestamped |
| Score/verification | `bridge_score_snapshots`, `verification_reviews` | versioned private score history; internal evidence dimensions excluded from member API; reviewer decisions are not enabled until staff roles and audit workflow exist |
| Intent/discovery | `professional_intents`, `member_intents` | at most 3 active intents per user; API validates before replacing selections |
| Connections/chat | `connection_requests`, `member_blocks`, `direct_conversations`, `direct_messages` | accepted connection required for chat; membership and block status checked for reads and writes |
| Cohorts/Pods | `cohorts`, `cohort_memberships`, `cohort_posts`, `pods`, `pod_memberships`, `pod_messages` | Cohort discussions require membership; private Pod data requires membership; Pod capacity check locks the Pod row and caps membership at 10 |
| Referrals/safety | `referral_requests`, `member_reports` | accepted connection required; requester row lock serializes the maximum of 10 lifetime requests |
| Platform | `bridge_notifications` | owner-scoped in-app inbox with read timestamps; device tokens, outbox delivery, export/deletion requests remain future work |

Notification/device-token/outbox, comments/reactions, invitations/resources, moderation decision/audit, and account export/deletion tables remain future schema work.

## Data lifecycle

- Raw resume uploads are private objects and are deleted according to the approved retention policy; public profiles never contain raw resume files.
- Profile-source provenance distinguishes imported, user-edited, and system-calculated values.
- Private messages and group data are only returned to authorized members; moderation access is audited.
- Account export and deletion use auditable request records and honor legal/retention obligations.
