import hashlib
import hmac
import secrets

from pydantic import SecretStr


def new_opaque_token() -> str:
    return secrets.token_urlsafe(32)


def new_verification_code() -> str:
    return f"{secrets.randbelow(1_000_000):06d}"


def sha256_hex(value: str) -> str:
    return hashlib.sha256(value.encode("utf-8")).hexdigest()


def keyed_hash(value: str, pepper: SecretStr) -> str:
    return hmac.new(
        pepper.get_secret_value().encode("utf-8"),
        value.encode("utf-8"),
        hashlib.sha256,
    ).hexdigest()
