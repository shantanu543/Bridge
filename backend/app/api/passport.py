from __future__ import annotations

from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.auth import AuthenticatedMember, get_authenticated_member
from app.db import get_session
from app.models import BridgeScoreSnapshot, Profile, VerificationReview
from app.schemas import (
    BridgeScoreHistoryItem,
    BridgeScoreHistoryResponse,
    BridgeScoreResponse,
    PassportResponse,
)
from app.services.bridge_score import calculate_score, member_suggestions

router = APIRouter(prefix="/me", tags=["Bridge Score and Passport"])


async def _score_payload(session: AsyncSession, member: AuthenticatedMember, profile: Profile) -> BridgeScoreResponse:
    snapshot = await calculate_score(session, member.user.id, profile)
    review = await session.get(VerificationReview, member.user.id)
    await session.commit()
    return BridgeScoreResponse(
        state=snapshot.state,
        score=snapshot.score,
        calculated_at=snapshot.created_at,
        suggestions=[] if snapshot.state == "NOT_READY" else member_suggestions(snapshot.internal_dimensions),
        verification_state=review.state if review else "NOT_STARTED",
    )


@router.get("/bridge-score", response_model=BridgeScoreResponse)
async def bridge_score(
    member: AuthenticatedMember = Depends(get_authenticated_member),
    session: AsyncSession = Depends(get_session),
) -> BridgeScoreResponse:
    profile = await session.get(Profile, member.user.id)
    if profile is None:
        profile = Profile(user_id=member.user.id, visibility="private")
        session.add(profile)
        await session.flush()
    return await _score_payload(session, member, profile)


@router.get("/bridge-score/history", response_model=BridgeScoreHistoryResponse)
async def bridge_score_history(
    member: AuthenticatedMember = Depends(get_authenticated_member),
    session: AsyncSession = Depends(get_session),
) -> BridgeScoreHistoryResponse:
    rows = list(await session.scalars(
        select(BridgeScoreSnapshot)
        .where(BridgeScoreSnapshot.user_id == member.user.id)
        .order_by(BridgeScoreSnapshot.created_at.desc())
        .limit(50)
    ))
    return BridgeScoreHistoryResponse(items=[
        BridgeScoreHistoryItem(state=row.state, score=row.score, calculated_at=row.created_at)
        for row in rows
    ])


@router.get("/passport", response_model=PassportResponse)
async def private_passport(
    member: AuthenticatedMember = Depends(get_authenticated_member),
    session: AsyncSession = Depends(get_session),
) -> PassportResponse:
    profile = await session.get(Profile, member.user.id)
    if profile is None:
        profile = Profile(user_id=member.user.id, visibility="private")
        session.add(profile)
        await session.flush()
    score = await _score_payload(session, member, profile)
    return PassportResponse(
        full_name=profile.full_name,
        headline=profile.headline,
        current_role=profile.current_role,
        current_company=profile.current_company,
        summary=profile.summary,
        years_experience=profile.years_experience,
        skills=(profile.skills or [])[:6],
        work_history=profile.work_history or [],
        education=profile.education or [],
        projects=profile.projects or [],
        achievements=profile.achievements or [],
        portfolio_links=profile.portfolio_links or [],
        score=score,
    )
