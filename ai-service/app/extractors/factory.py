"""
Extractor factory.

The ONE place that decides which concrete extractor the app uses. The route
asks for `get_extractor()` and never knows which provider it got. When a real
provider lands, add a branch here — do not touch the route.

Selection is driven by EXTRACTOR_PROVIDER (default: "stub").
"""

from __future__ import annotations

from functools import lru_cache

from app.config import settings
from app.extractors.base import Extractor
from app.extractors.stub import StubExtractor


@lru_cache(maxsize=1)
def get_extractor() -> Extractor:
    provider = settings.extractor_provider.lower()

    if provider == "stub":
        return StubExtractor()

    # Future providers slot in here, e.g.:
    # if provider == "anthropic":
    #     from app.extractors.anthropic_extractor import AnthropicExtractor
    #     return AnthropicExtractor(api_key=settings.llm_api_key, model=settings.llm_model)
    # if provider == "openai":
    #     from app.extractors.openai_extractor import OpenAIExtractor
    #     return OpenAIExtractor(api_key=settings.llm_api_key, model=settings.llm_model)

    raise ValueError(f"Unknown EXTRACTOR_PROVIDER: {settings.extractor_provider!r}")