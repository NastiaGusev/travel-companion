"""
Configuration, entirely env-driven — same philosophy as the Kotlin service
(secrets and provider choice come from the environment, never hardcoded).

For the skeleton only `extractor_provider` matters (defaults to "stub").
The LLM fields are here so the seam is ready; they stay unset until a real
provider is wired.
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
    llm_model: Optional[str] = None

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8")


settings = Settings()