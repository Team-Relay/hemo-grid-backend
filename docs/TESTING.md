# Backend testing

Integration tests use the real Spring context and PostgreSQL because transaction
locking, unique constraints, Flyway migrations, and organization-scoped queries
cannot be proven faithfully with an in-memory substitute.

Use a dedicated disposable database:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/hemogrid_test
export DB_USERNAME=hemogrid
export DB_PASSWORD=hemogrid
export JWT_SECRET=test-jwt-secret-with-at-least-32-bytes
./mvnw test
```

`reset-test-data.sql` restores known UUIDs and stock before each applicable test.
The suites cover authentication, the HTTP/error contract, organization
isolation, matching, inventory updates, lifecycle transitions, and concurrent
provider acceptance. Never point these tests at a database containing data that
must be retained.
