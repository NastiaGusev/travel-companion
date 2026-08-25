"""
Extractor factory.

The ONE place that decides which concrete extractor the app uses. The route
asks for `get_extractor()` and never knows which provider it got. Adding a
new provider is a branch here — nothing else changes.

Selection is driven by EXTRACTOR_PROVIDER (default: "stub").
"""

from __future__ import annotations

from functools import lru_cache

from app.config import settings
from app.extractors.base import Extractor
from app.extractors.gemini_extractor import GeminiExtractor
from app.extractors.stub import StubExtractor


@lru_cache(maxsize=1)
def get_extractor() -> Extractor:
    provider = settings.extractor_provider.lower()

    if provider == "stub":
        return StubExtractor()

    if provider == "gemini":
        if not settings.llm_api_key:
            raise ValueError("EXTRACTOR_PROVIDER=gemini requires LLM_API_KEY to be set")
        return GeminiExtractor(
            api_key=settings.llm_api_key,
            model=settings.llm_model,
            timeout_seconds=settings.llm_timeout_seconds,
            max_output_tokens=settings.llm_max_output_tokens,
        )

    raise ValueError(f"Unknown EXTRACTOR_PROVIDER: {settings.extractor_provider!r}")
