# AI Travel Companion — Backend Service

REST API for trip planning: itineraries, real-place search, sharing, and AI-assisted quick-add — built to demonstrate production backend patterns (auth, concurrency, resilience, CI/CD), not just CRUD.

## Tech stack
- **Kotlin** + **Spring Boot 4.1**, layered architecture (controller → service → repository)
- **PostgreSQL** + **Flyway** migrations, **JPA/Hibernate**
- **Spring Security + JWT**, ownership-scoped resources (cross-user access returns `404`, never `403`)
- **Google Places API (New)** for geocoding/search, behind **Resilience4j** (retry + circuit breaker) and **Caffeine** caching
- **AI quick-add service** — Python/FastAPI microservice using **Gemini** (JSON-schema-constrained structured output) to turn free text into itinerary stops
- **OpenAPI/Swagger** docs
- **JUnit + Testcontainers** (integration), **WireMock** (external APIs stubbed — nothing external is ever called in CI)
- **Docker Compose** — app + ai-service + Postgres, one command to run the full stack
- **AWS** (ECS Fargate, ECR, RDS, SSM Parameter Store, IAM) deployed via **GitHub Actions CI/CD**

## Design highlights
- **Concurrency:** optimistic locking (`@Version`) on form-edited resources (trips, days); transaction + unique constraint on system-reordered ones (stops) — the right tool per mutation pattern, not one blanket strategy.
- **Resilience:** every external call (Places, AI service) wrapped in retry + circuit breaker with distinguishable failure codes (`503 PLACES_UNAVAILABLE` vs `503 AI_SERVICE_UNAVAILABLE`); a failed lookup on one stop degrades that stop to title-only instead of failing the whole request.
- **Errors:** [RFC 7807](https://datatracker.ietf.org/doc/html/rfc7807) Problem Details everywhere — consistent shape, machine-readable `code`.
- **Sharing:** owner + editor roles on trips, access checks centralized in one place.
- **AI done carefully:** the extractor sits behind a swappable interface, output is schema-constrained (not free-form parsing), and it's never called from tests.

## Running locally
```bash
git clone https://github.com/NastiaGusev/travel-companion.git
cd travel-companion
cp .env.example .env
docker compose up --build
```
Starts the app, AI service, and Postgres together.
- API: http://localhost:8080 — Swagger: http://localhost:8080/swagger-ui.html
- Register/login via `/api/auth`, then **Authorize** in Swagger with the returned token.

## Testing
```bash
./gradlew test
```
Integration tests spin up a real Postgres via Testcontainers; Google Places and the AI service are stubbed with WireMock — no external calls in CI.

## Deployed on AWS
ECS Fargate + private RDS + ECR + SSM-managed secrets + least-privilege IAM. Configuration is entirely environment-driven, so the same image runs locally and in prod unchanged. Auto-deployed via GitHub Actions on every push to `main`.
