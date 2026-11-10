# HemoGrid Backend - Codex Agent Instructions

## 1. Read this first

You are working on the backend for **HemoGrid**, a 72-hour HealthTech hackathon MVP.

Before implementing or modifying application code, read:

- `docs/HEMOGRID_BACKEND_BUILD_SPEC.md`

Treat that file as the product and engineering source of truth. If the repository already contains code that conflicts with the spec, preserve working code where reasonable but flag the conflict before changing architecture or product behavior.

## 2. Mission

Build a reliable **Spring Boot modular monolith** that lets hospitals discover nearby blood banks with sufficient screened blood inventory, create emergency blood requests, let blood banks accept and progress those requests, and push status changes back to the requesting hospital in near real time.

The hackathon priority order is:

1. Working end-to-end flow.
2. Correct inventory/reservation behavior.
3. Stable API contract for the frontend engineer.
4. Clear Swagger/OpenAPI documentation.
5. Realtime request updates.
6. Demo polish.
7. Extra features only after P0 is complete.

## 3. Fixed technology decisions

Do not change these unless the user explicitly asks:

- Java 21
- Spring Boot 4.1.x
- Maven
- Spring MVC / REST
- Spring Data JPA
- PostgreSQL
- Flyway for all schema changes
- Spring Security
- JWT bearer authentication using Spring Security's JWT support
- Bean Validation
- WebSocket support for realtime events, with polling fallback allowed
- Spring Boot Actuator
- springdoc-openapi / Swagger UI
- Docker Compose for local infrastructure
- `application.yaml` for application configuration, with secrets supplied by environment variables
- JUnit / Spring Boot tests
- Testcontainers PostgreSQL for important integration tests

## 4. Architecture rules

- This is a **modular monolith**, not microservices.
- Package by feature/domain, not by global technical layer.
- Controllers must not contain business logic.
- Services own business rules and transaction boundaries.
- Repositories only handle persistence/query responsibilities.
- API DTOs are separate from JPA entities.
- Never expose JPA entities directly from controllers.
- Flyway owns the database schema. Hibernate must use `ddl-auto: validate`.
- Use UUID primary keys.
- Store enums as strings.
- Use UTC timestamps (`Instant`) in the backend.
- Use BigDecimal for coordinates only if needed for schema precision; doubles are acceptable in Java for Haversine calculations.
- Validate all incoming request bodies.
- Use a single global exception handler and one consistent error response shape.
- Never hardcode secrets or production credentials.

## 5. MVP boundaries

P0 actors:

- Hospital user
- Blood bank user
- Platform admin only where needed for seeded/demo data

P0 capabilities:

- Login and current-user endpoint
- Organization-aware authorization
- Blood bank inventory CRUD/update
- Hospital emergency blood request creation
- Candidate blood-bank matching
- Request acceptance/decline
- Inventory reservation on acceptance
- Request lifecycle updates: REQUESTED -> ACCEPTED -> PREPARING -> IN_TRANSIT -> DELIVERED
- Cancellation where valid
- Inventory reservation release on cancellation
- Hospital and blood-bank request history
- Swagger documentation
- Health endpoint
- Demo seed data

Do not add donor matching, payments, ambulance dispatch, Kafka, Redis, RabbitMQ, Kubernetes, Elasticsearch, full clinical records, or AI chat features in P0.

## 6. Coding behavior expected from Codex

For each implementation task:

1. Inspect existing files first.
2. State the smallest plan that satisfies the task.
3. Implement only the requested scope plus necessary supporting code.
4. Add or update tests for business-critical behavior.
5. Run formatting/build/tests relevant to the change.
6. Fix compilation and test failures before stopping.
7. Update Swagger annotations/DTO schemas when an API contract changes.
8. Update Flyway migrations only by adding new migrations; do not rewrite an already-applied migration unless the user explicitly says the database is disposable.
9. Summarize changed files and any remaining risks.

## 7. Commands

Prefer the Maven wrapper when present:

```bash
./mvnw clean test
./mvnw spring-boot:run
```

Local infrastructure:

```bash
docker compose up -d
docker compose ps
docker compose logs -f postgres
```

Before declaring a task complete, at minimum run:

```bash
./mvnw test
```

For packaging/deployment changes also run:

```bash
./mvnw clean package
```

## 8. Definition of done

A feature is not done merely because it compiles. It is done when:

- authorization is correct;
- validation is present;
- business invariants are enforced;
- persistence is correct;
- errors use the common response shape;
- OpenAPI reflects the real endpoint contract;
- tests cover the critical path;
- the full project tests pass.

## 9. Hackathon bias

Prefer simple, explicit, boring code that works over sophisticated abstractions. Do not introduce infrastructure or patterns merely because they are enterprise-looking. The live demo must survive.
