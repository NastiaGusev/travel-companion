# AI Travel Companion — Backend Service
A REST API for managing trips, day-by-day itineraries, and stops grounded in real places via Google Places, with JWT authentication and PostgreSQL persistence. Every resource is ownership-scoped and shareable with other users, concurrent edits are guarded with optimistic locking, the schema is migration-managed, and the whole stack runs locally with a single command and is deployed to AWS with automated CI/CD.
## Tech stack
- **Language:** Kotlin
- **Framework:** Spring Boot 4.1
- **Database:** PostgreSQL with Flyway migrations
- **Persistence:** JPA / Hibernate
- **Auth:** Spring Security + JWT
- **Docs:** OpenAPI / Swagger UI (springdoc)
- **External APIs:** Google Places (New), with Resilience4j (retry + circuit breaker) and Caffeine caching
- **Testing:** JUnit, Testcontainers (integration), unit tests
- **Tooling:** Docker, Docker Compose
- **Infrastructure:** AWS (ECS Fargate, ECR, RDS, SSM Parameter Store, IAM)
- **CI/CD:** GitHub Actions (test, build, push to ECR, deploy to Fargate)
## Architecture
Layered architecture (controller → service → repository) with a clear separation of concerns. Resources are scoped to their owner, so a user can only reach trips they own or have been given access to. The domain models trips, itinerary days (auto-numbered, with dates derived from the trip's start date), and stops (auto-ordered by time within a day).

## Collaboration & concurrency
Trips can be shared with other registered users by email. Each trip has one **owner** plus **editors** (stored in a `trip_collaborators` table); access is centralized in `TripAccessService` — unauthorized users get `404`, and owner-only actions like managing collaborators return `403` to editors. Roles are stored as strings so a `VIEWER` role can be added without a migration.

Concurrent edits are handled explicitly: form-edited resources (`Trip`, `ItineraryDay`) use **optimistic locking** via a JPA `@Version` column — a stale version returns `409 Conflict`. Ordered resources (`Stop`) are resequenced on every change, so they rely on the transaction and a `unique(day_id, position)` constraint instead of row versioning.

Collaborator endpoints: `POST` / `GET` / `DELETE` on `/api/trips/{tripId}/collaborators`.

## Place search & geocoding
Trips and stops are grounded in real places via the **Google Places API (New)**, proxied
through the backend so the API key stays server-side. Autocomplete search
(`GET /api/places/search?query=`, biased toward the trip's destination when set) powers
picking destinations and adding stops; when a stop or destination is created with a place id,
the server resolves its coordinates and address and snapshots them onto the record.

Outbound calls are wrapped with **Resilience4j** — retry on transient failures plus a circuit
breaker that fast-fails when Google is down (a missing place returns `404 PLACE_NOT_FOUND`;
an outage returns `503 PLACES_UNAVAILABLE`). Place details are cached with **Caffeine**
(keyed by place id, 24h TTL), so repeating the same lookup skips the network — and the
resilience layer — entirely. Google is never called from tests: the client is stubbed with
**WireMock** at the HTTP layer.

## Error handling
All errors return [RFC 7807 Problem Details](https://datatracker.ietf.org/doc/html/rfc7807) (`application/problem+json`) with a consistent shape — `title`, `status`, `detail`, plus a machine-readable `code` clients can switch on. For example, a stale-version edit returns `409` with `code: VERSION_CONFLICT`, so a client can prompt the user to reload rather than parsing error text.

## Deployment
Deployed to AWS, containerized and running without managed servers:
- ECS Fargate — runs the container
- Amazon ECR — image registry
- Amazon RDS (PostgreSQL) — managed database, private (not publicly accessible)
- SSM Parameter Store — encrypted secrets (DB password, JWT key), injected at runtime
- IAM — least-privilege execution role, scoped to the required secrets only
  Configuration is entirely environment-driven, so the same image runs locally (Docker Compose) and in the cloud unchanged.
## Running locally
Run the entire stack — app and database — locally with Docker Compose:
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