"""Add private score history and verification review state.

Revision ID: 20261105_0002
Revises: 20261104_0001
"""

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

revision = "20261105_0002"
down_revision = "20261104_0001"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.add_column("profiles", sa.Column("score_visibility", sa.Boolean(), nullable=False, server_default=sa.true()))
    op.create_table(
        "bridge_score_snapshots",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column("user_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("bridge_users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("state", sa.String(24), nullable=False),
        sa.Column("score", sa.Integer()),
        sa.Column("model_version", sa.String(64), nullable=False),
        sa.Column("internal_dimensions", postgresql.JSONB(), nullable=False, server_default=sa.text("'{}'::jsonb")),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
    )
    op.create_index("ix_bridge_score_snapshots_user_created", "bridge_score_snapshots", ["user_id", "created_at"])
    op.create_table(
        "verification_reviews",
        sa.Column("user_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("bridge_users.id", ondelete="CASCADE"), primary_key=True),
        sa.Column("state", sa.String(24), nullable=False, server_default="NOT_STARTED"),
        sa.Column("requested_at", sa.DateTime(timezone=True)),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
    )


def downgrade() -> None:
    op.drop_table("verification_reviews")
    op.drop_index("ix_bridge_score_snapshots_user_created", table_name="bridge_score_snapshots")
    op.drop_table("bridge_score_snapshots")
    op.drop_column("profiles", "score_visibility")
