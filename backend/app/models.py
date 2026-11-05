from __future__ import annotations

from datetime import datetime
from typing import Any
from uuid import UUID, uuid4

from sqlalchemy import (
    Boolean,
    DateTime,
    ForeignKey,
    Integer,
    LargeBinary,
    String,
    Text,
    UniqueConstraint,
    func,
)
from sqlalchemy.dialects.postgresql import JSONB, UUID as PostgresUUID
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column, relationship


class Base(DeclarativeBase):
    pass


class BridgeUser(Base):
    __tablename__ = "bridge_users"

    id: Mapped[UUID] = mapped_column(PostgresUUID(as_uuid=True), primary_key=True, default=uuid4)
    email: Mapped[str | None] = mapped_column(String(320), unique=True)
    account_status: Mapped[str] = mapped_column(String(24), nullable=False, default="active")
    email_verified_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, server_default=func.now(), onupdate=func.now()
    )
    deleted_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))

    identities: Mapped[list[LinkedIdentity]] = relationship(back_populates="user")
    consents: Mapped[list[ConsentRecord]] = relationship(back_populates="user")
    profile: Mapped[Profile | None] = relationship(back_populates="user", uselist=False)


class LinkedIdentity(Base):
    __tablename__ = "linked_identities"
    __table_args__ = (UniqueConstraint("issuer", "subject", name="uq_linked_identity_issuer_subject"),)

    id: Mapped[UUID] = mapped_column(PostgresUUID(as_uuid=True), primary_key=True, default=uuid4)
    user_id: Mapped[UUID] = mapped_column(
        PostgresUUID(as_uuid=True), ForeignKey("bridge_users.id", ondelete="CASCADE"), nullable=False
    )
    provider: Mapped[str] = mapped_column(String(32), nullable=False)
    issuer: Mapped[str] = mapped_column(String(512), nullable=False)
    subject: Mapped[str] = mapped_column(String(512), nullable=False)
    provider_email: Mapped[str | None] = mapped_column(String(320))
    provider_email_verified: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    access_token_ciphertext: Mapped[bytes | None] = mapped_column(LargeBinary)
    refresh_token_ciphertext: Mapped[bytes | None] = mapped_column(LargeBinary)
    token_expires_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    connected_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )

    user: Mapped[BridgeUser] = relationship(back_populates="identities")


class ConsentRecord(Base):
    __tablename__ = "consent_records"
    __table_args__ = (
        UniqueConstraint(
            "user_id", "consent_type", "policy_version", name="uq_consent_user_type_version"
        ),
    )

    id: Mapped[UUID] = mapped_column(PostgresUUID(as_uuid=True), primary_key=True, default=uuid4)
    user_id: Mapped[UUID] = mapped_column(
        PostgresUUID(as_uuid=True), ForeignKey("bridge_users.id", ondelete="CASCADE"), nullable=False
    )
    consent_type: Mapped[str] = mapped_column(String(48), nullable=False)
    policy_version: Mapped[str] = mapped_column(String(64), nullable=False)
    accepted_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )
    provider: Mapped[str | None] = mapped_column(String(32))

    user: Mapped[BridgeUser] = relationship(back_populates="consents")


class Profile(Base):
    __tablename__ = "profiles"

    user_id: Mapped[UUID] = mapped_column(
        PostgresUUID(as_uuid=True), ForeignKey("bridge_users.id", ondelete="CASCADE"), primary_key=True
    )
    full_name: Mapped[str | None] = mapped_column(String(160))
    headline: Mapped[str | None] = mapped_column(String(240))
    current_role: Mapped[str | None] = mapped_column(String(160))
    current_company: Mapped[str | None] = mapped_column(String(160))
    summary: Mapped[str | None] = mapped_column(Text)
    years_experience: Mapped[int | None] = mapped_column(Integer)
    location: Mapped[str | None] = mapped_column(String(160))
    timezone: Mapped[str | None] = mapped_column(String(64))
    work_history: Mapped[list[dict[str, Any]]] = mapped_column(
        JSONB, nullable=False, default=list, server_default="[]"
    )
    education: Mapped[list[dict[str, Any]]] = mapped_column(
        JSONB, nullable=False, default=list, server_default="[]"
    )
    skills: Mapped[list[str]] = mapped_column(JSONB, nullable=False, default=list, server_default="[]")
    projects: Mapped[list[dict[str, Any]]] = mapped_column(
        JSONB, nullable=False, default=list, server_default="[]"
    )
    achievements: Mapped[list[dict[str, Any]]] = mapped_column(
        JSONB, nullable=False, default=list, server_default="[]"
    )
    certifications: Mapped[list[dict[str, Any]]] = mapped_column(
        JSONB, nullable=False, default=list, server_default="[]"
    )
    portfolio_links: Mapped[list[dict[str, str]]] = mapped_column(
        JSONB, nullable=False, default=list, server_default="[]"
    )
    visibility: Mapped[str] = mapped_column(String(24), nullable=False, default="private")
    score_visibility: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True, server_default="true")
    confirmed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    published_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, server_default=func.now(), onupdate=func.now()
    )

    user: Mapped[BridgeUser] = relationship(back_populates="profile")
    provenance: Mapped[list[ProfileFieldProvenance]] = relationship(back_populates="profile")


class ProfileFieldProvenance(Base):
    __tablename__ = "profile_field_provenance"
    __table_args__ = (
        UniqueConstraint("user_id", "field_key", "source_type", name="uq_profile_field_source"),
    )

    id: Mapped[UUID] = mapped_column(PostgresUUID(as_uuid=True), primary_key=True, default=uuid4)
    user_id: Mapped[UUID] = mapped_column(
        PostgresUUID(as_uuid=True), ForeignKey("profiles.user_id", ondelete="CASCADE"), nullable=False
    )
    field_key: Mapped[str] = mapped_column(String(128), nullable=False)
    source_type: Mapped[str] = mapped_column(String(24), nullable=False)
    source_reference: Mapped[str | None] = mapped_column(String(512))
    imported_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    user_confirmed: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    visibility: Mapped[str] = mapped_column(String(24), nullable=False, default="private")

    profile: Mapped[Profile] = relationship(back_populates="provenance")


class ResumeAsset(Base):
    __tablename__ = "resume_assets"

    id: Mapped[UUID] = mapped_column(PostgresUUID(as_uuid=True), primary_key=True, default=uuid4)
    user_id: Mapped[UUID] = mapped_column(
        PostgresUUID(as_uuid=True), ForeignKey("bridge_users.id", ondelete="CASCADE"), nullable=False
    )
    object_key: Mapped[str] = mapped_column(String(1024), unique=True, nullable=False)
    content_type: Mapped[str] = mapped_column(String(128), nullable=False)
    file_size: Mapped[int] = mapped_column(Integer, nullable=False)
    status: Mapped[str] = mapped_column(String(24), nullable=False, default="pending_upload")
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )
    deleted_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))


class ImportJob(Base):
    __tablename__ = "profile_import_jobs"

    id: Mapped[UUID] = mapped_column(PostgresUUID(as_uuid=True), primary_key=True, default=uuid4)
    user_id: Mapped[UUID] = mapped_column(
        PostgresUUID(as_uuid=True), ForeignKey("bridge_users.id", ondelete="CASCADE"), nullable=False
    )
    source_type: Mapped[str] = mapped_column(String(24), nullable=False)
    status: Mapped[str] = mapped_column(String(24), nullable=False, default="queued")
    failure_code: Mapped[str | None] = mapped_column(String(64))
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, server_default=func.now(), onupdate=func.now()
    )


class PasswordCredential(Base):
    __tablename__ = "password_credentials"

    user_id: Mapped[UUID] = mapped_column(
        PostgresUUID(as_uuid=True), ForeignKey("bridge_users.id", ondelete="CASCADE"), primary_key=True
    )
    password_hash: Mapped[str] = mapped_column(String(512), nullable=False)
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, server_default=func.now(), onupdate=func.now()
    )


class AuthChallenge(Base):
    __tablename__ = "auth_challenges"

    id: Mapped[UUID] = mapped_column(PostgresUUID(as_uuid=True), primary_key=True, default=uuid4)
    purpose: Mapped[str] = mapped_column(String(32), nullable=False)
    user_id: Mapped[UUID | None] = mapped_column(
        PostgresUUID(as_uuid=True), ForeignKey("bridge_users.id", ondelete="CASCADE")
    )
    subject_hash: Mapped[str | None] = mapped_column(String(64))
    code_hash: Mapped[str] = mapped_column(String(64), nullable=False)
    nonce_hash: Mapped[str | None] = mapped_column(String(64))
    email: Mapped[str | None] = mapped_column(String(320))
    expires_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    consumed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    attempts: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )


class UserSession(Base):
    __tablename__ = "user_sessions"

    id: Mapped[UUID] = mapped_column(PostgresUUID(as_uuid=True), primary_key=True, default=uuid4)
    family_id: Mapped[UUID] = mapped_column(PostgresUUID(as_uuid=True), nullable=False)
    user_id: Mapped[UUID] = mapped_column(
        PostgresUUID(as_uuid=True), ForeignKey("bridge_users.id", ondelete="CASCADE"), nullable=False
    )
    access_token_hash: Mapped[str] = mapped_column(String(64), unique=True, nullable=False)
    refresh_token_hash: Mapped[str] = mapped_column(String(64), unique=True, nullable=False)
    access_expires_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    refresh_expires_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )
    last_used_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    revoked_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    device_label: Mapped[str | None] = mapped_column(String(120))


class RefreshTokenHistory(Base):
    __tablename__ = "refresh_token_history"

    token_hash: Mapped[str] = mapped_column(String(64), primary_key=True)
    session_id: Mapped[UUID] = mapped_column(
        PostgresUUID(as_uuid=True), ForeignKey("user_sessions.id", ondelete="CASCADE"), nullable=False
    )
    used_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )


class RateLimitBucket(Base):
    __tablename__ = "rate_limit_buckets"
    __table_args__ = (UniqueConstraint("action", "bucket_key", name="uq_rate_limit_action_bucket"),)

    id: Mapped[UUID] = mapped_column(PostgresUUID(as_uuid=True), primary_key=True, default=uuid4)
    action: Mapped[str] = mapped_column(String(48), nullable=False)
    bucket_key: Mapped[str] = mapped_column(String(64), nullable=False)
    window_started_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    request_count: Mapped[int] = mapped_column(Integer, nullable=False, default=1)


class BridgeScoreSnapshot(Base):
    """Private, versioned score history. Internal dimensions must never reach member APIs."""

    __tablename__ = "bridge_score_snapshots"

    id: Mapped[UUID] = mapped_column(PostgresUUID(as_uuid=True), primary_key=True, default=uuid4)
    user_id: Mapped[UUID] = mapped_column(
        PostgresUUID(as_uuid=True), ForeignKey("bridge_users.id", ondelete="CASCADE"), nullable=False
    )
    state: Mapped[str] = mapped_column(String(24), nullable=False)
    score: Mapped[int | None] = mapped_column(Integer)
    model_version: Mapped[str] = mapped_column(String(64), nullable=False)
    internal_dimensions: Mapped[dict[str, Any]] = mapped_column(JSONB, nullable=False, default=dict)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )


class VerificationReview(Base):
    __tablename__ = "verification_reviews"

    user_id: Mapped[UUID] = mapped_column(
        PostgresUUID(as_uuid=True), ForeignKey("bridge_users.id", ondelete="CASCADE"), primary_key=True
    )
    state: Mapped[str] = mapped_column(String(24), nullable=False, default="NOT_STARTED")
    requested_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, server_default=func.now(), onupdate=func.now()
    )


class ProfessionalIntent(Base):
    __tablename__ = "professional_intents"

    id: Mapped[UUID] = mapped_column(PostgresUUID(as_uuid=True), primary_key=True, default=uuid4)
    slug: Mapped[str] = mapped_column(String(64), unique=True, nullable=False)
    label: Mapped[str] = mapped_column(String(80), unique=True, nullable=False)
    active: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True, server_default="true")


class MemberIntent(Base):
    __tablename__ = "member_intents"
    __table_args__ = (UniqueConstraint("user_id", "intent_id", name="uq_member_intent"),)

    id: Mapped[UUID] = mapped_column(PostgresUUID(as_uuid=True), primary_key=True, default=uuid4)
    user_id: Mapped[UUID] = mapped_column(
        PostgresUUID(as_uuid=True), ForeignKey("bridge_users.id", ondelete="CASCADE"), nullable=False
    )
    intent_id: Mapped[UUID] = mapped_column(
        PostgresUUID(as_uuid=True), ForeignKey("professional_intents.id", ondelete="CASCADE"), nullable=False
    )
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, server_default=func.now())


class ConnectionRequest(Base):
    __tablename__ = "connection_requests"
    __table_args__ = (UniqueConstraint("requester_id", "recipient_id", name="uq_connection_direction"),)

    id: Mapped[UUID] = mapped_column(PostgresUUID(as_uuid=True), primary_key=True, default=uuid4)
    requester_id: Mapped[UUID] = mapped_column(PostgresUUID(as_uuid=True), ForeignKey("bridge_users.id", ondelete="CASCADE"), nullable=False)
    recipient_id: Mapped[UUID] = mapped_column(PostgresUUID(as_uuid=True), ForeignKey("bridge_users.id", ondelete="CASCADE"), nullable=False)
    status: Mapped[str] = mapped_column(String(16), nullable=False, default="PENDING")
    note: Mapped[str | None] = mapped_column(String(500))
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, server_default=func.now())
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, server_default=func.now(), onupdate=func.now())


class MemberBlock(Base):
    __tablename__ = "member_blocks"
    __table_args__ = (UniqueConstraint("blocker_id", "blocked_id", name="uq_member_block"),)

    id: Mapped[UUID] = mapped_column(PostgresUUID(as_uuid=True), primary_key=True, default=uuid4)
    blocker_id: Mapped[UUID] = mapped_column(PostgresUUID(as_uuid=True), ForeignKey("bridge_users.id", ondelete="CASCADE"), nullable=False)
    blocked_id: Mapped[UUID] = mapped_column(PostgresUUID(as_uuid=True), ForeignKey("bridge_users.id", ondelete="CASCADE"), nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, server_default=func.now())


class DirectConversation(Base):
    __tablename__ = "direct_conversations"
    __table_args__ = (UniqueConstraint("member_low_id", "member_high_id", name="uq_direct_pair"),)

    id: Mapped[UUID] = mapped_column(PostgresUUID(as_uuid=True), primary_key=True, default=uuid4)
    member_low_id: Mapped[UUID] = mapped_column(PostgresUUID(as_uuid=True), ForeignKey("bridge_users.id", ondelete="CASCADE"), nullable=False)
    member_high_id: Mapped[UUID] = mapped_column(PostgresUUID(as_uuid=True), ForeignKey("bridge_users.id", ondelete="CASCADE"), nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, server_default=func.now())


class DirectMessage(Base):
    __tablename__ = "direct_messages"

    id: Mapped[UUID] = mapped_column(PostgresUUID(as_uuid=True), primary_key=True, default=uuid4)
    conversation_id: Mapped[UUID] = mapped_column(PostgresUUID(as_uuid=True), ForeignKey("direct_conversations.id", ondelete="CASCADE"), nullable=False)
    sender_id: Mapped[UUID] = mapped_column(PostgresUUID(as_uuid=True), ForeignKey("bridge_users.id", ondelete="CASCADE"), nullable=False)
    body: Mapped[str] = mapped_column(Text, nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, server_default=func.now())


class Cohort(Base):
    __tablename__ = "cohorts"

    id: Mapped[UUID] = mapped_column(PostgresUUID(as_uuid=True), primary_key=True, default=uuid4)
    name: Mapped[str] = mapped_column(String(120), nullable=False)
    topic: Mapped[str] = mapped_column(String(100), nullable=False)
    description: Mapped[str] = mapped_column(Text, nullable=False)
    visibility: Mapped[str] = mapped_column(String(16), nullable=False, default="PUBLIC")
    created_by: Mapped[UUID] = mapped_column(PostgresUUID(as_uuid=True), ForeignKey("bridge_users.id", ondelete="CASCADE"), nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, server_default=func.now())


class CohortMembership(Base):
    __tablename__ = "cohort_memberships"
    __table_args__ = (UniqueConstraint("cohort_id", "user_id", name="uq_cohort_member"),)

    id: Mapped[UUID] = mapped_column(PostgresUUID(as_uuid=True), primary_key=True, default=uuid4)
    cohort_id: Mapped[UUID] = mapped_column(PostgresUUID(as_uuid=True), ForeignKey("cohorts.id", ondelete="CASCADE"), nullable=False)
    user_id: Mapped[UUID] = mapped_column(PostgresUUID(as_uuid=True), ForeignKey("bridge_users.id", ondelete="CASCADE"), nullable=False)
    role: Mapped[str] = mapped_column(String(16), nullable=False, default="MEMBER")
    status: Mapped[str] = mapped_column(String(16), nullable=False, default="ACTIVE")
    joined_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, server_default=func.now())


class CohortPost(Base):
    __tablename__ = "cohort_posts"

    id: Mapped[UUID] = mapped_column(PostgresUUID(as_uuid=True), primary_key=True, default=uuid4)
    cohort_id: Mapped[UUID] = mapped_column(PostgresUUID(as_uuid=True), ForeignKey("cohorts.id", ondelete="CASCADE"), nullable=False)
    author_id: Mapped[UUID] = mapped_column(PostgresUUID(as_uuid=True), ForeignKey("bridge_users.id", ondelete="CASCADE"), nullable=False)
    body: Mapped[str] = mapped_column(Text, nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, server_default=func.now())


class Pod(Base):
    __tablename__ = "pods"

    id: Mapped[UUID] = mapped_column(PostgresUUID(as_uuid=True), primary_key=True, default=uuid4)
    name: Mapped[str] = mapped_column(String(120), nullable=False)
    goal: Mapped[str] = mapped_column(String(500), nullable=False)
    guidelines: Mapped[str | None] = mapped_column(String(2000))
    discoverable: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    status: Mapped[str] = mapped_column(String(16), nullable=False, default="ACTIVE")
    expires_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    created_by: Mapped[UUID] = mapped_column(PostgresUUID(as_uuid=True), ForeignKey("bridge_users.id", ondelete="CASCADE"), nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, server_default=func.now())


class PodMembership(Base):
    __tablename__ = "pod_memberships"
    __table_args__ = (UniqueConstraint("pod_id", "user_id", name="uq_pod_member"),)

    id: Mapped[UUID] = mapped_column(PostgresUUID(as_uuid=True), primary_key=True, default=uuid4)
    pod_id: Mapped[UUID] = mapped_column(PostgresUUID(as_uuid=True), ForeignKey("pods.id", ondelete="CASCADE"), nullable=False)
    user_id: Mapped[UUID] = mapped_column(PostgresUUID(as_uuid=True), ForeignKey("bridge_users.id", ondelete="CASCADE"), nullable=False)
    role: Mapped[str] = mapped_column(String(16), nullable=False, default="MEMBER")
    joined_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, server_default=func.now())


class PodMessage(Base):
    __tablename__ = "pod_messages"

    id: Mapped[UUID] = mapped_column(PostgresUUID(as_uuid=True), primary_key=True, default=uuid4)
    pod_id: Mapped[UUID] = mapped_column(PostgresUUID(as_uuid=True), ForeignKey("pods.id", ondelete="CASCADE"), nullable=False)
    sender_id: Mapped[UUID] = mapped_column(PostgresUUID(as_uuid=True), ForeignKey("bridge_users.id", ondelete="CASCADE"), nullable=False)
    body: Mapped[str] = mapped_column(Text, nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, server_default=func.now())


class ReferralRequest(Base):
    __tablename__ = "referral_requests"

    id: Mapped[UUID] = mapped_column(PostgresUUID(as_uuid=True), primary_key=True, default=uuid4)
    requester_id: Mapped[UUID] = mapped_column(PostgresUUID(as_uuid=True), ForeignKey("bridge_users.id", ondelete="CASCADE"), nullable=False)
    recipient_id: Mapped[UUID] = mapped_column(PostgresUUID(as_uuid=True), ForeignKey("bridge_users.id", ondelete="CASCADE"), nullable=False)
    company: Mapped[str] = mapped_column(String(160), nullable=False)
    role: Mapped[str] = mapped_column(String(160), nullable=False)
    message: Mapped[str] = mapped_column(String(2000), nullable=False)
    status: Mapped[str] = mapped_column(String(16), nullable=False, default="SENT")
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, server_default=func.now())


class MemberReport(Base):
    __tablename__ = "member_reports"

    id: Mapped[UUID] = mapped_column(PostgresUUID(as_uuid=True), primary_key=True, default=uuid4)
    reporter_id: Mapped[UUID] = mapped_column(PostgresUUID(as_uuid=True), ForeignKey("bridge_users.id", ondelete="CASCADE"), nullable=False)
    reported_id: Mapped[UUID] = mapped_column(PostgresUUID(as_uuid=True), ForeignKey("bridge_users.id", ondelete="CASCADE"), nullable=False)
    reason: Mapped[str] = mapped_column(String(40), nullable=False)
    details: Mapped[str | None] = mapped_column(String(2000))
    status: Mapped[str] = mapped_column(String(16), nullable=False, default="OPEN")
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, server_default=func.now())


class BridgeNotification(Base):
    __tablename__ = "bridge_notifications"

    id: Mapped[UUID] = mapped_column(PostgresUUID(as_uuid=True), primary_key=True, default=uuid4)
    user_id: Mapped[UUID] = mapped_column(PostgresUUID(as_uuid=True), ForeignKey("bridge_users.id", ondelete="CASCADE"), nullable=False)
    kind: Mapped[str] = mapped_column(String(32), nullable=False)
    title: Mapped[str] = mapped_column(String(160), nullable=False)
    body: Mapped[str] = mapped_column(String(500), nullable=False)
    payload: Mapped[dict[str, Any]] = mapped_column(JSONB, nullable=False, default=dict)
    read_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, server_default=func.now())
