"""
Unit tests for GeminiExtractor — all against a mocked Gemini client, never a
real API call. Pins the same behavior contract the stub tests pin for the
route: valid output parses, malformed/invalid output raises ExtractorError,
provider/transport failures raise ExtractorError, "nothing found" is a valid
empty result rather than an error.
"""

from __future__ import annotations

import asyncio
import json
from types import SimpleNamespace
from unittest.mock import AsyncMock

import pytest
from google.genai import errors as genai_errors

from app.extractors.base import ExtractorError
from app.extractors.gemini_extractor import GeminiExtractor


def make_extractor() -> GeminiExtractor:
    # Client construction makes no network call, so a fake key is safe here.
    return GeminiExtractor(
        api_key="test-key",
        model="gemini-3.5-flash-lite",
        timeout_seconds=15.0,
        max_output_tokens=1024,
    )


def mock_response(text: str | None) -> SimpleNamespace:
    return SimpleNamespace(text=text)


def run(coro):
    return asyncio.run(coro)


def test_valid_response_parses_into_contract_shape():
    extractor = make_extractor()
    extractor._client.aio.models.generate_content = AsyncMock(
        return_value=mock_response(
            json.dumps(
                {
                    "days": [
                        {
                            "position": 1,
                            "stops": [
                                {"search_query": "Tsukiji Market", "name": "Tsukiji Market", "time_hint": "12:30"},
                                {"search_query": "TeamLab", "name": "TeamLab", "time_hint": None},
                            ],
                        }
                    ]
                }
            )
        )
    )

    result = run(extractor.extract("day one: lunch at Tsukiji 12:30, then TeamLab"))

    assert result.days[0].position == 1
    assert result.days[0].stops[0].search_query == "Tsukiji Market"
    assert result.days[0].stops[0].time_hint == "12:30"
    assert result.days[0].stops[1].time_hint is None


def test_nothing_extractable_returns_empty_days_not_an_error():
    extractor = make_extractor()
    extractor._client.aio.models.generate_content = AsyncMock(return_value=mock_response(json.dumps({"days": []})))

    result = run(extractor.extract("what a lovely day"))

    assert result.days == []


def test_malformed_json_raises_extractor_error():
    extractor = make_extractor()
    extractor._client.aio.models.generate_content = AsyncMock(return_value=mock_response("not valid json"))

    with pytest.raises(ExtractorError):
        run(extractor.extract("some trip text"))


def test_schema_mismatch_raises_extractor_error():
    extractor = make_extractor()
    # "position" must be an int per the contract — this should fail Pydantic validation.
    extractor._client.aio.models.generate_content = AsyncMock(
        return_value=mock_response(json.dumps({"days": [{"position": "one", "stops": []}]}))
    )

    with pytest.raises(ExtractorError):
        run(extractor.extract("some trip text"))


def test_blocked_or_empty_response_raises_extractor_error():
    extractor = make_extractor()
    extractor._client.aio.models.generate_content = AsyncMock(return_value=mock_response(None))

    with pytest.raises(ExtractorError):
        run(extractor.extract("some trip text"))


def test_api_error_raises_extractor_error():
    extractor = make_extractor()
    extractor._client.aio.models.generate_content = AsyncMock(
        side_effect=genai_errors.ServerError(503, {"error": {"message": "unavailable"}})
    )

    with pytest.raises(ExtractorError):
        run(extractor.extract("some trip text"))


def test_timeout_raises_extractor_error():
    extractor = make_extractor()
    extractor._client.aio.models.generate_content = AsyncMock(side_effect=TimeoutError("timed out"))

    with pytest.raises(ExtractorError):
        run(extractor.extract("some trip text"))
