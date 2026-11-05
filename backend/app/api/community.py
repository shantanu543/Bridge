from __future__ import annotations

from datetime import datetime, timezone
from uuid import UUID, uuid4

from fastapi import APIRouter, Depends, HTTPException, Query
from pydantic import BaseModel, Field
from sqlalchemy import and_, or_, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.auth import AuthenticatedMember, get_authenticated_member
from app.db import get_session
from app.models import (
    BridgeNotification, BridgeUser, ConnectionRequest, DirectConversation, MemberBlock, MemberIntent, ProfessionalIntent, Profile,
)

router = APIRouter(tags=["Discover and Connections"])


class IntentItem(BaseModel):
    id: UUID
    slug: str
    label: str
    selected: bool = False


class IntentSelection(BaseModel):
    intent_ids: list[UUID] = Field(max_length=3)


class ConnectionCreate(BaseModel):
    recipient_id: UUID
    note: str | None = Field(default=None, max_length=500)


class ConnectionDecision(BaseModel):
    decision: str = Field(pattern="^(ACCEPTED|DECLINED)$")


class MemberCard(BaseModel):
    user_id: UUID
    full_name: str
    headline: str | None = None
    location: str | None = None
    skills: list[str]
    intents: list[str]
    relationship: str | None = None


@router.get("/me/intents", response_model=list[IntentItem])
async def list_intents(
    member: AuthenticatedMember = Depends(get_authenticated_member),
    session: AsyncSession = Depends(get_session),
) -> list[IntentItem]:
    selected = set(await session.scalars(select(MemberIntent.intent_id).where(MemberIntent.user_id == member.user.id)))
    rows = list(await session.scalars(select(ProfessionalIntent).where(ProfessionalIntent.active.is_(True)).order_by(ProfessionalIntent.label)))
    return [IntentItem(id=row.id, slug=row.slug, label=row.label, selected=row.id in selected) for row in rows]


@router.put("/me/intents", response_model=list[IntentItem])
async def save_intents(
    body: IntentSelection,
    member: AuthenticatedMember = Depends(get_authenticated_member),
    session: AsyncSession = Depends(get_session),
) -> list[IntentItem]:
    if len(set(body.intent_ids)) != len(body.intent_ids):
        raise HTTPException(422, detail={"code": "duplicate_intent", "message": "Choose each intent once."})
    rows = list(await session.scalars(select(ProfessionalIntent).where(
        ProfessionalIntent.id.in_(body.intent_ids), ProfessionalIntent.active.is_(True)
    ))) if body.intent_ids else []
    if len(rows) != len(body.intent_ids):
        raise HTTPException(422, detail={"code": "unknown_intent", "message": "One or more intents are unavailable."})
    await session.execute(MemberIntent.__table__.delete().where(MemberIntent.user_id == member.user.id))
    new_rows = [MemberIntent(id=uuid4(), user_id=member.user.id, intent_id=identity) for identity in body.intent_ids]
    session.add_all(new_rows)
    await session.commit()
    all_rows = list(await session.scalars(select(ProfessionalIntent).where(ProfessionalIntent.active.is_(True)).order_by(ProfessionalIntent.label)))
    selected = set(body.intent_ids)
    return [IntentItem(id=row.id, slug=row.slug, label=row.label, selected=row.id in selected) for row in all_rows]


@router.get("/discover", response_model=list[MemberCard])
async def discover(
    q: str = Query(default="", max_length=80),
    skill: str | None = Query(default=None, max_length=80),
    member: AuthenticatedMember = Depends(get_authenticated_member),
    session: AsyncSession = Depends(get_session),
) -> list[MemberCard]:
    blocked = set(await session.scalars(select(MemberBlock.blocked_id).where(MemberBlock.blocker_id == member.user.id)))
    blocked_by = set(await session.scalars(select(MemberBlock.blocker_id).where(MemberBlock.blocked_id == member.user.id)))
    excluded = blocked | blocked_by | {member.user.id}
    statement = select(Profile, BridgeUser).join(BridgeUser, BridgeUser.id == Profile.user_id).where(
        Profile.confirmed_at.is_not(None), Profile.visibility.in_(["members", "connections"]), BridgeUser.account_status == "active"
    )
    if excluded:
        statement = statement.where(Profile.user_id.not_in(excluded))
    term = q.strip()
    if term:
        pattern = f"%{term}%"
        statement = statement.where(or_(Profile.full_name.ilike(pattern), Profile.headline.ilike(pattern), Profile.summary.ilike(pattern), Profile.current_role.ilike(pattern)))
    if skill:
        statement = statement.where(Profile.skills.cast(__import__("sqlalchemy").String).ilike(f"%{skill.strip()}%"))
    results = (await session.execute(statement.order_by(Profile.updated_at.desc()).limit(50))).all()
    cards: list[MemberCard] = []
    for profile, user in results:
        intents = list(await session.scalars(
            select(ProfessionalIntent.label).join(MemberIntent, MemberIntent.intent_id == ProfessionalIntent.id)
            .where(MemberIntent.user_id == user.id)
        ))
        relationship_row = await session.scalar(select(ConnectionRequest).where(or_(
            and_(ConnectionRequest.requester_id == member.user.id, ConnectionRequest.recipient_id == user.id),
            and_(ConnectionRequest.requester_id == user.id, ConnectionRequest.recipient_id == member.user.id),
        )))
        relationship = None
        if relationship_row is not None:
            relationship = "CONNECTED" if relationship_row.status == "ACCEPTED" else (
                "REQUEST_SENT" if relationship_row.requester_id == member.user.id else "REQUEST_RECEIVED"
            ) if relationship_row.status == "PENDING" else None
        cards.append(MemberCard(
            user_id=user.id, full_name=profile.full_name or "Bridge member", headline=profile.headline,
            location=profile.location, skills=(profile.skills or [])[:8], intents=intents, relationship=relationship,
        ))
    return cards


@router.get("/connections", response_model=list[dict])
async def connections(
    member: AuthenticatedMember = Depends(get_authenticated_member),
    session: AsyncSession = Depends(get_session),
) -> list[dict]:
    rows = list(await session.scalars(select(ConnectionRequest).where(
        or_(ConnectionRequest.requester_id == member.user.id, ConnectionRequest.recipient_id == member.user.id)
    ).order_by(ConnectionRequest.created_at.desc()).limit(100)))
    result = []
    for row in rows:
        other_id = row.recipient_id if row.requester_id == member.user.id else row.requester_id
        profile = await session.get(Profile, other_id)
        result.append({"id": row.id, "member_id": other_id, "full_name": profile.full_name if profile else "Bridge member", "status": row.status, "incoming": row.recipient_id == member.user.id, "created_at": row.created_at})
    return result


@router.post("/connections", status_code=201)
async def request_connection(
    body: ConnectionCreate,
    member: AuthenticatedMember = Depends(get_authenticated_member),
    session: AsyncSession = Depends(get_session),
) -> dict:
    if body.recipient_id == member.user.id:
        raise HTTPException(422, detail={"code": "self_connection", "message": "You cannot connect with yourself."})
    blocked = await session.scalar(select(MemberBlock.id).where(or_(
        and_(MemberBlock.blocker_id == member.user.id, MemberBlock.blocked_id == body.recipient_id),
        and_(MemberBlock.blocker_id == body.recipient_id, MemberBlock.blocked_id == member.user.id),
    )))
    recipient = await session.scalar(select(Profile).where(Profile.user_id == body.recipient_id, Profile.confirmed_at.is_not(None), Profile.visibility.in_(["members", "connections"])))
    if blocked or recipient is None:
        raise HTTPException(404, detail={"code": "member_unavailable", "message": "This member is not available for a connection request."})
    existing = await session.scalar(select(ConnectionRequest).where(or_(
        and_(ConnectionRequest.requester_id == member.user.id, ConnectionRequest.recipient_id == body.recipient_id),
        and_(ConnectionRequest.requester_id == body.recipient_id, ConnectionRequest.recipient_id == member.user.id),
    )))
    if existing:
        if existing.status == "PENDING" and existing.requester_id == body.recipient_id:
            existing.status = "ACCEPTED"
            low, high = sorted((member.user.id, body.recipient_id))
            if await session.scalar(select(DirectConversation.id).where(DirectConversation.member_low_id == low, DirectConversation.member_high_id == high)) is None:
                session.add(DirectConversation(id=uuid4(), member_low_id=low, member_high_id=high))
            session.add(BridgeNotification(id=uuid4(), user_id=body.recipient_id, kind="CONNECTION_ACCEPTED", title="Connection accepted", body="A Bridge member accepted your request.", payload={"member_id": str(member.user.id)}))
            await session.commit()
            return {"id": str(existing.id), "status": existing.status}
        raise HTTPException(409, detail={"code": "relationship_exists", "message": "A connection request or connection already exists."})
    row = ConnectionRequest(id=uuid4(), requester_id=member.user.id, recipient_id=body.recipient_id, note=body.note)
    session.add(row)
    sender_profile = await session.get(Profile, member.user.id)
    session.add(BridgeNotification(id=uuid4(), user_id=body.recipient_id, kind="CONNECTION_REQUEST", title="New connection request", body=f"{sender_profile.full_name if sender_profile and sender_profile.full_name else 'A Bridge member'} would like to connect.", payload={"request_id": str(row.id)}))
    await session.commit()
    return {"id": str(row.id), "status": row.status}


@router.post("/connections/{request_id}/decision")
async def decide_connection(
    request_id: UUID,
    body: ConnectionDecision,
    member: AuthenticatedMember = Depends(get_authenticated_member),
    session: AsyncSession = Depends(get_session),
) -> dict:
    row = await session.get(ConnectionRequest, request_id)
    if row is None or row.recipient_id != member.user.id or row.status != "PENDING":
        raise HTTPException(404, detail={"code": "request_unavailable", "message": "This pending request is unavailable."})
    row.status = body.decision
    if body.decision == "ACCEPTED":
        low, high = sorted((row.requester_id, row.recipient_id))
        if await session.scalar(select(DirectConversation.id).where(DirectConversation.member_low_id == low, DirectConversation.member_high_id == high)) is None:
            session.add(DirectConversation(id=uuid4(), member_low_id=low, member_high_id=high))
    session.add(BridgeNotification(id=uuid4(), user_id=row.requester_id, kind="CONNECTION_DECISION", title="Connection request updated", body="Your connection request was " + ("accepted." if body.decision == "ACCEPTED" else "declined."), payload={"request_id": str(row.id)}))
    await session.commit()
    return {"id": str(row.id), "status": row.status}


@router.post("/members/{user_id}/block", status_code=204)
async def block_member(
    user_id: UUID,
    member: AuthenticatedMember = Depends(get_authenticated_member),
    session: AsyncSession = Depends(get_session),
) -> None:
    if user_id == member.user.id:
        raise HTTPException(422, detail={"code": "self_block", "message": "You cannot block yourself."})
    existing = await session.scalar(select(MemberBlock).where(MemberBlock.blocker_id == member.user.id, MemberBlock.blocked_id == user_id))
    if existing is None:
        session.add(MemberBlock(id=uuid4(), blocker_id=member.user.id, blocked_id=user_id))
    await session.execute(ConnectionRequest.__table__.delete().where(or_(
        and_(ConnectionRequest.requester_id == member.user.id, ConnectionRequest.recipient_id == user_id),
        and_(ConnectionRequest.requester_id == user_id, ConnectionRequest.recipient_id == member.user.id),
    )))
    await session.commit()


@router.get("/notifications")
async def list_notifications(
    member: AuthenticatedMember = Depends(get_authenticated_member),
    session: AsyncSession = Depends(get_session),
) -> list[dict]:
    rows = list(await session.scalars(select(BridgeNotification).where(BridgeNotification.user_id == member.user.id).order_by(BridgeNotification.created_at.desc()).limit(100)))
    return [{"id": row.id, "kind": row.kind, "title": row.title, "body": row.body, "payload": row.payload, "read_at": row.read_at, "created_at": row.created_at} for row in rows]


@router.post("/notifications/{notification_id}/read")
async def mark_notification_read(
    notification_id: UUID,
    member: AuthenticatedMember = Depends(get_authenticated_member),
    session: AsyncSession = Depends(get_session),
) -> dict:
    row = await session.scalar(select(BridgeNotification).where(BridgeNotification.id == notification_id, BridgeNotification.user_id == member.user.id))
    if row is None:
        raise HTTPException(404, detail={"code": "notification_unavailable", "message": "Notification not found."})
    if row.read_at is None:
        row.read_at = datetime.now(timezone.utc)
        await session.commit()
    return {"id": row.id, "read": True}
