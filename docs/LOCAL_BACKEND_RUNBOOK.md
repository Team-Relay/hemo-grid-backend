# Local Backend Runbook

Use this when starting the backend for frontend development.

## Requirements

- Java 21 or newer with Maven wrapper support.
- Docker Desktop or another Docker runtime.
- Port `5432` available for PostgreSQL.
- Port `8080` available for the Spring Boot app.

## Start PostgreSQL

```bash
docker compose up -d
docker compose ps
```

The local database config is:

```text
host: localhost
port: 5432
database: hemogrid
username: hemogrid
password: hemogrid
```

## Run Tests

```bash
./mvnw test
```

The tests currently use the local PostgreSQL database from Docker Compose. If PostgreSQL is not running or Docker is unavailable, the Spring test context fails before assertions run.

## Run The Backend

Set a local JWT secret with at least 32 bytes.

```bash
export JWT_SECRET=local-development-jwt-secret-32-bytes
./mvnw spring-boot:run
```

Open:

```text
http://localhost:8080/swagger-ui.html
```

## Frontend CORS

The backend allows these frontend origins by default:

```text
http://localhost:3000
http://localhost:5173
```

Override with:

```bash
export CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
```

## Demo Login Credentials

Hospital:

```text
hospital.demo@hemogrid.local
HospitalDemo123!
```

Blood bank:

```text
bank.demo@hemogrid.local
BankDemo123!
```

Platform admin:

```text
admin.demo@hemogrid.local
AdminDemo123!
```

## Swagger Demo Order

1. `POST /api/v1/auth/login` as hospital.
2. Authorize Swagger with `Bearer <hospitalToken>`.
3. `POST /api/v1/blood-requests`.
4. `GET /api/v1/blood-requests/{requestId}/candidates`.
5. `POST /api/v1/blood-requests/{requestId}/select-provider`.
6. Login as blood bank.
7. Authorize Swagger with `Bearer <bankToken>`.
8. `GET /api/v1/provider/requests`.
9. `POST /api/v1/provider/requests/{requestId}/accept`.
10. `POST /api/v1/provider/requests/{requestId}/status` with `PREPARING`.
11. `POST /api/v1/provider/requests/{requestId}/status` with `IN_TRANSIT`.
12. `POST /api/v1/provider/requests/{requestId}/status` with `DELIVERED`.
13. `GET /api/v1/inventory` to confirm reserved units were consumed.

