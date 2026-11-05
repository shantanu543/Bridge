"""Add conversations, cohorts, Pods, and referral records."""

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

revision = "20261106_0004"
down_revision = "20261106_0003"
branch_labels = None
depends_on = None


def upgrade() -> None:
    uuid = postgresql.UUID(as_uuid=True)
    user_fk = lambda name: sa.ForeignKey("bridge_users.id", ondelete="CASCADE")
    op.create_table("direct_conversations",
        sa.Column("id", uuid, primary_key=True), sa.Column("member_low_id", uuid, user_fk("member_low_id"), nullable=False),
        sa.Column("member_high_id", uuid, user_fk("member_high_id"), nullable=False), sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
        sa.UniqueConstraint("member_low_id", "member_high_id", name="uq_direct_pair"))
    op.create_table("direct_messages", sa.Column("id", uuid, primary_key=True),
        sa.Column("conversation_id", uuid, sa.ForeignKey("direct_conversations.id", ondelete="CASCADE"), nullable=False),
        sa.Column("sender_id", uuid, user_fk("sender_id"), nullable=False), sa.Column("body", sa.Text(), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()))
    op.create_index("ix_direct_messages_conversation_created", "direct_messages", ["conversation_id", "created_at"])
    op.create_table("cohorts", sa.Column("id", uuid, primary_key=True), sa.Column("name", sa.String(120), nullable=False),
        sa.Column("topic", sa.String(100), nullable=False), sa.Column("description", sa.Text(), nullable=False),
        sa.Column("visibility", sa.String(16), nullable=False, server_default="PUBLIC"), sa.Column("created_by", uuid, user_fk("created_by"), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()))
    op.create_table("cohort_memberships", sa.Column("id", uuid, primary_key=True),
        sa.Column("cohort_id", uuid, sa.ForeignKey("cohorts.id", ondelete="CASCADE"), nullable=False), sa.Column("user_id", uuid, user_fk("user_id"), nullable=False),
        sa.Column("role", sa.String(16), nullable=False, server_default="MEMBER"), sa.Column("status", sa.String(16), nullable=False, server_default="ACTIVE"),
        sa.Column("joined_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()), sa.UniqueConstraint("cohort_id", "user_id", name="uq_cohort_member"))
    op.create_table("cohort_posts", sa.Column("id", uuid, primary_key=True), sa.Column("cohort_id", uuid, sa.ForeignKey("cohorts.id", ondelete="CASCADE"), nullable=False),
        sa.Column("author_id", uuid, user_fk("author_id"), nullable=False), sa.Column("body", sa.Text(), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()))
    op.create_index("ix_cohort_posts_cohort_created", "cohort_posts", ["cohort_id", "created_at"])
    op.create_table("pods", sa.Column("id", uuid, primary_key=True), sa.Column("name", sa.String(120), nullable=False), sa.Column("goal", sa.String(500), nullable=False),
        sa.Column("guidelines", sa.String(2000)), sa.Column("discoverable", sa.Boolean(), nullable=False, server_default=sa.false()), sa.Column("status", sa.String(16), nullable=False, server_default="ACTIVE"),
        sa.Column("expires_at", sa.DateTime(timezone=True), nullable=False), sa.Column("created_by", uuid, user_fk("created_by"), nullable=False), sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()))
    op.create_table("pod_memberships", sa.Column("id", uuid, primary_key=True), sa.Column("pod_id", uuid, sa.ForeignKey("pods.id", ondelete="CASCADE"), nullable=False),
        sa.Column("user_id", uuid, user_fk("user_id"), nullable=False), sa.Column("role", sa.String(16), nullable=False, server_default="MEMBER"),
        sa.Column("joined_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()), sa.UniqueConstraint("pod_id", "user_id", name="uq_pod_member"))
    op.create_index("ix_pod_memberships_user", "pod_memberships", ["user_id"])
    op.create_table("pod_messages", sa.Column("id", uuid, primary_key=True), sa.Column("pod_id", uuid, sa.ForeignKey("pods.id", ondelete="CASCADE"), nullable=False),
        sa.Column("sender_id", uuid, user_fk("sender_id"), nullable=False), sa.Column("body", sa.Text(), nullable=False), sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()))
    op.create_index("ix_pod_messages_pod_created", "pod_messages", ["pod_id", "created_at"])
    op.create_table("referral_requests", sa.Column("id", uuid, primary_key=True), sa.Column("requester_id", uuid, user_fk("requester_id"), nullable=False),
        sa.Column("recipient_id", uuid, user_fk("recipient_id"), nullable=False), sa.Column("company", sa.String(160), nullable=False), sa.Column("role", sa.String(160), nullable=False),
        sa.Column("message", sa.String(2000), nullable=False), sa.Column("status", sa.String(16), nullable=False, server_default="SENT"),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()))
    op.create_index("ix_referral_requests_requester_created", "referral_requests", ["requester_id", "created_at"])
    op.create_table("member_reports", sa.Column("id", uuid, primary_key=True), sa.Column("reporter_id", uuid, user_fk("reporter_id"), nullable=False),
        sa.Column("reported_id", uuid, user_fk("reported_id"), nullable=False), sa.Column("reason", sa.String(40), nullable=False), sa.Column("details", sa.String(2000)),
        sa.Column("status", sa.String(16), nullable=False, server_default="OPEN"), sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()))


def downgrade() -> None:
    op.drop_table("member_reports")
    op.drop_index("ix_referral_requests_requester_created", table_name="referral_requests")
    op.drop_table("referral_requests")
    op.drop_index("ix_pod_messages_pod_created", table_name="pod_messages")
    op.drop_table("pod_messages")
    op.drop_index("ix_pod_memberships_user", table_name="pod_memberships")
    op.drop_table("pod_memberships")
    op.drop_table("pods")
    op.drop_index("ix_cohort_posts_cohort_created", table_name="cohort_posts")
    op.drop_table("cohort_posts")
    op.drop_table("cohort_memberships")
    op.drop_table("cohorts")
    op.drop_index("ix_direct_messages_conversation_created", table_name="direct_messages")
    op.drop_table("direct_messages")
    op.drop_table("direct_conversations")
