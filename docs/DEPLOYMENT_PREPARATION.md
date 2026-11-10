# Render and Neon preparation for Phase 10

Phase 8 prepares deployment artifacts but does not create cloud resources.

Render should build the repository `Dockerfile` and use
`/actuator/health/readiness` as its readiness endpoint. Required environment
variables are `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, and
`CORS_ALLOWED_ORIGINS`; Render supplies `PORT`. Use a new production JWT secret
with at least 32 bytes and set CORS to the exact Vercel HTTPS origin.

For Neon, convert the supplied connection information to a JDBC URL, retaining
TLS requirements, for example:

```text
jdbc:postgresql://host/database?sslmode=require
```

Flyway runs at startup and Hibernate validates the result. Take a Neon branch or
backup before applying future migrations. Do not run `reset-test-data.sql` in
production. Decide before launch whether Swagger should remain public and rotate
every credential previously used for local development.
