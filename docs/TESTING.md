# Backend testing

The planner core also has a dependency-free check and a controlled scenario comparison.

```bash
bash scripts/check_planner.sh
bash scripts/benchmark_batch_dispatch.sh
```

`BatchDispatchPlannerTest` runs the same core regression checks through JUnit. `DispatchPlanServiceIntegrationTests` covers coordinator access, explicit approval, current shared stock, quarantine changes and hospital ownership of request approval. The HTTP suite also checks planner endpoint authorization.

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

## GitHub verification

The [backend verification workflow](../.github/workflows/backend-verification.yml) runs on pull requests targeting `main` and can also be started manually. It uses Java 21 and a disposable PostgreSQL 17 service. It runs the planner checks, regenerates both benchmark summaries and compares them byte for byte with the committed evidence before running `./mvnw -B -ntp test`.

[Run 42928089586](https://github.com/Team-Relay/hemo-grid-backend/actions/runs/42928089586) passed all 57 tests with zero failures, errors or skips, including migration of a fresh database through V8. The planner checks include 2,000 comparisons against exhaustive enumeration. The batch benchmark evaluates 3,321 controlled synthetic scenarios.
