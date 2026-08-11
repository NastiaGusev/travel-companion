# AI Travel Companion

A backend service for planning and organizing trips — built as a production-quality showcase of backend engineering in Kotlin and Spring Boot.

> 🚧 **In active development.** The core API and foundation are complete; cloud deployment and later features are on the roadmap below.

## Overview

A REST API for managing trips, day-by-day itineraries, and stops, with JWT authentication and PostgreSQL persistence. Every resource is ownership-scoped, the schema is migration-managed, and the whole stack runs locally with a single command.

## Tech stack

- **Language:** Kotlin
- **Framework:** Spring Boot 4.1
- **Database:** PostgreSQL with Flyway migrations
- **Persistence:** JPA / Hibernate
- **Auth:** Spring Security + JWT
- **Docs:** OpenAPI / Swagger UI (springdoc)
- **Testing:** JUnit, Testcontainers (integration), unit tests
- **Tooling:** Docker, Docker Compose

## Architecture

Layered architecture (controller → service → repository) with a clear separation of concerns. Resources are scoped to their owner, so a user can only ever reach their own data — cross-user access is indistinguishable from not-found. The domain models trips, itinerary days (auto-numbered, with dates derived from the trip's start date), and stops (auto-ordered by time within a day).

## Getting started

The whole stack — the app and its database — runs in containers.

```bash
# 1. Clone
git clone https://github.com/NastiaGusev/travel-companion.git
cd travel-companion

# 2. Create your local environment file from the template
cp .env.example .env

# 3. Build and run (app + PostgreSQL)
docker compose up --build
```

Once it's up:

- **API:** http://localhost:8080
- **Interactive API docs (Swagger UI):** http://localhost:8080/swagger-ui.html
- **Health check:** http://localhost:8080/actuator/health

To try a protected endpoint from Swagger, register or log in via the `auth` endpoints, copy the returned token, click **Authorize**, and paste it.

## Testing

The project is covered by integration tests (spinning up a real PostgreSQL via Testcontainers to exercise the full HTTP → service → database path) and unit tests for isolated business logic. Run them with:

```bash
./gradlew test
```

## Roadmap

- ✅ Core API — auth, trips, itineraries, stops
- ✅ PostgreSQL persistence with Flyway migrations
- ✅ Containerized (Docker + Docker Compose)
- ✅ OpenAPI / Swagger documentation
- 🔲 Deployment to AWS with CI/CD
- 🔲 Maps / places integration with caching
- 🔲 Trip collaboration and sharing
- 🔲 AI-assisted planning and semantic search (separate Python service)
