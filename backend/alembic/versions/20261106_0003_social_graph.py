"""Add intents, connection requests, and blocking for Bridge Discover."""

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

revision = "20261106_0003"
down_revision = "20261105_0002"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        "professional_intents",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column("slug", sa.String(64), nullable=False, unique=True),
        sa.Column("label", sa.String(80), nullable=False, unique=True),
        sa.Column("active", sa.Boolean(), nullable=False, server_default=sa.true()),
    )
    intents = ["Job Switch", "Mentorship", "Interview Prep", "Co-founder", "System Design", "Open Source", "Collaboration", "Hiring"]
    op.bulk_insert(
        sa.table("professional_intents", sa.column("id", postgresql.UUID(as_uuid=True)), sa.column("slug"), sa.column("label"), sa.column("active")),
        [{"id": __import__("uuid").uuid4(), "slug": item.lower().replace(" ", "-"), "label": item, "active": True} for item in intents],
    )
    op.create_table(
        "member_intents",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column("user_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("bridge_users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("intent_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("professional_intents.id", ondelete="CASCADE"), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
        sa.UniqueConstraint("user_id", "intent_id", name="uq_member_intent"),
    )
    op.create_table(
        "connection_requests",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column("requester_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("bridge_users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("recipient_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("bridge_users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("status", sa.String(16), nullable=False, server_default="PENDING"),
        sa.Column("note", sa.String(500)),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
        sa.UniqueConstraint("requester_id", "recipient_id", name="uq_connection_direction"),
    )
    op.create_index("ix_connection_requests_recipient_status", "connection_requests", ["recipient_id", "status"])
    op.create_table(
        "member_blocks",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column("blocker_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("bridge_users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("blocked_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("bridge_users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
        sa.UniqueConstraint("blocker_id", "blocked_id", name="uq_member_block"),
    )


def downgrade() -> None:
    op.drop_table("member_blocks")
    op.drop_index("ix_connection_requests_recipient_status", table_name="connection_requests")
    op.drop_table("connection_requests")
    op.drop_table("member_intents")
    op.drop_table("professional_intents")
