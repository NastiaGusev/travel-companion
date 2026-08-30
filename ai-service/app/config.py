"""
Configuration, entirely env-driven — same philosophy as the Kotlin service
(secrets and provider choice come from the environment, never hardcoded).

`extractor_provider` picks the implementation ("stub" or "gemini"). The LLM
fields are unset until GEMINI_API_KEY is provided; timeout/max-tokens are
guardrails so a slow/runaway call can't stall past Kotlin's own retry budget.
"""

from __future__ import annotations

from typing import Optional

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    # Which extractor implementation to use: "stub" for now.
    extractor_provider: str = "stub"

    # Reserved for a real provider (unused by the stub). Never hardcode a real
    # value — supply via env / secrets manager, mirroring the Kotlin approach.
    llm_api_key: Optional[str] = None
    llm_model: str = "gemini-3.5-flash-lite"

    # Guardrails on the LLM call so a slow/hung request can't outlast Kotlin's
    # own Resilience4j retry+circuit-breaker budget on /extract.
    llm_timeout_seconds: float = 10.0
    llm_max_output_tokens: int = 1024

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8")


settings = Settings()