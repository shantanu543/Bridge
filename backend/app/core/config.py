from functools import lru_cache
from typing import Literal

from pydantic import Field, SecretStr, model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    app_name: str = "Bridge API"
    environment: Literal["dev", "stag", "prod"] = Field(
        default="dev",
        validation_alias="BRIDGE_ENV",
    )
    dev_auth_bypass_enabled: bool = Field(default=False, validation_alias="DEV_AUTH_BYPASS_ENABLED")
    api_prefix: str = "/api/v1"
    database_url: str = "postgresql+asyncpg://bridge:bridge_local@localhost:5432/bridge_dev"
    log_level: str = "INFO"
    google_server_client_id: str | None = Field(
        default=None,
        validation_alias="GOOGLE_SERVER_CLIENT_ID",
    )
    email_token_pepper: SecretStr | None = None
    email_from: str = "Bridge <no-reply@example.invalid>"
    smtp_host: str = "localhost"
    smtp_port: int = Field(default=1025, ge=1, le=65535)
    smtp_username: str | None = None
    smtp_password: SecretStr | None = None
    smtp_starttls: bool = False
    access_token_ttl_seconds: int = Field(default=900, ge=300, le=3600)
    refresh_token_ttl_days: int = Field(default=30, ge=1, le=90)
    terms_policy_version: str | None = Field(default=None, validation_alias="TERMS_POLICY_VERSION")
    terms_policy_url: str | None = Field(default=None, validation_alias="TERMS_POLICY_URL")
    privacy_policy_version: str | None = Field(default=None, validation_alias="PRIVACY_POLICY_VERSION")
    privacy_policy_url: str | None = Field(default=None, validation_alias="PRIVACY_POLICY_URL")
    import_consent_version: str | None = Field(default=None, validation_alias="IMPORT_CONSENT_VERSION")

    @model_validator(mode="after")
    def validate_production_auth_configuration(self) -> "Settings":
        if self.dev_auth_bypass_enabled and self.environment != "dev":
            raise ValueError("DEV_AUTH_BYPASS_ENABLED may only be enabled when BRIDGE_ENV=dev")
        if self.environment == "prod":
            if not self.google_server_client_id:
                raise ValueError("GOOGLE_SERVER_CLIENT_ID is required in production")
            if (
                self.email_token_pepper is None
                or len(self.email_token_pepper.get_secret_value().encode("utf-8")) < 32
            ):
                raise ValueError("EMAIL_TOKEN_PEPPER is required in production")
            if not self.smtp_starttls:
                raise ValueError("SMTP_STARTTLS must be enabled in production")
            if (
                self.smtp_host in {"localhost", "127.0.0.1"}
                or not self.smtp_username
                or self.smtp_password is None
                or not self.smtp_password.get_secret_value()
            ):
                raise ValueError("A verified production SMTP provider and credentials are required")
            if not self.terms_policy_version or not self.terms_policy_url:
                raise ValueError("Terms policy version and URL are required in production")
            if not self.privacy_policy_version or not self.privacy_policy_url:
                raise ValueError("Privacy policy version and URL are required in production")
        return self

    @property
    def openapi_enabled(self) -> bool:
        return self.environment == "dev"


@lru_cache
def get_settings() -> Settings:
    return Settings()
