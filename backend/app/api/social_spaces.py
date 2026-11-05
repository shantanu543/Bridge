from __future__ import annotations

from datetime import datetime, timedelta, timezone
from uuid import UUID, uuid4

from fastapi import APIRouter, Depends, HTTPException, Query, status
from pydantic import BaseModel, Field
from sqlalchemy import and_, func, or_, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.auth import AuthenticatedMember, get_authenticated_member
from app.db import get_session
from app.models import (
    BridgeNotification, BridgeUser, Cohort, CohortMembership, CohortPost, ConnectionRequest, DirectConversation,
    DirectMessage, MemberBlock, MemberReport, Pod, PodMembership, PodMessage, Profile, ReferralRequest,
)

router = APIRouter(tags=["Community", "Chat", "Pods", "Referrals", "Safety"])


class TextBody(BaseModel):
    body: str = Field(min_length=1, max_length=4000)


class CohortCreate(BaseModel):
    name: str = Field(min_length=3, max_length=120)
    topic: str = Field(min_length=2, max_length=100)
    description: str = Field(min_length=10, max_length=2000)


class PodCreate(BaseModel):
    name: str = Field(min_length=3, max_length=120)
    goal: str = Field(min_length=10, max_length=500)
    guidelines: str | None = Field(default=None, max_length=2000)
    discoverable: bool = False
    duration_weeks: int = Field(default=4, ge=1, le=12)


class ReferralCreate(BaseModel):
    recipient_id: UUID
    company: str = Field(min_length=2, max_length=160)
    role: str = Field(min_length=2, max_length=160)
    message: str = Field(min_length=20, max_length=2000)


class ReferralDecision(BaseModel):
    decision: str = Field(pattern="^(ACCEPTED|DECLINED)$")


class ReportCreate(BaseModel):
    reason: str = Field(pattern="^(HARASSMENT|SPAM|IMPERSONATION|INAPPROPRIATE|OTHER)$")
    details: str | None = Field(default=None, max_length=2000)


async def _ensure_connection(session: AsyncSession, first: UUID, second: UUID) -> None:
    request = await session.scalar(select(ConnectionRequest).where(
        ConnectionRequest.status == "ACCEPTED",
        or_(and_(ConnectionRequest.requester_id == first, ConnectionRequest.recipient_id == second),
            and_(ConnectionRequest.requester_id == second, ConnectionRequest.recipient_id == first)),
    ))
    blocked = await session.scalar(select(MemberBlock.id).where(or_(
        and_(MemberBlock.blocker_id == first, MemberBlock.blocked_id == second),
        and_(MemberBlock.blocker_id == second, MemberBlock.blocked_id == first),
    )))
    if request is None or blocked:
        raise HTTPException(403, detail={"code": "connection_required", "message": "An active, unblocked connection is required."})


def _pod_member_query(pod_id: UUID, user_id: UUID):
    return select(PodMembership).where(PodMembership.pod_id == pod_id, PodMembership.user_id == user_id)


# Bridge Chat: persistent message history is available only to accepted connections.
@router.get("/conversations")
async def list_conversations(member: AuthenticatedMember = Depends(get_authenticated_member), session: AsyncSession = Depends(get_session)) -> list[dict]:
    rows = list(await session.scalars(select(DirectConversation).where(or_(DirectConversation.member_low_id == member.user.id, DirectConversation.member_high_id == member.user.id))))
    output = []
    for row in rows:
        peer_id = row.member_high_id if row.member_low_id == member.user.id else row.member_low_id
        peer = await session.get(Profile, peer_id)
        last = await session.scalar(select(DirectMessage).where(DirectMessage.conversation_id == row.id).order_by(DirectMessage.created_at.desc()).limit(1))
        output.append({"id": row.id, "member_id": peer_id, "full_name": (peer.full_name if peer else None) or "Bridge member", "last_message": last.body if last else None, "updated_at": last.created_at if last else row.created_at})
    return output


@router.get("/conversations/{conversation_id}/messages")
async def list_messages(conversation_id: UUID, before: datetime | None = None, limit: int = Query(default=50, ge=1, le=100), member: AuthenticatedMember = Depends(get_authenticated_member), session: AsyncSession = Depends(get_session)) -> list[dict]:
    conversation = await session.get(DirectConversation, conversation_id)
    if conversation is None or member.user.id not in (conversation.member_low_id, conversation.member_high_id):
        raise HTTPException(404, detail={"code": "conversation_unavailable", "message": "Conversation not found."})
    peer = conversation.member_high_id if conversation.member_low_id == member.user.id else conversation.member_low_id
    await _ensure_connection(session, member.user.id, peer)
    query = select(DirectMessage).where(DirectMessage.conversation_id == conversation_id)
    if before:
        query = query.where(DirectMessage.created_at < before)
    rows = list(await session.scalars(query.order_by(DirectMessage.created_at.desc()).limit(limit)))
    return [{"id": row.id, "sender_id": row.sender_id, "body": row.body, "created_at": row.created_at} for row in reversed(rows)]


@router.post("/members/{peer_id}/messages", status_code=201)
async def send_message(peer_id: UUID, body: TextBody, member: AuthenticatedMember = Depends(get_authenticated_member), session: AsyncSession = Depends(get_session)) -> dict:
    await _ensure_connection(session, member.user.id, peer_id)
    low, high = sorted((member.user.id, peer_id))
    conversation = await session.scalar(select(DirectConversation).where(DirectConversation.member_low_id == low, DirectConversation.member_high_id == high))
    if conversation is None:
        conversation = DirectConversation(id=uuid4(), member_low_id=low, member_high_id=high)
        session.add(conversation)
        await session.flush()
    row = DirectMessage(id=uuid4(), conversation_id=conversation.id, sender_id=member.user.id, body=body.body.strip())
    session.add(row)
    session.add(BridgeNotification(id=uuid4(), user_id=peer_id, kind="DIRECT_MESSAGE", title="New Bridge Chat message", body="You received a new message.", payload={"conversation_id": str(conversation.id)}))
    await session.commit()
    return {"id": row.id, "conversation_id": conversation.id, "sender_id": row.sender_id, "body": row.body, "created_at": row.created_at}


# Bridge Cohorts: public directory, explicit membership, member-only discussions.
@router.get("/cohorts")
async def list_cohorts(q: str = Query(default="", max_length=100), member: AuthenticatedMember = Depends(get_authenticated_member), session: AsyncSession = Depends(get_session)) -> list[dict]:
    query = select(Cohort).where(Cohort.visibility == "PUBLIC")
    if q.strip():
        match = f"%{q.strip()}%"
        query = query.where(or_(Cohort.name.ilike(match), Cohort.topic.ilike(match), Cohort.description.ilike(match)))
    rows = list(await session.scalars(query.order_by(Cohort.name).limit(100)))
    result = []
    for row in rows:
        membership = await session.scalar(select(CohortMembership).where(CohortMembership.cohort_id == row.id, CohortMembership.user_id == member.user.id))
        count = await session.scalar(select(func.count()).select_from(CohortMembership).where(CohortMembership.cohort_id == row.id, CohortMembership.status == "ACTIVE"))
        result.append({"id": row.id, "name": row.name, "topic": row.topic, "description": row.description, "members": count or 0, "joined": bool(membership and membership.status == "ACTIVE")})
    return result


@router.post("/cohorts", status_code=201)
async def create_cohort(body: CohortCreate, member: AuthenticatedMember = Depends(get_authenticated_member), session: AsyncSession = Depends(get_session)) -> dict:
    row = Cohort(id=uuid4(), name=body.name.strip(), topic=body.topic.strip(), description=body.description.strip(), visibility="PUBLIC", created_by=member.user.id)
    session.add(row)
    await session.flush()
    session.add(CohortMembership(id=uuid4(), cohort_id=row.id, user_id=member.user.id, role="MODERATOR", status="ACTIVE"))
    await session.commit()
    return {"id": row.id, "name": row.name, "topic": row.topic, "description": row.description, "members": 1, "joined": True}


@router.post("/cohorts/{cohort_id}/join")
async def join_cohort(cohort_id: UUID, member: AuthenticatedMember = Depends(get_authenticated_member), session: AsyncSession = Depends(get_session)) -> dict:
    cohort = await session.get(Cohort, cohort_id)
    if cohort is None or cohort.visibility != "PUBLIC":
        raise HTTPException(404, detail={"code": "cohort_unavailable", "message": "Cohort not found."})
    existing = await session.scalar(select(CohortMembership).where(CohortMembership.cohort_id == cohort_id, CohortMembership.user_id == member.user.id))
    if existing is None:
        session.add(CohortMembership(id=uuid4(), cohort_id=cohort_id, user_id=member.user.id, status="ACTIVE"))
    elif existing.status != "ACTIVE":
        existing.status = "ACTIVE"
    await session.commit()
    return {"cohort_id": str(cohort_id), "joined": True}


@router.get("/cohorts/{cohort_id}/posts")
async def cohort_posts(cohort_id: UUID, member: AuthenticatedMember = Depends(get_authenticated_member), session: AsyncSession = Depends(get_session)) -> list[dict]:
    cohort = await session.get(Cohort, cohort_id)
    membership = await session.scalar(select(CohortMembership).where(CohortMembership.cohort_id == cohort_id, CohortMembership.user_id == member.user.id, CohortMembership.status == "ACTIVE"))
    if cohort is None or membership is None:
        raise HTTPException(403, detail={"code": "cohort_membership_required", "message": "Join this Cohort to view its discussions."})
    posts = list(await session.scalars(select(CohortPost).where(CohortPost.cohort_id == cohort_id).order_by(CohortPost.created_at.desc()).limit(100)))
    return [{"id": row.id, "author_id": row.author_id, "body": row.body, "created_at": row.created_at} for row in posts]


@router.post("/cohorts/{cohort_id}/posts", status_code=201)
async def create_cohort_post(cohort_id: UUID, body: TextBody, member: AuthenticatedMember = Depends(get_authenticated_member), session: AsyncSession = Depends(get_session)) -> dict:
    membership = await session.scalar(select(CohortMembership).where(CohortMembership.cohort_id == cohort_id, CohortMembership.user_id == member.user.id, CohortMembership.status == "ACTIVE"))
    if membership is None:
        raise HTTPException(403, detail={"code": "cohort_membership_required", "message": "Join this Cohort to post."})
    row = CohortPost(id=uuid4(), cohort_id=cohort_id, author_id=member.user.id, body=body.body.strip())
    session.add(row)
    recipient_ids = list(await session.scalars(select(CohortMembership.user_id).where(CohortMembership.cohort_id == cohort_id, CohortMembership.user_id != member.user.id, CohortMembership.status == "ACTIVE")))
    session.add_all([BridgeNotification(id=uuid4(), user_id=user_id, kind="COHORT_POST", title="New Cohort discussion", body="A member shared a post in your Cohort.", payload={"cohort_id": str(cohort_id), "post_id": str(row.id)}) for user_id in recipient_ids])
    await session.commit()
    return {"id": row.id, "author_id": row.author_id, "body": row.body, "created_at": row.created_at}


# Bridge Pods: private by default; the capacity check is serialized with a Pod row lock.
@router.get("/pods")
async def list_pods(member: AuthenticatedMember = Depends(get_authenticated_member), session: AsyncSession = Depends(get_session)) -> list[dict]:
    member_ids = select(PodMembership.pod_id).where(PodMembership.user_id == member.user.id)
    rows = list(await session.scalars(select(Pod).where(or_(Pod.id.in_(member_ids), Pod.discoverable.is_(True))).order_by(Pod.created_at.desc())))
    result = []
    for row in rows:
        is_member = await session.scalar(select(PodMembership.id).where(PodMembership.pod_id == row.id, PodMembership.user_id == member.user.id))
        count = await session.scalar(select(func.count()).select_from(PodMembership).where(PodMembership.pod_id == row.id))
        result.append({"id": row.id, "name": row.name, "goal": row.goal, "status": row.status, "expires_at": row.expires_at, "members": count or 0, "is_member": is_member is not None, "discoverable": row.discoverable})
    return result


@router.post("/pods", status_code=201)
async def create_pod(body: PodCreate, member: AuthenticatedMember = Depends(get_authenticated_member), session: AsyncSession = Depends(get_session)) -> dict:
    now = datetime.now(timezone.utc)
    row = Pod(id=uuid4(), name=body.name.strip(), goal=body.goal.strip(), guidelines=body.guidelines, discoverable=body.discoverable, expires_at=now + timedelta(weeks=body.duration_weeks), created_by=member.user.id)
    session.add(row)
    await session.flush()
    session.add(PodMembership(id=uuid4(), pod_id=row.id, user_id=member.user.id, role="ADMIN"))
    await session.commit()
    return {"id": row.id, "name": row.name, "goal": row.goal, "status": row.status, "expires_at": row.expires_at, "members": 1, "is_member": True, "discoverable": row.discoverable}


@router.post("/pods/{pod_id}/join")
async def join_pod(pod_id: UUID, member: AuthenticatedMember = Depends(get_authenticated_member), session: AsyncSession = Depends(get_session)) -> dict:
    pod = await session.scalar(select(Pod).where(Pod.id == pod_id).with_for_update())
    if pod is None or pod.status != "ACTIVE" or pod.expires_at <= datetime.now(timezone.utc) or not pod.discoverable:
        raise HTTPException(404, detail={"code": "pod_unavailable", "message": "This Pod is unavailable to join."})
    existing = await session.scalar(select(PodMembership).where(PodMembership.pod_id == pod_id, PodMembership.user_id == member.user.id))
    if existing:
        return {"pod_id": str(pod_id), "joined": True}
    count = await session.scalar(select(func.count()).select_from(PodMembership).where(PodMembership.pod_id == pod_id))
    if (count or 0) >= 10:
        raise HTTPException(409, detail={"code": "pod_full", "message": "This Pod has reached its 10-member limit."})
    session.add(PodMembership(id=uuid4(), pod_id=pod_id, user_id=member.user.id))
    await session.commit()
    return {"pod_id": str(pod_id), "joined": True}


@router.get("/pods/{pod_id}/messages")
async def pod_messages(pod_id: UUID, member: AuthenticatedMember = Depends(get_authenticated_member), session: AsyncSession = Depends(get_session)) -> list[dict]:
    membership = await session.scalar(_pod_member_query(pod_id, member.user.id))
    if membership is None:
        raise HTTPException(403, detail={"code": "pod_membership_required", "message": "Only Pod members can view messages."})
    rows = list(await session.scalars(select(PodMessage).where(PodMessage.pod_id == pod_id).order_by(PodMessage.created_at.desc()).limit(100)))
    return [{"id": row.id, "sender_id": row.sender_id, "body": row.body, "created_at": row.created_at} for row in reversed(rows)]


@router.post("/pods/{pod_id}/messages", status_code=201)
async def send_pod_message(pod_id: UUID, body: TextBody, member: AuthenticatedMember = Depends(get_authenticated_member), session: AsyncSession = Depends(get_session)) -> dict:
    membership = await session.scalar(_pod_member_query(pod_id, member.user.id))
    pod = await session.get(Pod, pod_id)
    if membership is None or pod is None:
        raise HTTPException(403, detail={"code": "pod_membership_required", "message": "Only Pod members can send messages."})
    if pod.status != "ACTIVE" or pod.expires_at <= datetime.now(timezone.utc):
        raise HTTPException(409, detail={"code": "pod_archived", "message": "This Pod is read-only."})
    row = PodMessage(id=uuid4(), pod_id=pod_id, sender_id=member.user.id, body=body.body.strip())
    session.add(row)
    recipient_ids = list(await session.scalars(select(PodMembership.user_id).where(PodMembership.pod_id == pod_id, PodMembership.user_id != member.user.id)))
    session.add_all([BridgeNotification(id=uuid4(), user_id=user_id, kind="POD_MESSAGE", title="New Pod message", body="A Pod member sent a message.", payload={"pod_id": str(pod_id)}) for user_id in recipient_ids])
    await session.commit()
    return {"id": row.id, "sender_id": row.sender_id, "body": row.body, "created_at": row.created_at}


# Referrals: accepted connection required and strict account-lifetime quota.
@router.get("/referrals/quota")
async def referral_quota(member: AuthenticatedMember = Depends(get_authenticated_member), session: AsyncSession = Depends(get_session)) -> dict:
    used = await session.scalar(select(func.count()).select_from(ReferralRequest).where(ReferralRequest.requester_id == member.user.id))
    return {"used": used or 0, "limit": 10, "remaining": max(0, 10 - (used or 0))}


@router.get("/referrals")
async def referral_history(member: AuthenticatedMember = Depends(get_authenticated_member), session: AsyncSession = Depends(get_session)) -> list[dict]:
    rows = list(await session.scalars(select(ReferralRequest).where(or_(ReferralRequest.requester_id == member.user.id, ReferralRequest.recipient_id == member.user.id)).order_by(ReferralRequest.created_at.desc()).limit(100)))
    return [{"id": row.id, "requester_id": row.requester_id, "recipient_id": row.recipient_id, "company": row.company, "role": row.role, "message": row.message, "status": row.status, "created_at": row.created_at} for row in rows]


@router.post("/referrals", status_code=201)
async def create_referral(body: ReferralCreate, member: AuthenticatedMember = Depends(get_authenticated_member), session: AsyncSession = Depends(get_session)) -> dict:
    await _ensure_connection(session, member.user.id, body.recipient_id)
    user = await session.scalar(select(BridgeUser).where(BridgeUser.id == member.user.id).with_for_update())
    used = await session.scalar(select(func.count()).select_from(ReferralRequest).where(ReferralRequest.requester_id == member.user.id))
    if (used or 0) >= 10:
        raise HTTPException(409, detail={"code": "referral_quota_reached", "message": "You have used all 10 referral requests available for this account."})
    row = ReferralRequest(id=uuid4(), requester_id=member.user.id, recipient_id=body.recipient_id, company=body.company.strip(), role=body.role.strip(), message=body.message.strip())
    session.add(row)
    session.add(BridgeNotification(id=uuid4(), user_id=body.recipient_id, kind="REFERRAL_REQUEST", title="Referral request", body="A connection asked you about a referral.", payload={"request_id": str(row.id)}))
    await session.commit()
    return {"id": row.id, "status": row.status, "remaining": 9 - (used or 0)}


@router.post("/referrals/{request_id}/decision")
async def decide_referral(request_id: UUID, body: ReferralDecision, member: AuthenticatedMember = Depends(get_authenticated_member), session: AsyncSession = Depends(get_session)) -> dict:
    row = await session.get(ReferralRequest, request_id)
    if row is None or row.recipient_id != member.user.id or row.status != "SENT":
        raise HTTPException(404, detail={"code": "referral_unavailable", "message": "This referral request is unavailable."})
    row.status = body.decision
    session.add(BridgeNotification(id=uuid4(), user_id=row.requester_id, kind="REFERRAL_DECISION", title="Referral request updated", body="Your referral request was " + ("accepted." if body.decision == "ACCEPTED" else "declined."), payload={"request_id": str(row.id)}))
    await session.commit()
    return {"id": row.id, "status": row.status}


@router.post("/members/{user_id}/reports", status_code=201)
async def report_member(user_id: UUID, body: ReportCreate, member: AuthenticatedMember = Depends(get_authenticated_member), session: AsyncSession = Depends(get_session)) -> dict:
    if user_id == member.user.id or await session.get(BridgeUser, user_id) is None:
        raise HTTPException(404, detail={"code": "member_unavailable", "message": "Member not found."})
    row = MemberReport(id=uuid4(), reporter_id=member.user.id, reported_id=user_id, reason=body.reason, details=body.details)
    session.add(row)
    await session.commit()
    return {"id": row.id, "status": row.status, "message": "Your report has been sent for review."}
