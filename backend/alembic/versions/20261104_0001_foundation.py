"""Create user, identity, consent, profile, and private resume metadata.

Revision ID: 20261104_0001
Revises:
"""

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

revision = "20261104_0001"
down_revision = None
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        "bridge_users",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column("email", sa.String(320), unique=True),
        sa.Column("account_status", sa.String(24), nullable=False, server_default="active"),
        sa.Column("email_verified_at", sa.DateTime(timezone=True)),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
        sa.Column("deleted_at", sa.DateTime(timezone=True)),
    )
    op.create_table(
        "linked_identities",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column(
            "user_id",
            postgresql.UUID(as_uuid=True),
            sa.ForeignKey("bridge_users.id", ondelete="CASCADE"),
            nullable=False,
        ),
        sa.Column("provider", sa.String(32), nullable=False),
        sa.Column("issuer", sa.String(512), nullable=False),
        sa.Column("subject", sa.String(512), nullable=False),
        sa.Column("provider_email", sa.String(320)),
        sa.Column("provider_email_verified", sa.Boolean(), nullable=False, server_default=sa.false()),
        sa.Column("access_token_ciphertext", sa.LargeBinary()),
        sa.Column("refresh_token_ciphertext", sa.LargeBinary()),
        sa.Column("token_expires_at", sa.DateTime(timezone=True)),
        sa.Column("connected_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
        sa.UniqueConstraint("issuer", "subject", name="uq_linked_identity_issuer_subject"),
    )
    op.create_index("ix_linked_identities_user_id", "linked_identities", ["user_id"])
    op.create_table(
        "consent_records",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column(
            "user_id",
            postgresql.UUID(as_uuid=True),
            sa.ForeignKey("bridge_users.id", ondelete="CASCADE"),
            nullable=False,
        ),
        sa.Column("consent_type", sa.String(48), nullable=False),
        sa.Column("policy_version", sa.String(64), nullable=False),
        sa.Column("accepted_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
        sa.Column("provider", sa.String(32)),
        sa.UniqueConstraint("user_id", "consent_type", "policy_version", name="uq_consent_user_type_version"),
    )
    op.create_index("ix_consent_records_user_id", "consent_records", ["user_id"])
    op.create_table(
        "profiles",
        sa.Column(
            "user_id",
            postgresql.UUID(as_uuid=True),
            sa.ForeignKey("bridge_users.id", ondelete="CASCADE"),
            primary_key=True,
        ),
        sa.Column("full_name", sa.String(160)),
        sa.Column("headline", sa.String(240)),
        sa.Column("current_role", sa.String(160)),
        sa.Column("current_company", sa.String(160)),
        sa.Column("summary", sa.Text()),
        sa.Column("years_experience", sa.Integer()),
        sa.Column("location", sa.String(160)),
        sa.Column("timezone", sa.String(64)),
        sa.Column("work_history", postgresql.JSONB(), nullable=False, server_default=sa.text("'[]'::jsonb")),
        sa.Column("education", postgresql.JSONB(), nullable=False, server_default=sa.text("'[]'::jsonb")),
        sa.Column("skills", postgresql.JSONB(), nullable=False, server_default=sa.text("'[]'::jsonb")),
        sa.Column("projects", postgresql.JSONB(), nullable=False, server_default=sa.text("'[]'::jsonb")),
        sa.Column("achievements", postgresql.JSONB(), nullable=False, server_default=sa.text("'[]'::jsonb")),
        sa.Column("certifications", postgresql.JSONB(), nullable=False, server_default=sa.text("'[]'::jsonb")),
        sa.Column("portfolio_links", postgresql.JSONB(), nullable=False, server_default=sa.text("'[]'::jsonb")),
        sa.Column("visibility", sa.String(24), nullable=False, server_default="private"),
        sa.Column("confirmed_at", sa.DateTime(timezone=True)),
        sa.Column("published_at", sa.DateTime(timezone=True)),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
    )
    op.create_table(
        "profile_field_provenance",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column(
            "user_id",
            postgresql.UUID(as_uuid=True),
            sa.ForeignKey("profiles.user_id", ondelete="CASCADE"),
            nullable=False,
        ),
        sa.Column("field_key", sa.String(128), nullable=False),
        sa.Column("source_type", sa.String(24), nullable=False),
        sa.Column("source_reference", sa.String(512)),
        sa.Column("imported_at", sa.DateTime(timezone=True)),
        sa.Column("user_confirmed", sa.Boolean(), nullable=False, server_default=sa.false()),
        sa.Column("visibility", sa.String(24), nullable=False, server_default="private"),
        sa.UniqueConstraint("user_id", "field_key", "source_type", name="uq_profile_field_source"),
    )
    op.create_index("ix_profile_field_provenance_user_id", "profile_field_provenance", ["user_id"])
    op.create_table(
        "resume_assets",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column(
            "user_id",
            postgresql.UUID(as_uuid=True),
            sa.ForeignKey("bridge_users.id", ondelete="CASCADE"),
            nullable=False,
        ),
        sa.Column("object_key", sa.String(1024), unique=True, nullable=False),
        sa.Column("content_type", sa.String(128), nullable=False),
        sa.Column("file_size", sa.Integer(), nullable=False),
        sa.Column("status", sa.String(24), nullable=False, server_default="pending_upload"),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
        sa.Column("deleted_at", sa.DateTime(timezone=True)),
    )
    op.create_index("ix_resume_assets_user_id", "resume_assets", ["user_id"])
    op.create_table(
        "profile_import_jobs",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column(
            "user_id",
            postgresql.UUID(as_uuid=True),
            sa.ForeignKey("bridge_users.id", ondelete="CASCADE"),
            nullable=False,
        ),
        sa.Column("source_type", sa.String(24), nullable=False),
        sa.Column("status", sa.String(24), nullable=False, server_default="queued"),
        sa.Column("failure_code", sa.String(64)),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
    )
    op.create_index("ix_profile_import_jobs_user_id", "profile_import_jobs", ["user_id"])
    op.create_table(
        "password_credentials",
        sa.Column(
            "user_id",
            postgresql.UUID(as_uuid=True),
            sa.ForeignKey("bridge_users.id", ondelete="CASCADE"),
            primary_key=True,
        ),
        sa.Column("password_hash", sa.String(512), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
    )
    op.create_table(
        "auth_challenges",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column("purpose", sa.String(32), nullable=False),
        sa.Column("user_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("bridge_users.id", ondelete="CASCADE")),
        sa.Column("subject_hash", sa.String(64)),
        sa.Column("code_hash", sa.String(64), nullable=False),
        sa.Column("nonce_hash", sa.String(64)),
        sa.Column("email", sa.String(320)),
        sa.Column("expires_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("consumed_at", sa.DateTime(timezone=True)),
        sa.Column("attempts", sa.Integer(), nullable=False, server_default="0"),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
    )
    op.create_index("ix_auth_challenges_subject_hash", "auth_challenges", ["subject_hash"])
    op.create_table(
        "user_sessions",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column("family_id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column(
            "user_id",
            postgresql.UUID(as_uuid=True),
            sa.ForeignKey("bridge_users.id", ondelete="CASCADE"),
            nullable=False,
        ),
        sa.Column("access_token_hash", sa.String(64), unique=True, nullable=False),
        sa.Column("refresh_token_hash", sa.String(64), unique=True, nullable=False),
        sa.Column("access_expires_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("refresh_expires_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
        sa.Column("last_used_at", sa.DateTime(timezone=True)),
        sa.Column("revoked_at", sa.DateTime(timezone=True)),
        sa.Column("device_label", sa.String(120)),
    )
    op.create_index("ix_user_sessions_user_id", "user_sessions", ["user_id"])
    op.create_table(
        "refresh_token_history",
        sa.Column("token_hash", sa.String(64), primary_key=True),
        sa.Column(
            "session_id",
            postgresql.UUID(as_uuid=True),
            sa.ForeignKey("user_sessions.id", ondelete="CASCADE"),
            nullable=False,
        ),
        sa.Column("used_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
    )
    op.create_table(
        "rate_limit_buckets",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column("action", sa.String(48), nullable=False),
        sa.Column("bucket_key", sa.String(64), nullable=False),
        sa.Column("window_started_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("request_count", sa.Integer(), nullable=False, server_default="1"),
        sa.UniqueConstraint("action", "bucket_key", name="uq_rate_limit_action_bucket"),
    )


def downgrade() -> None:
    op.drop_table("rate_limit_buckets")
    op.drop_table("refresh_token_history")
    op.drop_index("ix_user_sessions_user_id", table_name="user_sessions")
    op.drop_table("user_sessions")
    op.drop_index("ix_auth_challenges_subject_hash", table_name="auth_challenges")
    op.drop_table("auth_challenges")
    op.drop_table("password_credentials")
    op.drop_index("ix_profile_import_jobs_user_id", table_name="profile_import_jobs")
    op.drop_table("profile_import_jobs")
    op.drop_index("ix_resume_assets_user_id", table_name="resume_assets")
    op.drop_table("resume_assets")
    op.drop_index("ix_profile_field_provenance_user_id", table_name="profile_field_provenance")
    op.drop_table("profile_field_provenance")
    op.drop_table("profiles")
    op.drop_index("ix_consent_records_user_id", table_name="consent_records")
    op.drop_table("consent_records")
    op.drop_index("ix_linked_identities_user_id", table_name="linked_identities")
    op.drop_table("linked_identities")
    op.drop_table("bridge_users")
