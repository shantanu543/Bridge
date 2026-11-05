from contextlib import asynccontextmanager
from typing import AsyncIterator
from uuid import uuid4

from fastapi import FastAPI, HTTPException, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException
from starlette.middleware.base import RequestResponseEndpoint
from starlette.responses import Response

from app.api.auth import router as auth_router
from app.api.community import router as community_router
from app.api.onboarding import router as onboarding_router
from app.api.passport import router as passport_router
from app.api.social_spaces import router as social_spaces_router
from app.core.config import get_settings
from app.db import create_engine, create_session_factory, database_is_ready

settings = get_settings()


@asynccontextmanager
async def lifespan(application: FastAPI) -> AsyncIterator[None]:
    engine = create_engine()
    application.state.database_engine = engine
    application.state.session_factory = create_session_factory(engine)
    try:
        yield
    finally:
        await engine.dispose()


app = FastAPI(
    title=settings.app_name,
    version="0.1.0",
    docs_url="/docs" if settings.openapi_enabled else None,
    redoc_url=None,
    openapi_url="/openapi.json" if settings.openapi_enabled else None,
    lifespan=lifespan,
)
app.include_router(auth_router, prefix=settings.api_prefix)
app.include_router(onboarding_router, prefix=settings.api_prefix)
app.include_router(passport_router, prefix=settings.api_prefix)
app.include_router(community_router, prefix=settings.api_prefix)
app.include_router(social_spaces_router, prefix=settings.api_prefix)


@app.middleware("http")
async def add_request_id(
    request: Request,
    call_next: RequestResponseEndpoint,
) -> Response:
    request.state.request_id = uuid4().hex
    response = await call_next(request)
    response.headers["X-Request-ID"] = request.state.request_id
    return response


@app.exception_handler(StarletteHTTPException)
async def http_error_response(request: Request, error: StarletteHTTPException) -> JSONResponse:
    detail = error.detail if isinstance(error.detail, dict) else {}
    return JSONResponse(
        status_code=error.status_code,
        headers=error.headers,
        content={
            "error": {
                "code": detail.get("code", "request_error"),
                "message": detail.get("message", "The request could not be completed."),
                "request_id": getattr(request.state, "request_id", ""),
                "details": detail.get("details", {}),
            }
        },
    )


@app.exception_handler(RequestValidationError)
async def validation_error_response(
    request: Request,
    error: RequestValidationError,
) -> JSONResponse:
    fields = [
        {"field": ".".join(str(part) for part in item.get("loc", ())), "message": item.get("msg", "Invalid value.")}
        for item in error.errors()
    ]
    return JSONResponse(
        status_code=422,
        content={
            "error": {
                "code": "invalid_request",
                "message": "One or more fields are invalid.",
                "request_id": getattr(request.state, "request_id", ""),
                "details": {"fields": fields},
            }
        },
    )


@app.get("/health/live", tags=["health"])
async def live() -> dict[str, str]:
    return {"status": "live"}


@app.get("/health/ready", tags=["health"])
async def ready(request: Request) -> dict[str, str]:
    engine = request.app.state.database_engine
    try:
        await database_is_ready(engine)
    except Exception as error:
        raise HTTPException(status_code=503, detail="Database is unavailable") from error
    return {"status": "ready"}


@app.get(f"{settings.api_prefix}/meta", tags=["meta"])
async def metadata() -> dict[str, str]:
    return {"api_version": "v1", "environment": settings.environment}
