"""
Stub extractor: no LLM, deterministic output.

Lets the whole service run end-to-end (and lets the Kotlin side build its
client + WireMock tests) before any prompt work exists. Returns a fixed,
contract-valid response so the shape is real even though the parsing isn't.
Swap for a real provider later via factory.py — nothing else changes.
"""

from __future__ import annotations

from app.extractors.base import Extractor
from app.models import ExtractedDay, ExtractedStop, ExtractResponse


class StubExtractor(Extractor):
    async def extract(self, text: str) -> ExtractResponse:
        # Deterministic placeholder exercising every field of the contract:
        # two days, a clock time, a general slot, and a stop with no timeHint.
        return ExtractResponse(
            days=[
                ExtractedDay(
                    position=1,
                    stops=[
                        ExtractedStop(searchQuery="Tsukiji Market", name="Tsukiji Market", timeHint="12:30"),
                        ExtractedStop(searchQuery="TeamLab", name="TeamLab", timeHint="afternoon"),
                    ],
                ),
                ExtractedDay(
                    position=2,
                    stops=[
                        ExtractedStop(searchQuery="Gonpachi", name="Gonpachi"),
                    ],
                ),
            ]
        )