"""
Tests for the ExtractorError -> 503 mapping registered in app/main.py.

Uses a fake extractor via FastAPI's dependency override rather than mocking
Gemini directly — this only cares that *any* ExtractorError, from any
provider, becomes a 503 with a body that doesn't leak the raw exception text.
"""

from __future__ import annotations

from fastapi.testclient import TestClient

from app.extractors.base import ExtractorError
from app.extractors.factory import get_extractor
from app.main import app
from app.models import ExtractResponse


class FailingExtractor:
    async def extract(self, text: str) -> ExtractResponse:
        raise ExtractorError("simulated provider outage with sensitive-looking detail")


def test_extractor_error_becomes_503_with_generic_body():
    app.dependency_overrides[get_extractor] = lambda: FailingExtractor()
    try:
        client = TestClient(app)
        response = client.post("/extract", json={"text": "some trip text"})

        assert response.status_code == 503
        body = response.json()
        assert "sensitive-looking detail" not in body["detail"]
    finally:
        app.dependency_overrides.clear()
