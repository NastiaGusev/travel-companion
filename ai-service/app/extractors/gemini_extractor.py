"""
Gemini-backed extractor.

Turns free trip text into the /extract contract shape using Gemini's native
structured-output mode (response_schema + response_mime_type=application/json),
so the model is constrained to emit schema-shaped JSON directly rather than
us regex-parsing free-form prose out of a chat response.

Raises ExtractorError on any genuine provider/transport failure (auth,
timeout, rate limit, blocked/empty response, output that doesn't validate
against the contract) — never for "nothing found in the text", which the
model itself represents as a valid empty ExtractResponse per the prompt.
"""

from __future__ import annotations

import json

from google import genai
from google.genai import errors as genai_errors
from google.genai import types
from pydantic import ValidationError

from app.extractors.base import Extractor, ExtractorError
from app.models import ExtractResponse

_SYSTEM_INSTRUCTION = """You extract structured trip stops from free text a user typed describing a trip.

Rules:
- Preserve the order the user described days and stops in. `position` is 1-based and reflects that order, not a calendar date.
- If the text has no day structure (no "day one/two", dates, etc.), put every stop in a single day at position 1.
- If nothing extractable is in the text, return {"days": []}.
- For each stop, `search_query` is a short string suitable for a Google Places search (e.g. a landmark or restaurant name, optionally with a city). `name` is a human-readable label for display — usually the same as search_query.
- `time_hint` is optional: a clock time like "12:30", one of "morning"/"afternoon"/"evening", or omitted entirely when the text implies no time.
- Never invent stops, locations, or times that are not implied by the text.
"""

_STOP_SCHEMA = types.Schema(
    type=types.Type.OBJECT,
    properties={
        "search_query": types.Schema(type=types.Type.STRING),
        "name": types.Schema(type=types.Type.STRING),
        "time_hint": types.Schema(type=types.Type.STRING, nullable=True),
    },
    required=["search_query", "name"],
)

_DAY_SCHEMA = types.Schema(
    type=types.Type.OBJECT,
    properties={
        "position": types.Schema(type=types.Type.INTEGER),
        "stops": types.Schema(type=types.Type.ARRAY, items=_STOP_SCHEMA),
    },
    required=["position", "stops"],
)

_RESPONSE_SCHEMA = types.Schema(
    type=types.Type.OBJECT,
    properties={"days": types.Schema(type=types.Type.ARRAY, items=_DAY_SCHEMA)},
    required=["days"],
)


class GeminiExtractor(Extractor):
    def __init__(self, api_key: str, model: str, timeout_seconds: float, max_output_tokens: int) -> None:
        self._model = model
        self._client = genai.Client(
            api_key=api_key,
            http_options=types.HttpOptions(
                timeout=int(timeout_seconds * 1000),
                # The SDK retries 5x by default on 408/429/5xx — Kotlin's Resilience4j
                # already retries the whole /extract call 3x with backoff, so a second
                # retry layer here would silently multiply worst-case latency and cost.
                retry_options=types.HttpRetryOptions(attempts=1),
            ),
        )
        self._config = types.GenerateContentConfig(
            system_instruction=_SYSTEM_INSTRUCTION,
            response_mime_type="application/json",
            response_schema=_RESPONSE_SCHEMA,
            max_output_tokens=max_output_tokens,
            temperature=0.0,
        )

    async def extract(self, text: str) -> ExtractResponse:
        try:
            response = await self._client.aio.models.generate_content(
                model=self._model,
                contents=text,
                config=self._config,
            )
        except genai_errors.APIError as e:
            raise ExtractorError(f"Gemini API call failed: {e}") from e
        except Exception as e:
            # Transport-level failures (timeout, connection reset) surface as plain
            # exceptions from the underlying HTTP client, not genai_errors.APIError.
            raise ExtractorError(f"Gemini call failed: {e}") from e

        raw = response.text
        if raw is None:
            raise ExtractorError("Gemini returned no text content (possibly blocked by safety filters)")

        try:
            return ExtractResponse.model_validate(json.loads(raw))
        except (json.JSONDecodeError, ValidationError) as e:
            raise ExtractorError(f"Gemini response didn't match the contract shape: {e}") from e
