"""
HTTP routes (the "controller" layer).

Endpoints are grouped on an APIRouter and included by main.py. The route
depends only on the Extractor interface (via the factory), so the LLM
provider is swappable without touching this file.
"""

from __future__ import annotations

from fastapi import APIRouter, Depends

from app.extractors.base import Extractor
from app.extractors.factory import get_extractor
from app.models import ExtractRequest, ExtractResponse

router = APIRouter()


@router.get("/health", tags=["ops"])
async def health() -> dict[str, str]:
    return {"status": "UP"}


@router.post(
    "/extract",
    response_model=ExtractResponse,
    response_model_by_alias=True,     # emit camelCase (searchQuery, timeHint)
    response_model_exclude_none=True, # omit timeHint entirely when absent
    tags=["extraction"],
)
async def extract(
        request: ExtractRequest,
        extractor: Extractor = Depends(get_extractor),
) -> ExtractResponse:
    """
    Extract structured stops from free text.

    Returns 200 with { "days": [] } when nothing is found (not an error).
    Genuine provider failures surface as 5xx (handled once a real provider lands).
    """
    return await extractor.extract(request.text)