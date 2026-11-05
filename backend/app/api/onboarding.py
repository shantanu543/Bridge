from __future__ import annotations

from datetime import datetime, timezone
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.auth import AuthenticatedMember, get_authenticated_member
from app.core.config import Settings, get_settings
from app.db import get_session
from app.models import ConsentRecord, LinkedIdentity, Profile, ProfileFieldProvenance
from app.services.bridge_score import calculate_score
from app.schemas import (
    ConsentReceipt,
    ConsentUpdate,
    ActivePolicy,
    OnboardingStatus,
    ProfileFieldSource,
    ProfileResponse,
    ProfileUpdate,
)

router = APIRouter(prefix="/me", tags=["onboarding"])


def _now() -> datetime:
    return datetime.now(timezone.utc)


async def _get_or_create_profile(session: AsyncSession, user_id: UUID) -> Profile:
    profile = await session.get(Profile, user_id)
    if profile is None:
        profile = Profile(user_id=user_id, visibility="private")
        session.add(profile)
        await session.flush()
    return profile


def _profile_response(profile: Profile, provenance_rows: list[ProfileFieldProvenance]) -> ProfileResponse:
    return ProfileResponse(
        user_id=profile.user_id,
        full_name=profile.full_name,
        headline=profile.headline,
        current_role=profile.current_role,
        current_company=profile.current_company,
        summary=profile.summary,
        years_experience=profile.years_experience,
        location=profile.location,
        timezone=profile.timezone,
        work_history=profile.work_history or [],
        education=profile.education or [],
        skills=profile.skills or [],
        projects=profile.projects or [],
        achievements=profile.achievements or [],
        certifications=profile.certifications or [],
        portfolio_links=profile.portfolio_links or [],
        visibility=profile.visibility,
        confirmed_at=profile.confirmed_at,
        published_at=profile.published_at,
        updated_at=profile.updated_at,
        provenance=[
            ProfileFieldSource(
                field_key=row.field_key,
                source_type=row.source_type,  # type: ignore[arg-type]
                imported_at=row.imported_at,
                user_confirmed=row.user_confirmed,
                visibility=row.visibility,  # type: ignore[arg-type]
            )
            for row in provenance_rows
        ],
    )


@router.get("/onboarding", response_model=OnboardingStatus)
async def onboarding_status(
    member: AuthenticatedMember = Depends(get_authenticated_member),
    session: AsyncSession = Depends(get_session),
    settings: Settings = Depends(get_settings),
) -> OnboardingStatus:
    consent_rows = list(await session.scalars(
        select(ConsentRecord).where(
            ConsentRecord.user_id == member.user.id,
            ConsentRecord.consent_type.in_(["TERMS", "PRIVACY"]),
        )
    ))
    required = {row.consent_type: row.policy_version for row in consent_rows}
    terms_ok = bool(settings.terms_policy_version) and required.get("TERMS") == settings.terms_policy_version
    privacy_ok = bool(settings.privacy_policy_version) and required.get("PRIVACY") == settings.privacy_policy_version
    profile = await session.get(Profile, member.user.id)
    identity_rows = await session.scalars(
        select(LinkedIdentity.provider).where(LinkedIdentity.user_id == member.user.id)
    )
    return OnboardingStatus(
        account_created=True,
        required_consents_recorded=terms_ok and privacy_ok,
        profile_started=profile is not None
        and any(
            value is not None
            for value in (profile.full_name, profile.headline, profile.current_role, profile.summary)
        ),
        profile_confirmed=profile is not None and profile.confirmed_at is not None,
        profile_published=profile is not None and profile.published_at is not None,
        connected_sources=sorted(set(identity_rows)),
    )


@router.get("/consent-policies", response_model=list[ActivePolicy])
async def active_consent_policies(
    member: AuthenticatedMember = Depends(get_authenticated_member),
    settings: Settings = Depends(get_settings),
) -> list[ActivePolicy]:
    if not all(
        (settings.terms_policy_version, settings.terms_policy_url, settings.privacy_policy_version, settings.privacy_policy_url)
    ):
        raise HTTPException(
            status_code=503,
            detail={"code": "legal_policy_not_configured", "message": "Current Terms and Privacy documents are not configured."},
        )
    return [
        ActivePolicy(policy_type="TERMS", version=settings.terms_policy_version, url=settings.terms_policy_url),
        ActivePolicy(policy_type="PRIVACY", version=settings.privacy_policy_version, url=settings.privacy_policy_url),
    ]


@router.post("/consents", response_model=list[ConsentReceipt])
async def record_consents(
    body: ConsentUpdate,
    member: AuthenticatedMember = Depends(get_authenticated_member),
    session: AsyncSession = Depends(get_session),
    settings: Settings = Depends(get_settings),
) -> list[ConsentReceipt]:
    if not body.accept_terms or not body.accept_privacy:
        raise HTTPException(
            status_code=400,
            detail={"code": "required_consent_missing", "message": "Terms and Privacy acceptance are required."},
        )
    if not settings.terms_policy_version or not settings.privacy_policy_version:
        raise HTTPException(
            status_code=503,
            detail={"code": "legal_policy_not_configured", "message": "Current Terms and Privacy documents are not configured."},
        )

    requested: list[tuple[str, str, str | None]] = [
        ("TERMS", settings.terms_policy_version, None),
        ("PRIVACY", settings.privacy_policy_version, None),
    ]
    requested.extend(
        (f"PROVIDER:{provider.upper()}", settings.import_consent_version or "", provider)
        for provider in body.provider_data
    )
    if body.resume_processing:
        requested.append(("RESUME_PROCESSING", settings.import_consent_version or "", None))
    if any(not version for _, version, _ in requested):
        raise HTTPException(
            status_code=503,
            detail={"code": "import_consent_not_configured", "message": "Import consent wording is not configured."},
        )

    receipts: list[ConsentReceipt] = []
    for consent_type, version, provider in requested:
        row = await session.scalar(
            select(ConsentRecord).where(
                ConsentRecord.user_id == member.user.id,
                ConsentRecord.consent_type == consent_type,
                ConsentRecord.policy_version == version,
            )
        )
        if row is None:
            row = ConsentRecord(
                user_id=member.user.id,
                consent_type=consent_type,
                policy_version=version,
                provider=provider,
            )
            session.add(row)
            await session.flush()
        receipts.append(
            ConsentReceipt(
                policy_type=row.consent_type,
                policy_version=row.policy_version,
                accepted_at=row.accepted_at,
                provider=row.provider,
            )
        )
    await session.commit()
    return receipts


@router.get("/profile", response_model=ProfileResponse)
async def read_profile(
    member: AuthenticatedMember = Depends(get_authenticated_member),
    session: AsyncSession = Depends(get_session),
) -> ProfileResponse:
    profile = await _get_or_create_profile(session, member.user.id)
    provenance = list(
        await session.scalars(
            select(ProfileFieldProvenance).where(
                ProfileFieldProvenance.user_id == member.user.id
            )
        )
    )
    await session.commit()
    return _profile_response(profile, provenance)


@router.put("/profile", response_model=ProfileResponse)
async def update_profile(
    body: ProfileUpdate,
    member: AuthenticatedMember = Depends(get_authenticated_member),
    session: AsyncSession = Depends(get_session),
) -> ProfileResponse:
    profile = await _get_or_create_profile(session, member.user.id)
    updates = body.model_dump(exclude_unset=True, mode="json")
    if "portfolio_links" in updates:
        updates["portfolio_links"] = [item.model_dump(mode="json") for item in body.portfolio_links]
    for field_key, value in updates.items():
        setattr(profile, field_key, value)
        if field_key == "visibility":
            continue
        provenance = await session.scalar(
            select(ProfileFieldProvenance).where(
                ProfileFieldProvenance.user_id == member.user.id,
                ProfileFieldProvenance.field_key == field_key,
                ProfileFieldProvenance.source_type == "MANUAL",
            )
        )
        if provenance is None:
            session.add(
                ProfileFieldProvenance(
                    user_id=member.user.id,
                    field_key=field_key,
                    source_type="MANUAL",
                    user_confirmed=False,
                    visibility="private",
                )
            )
        else:
            provenance.user_confirmed = False
    if updates:
        profile.confirmed_at = None
    await session.flush()
    if updates:
        await calculate_score(session, member.user.id, profile)
    await session.refresh(profile, attribute_names=["updated_at"])
    provenance_rows = list(
        await session.scalars(
            select(ProfileFieldProvenance).where(
                ProfileFieldProvenance.user_id == member.user.id
            )
        )
    )
    await session.commit()
    return _profile_response(profile, provenance_rows)


@router.post("/profile/confirm", response_model=ProfileResponse)
async def confirm_profile(
    member: AuthenticatedMember = Depends(get_authenticated_member),
    session: AsyncSession = Depends(get_session),
    settings: Settings = Depends(get_settings),
) -> ProfileResponse:
    profile = await _get_or_create_profile(session, member.user.id)
    if not profile.full_name or not profile.headline:
        raise HTTPException(
            status_code=400,
            detail={"code": "profile_incomplete", "message": "Add your name and professional headline before confirming."},
        )
    incomplete_work = any(
        not str(item.get("role", "")).strip() or not str(item.get("company", "")).strip()
        for item in profile.work_history
    )
    incomplete_education = any(
        not str(item.get("institution", "")).strip() for item in profile.education
    )
    if incomplete_work or incomplete_education:
        raise HTTPException(
            status_code=400,
            detail={"code": "profile_entry_incomplete", "message": "Complete or remove unfinished work and education entries."},
        )
    unreviewed_import = await session.scalar(
        select(ProfileFieldProvenance.id).where(
            ProfileFieldProvenance.user_id == member.user.id,
            ProfileFieldProvenance.source_type != "MANUAL",
            ProfileFieldProvenance.user_confirmed.is_(False),
        ).limit(1)
    )
    if unreviewed_import is not None:
        raise HTTPException(
            status_code=400,
            detail={"code": "profile_review_required", "message": "Review every imported profile field before confirming."},
        )
    imported_field = await session.scalar(
        select(ProfileFieldProvenance.id).where(
            ProfileFieldProvenance.user_id == member.user.id,
            ProfileFieldProvenance.source_type != "MANUAL",
        ).limit(1)
    )
    if imported_field is not None:
        if not settings.terms_policy_version or not settings.privacy_policy_version:
            raise HTTPException(
                status_code=503,
                detail={"code": "legal_policy_not_configured", "message": "Current Terms and Privacy documents are not configured."},
            )
        required_types = {"TERMS": settings.terms_policy_version, "PRIVACY": settings.privacy_policy_version}
        for consent_type, version in required_types.items():
            consent = await session.scalar(
                select(ConsentRecord).where(
                    ConsentRecord.user_id == member.user.id,
                    ConsentRecord.consent_type == consent_type,
                    ConsentRecord.policy_version == version,
                )
            )
            if consent is None:
                raise HTTPException(
                    status_code=403,
                    detail={"code": "required_consent_missing", "message": "Accept the current Terms and Privacy policy before importing data."},
                )
    profile.confirmed_at = _now()
    provenance = list(
        await session.scalars(
            select(ProfileFieldProvenance).where(
                ProfileFieldProvenance.user_id == member.user.id
            )
        )
    )
    for row in provenance:
        if row.source_type == "MANUAL":
            row.user_confirmed = True
    await calculate_score(session, member.user.id, profile)
    await session.flush()
    await session.refresh(profile, attribute_names=["updated_at"])
    await session.commit()
    return _profile_response(profile, provenance)
