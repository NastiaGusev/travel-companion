"""
Request/response models for the /extract contract.

Single source of truth for the JSON shape shared with the Kotlin backend.
Keep in lockstep with the Kotlin DTOs — the WireMock stubs on the Kotlin
side assert against exactly this shape.

Contract:
  POST /extract
  Request:  { "text": "..." }
  Response: { "days": [ { "position": 1, "stops": [ {searchQuery, name, timeHint?} ] } ] }

Rules baked into the models:
  - `days` are returned in the order the user described them; `position` is
    1-based and reflects that order. The model never touches calendar dates.
  - No day structure in the text -> all stops in one day at position 1.
  - Nothing extractable -> { "days": [] }.
  - `timeHint` is optional: a clock string ("12:30"), a general slot
    ("morning"/"afternoon"/"evening"), or omitted entirely.
"""

from __future__ import annotations

from enum import Enum
from typing import Optional

from pydantic import BaseModel, Field


class ExtractRequest(BaseModel):
    text: str = Field(
        ...,
        min_length=1,
        description="Free text describing the trip, as typed by the user.",
        examples=["day one: lunch at Tsukiji 12:30, then TeamLab. day two: dinner at Gonpachi"],
    )


class TimeSlot(str, Enum):
    """General time-of-day slots the model may emit when no clock time is given."""
    MORNING = "morning"
    AFTERNOON = "afternoon"
    EVENING = "evening"


class ExtractedStop(BaseModel):
    search_query: str = Field(
        ...,
        alias="searchQuery",
        description="Search string Kotlin feeds to Google Places to resolve this stop.",
        examples=["Tsukiji Market"],
    )
    name: str = Field(
        ...,
        description="Human-readable label for display / fallback if resolution fails.",
        examples=["Tsukiji Market"],
    )
    time_hint: Optional[str] = Field(
        None,
        alias="timeHint",
        description=(
            "Optional. A clock time like '12:30', or one of "
            "morning/afternoon/evening, or omitted when the text implies no time."
        ),
        examples=["12:30", "afternoon"],
    )

    model_config = {"populate_by_name": True}


class ExtractedDay(BaseModel):
    position: int = Field(
        ...,
        ge=1,
        description="1-based day order as described by the user (not a calendar date).",
    )
    stops: list[ExtractedStop] = Field(default_factory=list)


class ExtractResponse(BaseModel):
    days: list[ExtractedDay] = Field(default_factory=list)