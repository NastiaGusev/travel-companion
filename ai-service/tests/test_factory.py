"""
Tests for the extractor factory's provider selection.

Patches app.extractors.factory.settings directly (not env vars) so each case
is explicit and isolated; clears the @lru_cache before/after each test so one
test's cached instance can't leak into the next. No network calls — Gemini
client construction alone never talks to the network.
"""

from __future__ import annotations

import pytest

from app.extractors.factory import get_extractor
from app.extractors.gemini_extractor import GeminiExtractor
from app.extractors.stub import StubExtractor


@pytest.fixture(autouse=True)
def clear_cache():
    get_extractor.cache_clear()
    yield
    get_extractor.cache_clear()


def test_stub_provider_returns_stub_extractor(monkeypatch):
    monkeypatch.setattr("app.extractors.factory.settings.extractor_provider", "stub")

    assert isinstance(get_extractor(), StubExtractor)


def test_gemini_provider_returns_gemini_extractor(monkeypatch):
    monkeypatch.setattr("app.extractors.factory.settings.extractor_provider", "gemini")
    monkeypatch.setattr("app.extractors.factory.settings.llm_api_key", "test-key")
    monkeypatch.setattr("app.extractors.factory.settings.llm_model", "gemini-3.5-flash-lite")
    monkeypatch.setattr("app.extractors.factory.settings.llm_timeout_seconds", 15.0)
    monkeypatch.setattr("app.extractors.factory.settings.llm_max_output_tokens", 1024)

    assert isinstance(get_extractor(), GeminiExtractor)


def test_gemini_provider_without_api_key_raises(monkeypatch):
    monkeypatch.setattr("app.extractors.factory.settings.extractor_provider", "gemini")
    monkeypatch.setattr("app.extractors.factory.settings.llm_api_key", None)

    with pytest.raises(ValueError, match="LLM_API_KEY"):
        get_extractor()


def test_unknown_provider_raises(monkeypatch):
    monkeypatch.setattr("app.extractors.factory.settings.extractor_provider", "openai")

    with pytest.raises(ValueError, match="Unknown EXTRACTOR_PROVIDER"):
        get_extractor()
