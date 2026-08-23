"""
FastAPI entrypoint (bootstrap only).

Creates the app and mounts the routes. Kept thin on purpose — endpoints live
in app/api/routes.py, config in app/config.py, business logic in app/extractors/.
"""

from __future__ import annotations

from fastapi import FastAPI

from app.api.routes import router

app = FastAPI(
    title="Travel Companion — AI Extraction Service",
    version="0.1.0",
    description="Turns free trip text into structured days/stops for the quick-add feature.",
)

app.include_router(router)