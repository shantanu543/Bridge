from functools import lru_cache
from typing import Any

import cachecontrol
import requests
from google.auth.transport.requests import Request
from google.oauth2 import id_token
from starlette.concurrency import run_in_threadpool


class BoundedSession(requests.Session):
    def request(self, method: str, url: str, **kwargs: Any) -> requests.Response:
        timeout = kwargs.get("timeout")
        kwargs["timeout"] = min(timeout, 5.0) if isinstance(timeout, (int, float)) else 5.0
        return super().request(method, url, **kwargs)


@lru_cache(maxsize=1)
def _google_request() -> Request:
    return Request(session=cachecontrol.CacheControl(BoundedSession()))


def _verify_sync(token: str, audience: str) -> dict[str, Any]:
    claims = id_token.verify_oauth2_token(token, _google_request(), audience=audience)
    if claims.get("iss") not in {"accounts.google.com", "https://accounts.google.com"}:
        raise ValueError("Invalid Google token issuer")
    if claims.get("email_verified") is not True:
        raise ValueError("A verified Google email is required")
    if not isinstance(claims.get("sub"), str) or not claims["sub"]:
        raise ValueError("Google token is missing a subject")
    return dict(claims)


async def verify_google_id_token(token: str, audience: str) -> dict[str, Any]:
    return await run_in_threadpool(_verify_sync, token, audience)
