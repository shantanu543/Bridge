from __future__ import annotations

from datetime import datetime, timezone
from uuid import UUID

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models import BridgeScoreSnapshot, Profile

MODEL_VERSION = "evidence-completeness-v1"


def _evidence(profile: Profile) -> dict[str, bool]:
    # Internal rubric: category values and weights are deliberately excluded from member responses.
    return {
        "professional_identity": bool(profile.full_name and profile.headline),
        "professional_summary": bool(profile.summary and profile.summary.strip()),
        "work_evidence": bool(profile.work_history),
        "education_evidence": bool(profile.education),
        "skills_evidence": bool(profile.skills),
        "project_evidence": bool(profile.projects or profile.achievements),
        "public_work_sample": bool(profile.portfolio_links),
    }


def _suggestions(evidence: dict[str, bool]) -> list[str]:
    suggestions: list[str] = []
    if not evidence["professional_summary"]:
        suggestions.append("Add a short summary of the work you do and the problems you solve.")
    if not evidence["work_evidence"]:
        suggestions.append("Add relevant work experience to give your profile more context.")
    if not evidence["skills_evidence"]:
        suggestions.append("Add up to six skills that best describe your professional focus.")
    if not evidence["project_evidence"]:
        suggestions.append("Add a project or achievement that shows your work in practice.")
    if not evidence["public_work_sample"]:
        suggestions.append("Add a portfolio or project link if you have one to share.")
    return suggestions[:3]


def member_suggestions(evidence: dict[str, bool]) -> list[str]:
    return _suggestions(evidence)


async def calculate_score(session: AsyncSession, user_id: UUID, profile: Profile) -> BridgeScoreSnapshot:
    now = datetime.now(timezone.utc)
    evidence = _evidence(profile)
    has_minimum_evidence = evidence["professional_identity"] and sum(evidence.values()) >= 3
    state = "CALCULATED" if profile.confirmed_at is not None and has_minimum_evidence else "NOT_READY"
    score: int | None = None
    if state == "CALCULATED":
        # Stable completeness score. Missing optional provider sources never reduce it.
        weights = {
            "professional_identity": 20,
            "professional_summary": 15,
            "work_evidence": 20,
            "education_evidence": 10,
            "skills_evidence": 15,
            "project_evidence": 15,
            "public_work_sample": 5,
        }
        score = sum(weights[key] for key, present in evidence.items() if present)
    latest = await session.scalar(
        select(BridgeScoreSnapshot)
        .where(BridgeScoreSnapshot.user_id == user_id)
        .order_by(BridgeScoreSnapshot.created_at.desc())
        .limit(1)
    )
    # Keep history quiet when the result and internal evidence have not changed.
    if latest and latest.state == state and latest.score == score and latest.internal_dimensions == evidence:
        return latest
    snapshot = BridgeScoreSnapshot(
        user_id=user_id,
        state=state,
        score=score,
        model_version=MODEL_VERSION,
        internal_dimensions=evidence,
        created_at=now,
    )
    session.add(snapshot)
    await session.flush()
    return snapshot
