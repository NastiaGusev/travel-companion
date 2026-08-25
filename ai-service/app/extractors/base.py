"""
The extractor seam.

Everything depends on this Protocol, never on a concrete LLM SDK. Swapping
stub <-> Anthropic <-> OpenAI is a one-line change in factory.py — the route,
the models, and the tests never change. ("Abstract behind an interface.")
"""

from __future__ import annotations

from typing import Protocol, runtime_checkable

from app.models import ExtractResponse


@runtime_checkable
class Extractor(Protocol):
    """Turns free trip text into the structured contract shape."""

    async def extract(self, text: str) -> ExtractResponse:
        """
        Parse `text` into days/stops per the /extract contract.

        Implementations must NOT raise on 'nothing found' — return an empty
        ExtractResponse (days=[]) instead. They should raise only on genuine
        provider/transport failures, which the route maps to a 5xx.
        """
        ...


class ExtractorError(Exception):
    """
    Raised by an Extractor implementation on a genuine provider/transport
    failure (auth, timeout, rate limit, malformed/unparseable output) — never
    for "nothing found in the text", which is a valid empty result instead.

    routes.py maps this to a 503, so from the Kotlin side an ai-service
    failure looks the same regardless of cause: QuickAddService already
    treats any 5xx from /extract as AiServiceUnavailableException.
    """
