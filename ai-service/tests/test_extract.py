"""
Contract + behavior tests for the extraction service.

These force the stub extractor via a dependency override, so they pin the
HTTP contract shape and input validation regardless of what EXTRACTOR_PROVIDER
happens to be set to locally — this file's own claim of "never touches a real
LLM" used to depend on the ambient .env, which is exactly what broke earlier
when .env was left on "gemini". Now it's actually guaranteed by the test.
"""

import pytest
from fastapi.testclient import TestClient

from app.extractors.factory import get_extractor
from app.extractors.stub import StubExtractor
from app.main import app

client = TestClient(app)


@pytest.fixture(autouse=True)
def force_stub_extractor():
    app.dependency_overrides[get_extractor] = lambda: StubExtractor()
    yield
    app.dependency_overrides.clear()


def test_health():
    r = client.get("/health")
    assert r.status_code == 200
    assert r.json() == {"status": "UP"}


def test_extract_returns_contract_shape():
    r = client.post("/extract", json={"text": "some trip text"})
    assert r.status_code == 200
    body = r.json()

    assert "days" in body
    first_day = body["days"][0]
    assert first_day["position"] == 1

    first_stop = first_day["stops"][0]
    # camelCase aliases on the wire
    assert first_stop["searchQuery"] == "Tsukiji Market"
    assert first_stop["name"] == "Tsukiji Market"
    assert first_stop["timeHint"] == "12:30"


def test_timehint_omitted_when_absent():
    r = client.post("/extract", json={"text": "x"})
    body = r.json()
    stop_without_time = body["days"][1]["stops"][0]
    assert "timeHint" not in stop_without_time  # absent, not null


def test_empty_text_rejected():
    r = client.post("/extract", json={"text": ""})
    assert r.status_code == 422


def test_missing_text_rejected():
    r = client.post("/extract", json={})
    assert r.status_code == 422
