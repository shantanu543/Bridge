from __future__ import annotations

import hmac
import secrets
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from typing import Any
from uuid import UUID, uuid4

from fastapi import APIRouter, Depends, HTTPException, Request, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from pwdlib import PasswordHash
from sqlalchemy import and_, case, delete, or_, select
from sqlalchemy.dialects.postgresql import insert
from sqlalchemy.ext.asyncio import AsyncSession
from starlette.concurrency import run_in_threadpool

from pydantic import SecretStr
from app.core.config import Settings, get_settings
from app.core.email_delivery import send_security_code
from app.core.google_identity import verify_google_id_token
from app.core.tokens import keyed_hash, new_opaque_token, new_verification_code, sha256_hex
from app.db import get_session
from app.models import (
    AuthChallenge,
    Cohort,
    CohortMembership,
    CohortPost,
    BridgeUser,
    ConsentRecord,
    LinkedIdentity,
    ConnectionRequest,
    DirectConversation,
    DirectMessage,
    MemberIntent,
    Pod,
    PodMembership,
    PodMessage,
    ProfessionalIntent,
    PasswordCredential,
    Profile,
    RateLimitBucket,
    RefreshTokenHistory,
    UserSession,
)
from app.schemas import (
    AuthNotice,
    CurrentUserResponse,
    EmailLoginRequest,
    EmailLinkRequest,
    EmailRegistrationRequest,
    EmailVerificationRequest,
    GoogleChallengeResponse,
    GoogleSignInRequest,
    PasswordResetConfirmRequest,
    PasswordResetRequest,
    RefreshRequest,
    SessionResponse,
    SessionSummary,
)

router = APIRouter(prefix="/auth", tags=["auth"])
bearer_scheme = HTTPBearer(auto_error=False)
password_hasher = PasswordHash.recommended()
dummy_password_hash = password_hasher.hash(secrets.token_urlsafe(32))
GOOGLE_ISSUER = "https://accounts.google.com"
ACCESS_TOKEN_LIFETIME = timedelta(minutes=15)
CHALLENGE_LIFETIME = timedelta(minutes=5)
EMAIL_CODE_LIFETIME = timedelta(minutes=10)
RATE_WINDOW_SECONDS = 900


async def _local_demo_member(
    session: AsyncSession,
    email: str,
    name: str,
    headline: str,
    role: str,
    company: str,
    skills: list[str],
    now: datetime,
) -> BridgeUser:
    user = await session.scalar(select(BridgeUser).where(BridgeUser.email == email))
    if user is None:
        user = BridgeUser(email=email, account_status="active", email_verified_at=now)
        session.add(user)
        await session.flush()
    profile = await session.get(Profile, user.id)
    if profile is None:
        session.add(Profile(
            user_id=user.id,
            full_name=name,
            headline=headline,
            current_role=role,
            current_company=company,
            summary="Synthetic local demo profile. This data is for exploring the Bridge interface only.",
            years_experience=6,
            location="Local demo",
            skills=skills,
            visibility="members",
            confirmed_at=now,
            published_at=now,
        ))
    return user


@router.post("/dev/session", response_model=SessionResponse)
async def create_local_demo_session(
    session: AsyncSession = Depends(get_session),
    settings: Settings = Depends(get_settings),
) -> SessionResponse:
    """Create a marked synthetic member session only when the local dev flag is enabled."""
    if settings.environment != "dev" or not settings.dev_auth_bypass_enabled:
        raise HTTPException(status_code=404, detail={"code": "not_found", "message": "Not found."})

    demo_email = "local-demo@bridge.invalid"
    user = await session.scalar(select(BridgeUser).where(BridgeUser.email == demo_email))
    now = _now()
    if user is None:
        user = BridgeUser(email=demo_email, account_status="active", email_verified_at=now)
        session.add(user)
        await session.flush()
    profile = await session.get(Profile, user.id)
    if profile is None:
        profile = Profile(
            user_id=user.id,
            full_name="Local Demo Member",
            headline="Synthetic profile for local development",
            current_role="Product Engineer",
            current_company="Bridge Demo Workspace",
            summary="This is clearly labelled synthetic data for trying Bridge features in a local development build.",
            years_experience=5,
            location="Local development",
            skills=["Kotlin", "Product design", "Backend APIs"],
            visibility="members",
            confirmed_at=now,
            published_at=now,
        )
        session.add(profile)

    # Clearly synthetic local records make the dev screens usable without importing real people.
    aditi = await _local_demo_member(
        session, "demo-aditi@bridge.invalid", "Demo Aditi S.", "Staff Software Engineer · System Design",
        "Staff Software Engineer", "Demo Workspace", ["System Design", "Kotlin", "Mentorship"], now,
    )
    rohan = await _local_demo_member(
        session, "demo-rohan@bridge.invalid", "Demo Rohan K.", "Senior Software Engineer · Platform",
        "Senior Software Engineer", "Demo Workspace", ["Platform", "Open Source", "Interview Prep"], now,
    )
    intent_rows = {row.slug: row.id for row in await session.scalars(select(ProfessionalIntent).where(ProfessionalIntent.active.is_(True)))}
    # Give the clearly synthetic local profile a useful starting discovery context.
    for slug in ("job-switch", "system-design"):
        intent_id = intent_rows.get(slug)
        if intent_id and await session.scalar(select(MemberIntent.id).where(
            MemberIntent.user_id == user.id, MemberIntent.intent_id == intent_id
        )) is None:
            session.add(MemberIntent(id=uuid4(), user_id=user.id, intent_id=intent_id))
    for demo_user, slugs in ((aditi, ["system-design", "mentorship"]), (rohan, ["interview-prep", "open-source"])):
        for slug in slugs:
            intent_id = intent_rows.get(slug)
            if intent_id and await session.scalar(select(MemberIntent.id).where(MemberIntent.user_id == demo_user.id, MemberIntent.intent_id == intent_id)) is None:
                session.add(MemberIntent(id=uuid4(), user_id=demo_user.id, intent_id=intent_id))

    relationship = await session.scalar(select(ConnectionRequest).where(
        or_(
            and_(ConnectionRequest.requester_id == user.id, ConnectionRequest.recipient_id == aditi.id),
            and_(ConnectionRequest.requester_id == aditi.id, ConnectionRequest.recipient_id == user.id),
        )
    ))
    if relationship is None:
        session.add(ConnectionRequest(id=uuid4(), requester_id=user.id, recipient_id=aditi.id, status="ACCEPTED", note="Synthetic local demo connection"))
    low, high = sorted((user.id, aditi.id))
    conversation = await session.scalar(select(DirectConversation).where(DirectConversation.member_low_id == low, DirectConversation.member_high_id == high))
    if conversation is None:
        conversation = DirectConversation(id=uuid4(), member_low_id=low, member_high_id=high)
        session.add(conversation)
        await session.flush()
    if await session.scalar(select(DirectMessage.id).where(DirectMessage.conversation_id == conversation.id)) is None:
        session.add_all([
            DirectMessage(id=uuid4(), conversation_id=conversation.id, sender_id=aditi.id, body="Hi! I saw your System Design intent. What are you working on?"),
            DirectMessage(id=uuid4(), conversation_id=conversation.id, sender_id=user.id, body="I’m exploring platform design patterns and learning from the community."),
        ])
    incoming = await session.scalar(select(ConnectionRequest.id).where(
        ConnectionRequest.requester_id == rohan.id, ConnectionRequest.recipient_id == user.id,
        ConnectionRequest.status == "PENDING",
    ))
    if incoming is None:
        session.add(ConnectionRequest(id=uuid4(), requester_id=rohan.id, recipient_id=user.id, status="PENDING", note="Synthetic local demo request"))

    cohort_specs = [
        ("Demo · System Design Prep", "System Design", "Practice architecture, trade-offs, and interview scenarios."),
        ("Demo · Product Builders", "Product Engineering", "Share ideas and learn with people building useful products."),
        ("Demo · Open Source Circle", "Open Source", "Find collaborators and contribute to community projects."),
    ]
    for cohort_name, topic, description in cohort_specs:
        cohort = await session.scalar(select(Cohort).where(Cohort.name == cohort_name, Cohort.created_by == aditi.id))
        if cohort is None:
            cohort = Cohort(id=uuid4(), name=cohort_name, topic=topic, description=description, visibility="PUBLIC", created_by=aditi.id)
            session.add(cohort)
            await session.flush()
            session.add_all([
                CohortMembership(id=uuid4(), cohort_id=cohort.id, user_id=aditi.id, role="MODERATOR", status="ACTIVE"),
                CohortMembership(id=uuid4(), cohort_id=cohort.id, user_id=user.id, status="ACTIVE"),
            ])
            session.add(CohortPost(id=uuid4(), cohort_id=cohort.id, author_id=aditi.id, body="Welcome to this local demo discussion. What would you like to learn this week?"))

    pod_name = "Demo · Job Switch Circle"
    pod = await session.scalar(select(Pod).where(Pod.created_by == user.id, Pod.name == pod_name))
    if pod is None:
        pod = Pod(id=uuid4(), name=pod_name, goal="Share weekly progress and prepare for engineering interviews.", guidelines="Be kind, specific, and constructive.", discoverable=False, created_by=user.id, expires_at=now + timedelta(weeks=4))
        session.add(pod)
        await session.flush()
        session.add_all([
            PodMembership(id=uuid4(), pod_id=pod.id, user_id=user.id, role="ADMIN"),
            PodMembership(id=uuid4(), pod_id=pod.id, user_id=aditi.id),
        ])
        session.add(PodMessage(id=uuid4(), pod_id=pod.id, sender_id=aditi.id, body="Welcome to the demo Pod. Share a goal for this week."))

    response, _ = await _issue_session(session, user, settings, "Bridge local demo")
    await session.commit()
    return response.model_copy(update={"onboarding_complete": True, "local_demo": True})


@dataclass(frozen=True)
class AuthenticatedMember:
    user: BridgeUser
    session: UserSession


def _pepper(settings: Settings) -> SecretStr:
    if settings.email_token_pepper is None or len(
        settings.email_token_pepper.get_secret_value().encode("utf-8")
    ) < 32:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail={"code": "auth_not_configured", "message": "Authentication is not configured."},
        )
    return settings.email_token_pepper


def _now() -> datetime:
    return datetime.now(timezone.utc)


def _challenge_window(now: datetime) -> datetime:
    start = int(now.timestamp() // RATE_WINDOW_SECONDS) * RATE_WINDOW_SECONDS
    return datetime.fromtimestamp(start, tz=timezone.utc)


async def _enforce_rate_limit(
    session: AsyncSession,
    request: Request,
    action: str,
    identifiers: list[str],
    maximum: int,
    settings: Settings,
) -> None:
    pepper = _pepper(settings)
    client_ip = request.client.host if request.client is not None else "unknown"
    now = _now()
    window_start = _challenge_window(now)
    await session.execute(
        delete(RateLimitBucket).where(
            RateLimitBucket.window_started_at < now - timedelta(days=1)
        )
    )
    for identifier in identifiers:
        key_inputs = [
            f"{action}:ip:{client_ip}",
            f"{action}:subject:{identifier.casefold()}",
        ]
        for key_input in key_inputs:
            bucket_key = keyed_hash(key_input, pepper)
            window_changed = RateLimitBucket.window_started_at < window_start
            statement = (
                insert(RateLimitBucket)
                .values(
                    id=uuid4(),
                    action=action,
                    bucket_key=bucket_key,
                    window_started_at=window_start,
                    request_count=1,
                )
                .on_conflict_do_update(
                    index_elements=[RateLimitBucket.action, RateLimitBucket.bucket_key],
                    set_={
                        "window_started_at": case(
                            (window_changed, window_start), else_=RateLimitBucket.window_started_at
                        ),
                        "request_count": case(
                            (window_changed, 1), else_=RateLimitBucket.request_count + 1
                        ),
                    },
                )
                .returning(RateLimitBucket.request_count)
            )
            count = await session.scalar(statement)
            await session.commit()
            if count is not None and count > maximum:
                raise HTTPException(
                    status_code=status.HTTP_429_TOO_MANY_REQUESTS,
                    detail={"code": "rate_limited", "message": "Too many attempts. Try again later."},
                    headers={"Retry-After": str(RATE_WINDOW_SECONDS)},
                )


async def _onboarding_complete(
    session: AsyncSession,
    user: BridgeUser,
    settings: Settings,
) -> bool:
    profile = await session.get(Profile, user.id)
    consent_rows = await session.execute(
        select(ConsentRecord.consent_type, ConsentRecord.policy_version).where(
            ConsentRecord.user_id == user.id,
            ConsentRecord.consent_type.in_(["TERMS", "PRIVACY"]),
        )
    )
    consent_versions = set(consent_rows.all())
    required_consents = (
        ("TERMS", settings.terms_policy_version),
        ("PRIVACY", settings.privacy_policy_version),
    )
    return (
        profile is not None
        and profile.confirmed_at is not None
        and all(
            version is not None and (kind, version) in consent_versions
            for kind, version in required_consents
        )
    )


async def _issue_session(
    session: AsyncSession,
    user: BridgeUser,
    settings: Settings,
    device_label: str | None,
) -> tuple[SessionResponse, UserSession]:
    now = _now()
    access_token = new_opaque_token()
    refresh_token = new_opaque_token()
    db_session = UserSession(
        id=uuid4(),
        family_id=uuid4(),
        user_id=user.id,
        access_token_hash=sha256_hex(access_token),
        refresh_token_hash=sha256_hex(refresh_token),
        access_expires_at=now + timedelta(seconds=settings.access_token_ttl_seconds),
        refresh_expires_at=now + timedelta(days=settings.refresh_token_ttl_days),
        device_label=device_label,
    )
    session.add(db_session)
    response = SessionResponse(
        access_token=access_token,
        refresh_token=refresh_token,
        expires_in=settings.access_token_ttl_seconds,
        user_id=user.id,
        onboarding_complete=await _onboarding_complete(session, user, settings),
    )
    return response, db_session


async def get_authenticated_member(
    credentials: HTTPAuthorizationCredentials | None = Depends(bearer_scheme),
    session: AsyncSession = Depends(get_session),
) -> AuthenticatedMember:
    if credentials is None or credentials.scheme.lower() != "bearer":
        raise HTTPException(status_code=401, detail={"code": "unauthorized", "message": "Sign in required."})
    now = _now()
    db_session = await session.scalar(
        select(UserSession).where(
            UserSession.access_token_hash == sha256_hex(credentials.credentials),
            UserSession.access_expires_at > now,
            UserSession.revoked_at.is_(None),
        )
    )
    if db_session is None:
        raise HTTPException(status_code=401, detail={"code": "session_expired", "message": "Sign in again."})
    user = await session.get(BridgeUser, db_session.user_id)
    if user is None or user.account_status != "active" or user.deleted_at is not None:
        raise HTTPException(status_code=401, detail={"code": "account_unavailable", "message": "Account unavailable."})
    return AuthenticatedMember(user=user, session=db_session)


async def _issue_google_challenge(
    session: AsyncSession,
    purpose: str,
    user_id: UUID | None = None,
) -> GoogleChallengeResponse:
    nonce = new_opaque_token()
    expires_at = _now() + CHALLENGE_LIFETIME
    session.add(
        AuthChallenge(
            id=(challenge_id := uuid4()),
            purpose=purpose,
            user_id=user_id,
            code_hash=sha256_hex(""),
            nonce_hash=sha256_hex(nonce),
            expires_at=expires_at,
        )
    )
    await session.commit()
    return GoogleChallengeResponse(challenge_id=challenge_id, nonce=nonce, expires_at=expires_at)


async def _verify_challenge(
    session: AsyncSession,
    challenge_id: UUID,
    purpose: str,
    nonce: str,
    user_id: UUID | None = None,
) -> AuthChallenge:
    challenge = await session.scalar(
        select(AuthChallenge)
        .where(
            AuthChallenge.id == challenge_id,
            AuthChallenge.purpose == purpose,
            AuthChallenge.consumed_at.is_(None),
            AuthChallenge.expires_at > _now(),
        )
        .with_for_update()
    )
    if (
        challenge is None
        or challenge.nonce_hash is None
        or not hmac.compare_digest(challenge.nonce_hash, sha256_hex(nonce))
        or (user_id is not None and challenge.user_id != user_id)
    ):
        raise HTTPException(status_code=401, detail={"code": "invalid_challenge", "message": "Sign-in challenge expired. Try again."})
    challenge.consumed_at = _now()
    return challenge


async def _google_claims(token: str, settings: Settings) -> dict[str, Any]:
    if not settings.google_server_client_id:
        raise HTTPException(
            status_code=503,
            detail={"code": "google_sign_in_unavailable", "message": "Google sign-in is not configured."},
        )
    try:
        claims = await verify_google_id_token(token, settings.google_server_client_id)
        return dict(claims)
    except Exception as error:
        raise HTTPException(
            status_code=401,
            detail={"code": "invalid_google_token", "message": "Google sign-in could not be verified."},
        ) from error


@router.post("/google/challenge", response_model=GoogleChallengeResponse)
async def google_challenge(
    request: Request,
    session: AsyncSession = Depends(get_session),
    settings: Settings = Depends(get_settings),
) -> GoogleChallengeResponse:
    await _enforce_rate_limit(session, request, "google_challenge", ["request"], 12, settings)
    return await _issue_google_challenge(session, "google_sign_in")


@router.post("/google/exchange", response_model=SessionResponse)
async def google_exchange(
    body: GoogleSignInRequest,
    request: Request,
    session: AsyncSession = Depends(get_session),
    settings: Settings = Depends(get_settings),
) -> SessionResponse:
    await _enforce_rate_limit(session, request, "google_exchange", [body.challenge_id.hex], 8, settings)
    claims = await _google_claims(body.id_token, settings)
    subject = str(claims["sub"])
    email = str(claims.get("email", "")).strip().lower()
    if not email:
        raise HTTPException(status_code=401, detail={"code": "google_email_missing", "message": "A verified Google email is required."})
    nonce = claims.get("nonce")
    if not isinstance(nonce, str):
        raise HTTPException(status_code=401, detail={"code": "google_nonce_missing", "message": "Restart Google sign-in."})
    await _verify_challenge(session, body.challenge_id, "google_sign_in", nonce)

    identity = await session.scalar(
        select(LinkedIdentity).where(
            LinkedIdentity.issuer == GOOGLE_ISSUER,
            LinkedIdentity.subject == subject,
        )
    )
    if identity is not None:
        user = await session.get(BridgeUser, identity.user_id)
        if user is None or user.account_status != "active":
            await session.commit()
            raise HTTPException(status_code=403, detail={"code": "account_unavailable", "message": "This Bridge account is unavailable."})
        identity.provider_email = email
        identity.provider_email_verified = True
    else:
        existing_user = await session.scalar(select(BridgeUser).where(BridgeUser.email == email))
        if existing_user is not None:
            await session.commit()
            raise HTTPException(
                status_code=409,
                detail={"code": "account_link_required", "message": "Sign in with your existing method, then link Google from account settings."},
            )
        user = BridgeUser(email=email, email_verified_at=_now(), account_status="active")
        session.add(user)
        await session.flush()
        session.add(
            LinkedIdentity(
                user_id=user.id,
                provider="google",
                issuer=GOOGLE_ISSUER,
                subject=subject,
                provider_email=email,
                provider_email_verified=True,
            )
        )
        session.add(Profile(user_id=user.id, visibility="private"))
    response, _ = await _issue_session(session, user, settings, body.device_label)
    await session.commit()
    return response


@router.post("/google/link-challenge", response_model=GoogleChallengeResponse)
async def google_link_challenge(
    request: Request,
    member: AuthenticatedMember = Depends(get_authenticated_member),
    session: AsyncSession = Depends(get_session),
    settings: Settings = Depends(get_settings),
) -> GoogleChallengeResponse:
    await _enforce_rate_limit(session, request, "google_link_challenge", [member.user.id.hex], 8, settings)
    return await _issue_google_challenge(session, "google_link", member.user.id)


@router.post("/google/link", response_model=AuthNotice)
async def link_google_identity(
    body: GoogleSignInRequest,
    request: Request,
    member: AuthenticatedMember = Depends(get_authenticated_member),
    session: AsyncSession = Depends(get_session),
    settings: Settings = Depends(get_settings),
) -> AuthNotice:
    await _enforce_rate_limit(session, request, "google_link", [member.user.id.hex], 8, settings)
    claims = await _google_claims(body.id_token, settings)
    nonce = claims.get("nonce")
    if not isinstance(nonce, str):
        raise HTTPException(status_code=401, detail={"code": "google_nonce_missing", "message": "Restart Google linking."})
    await _verify_challenge(session, body.challenge_id, "google_link", nonce, member.user.id)
    subject = str(claims["sub"])
    existing_identity = await session.scalar(
        select(LinkedIdentity).where(
            LinkedIdentity.issuer == GOOGLE_ISSUER,
            LinkedIdentity.subject == subject,
        )
    )
    if existing_identity is not None and existing_identity.user_id != member.user.id:
        await session.commit()
        raise HTTPException(status_code=409, detail={"code": "identity_already_linked", "message": "This Google account is linked to another Bridge account."})
    email = str(claims.get("email", "")).strip().lower()
    email_owner = await session.scalar(select(BridgeUser).where(BridgeUser.email == email)) if email else None
    if email_owner is not None and email_owner.id != member.user.id:
        await session.commit()
        raise HTTPException(status_code=409, detail={"code": "email_already_linked", "message": "Sign in with the Bridge account that uses this verified email."})
    if existing_identity is None:
        session.add(
            LinkedIdentity(
                user_id=member.user.id,
                provider="google",
                issuer=GOOGLE_ISSUER,
                subject=subject,
                provider_email=email or None,
                provider_email_verified=bool(email),
            )
        )
    if member.user.email is None and email:
        member.user.email = email
        member.user.email_verified_at = _now()
    await session.commit()
    return AuthNotice(message="Google is linked to your Bridge account.")


@router.post("/email/link", response_model=AuthNotice)
async def link_email_password(
    body: EmailLinkRequest,
    member: AuthenticatedMember = Depends(get_authenticated_member),
    session: AsyncSession = Depends(get_session),
) -> AuthNotice:
    if member.user.email is None or member.user.email_verified_at is None:
        raise HTTPException(status_code=409, detail={"code": "verified_email_required", "message": "Verify an email on your Bridge account before adding a password."})
    if member.session.created_at < _now() - timedelta(minutes=10):
        raise HTTPException(status_code=401, detail={"code": "reauthentication_required", "message": "Sign in again before adding a password."})
    if await session.get(PasswordCredential, member.user.id) is not None:
        raise HTTPException(status_code=409, detail={"code": "password_already_linked", "message": "This account already has a password."})
    password_hash = await run_in_threadpool(password_hasher.hash, body.password.get_secret_value())
    session.add(PasswordCredential(user_id=member.user.id, password_hash=password_hash))
    await session.commit()
    return AuthNotice(message="Email and password are linked to your Bridge account.")


@router.post("/email/register", response_model=AuthNotice, status_code=202)
async def register_email(
    body: EmailRegistrationRequest,
    request: Request,
    session: AsyncSession = Depends(get_session),
    settings: Settings = Depends(get_settings),
) -> AuthNotice:
    email = str(body.email).strip().lower()
    await _enforce_rate_limit(session, request, "email_register", [email], 5, settings)
    pepper = _pepper(settings)
    user = await session.scalar(select(BridgeUser).where(BridgeUser.email == email).with_for_update())
    can_send = False
    if user is None:
        password_hash = await run_in_threadpool(password_hasher.hash, body.password.get_secret_value())
        user = BridgeUser(email=email, account_status="pending_verification")
        session.add(user)
        await session.flush()
        session.add(PasswordCredential(user_id=user.id, password_hash=password_hash))
        can_send = True
    elif user.email_verified_at is None and user.account_status == "pending_verification":
        credential = await session.get(PasswordCredential, user.id)
        if credential is None:
            password_hash = await run_in_threadpool(password_hasher.hash, body.password.get_secret_value())
            session.add(PasswordCredential(user_id=user.id, password_hash=password_hash))
            can_send = True
        else:
            can_send = True
    if can_send:
        code = new_verification_code()
        session.add(
            AuthChallenge(
                purpose="email_verify",
                user_id=user.id,
                subject_hash=keyed_hash(email, pepper),
                email=email,
                code_hash=keyed_hash(code, pepper),
                expires_at=_now() + EMAIL_CODE_LIFETIME,
            )
        )
    await session.commit()
    if can_send:
        try:
            await send_security_code(email, code, "verify_email")
        except Exception as error:
            raise HTTPException(
                status_code=503,
                detail={"code": "email_delivery_unavailable", "message": "Email verification could not be sent. Try again shortly."},
            ) from error
    return AuthNotice(message="If this address can be registered, a verification code has been sent.", verification_required=True)


async def _latest_code_challenge(
    session: AsyncSession,
    purpose: str,
    email: str,
) -> AuthChallenge | None:
    pepper = _pepper(get_settings())
    return await session.scalar(
        select(AuthChallenge)
        .where(
            AuthChallenge.purpose == purpose,
            AuthChallenge.subject_hash == keyed_hash(email, pepper),
            AuthChallenge.consumed_at.is_(None),
            AuthChallenge.expires_at > _now(),
            AuthChallenge.attempts < 5,
        )
        .order_by(AuthChallenge.created_at.desc())
        .limit(1)
        .with_for_update()
    )


async def _validate_code(challenge: AuthChallenge | None, code: str, settings: Settings) -> bool:
    if challenge is None:
        return False
    expected = keyed_hash(code, _pepper(settings))
    if not hmac.compare_digest(challenge.code_hash, expected):
        challenge.attempts += 1
        if challenge.attempts >= 5:
            challenge.consumed_at = _now()
        return False
    challenge.consumed_at = _now()
    return True


@router.post("/email/verify", response_model=SessionResponse)
async def verify_email(
    body: EmailVerificationRequest,
    request: Request,
    session: AsyncSession = Depends(get_session),
    settings: Settings = Depends(get_settings),
) -> SessionResponse:
    email = str(body.email).strip().lower()
    await _enforce_rate_limit(session, request, "email_verify", [email], 8, settings)
    challenge = await _latest_code_challenge(session, "email_verify", email)
    if not await _validate_code(challenge, body.code, settings):
        await session.commit()
        raise HTTPException(status_code=400, detail={"code": "invalid_verification_code", "message": "The code is invalid or expired."})
    assert challenge is not None and challenge.user_id is not None
    user = await session.get(BridgeUser, challenge.user_id)
    if user is None:
        await session.commit()
        raise HTTPException(status_code=400, detail={"code": "invalid_verification_code", "message": "The code is invalid or expired."})
    user.email_verified_at = _now()
    user.account_status = "active"
    if await session.get(Profile, user.id) is None:
        session.add(Profile(user_id=user.id, visibility="private"))
    response, _ = await _issue_session(session, user, settings, body.device_label)
    await session.commit()
    return response


@router.post("/email/login", response_model=SessionResponse)
async def login_email(
    body: EmailLoginRequest,
    request: Request,
    session: AsyncSession = Depends(get_session),
    settings: Settings = Depends(get_settings),
) -> SessionResponse:
    email = str(body.email).strip().lower()
    await _enforce_rate_limit(session, request, "email_login", [email], 10, settings)
    user = await session.scalar(select(BridgeUser).where(BridgeUser.email == email))
    credential = await session.get(PasswordCredential, user.id) if user else None
    password_hash = credential.password_hash if credential else dummy_password_hash
    valid = await run_in_threadpool(
        password_hasher.verify,
        body.password.get_secret_value(),
        password_hash,
    )
    if not valid or user is None or user.email_verified_at is None or user.account_status != "active":
        raise HTTPException(status_code=401, detail={"code": "invalid_credentials", "message": "Email or password is incorrect."})
    response, _ = await _issue_session(session, user, settings, body.device_label)
    await session.commit()
    return response


@router.post("/password-reset", response_model=AuthNotice, status_code=202)
async def request_password_reset(
    body: PasswordResetRequest,
    request: Request,
    session: AsyncSession = Depends(get_session),
    settings: Settings = Depends(get_settings),
) -> AuthNotice:
    email = str(body.email).strip().lower()
    await _enforce_rate_limit(session, request, "password_reset", [email], 5, settings)
    pepper = _pepper(settings)
    user = await session.scalar(
        select(BridgeUser)
        .join(PasswordCredential, PasswordCredential.user_id == BridgeUser.id)
        .where(BridgeUser.email == email, BridgeUser.email_verified_at.is_not(None))
    )
    code: str | None = None
    if user is not None:
        code = new_verification_code()
        session.add(
            AuthChallenge(
                purpose="password_reset",
                user_id=user.id,
                subject_hash=keyed_hash(email, pepper),
                email=email,
                code_hash=keyed_hash(code, pepper),
                expires_at=_now() + EMAIL_CODE_LIFETIME,
            )
        )
    await session.commit()
    if code is not None:
        try:
            await send_security_code(email, code, "password_reset")
        except Exception as error:
            raise HTTPException(
                status_code=503,
                detail={"code": "email_delivery_unavailable", "message": "Password reset could not be sent. Try again shortly."},
            ) from error
    return AuthNotice(message="If an account matches this address, a reset code has been sent.")


@router.post("/password-reset/confirm", response_model=AuthNotice)
async def confirm_password_reset(
    body: PasswordResetConfirmRequest,
    request: Request,
    session: AsyncSession = Depends(get_session),
    settings: Settings = Depends(get_settings),
) -> AuthNotice:
    email = str(body.email).strip().lower()
    await _enforce_rate_limit(session, request, "password_reset_confirm", [email], 8, settings)
    challenge = await _latest_code_challenge(session, "password_reset", email)
    if not await _validate_code(challenge, body.code, settings):
        await session.commit()
        raise HTTPException(status_code=400, detail={"code": "invalid_reset_code", "message": "The code is invalid or expired."})
    assert challenge is not None and challenge.user_id is not None
    credential = await session.get(PasswordCredential, challenge.user_id)
    if credential is None:
        await session.commit()
        raise HTTPException(status_code=400, detail={"code": "invalid_reset_code", "message": "The code is invalid or expired."})
    credential.password_hash = await run_in_threadpool(
        password_hasher.hash,
        body.new_password.get_secret_value(),
    )
    await session.execute(
        UserSession.__table__.update()
        .where(UserSession.user_id == challenge.user_id, UserSession.revoked_at.is_(None))
        .values(revoked_at=_now())
    )
    await session.commit()
    return AuthNotice(message="Your password has been reset. Sign in with your new password.")


@router.post("/refresh", response_model=SessionResponse)
async def refresh_session(
    body: RefreshRequest,
    request: Request,
    session: AsyncSession = Depends(get_session),
    settings: Settings = Depends(get_settings),
) -> SessionResponse:
    raw_token = body.refresh_token.get_secret_value()
    await _enforce_rate_limit(session, request, "session_refresh", [sha256_hex(raw_token)], 60, settings)
    token_hash = sha256_hex(raw_token)
    db_session = await session.scalar(
        select(UserSession)
        .where(UserSession.refresh_token_hash == token_hash)
        .with_for_update()
    )
    now = _now()
    if db_session is None:
        used = await session.scalar(
            select(RefreshTokenHistory).where(RefreshTokenHistory.token_hash == token_hash)
        )
        if used is not None:
            used_session = await session.get(UserSession, used.session_id)
            if used_session is not None:
                await session.execute(
                    UserSession.__table__.update()
                    .where(UserSession.family_id == used_session.family_id)
                    .values(revoked_at=now)
                )
                await session.commit()
        raise HTTPException(status_code=401, detail={"code": "invalid_refresh_token", "message": "Sign in again."})
    if db_session.revoked_at is not None or db_session.refresh_expires_at <= now:
        db_session.revoked_at = now
        await session.commit()
        raise HTTPException(status_code=401, detail={"code": "invalid_refresh_token", "message": "Sign in again."})
    user = await session.get(BridgeUser, db_session.user_id)
    if user is None or user.account_status != "active":
        db_session.revoked_at = now
        await session.commit()
        raise HTTPException(status_code=401, detail={"code": "account_unavailable", "message": "Sign in again."})

    new_access = new_opaque_token()
    new_refresh = new_opaque_token()
    session.add(RefreshTokenHistory(token_hash=token_hash, session_id=db_session.id))
    db_session.access_token_hash = sha256_hex(new_access)
    db_session.refresh_token_hash = sha256_hex(new_refresh)
    db_session.access_expires_at = now + timedelta(seconds=settings.access_token_ttl_seconds)
    db_session.refresh_expires_at = now + timedelta(days=settings.refresh_token_ttl_days)
    db_session.last_used_at = now
    await session.commit()
    return SessionResponse(
        access_token=new_access,
        refresh_token=new_refresh,
        expires_in=settings.access_token_ttl_seconds,
        user_id=user.id,
        onboarding_complete=await _onboarding_complete(session, user, settings),
    )


@router.post("/logout", response_model=AuthNotice)
async def logout(
    body: RefreshRequest,
    request: Request,
    session: AsyncSession = Depends(get_session),
    settings: Settings = Depends(get_settings),
) -> AuthNotice:
    token_hash = sha256_hex(body.refresh_token.get_secret_value())
    await _enforce_rate_limit(session, request, "logout", [token_hash], 30, settings)
    db_session = await session.scalar(
        select(UserSession)
        .where(UserSession.refresh_token_hash == token_hash)
        .with_for_update()
    )
    if db_session is not None and db_session.revoked_at is None:
        db_session.revoked_at = _now()
        await session.commit()
    return AuthNotice(message="Signed out.")


@router.post("/logout-all", response_model=AuthNotice)
async def logout_all(
    member: AuthenticatedMember = Depends(get_authenticated_member),
    session: AsyncSession = Depends(get_session),
) -> AuthNotice:
    await session.execute(
        UserSession.__table__.update()
        .where(UserSession.user_id == member.user.id, UserSession.revoked_at.is_(None))
        .values(revoked_at=_now())
    )
    await session.commit()
    return AuthNotice(message="All Bridge sessions have been revoked.")


@router.get("/sessions", response_model=list[SessionSummary])
async def list_sessions(
    member: AuthenticatedMember = Depends(get_authenticated_member),
    session: AsyncSession = Depends(get_session),
) -> list[SessionSummary]:
    sessions = await session.scalars(
        select(UserSession)
        .where(UserSession.user_id == member.user.id, UserSession.revoked_at.is_(None))
        .order_by(UserSession.created_at.desc())
    )
    return [
        SessionSummary(
            id=item.id,
            device_label=item.device_label,
            created_at=item.created_at,
            last_used_at=item.last_used_at,
            current=item.id == member.session.id,
        )
        for item in sessions
    ]


@router.delete("/sessions/{session_id}", response_model=AuthNotice)
async def revoke_session(
    session_id: UUID,
    member: AuthenticatedMember = Depends(get_authenticated_member),
    session: AsyncSession = Depends(get_session),
) -> AuthNotice:
    target = await session.scalar(
        select(UserSession).where(
            UserSession.id == session_id,
            UserSession.user_id == member.user.id,
            UserSession.revoked_at.is_(None),
        )
    )
    if target is None:
        raise HTTPException(status_code=404, detail={"code": "session_not_found", "message": "Session not found."})
    target.revoked_at = _now()
    await session.commit()
    return AuthNotice(message="Session revoked.")


@router.get("/me", response_model=CurrentUserResponse)
async def current_user(
    member: AuthenticatedMember = Depends(get_authenticated_member),
) -> CurrentUserResponse:
    user = member.user
    return CurrentUserResponse(
        id=user.id,
        email=user.email,
        email_verified=user.email_verified_at is not None,
        account_status=user.account_status,
        created_at=user.created_at,
    )
