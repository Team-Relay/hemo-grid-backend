# HemoGrid Backend Memory

This file records implementation progress so a future Codex session can continue from the current state without relying on chat history.

## Project Rules

- Build HemoGrid as a Spring Boot modular monolith.
- Use Java 21, Spring Boot 4.1.x, Maven, PostgreSQL, Flyway, Spring Security, JWT, Actuator, and Swagger/OpenAPI.
- Package by feature, not by global technical layer.
- Expected package shape under `com.sentinel.hemo_grid`:
  - `auth`
  - `organization`
  - `inventory`
  - `request`
  - `matching`
  - `realtime`
  - `admin`
  - `config`
  - `common`
- Controllers must stay thin.
- Services own business rules and transaction boundaries.
- Repositories only handle persistence.
- DTOs must be separate from JPA entities.
- Flyway owns schema changes. Hibernate must validate, not generate schema.
- All frontend-facing endpoints must be documented in Swagger/OpenAPI.

## Product Goal

HemoGrid connects hospitals to participating blood banks with screened inventory.

Core MVP flow:

```text
Hospital logs in
  -> creates blood request
  -> backend matches eligible blood banks
  -> hospital selects provider
  -> selected blood bank accepts or declines
  -> acceptance reserves inventory transactionally
  -> request progresses through fulfilment statuses
  -> delivery consumes inventory
  -> both organizations can view request history/status
```

## Phase 0 Status

Phase 0 bootstrap was completed before this memory file was created.

Implemented:

- PostgreSQL Docker Compose file: `docker-compose.yml`
- Application configuration: `src/main/resources/application.yaml`
- Flyway bootstrap migration: `src/main/resources/db/migration/V1__bootstrap.sql`
- Maven compiler target set explicitly to Java 21 in `pom.xml`

Current local database settings:

```text
Host: localhost
Port: 5432
Database: hemogrid
User: hemogrid
Password: hemogrid
JDBC URL: jdbc:postgresql://localhost:5432/hemogrid
```

Docker command:

```bash
docker compose up -d
```

## Phase 1 Status

Phase 1 foundation is complete and verified.

Implemented:

- OpenAPI/Swagger configuration:
  - `src/main/java/com/sentinel/hemo_grid/config/OpenApiConfig.java`
- Security baseline:
  - `src/main/java/com/sentinel/hemo_grid/config/SecurityConfig.java`
- Application properties binding:
  - `src/main/java/com/sentinel/hemo_grid/config/AppProperties.java`
- Jackson JSON configuration:
  - `src/main/java/com/sentinel/hemo_grid/config/JacksonConfig.java`
- Common API error contract:
  - `src/main/java/com/sentinel/hemo_grid/common/exception/ApiErrorResponse.java`
  - `src/main/java/com/sentinel/hemo_grid/common/exception/BusinessException.java`
  - `src/main/java/com/sentinel/hemo_grid/common/exception/ErrorCode.java`
  - `src/main/java/com/sentinel/hemo_grid/common/exception/GlobalExceptionHandler.java`
- Added dependencies to `pom.xml`:
  - `springdoc-openapi-starter-webmvc-ui`
  - `spring-boot-starter-jackson`

Security behavior after Phase 1:

```text
Public:
  /actuator/health
  /actuator/info
  /swagger-ui.html
  /swagger-ui/**
  /v3/api-docs
  /v3/api-docs/**

Protected:
  everything else
```

Swagger URL:

```text
http://localhost:8080/swagger-ui.html
```

Verification command:

```bash
./mvnw test
```

Verification result:

```text
BUILD SUCCESS
Tests run: 1, Failures: 0, Errors: 0
```

Notes:

- Maven compiled with `release 21`.
- Tests connected successfully to PostgreSQL on `localhost:5432`.
- Flyway validated migration version `1`.
- JPA EntityManagerFactory initialized successfully.
- Springdoc logs warnings that Swagger and API docs are enabled by default. This is acceptable for local/hackathon development.

## Phase 2 Status

Phase 2 auth/domain foundation is complete and verified.

Implemented:

- JWT configuration:
  - `JWT_SECRET` must be supplied by the environment.
  - No local/default JWT secret is committed.
  - Tests provide a test-only JWT secret through `@SpringBootTest(properties = ...)`.
- Flyway migration:
  - `src/main/resources/db/migration/V2__create_organizations_and_users.sql`
  - creates `organizations`
  - creates `users`
  - adds constraints/indexes
  - seeds demo hospital, blood bank, platform admin users with BCrypt password hashes
- `organization` feature:
  - `src/main/java/com/sentinel/hemo_grid/organization/domain/OrganizationType.java`
  - `src/main/java/com/sentinel/hemo_grid/organization/domain/Organization.java`
  - `src/main/java/com/sentinel/hemo_grid/organization/persistence/OrganizationRepository.java`
  - `src/main/java/com/sentinel/hemo_grid/organization/api/OrganizationResponse.java`
  - `src/main/java/com/sentinel/hemo_grid/organization/api/OrganizationController.java`
- `auth` feature:
  - `src/main/java/com/sentinel/hemo_grid/auth/domain/UserRole.java`
  - `src/main/java/com/sentinel/hemo_grid/auth/domain/AppUser.java`
  - `src/main/java/com/sentinel/hemo_grid/auth/persistence/UserRepository.java`
  - `src/main/java/com/sentinel/hemo_grid/auth/api/LoginRequest.java`
  - `src/main/java/com/sentinel/hemo_grid/auth/api/LoginResponse.java`
  - `src/main/java/com/sentinel/hemo_grid/auth/api/UserResponse.java`
  - `src/main/java/com/sentinel/hemo_grid/auth/api/OrganizationSummaryResponse.java`
  - `src/main/java/com/sentinel/hemo_grid/auth/api/AuthController.java`
  - `src/main/java/com/sentinel/hemo_grid/auth/application/AuthService.java`
- JWT/password config:
  - `src/main/java/com/sentinel/hemo_grid/config/JwtConfig.java`
  - BCrypt `PasswordEncoder`
  - Spring Security `JwtEncoder`
  - Spring Security `JwtDecoder`
  - HS256 secret validation requiring at least 32 bytes
- Security config updated:
  - `POST /api/v1/auth/login` is public
  - JWT bearer authentication enabled for protected endpoints
  - `role` JWT claim maps to `ROLE_*` authorities
- `/api/v1/organizations/me` returns `RESOURCE_NOT_FOUND` for users without an organization, such as platform admin.
- API error response was simplified after Phase 2. Current error body includes only:
  - `timestamp`
  - `status`
  - `error`
  - `message`
- Removed `code`, `path`, and `fieldErrors` from API error responses.
- `/api/v1/organizations/me` response no longer exposes `latitude` or `longitude`; coordinates remain stored internally for later matching/distance ranking.
- Tests:
  - `src/test/java/com/sentinel/hemo_grid/auth/application/AuthServiceIntegrationTests.java`
  - verifies login success, JWT claims, and invalid-password rejection

Phase 2 endpoints:

```text
POST /api/v1/auth/login
GET  /api/v1/auth/me
GET  /api/v1/organizations/me
```

Local demo credentials:

```text
Hospital:
  email: hospital.demo@hemogrid.local
  password: HospitalDemo123!

Blood bank:
  email: bank.demo@hemogrid.local
  password: BankDemo123!

Platform admin:
  email: admin.demo@hemogrid.local
  password: AdminDemo123!
```

Verification command:

```bash
./mvnw test
```

Verification result:

```text
BUILD SUCCESS
Tests run: 3, Failures: 0, Errors: 0
```

Notes:

- Tests inject a test-only JWT secret.
- Local/deployed runtime requires `JWT_SECRET`.
- Tests connected to PostgreSQL on `localhost:5432`.
- Flyway validated 2 migrations.
- JPA found 2 repositories.
- JWT claims include `sub`, `email`, `role`, `organizationId`, `organizationType`, `iss`, `iat`, and `exp` where applicable.
- Platform admin has no organization, which is allowed by the database constraint.

## Phase 3 Status

Phase 3 inventory is complete and verified.

Implemented:

- Flyway migration:
  - `src/main/resources/db/migration/V3__create_blood_inventory.sql`
  - creates `blood_inventory`
  - adds unique constraint `(organization_id, blood_group, component)`
  - adds blood group/component check constraints
  - adds inventory invariant check: `units_available >= 0`, `units_reserved >= 0`, `units_reserved <= units_available`
  - seeds 8 `RED_CELLS` inventory rows for demo blood bank `Maitama Blood Centre`
- `inventory` feature:
  - `src/main/java/com/sentinel/hemo_grid/inventory/domain/BloodGroup.java`
  - `src/main/java/com/sentinel/hemo_grid/inventory/domain/BloodComponent.java`
  - `src/main/java/com/sentinel/hemo_grid/inventory/domain/BloodInventory.java`
  - `src/main/java/com/sentinel/hemo_grid/inventory/persistence/BloodInventoryRepository.java`
  - `src/main/java/com/sentinel/hemo_grid/inventory/api/InventoryResponse.java`
  - `src/main/java/com/sentinel/hemo_grid/inventory/api/UpdateInventoryRequest.java`
  - `src/main/java/com/sentinel/hemo_grid/inventory/api/InventoryController.java`
  - `src/main/java/com/sentinel/hemo_grid/inventory/application/InventoryService.java`
- Inventory endpoints:
  - `GET /api/v1/inventory`
  - `PUT /api/v1/inventory/{inventoryId}`
  - `PATCH /api/v1/inventory/{inventoryId}/units`
- Authorization:
  - only `BLOOD_BANK_ADMIN` and `BLOOD_BANK_STAFF` users attached to a `BLOOD_BANK` organization can access inventory.
  - hospital users cannot read or mutate inventory.
  - users can only update inventory rows owned by their own organization.
- Business rules:
  - `unitsFree` is derived as `unitsAvailable - unitsReserved`.
  - negative available units are rejected.
  - available units cannot be set below currently reserved units.
  - JPA `@Version` is mapped to the `version` column for optimistic locking.
- Tests:
  - `src/test/java/com/sentinel/hemo_grid/inventory/application/InventoryServiceIntegrationTests.java`
  - verifies blood-bank inventory read, update, negative-unit rejection, and hospital access denial.

Seeded demo inventory IDs:

```text
A_POSITIVE  / RED_CELLS: 30000000-0000-0000-0000-000000000001
A_NEGATIVE  / RED_CELLS: 30000000-0000-0000-0000-000000000002
B_POSITIVE  / RED_CELLS: 30000000-0000-0000-0000-000000000003
B_NEGATIVE  / RED_CELLS: 30000000-0000-0000-0000-000000000004
AB_POSITIVE / RED_CELLS: 30000000-0000-0000-0000-000000000005
AB_NEGATIVE / RED_CELLS: 30000000-0000-0000-0000-000000000006
O_POSITIVE  / RED_CELLS: 30000000-0000-0000-0000-000000000007
O_NEGATIVE  / RED_CELLS: 30000000-0000-0000-0000-000000000008
```

Verification command:

```bash
./mvnw test
```

Verification result:

```text
BUILD SUCCESS
Tests run: 7, Failures: 0, Errors: 0
```

Notes:

- Tests connected to PostgreSQL on `localhost:5432`.
- Flyway validated 3 migrations.
- JPA found 3 repositories.
- The single seeded blood bank has enough `O_NEGATIVE / RED_CELLS` stock for a 3-unit demo request. Additional blood banks can be seeded in the matching phase.

## Phase 4 Status

Phase 4 blood requests and matching is complete and verified.

Implemented:

- Flyway migration:
  - `src/main/resources/db/migration/V4__create_blood_requests_and_candidates.sql`
  - creates `blood_requests`
  - creates `request_candidates`
  - adds request/candidate constraints and indexes
  - seeds two additional demo blood-bank organizations:
    - `Garki Emergency Blood Bank` with 1 free `O_NEGATIVE / RED_CELLS` unit
    - `Wuse Regional Blood Bank` with 7 free `O_NEGATIVE / RED_CELLS` units
- `request` feature:
  - `src/main/java/com/sentinel/hemo_grid/request/domain/RequestUrgency.java`
  - `src/main/java/com/sentinel/hemo_grid/request/domain/RequestStatus.java`
  - `src/main/java/com/sentinel/hemo_grid/request/domain/BloodRequest.java`
  - `src/main/java/com/sentinel/hemo_grid/request/domain/RequestCandidate.java`
  - `src/main/java/com/sentinel/hemo_grid/request/persistence/BloodRequestRepository.java`
  - `src/main/java/com/sentinel/hemo_grid/request/persistence/RequestCandidateRepository.java`
  - `src/main/java/com/sentinel/hemo_grid/request/application/BloodRequestService.java`
  - request DTOs and controller under `src/main/java/com/sentinel/hemo_grid/request/api`
- `matching` feature:
  - `src/main/java/com/sentinel/hemo_grid/matching/application/HaversineDistanceCalculator.java`
  - `src/main/java/com/sentinel/hemo_grid/matching/application/MatchingService.java`
- `inventory` repository updated with matching queries.

Phase 4 endpoints:

```text
POST /api/v1/blood-requests
GET  /api/v1/blood-requests
GET  /api/v1/blood-requests/{requestId}
GET  /api/v1/blood-requests/{requestId}/candidates
POST /api/v1/blood-requests/{requestId}/select-provider
```

Request rules:

- Only hospital users can create/list/read hospital blood requests.
- Blood-bank users cannot create hospital requests.
- Hospitals can only read/select providers for their own requests.
- New requests start in `REQUESTED`.
- Selecting a provider does not reserve inventory yet and does not change status; it only sets `providerOrganization`.

Matching rules implemented:

- exact blood group match
- exact component match
- active blood-bank organizations only
- `unitsFree = unitsAvailable - unitsReserved`
- full-capacity candidates rank before partial candidates
- distance-known candidates rank before distance-missing candidates
- distance ascending
- more free units descending
- organization name ascending
- candidates are persisted in `request_candidates` for stable demo/audit ordering

Seeded `O_NEGATIVE / RED_CELLS` demo candidate behavior for a 3-unit request from `Central Care Hospital`:

```text
Rank 1: Maitama Blood Centre        5 free units, full match
Rank 2: Wuse Regional Blood Bank    7 free units, full match
Rank 3: Garki Emergency Blood Bank  1 free unit, partial match
```

Tests:

- `src/test/java/com/sentinel/hemo_grid/request/application/BloodRequestServiceIntegrationTests.java`
- verifies hospital request creation
- verifies ranked candidates
- verifies provider selection
- verifies blood-bank user cannot create hospital request

Verification command:

```bash
./mvnw test
```

Verification result:

```text
BUILD SUCCESS
Tests run: 10, Failures: 0, Errors: 0
```

Notes:

- Tests connected to PostgreSQL on `localhost:5432`.
- Flyway validated 4 migrations.
- JPA found 5 repositories.
- Provider acceptance/reservation is not implemented yet; that is the next phase.

## Phase 5 Status

Phase 5 provider fulfilment and inventory reservation is complete and verified.

Implemented:

- Provider fulfilment service:
  - `src/main/java/com/sentinel/hemo_grid/request/application/ProviderRequestService.java`
- Provider fulfilment controller:
  - `src/main/java/com/sentinel/hemo_grid/request/api/ProviderRequestController.java`
- Status update DTO:
  - `src/main/java/com/sentinel/hemo_grid/request/api/UpdateRequestStatusRequest.java`
- Hospital cancellation endpoint added to:
  - `src/main/java/com/sentinel/hemo_grid/request/api/BloodRequestController.java`
  - `src/main/java/com/sentinel/hemo_grid/request/application/BloodRequestService.java`
- Auth helper methods added:
  - `AuthService.requireHospitalUser(...)`
  - `AuthService.requireBloodBankUser(...)`
- Inventory reservation methods added to `BloodInventory`:
  - `reserve(int units)`
  - `releaseReservation(int units)`
  - `consumeReservation(int units)`
- Pessimistic row lock query added to `BloodInventoryRepository`:
  - `lockByOrganizationIdAndBloodGroupAndComponent(...)`
- Provider request queries added to `BloodRequestRepository`.

Phase 5 endpoints:

```text
GET  /api/v1/provider/requests
GET  /api/v1/provider/requests/{requestId}
POST /api/v1/provider/requests/{requestId}/accept
POST /api/v1/provider/requests/{requestId}/decline
POST /api/v1/provider/requests/{requestId}/status
POST /api/v1/blood-requests/{requestId}/cancel
```

Fulfilment rules:

- A blood bank can only see requests assigned to its own organization.
- Another blood bank cannot accept a request assigned elsewhere.
- Accepting a request requires status `REQUESTED`.
- Accepting uses a pessimistic database row lock on the matching inventory row.
- Accepting recomputes current free units before reserving.
- If free units are insufficient, acceptance returns a conflict.
- Accepted requests reserve `unitsRequired` by incrementing `unitsReserved`.
- Declining a request sets status to `DECLINED`.
- Provider status progression:
  - `ACCEPTED -> PREPARING`
  - `PREPARING -> IN_TRANSIT`
  - `IN_TRANSIT -> DELIVERED`
- Delivery consumes inventory:
  - `unitsAvailable -= unitsRequired`
  - `unitsReserved -= unitsRequired`
- Hospital cancellation supports `REQUESTED`, `ACCEPTED`, and `PREPARING`.
- Cancellation after acceptance releases reserved units.
- Delivered, declined, cancelled, and invalid-status requests cannot be progressed through unsupported transitions.

Tests:

- `src/test/java/com/sentinel/hemo_grid/request/application/ProviderRequestServiceIntegrationTests.java`
- verifies selected provider acceptance reserves inventory.
- verifies delivery consumes available and reserved inventory.
- verifies hospital cancellation after acceptance releases reservation.
- verifies another blood bank cannot accept a request assigned elsewhere.
- verifies acceptance rejects insufficient inventory.

Verification command:

```bash
./mvnw test
```

Verification result:

```text
BUILD SUCCESS
Tests run: 15, Failures: 0, Errors: 0
```

Notes:

- No new Flyway migration was needed for Phase 5.
- Tests connected to PostgreSQL on `localhost:5432`.
- Flyway validated 4 migrations.
- JPA found 5 repositories.

## Next Phase

Next recommended phase: Phase 6, API hardening and demo polish before realtime.

Build:

- Add or improve controller-level tests for actual HTTP endpoints if needed.
- Add provider endpoint payload examples to Swagger where useful.
- Verify full Swagger demo flow manually:
  - hospital login
  - create request
  - view candidates
  - select provider
  - blood-bank login
  - view provider request
  - accept
  - progress status
  - verify inventory change
- Consider adding a small admin/dashboard endpoint only if the frontend needs it.
- Decide whether to implement WebSocket realtime now or keep polling as the demo fallback.

## Phase 6 Status

Phase 6 frontend handoff and API hardening has been started.

Implemented:

- Frontend API handoff:
  - `docs/FRONTEND_API_HANDOFF.md`
  - documents base URLs, demo credentials, auth, enum values, endpoint payloads, response shapes, error shape, polling guidance, and the Swagger happy path.
- Local backend runbook:
  - `docs/LOCAL_BACKEND_RUNBOOK.md`
  - documents Docker PostgreSQL startup, test command, JWT secret, Swagger URL, CORS defaults, and demo flow.
- HTTP integration coverage:
  - `src/test/java/com/sentinel/hemo_grid/api/ApiFlowIntegrationTests.java`
  - verifies public health endpoint, protected auth boundary, hospital request creation, candidate listing, provider selection, provider dashboard listing, acceptance reservation, provider status progression, delivery consumption, and hospital-side delivered status over real MockMvc HTTP endpoints.
- Deterministic test data reset:
  - `src/test/resources/sql/reset-test-data.sql`
  - removes test-created requests/candidates and restores seeded inventory counts before integration tests.
- Swagger wording correction:
  - cancellation endpoint summary now matches implemented behavior: cancellation is allowed while status is `REQUESTED`, `ACCEPTED`, or `PREPARING`.

Verification note:

- `./mvnw test` requires local PostgreSQL on `localhost:5432`.
- If Docker/PostgreSQL is not running, tests fail during Spring context startup before assertions run.
