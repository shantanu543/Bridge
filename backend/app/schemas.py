from datetime import datetime
from typing import Literal
from uuid import UUID

from pydantic import BaseModel, ConfigDict, EmailStr, Field, HttpUrl, SecretStr


class WorkExperience(BaseModel):
    model_config = ConfigDict(extra="forbid")

    role: str = Field(max_length=160)
    company: str = Field(max_length=160)
    location: str | None = Field(default=None, max_length=160)
    start_date: str | None = Field(default=None, max_length=16)
    end_date: str | None = Field(default=None, max_length=16)
    is_current: bool = False
    description: str | None = Field(default=None, max_length=4000)


class EducationRecord(BaseModel):
    model_config = ConfigDict(extra="forbid")

    institution: str = Field(max_length=200)
    qualification: str | None = Field(default=None, max_length=160)
    field_of_study: str | None = Field(default=None, max_length=160)
    start_year: int | None = Field(default=None, ge=1900, le=2100)
    end_year: int | None = Field(default=None, ge=1900, le=2100)


class PortfolioLink(BaseModel):
    model_config = ConfigDict(extra="forbid")

    label: str = Field(min_length=1, max_length=80)
    url: HttpUrl


class ProfileUpdate(BaseModel):
    model_config = ConfigDict(extra="forbid")

    full_name: str | None = Field(default=None, max_length=160)
    headline: str | None = Field(default=None, max_length=240)
    current_role: str | None = Field(default=None, max_length=160)
    current_company: str | None = Field(default=None, max_length=160)
    summary: str | None = Field(default=None, max_length=4000)
    years_experience: int | None = Field(default=None, ge=0, le=70)
    location: str | None = Field(default=None, max_length=160)
    timezone: str | None = Field(default=None, max_length=64)
    visibility: Literal["private", "connections", "members"] = "private"
    work_history: list[WorkExperience] = Field(default_factory=list, max_length=50)
    education: list[EducationRecord] = Field(default_factory=list, max_length=30)
    skills: list[str] = Field(default_factory=list, max_length=100)
    projects: list[dict[str, str]] = Field(default_factory=list, max_length=50)
    achievements: list[dict[str, str]] = Field(default_factory=list, max_length=50)
    certifications: list[dict[str, str]] = Field(default_factory=list, max_length=50)
    portfolio_links: list[PortfolioLink] = Field(default_factory=list, max_length=30)


class ProfileFieldSource(BaseModel):
    field_key: str
    source_type: Literal["LINKEDIN", "GITHUB", "RESUME", "MANUAL"]
    imported_at: datetime | None
    user_confirmed: bool
    visibility: Literal["private", "connections", "members"]


class ProfileResponse(ProfileUpdate):
    user_id: UUID
    visibility: Literal["private", "connections", "members"]
    confirmed_at: datetime | None
    published_at: datetime | None
    updated_at: datetime
    provenance: list[ProfileFieldSource] = Field(default_factory=list)


class BridgeScoreResponse(BaseModel):
    state: Literal["NOT_READY", "PROCESSING", "CALCULATED", "NEEDS_REVIEW", "FAILED"]
    score: int | None = Field(default=None, ge=0, le=100)
    calculated_at: datetime | None = None
    suggestions: list[str] = Field(default_factory=list)
    verification_state: Literal["NOT_STARTED", "IN_REVIEW", "VERIFIED", "UNABLE_TO_VERIFY"]


class BridgeScoreHistoryItem(BaseModel):
    state: Literal["NOT_READY", "CALCULATED", "NEEDS_REVIEW", "FAILED"]
    score: int | None = Field(default=None, ge=0, le=100)
    calculated_at: datetime


class BridgeScoreHistoryResponse(BaseModel):
    items: list[BridgeScoreHistoryItem]


class PassportResponse(BaseModel):
    full_name: str | None = None
    headline: str | None = None
    current_role: str | None = None
    current_company: str | None = None
    summary: str | None = None
    years_experience: int | None = None
    skills: list[str] = Field(default_factory=list, max_length=6)
    work_history: list[WorkExperience] = Field(default_factory=list)
    education: list[EducationRecord] = Field(default_factory=list)
    projects: list[dict[str, str]] = Field(default_factory=list)
    achievements: list[dict[str, str]] = Field(default_factory=list)
    portfolio_links: list[PortfolioLink] = Field(default_factory=list)
    score: BridgeScoreResponse
    is_private_preview: bool = True


class ConsentUpdate(BaseModel):
    model_config = ConfigDict(extra="forbid")

    accept_terms: bool
    accept_privacy: bool
    provider_data: list[Literal["github", "linkedin"]] = Field(default_factory=list)
    resume_processing: bool = False


class ConsentReceipt(BaseModel):
    policy_type: str
    policy_version: str
    accepted_at: datetime
    provider: str | None = None


class ActivePolicy(BaseModel):
    policy_type: Literal["TERMS", "PRIVACY"]
    version: str
    url: HttpUrl


class OnboardingStatus(BaseModel):
    account_created: bool
    required_consents_recorded: bool
    profile_started: bool
    profile_confirmed: bool
    profile_published: bool
    connected_sources: list[str]


class GoogleChallengeResponse(BaseModel):
    challenge_id: UUID
    nonce: str
    expires_at: datetime


class GoogleSignInRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    challenge_id: UUID
    id_token: str = Field(min_length=100, max_length=10000)
    device_label: str | None = Field(default=None, max_length=120)


class EmailRegistrationRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    email: EmailStr
    password: SecretStr = Field(min_length=12, max_length=128)


class EmailLoginRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    email: EmailStr
    password: SecretStr = Field(min_length=1, max_length=128)
    device_label: str | None = Field(default=None, max_length=120)


class EmailLinkRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    password: SecretStr = Field(min_length=12, max_length=128)


class EmailVerificationRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    email: EmailStr
    code: str = Field(pattern=r"^\d{6}$")
    device_label: str | None = Field(default=None, max_length=120)


class PasswordResetRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    email: EmailStr


class PasswordResetConfirmRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    email: EmailStr
    code: str = Field(pattern=r"^\d{6}$")
    new_password: SecretStr = Field(min_length=12, max_length=128)


class RefreshRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    refresh_token: SecretStr = Field(min_length=40, max_length=256)


class SessionResponse(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "Bearer"
    expires_in: int
    user_id: UUID
    onboarding_complete: bool
    local_demo: bool = False


class AuthNotice(BaseModel):
    message: str
    verification_required: bool = False


class CurrentUserResponse(BaseModel):
    id: UUID
    email: EmailStr | None
    email_verified: bool
    account_status: str
    created_at: datetime


class SessionSummary(BaseModel):
    id: UUID
    device_label: str | None
    created_at: datetime
    last_used_at: datetime | None
    current: bool
