"""Add persisted in-app notifications."""

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

revision = "20261106_0005"
down_revision = "20261106_0004"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        "bridge_notifications",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column("user_id", postgresql.UUID(as_uuid=True), sa.ForeignKey("bridge_users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("kind", sa.String(32), nullable=False), sa.Column("title", sa.String(160), nullable=False),
        sa.Column("body", sa.String(500), nullable=False), sa.Column("payload", postgresql.JSONB(), nullable=False, server_default=sa.text("'{}'::jsonb")),
        sa.Column("read_at", sa.DateTime(timezone=True)), sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
    )
    op.create_index("ix_bridge_notifications_user_created", "bridge_notifications", ["user_id", "created_at"])


def downgrade() -> None:
    op.drop_index("ix_bridge_notifications_user_created", table_name="bridge_notifications")
    op.drop_table("bridge_notifications")
