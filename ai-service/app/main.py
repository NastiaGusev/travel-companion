"""
FastAPI entrypoint (bootstrap only).

Creates the app, mounts the routes, and registers the one cross-cutting
concern that lives at this layer: mapping ExtractorError to a 503. Config
and business logic stay in their own modules — see app/config.py and
app/extractors/.
"""

from __future__ import annotations

import logging

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from app.api.routes import router
from app.extractors.base import ExtractorError

logger = logging.getLogger(__name__)

app = FastAPI(
    title="Travel Companion — AI Extraction Service",
    version="0.1.0",
    description="Turns free trip text into structured days/stops for the quick-add feature.",
)

app.include_router(router)


@app.exception_handler(ExtractorError)
async def extractor_error_handler(request: Request, exc: ExtractorError) -> JSONResponse:
    """
    Any genuine extractor failure (provider outage, timeout, bad output) becomes a 503.
    Kotlin's QuickAddService already treats any 5xx from /extract as
    AiServiceUnavailableException, so the response body only needs to help a human
    reading Swagger/logs — the full exception detail goes to the server log instead
    of the client, so a provider error message can't leak internal detail.
    """
    logger.error("Extraction failed: %s", exc)
    return JSONResponse(
        status_code=503,
        content={"detail": "AI extraction service is temporarily unavailable"},
    )
