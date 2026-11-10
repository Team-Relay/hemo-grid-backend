# HemoGrid Backend

HemoGrid is a Java 21 and Spring Boot modular monolith for coordinating blood
requests between hospitals and blood-bank providers. PostgreSQL is the system of
record, Flyway owns schema evolution, JWTs secure the API, and Springdoc exposes
the OpenAPI contract.

## Start locally

```bash
docker compose up -d
export JWT_SECRET=local-development-jwt-secret-with-at-least-32-bytes
./mvnw spring-boot:run
```

Swagger UI is available at `http://localhost:8080/swagger-ui.html`; pgAdmin is
available at `http://localhost:5050`. Detailed credentials and the recommended
API walkthrough are in [docs/LOCAL_BACKEND_RUNBOOK.md](docs/LOCAL_BACKEND_RUNBOOK.md).

## Verify changes

```bash
./mvnw test
```

Integration tests use PostgreSQL and verify organization isolation, inventory
reservation invariants, lifecycle transitions, authorization, and the stable
error contract. Use a dedicated test database rather than a database containing
development data.

## Production contract

The backend expects `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, and
`CORS_ALLOWED_ORIGINS`. Render also supplies `PORT`, which the application reads
directly. Deployment is intentionally deferred to Phase 10; Phase 8 only makes
the repository review- and deployment-ready.
