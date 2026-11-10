# HemoGrid Backend Build Specification

**Purpose:** durable product + engineering context for Codex in IntelliJ  
**Project:** HemoGrid (working product name)  
**Build window:** 72-hour hackathon MVP  
**Backend style:** Spring Boot modular monolith  
**Primary consumer:** separate frontend engineer  
**Baseline:** Java 21, Spring Boot 4.1.x, Maven, PostgreSQL, Flyway, Docker Compose

---

## 1. Executive context

HemoGrid is a browser-based blood availability and emergency coordination platform.

The core product problem is not "find a donor." The MVP assumes blood in the participating blood-bank inventory is already screened and available for legitimate clinical use. The immediate problem HemoGrid solves is **visibility and coordination**:

- a hospital needs a specific blood group/component urgently;
- the hospital cannot easily see which nearby participating blood bank can satisfy the request;
- the blood may already exist elsewhere, but the requesting facility lacks a fast coordination layer;
- HemoGrid makes participating inventory searchable, ranks appropriate facilities, lets a hospital request stock, reserves it safely when accepted, and tracks fulfilment status.

### Product sentence

> HemoGrid is a real-time blood availability and emergency fulfilment network connecting hospitals to participating blood banks.

### Demo sentence

> A hospital requests 3 units of O-negative red cells; HemoGrid finds the nearest blood bank that can fulfil all 3 units, sends the request, the blood bank accepts it, and the hospital sees the status update immediately.

---

## 2. What the backend must accomplish in 72 hours

The backend must make the following live demo reliable:

1. Hospital user logs in.
2. Hospital creates a blood request.
3. Backend validates request and persists it.
4. Matching service finds eligible blood-bank organizations.
5. API returns ranked candidates with current availability and distance.
6. Hospital sends the request to a selected candidate, or the backend assigns the top candidate if the UX chooses that approach.
7. Blood-bank dashboard receives/loads the request.
8. Blood-bank user accepts the request.
9. Backend atomically reserves the requested units.
10. Hospital sees request status become `ACCEPTED`.
11. Blood bank progresses request through `PREPARING`, `IN_TRANSIT`, and `DELIVERED`.
12. On delivery, reserved inventory is finalized as consumed.
13. Request history remains queryable by both organizations.

If this flow is stable, the backend has achieved the hackathon goal.

---

## 3. Non-goals for the hackathon MVP

Do **not** spend time implementing these unless all P0 items are finished and tested:

- donor recruitment or donor-to-patient matching;
- clinical diagnosis or treatment recommendation;
- patient electronic medical records;
- ambulance dispatch;
- payment settlement;
- hospital ERP/EHR integrations;
- Redis;
- Kafka;
- RabbitMQ;
- microservices;
- service discovery;
- Kubernetes;
- Elasticsearch;
- Keycloak;
- event sourcing;
- CQRS;
- complex ML training pipelines;
- multi-region infrastructure;
- advanced stock forecasting.

A simple shortage indicator can be added later, but it must not compromise the core request flow.

---

## 4. Fixed technology stack

### Runtime/build

- Java 21
- Spring Boot 4.1.x
- Maven + Maven Wrapper

### HTTP/API

- Spring MVC
- JSON REST API
- Bean Validation
- springdoc-openapi / Swagger UI

### Persistence

- PostgreSQL
- Spring Data JPA / Hibernate
- Flyway migrations
- `spring.jpa.hibernate.ddl-auto=validate`
- `spring.jpa.open-in-view=false`

### Security

- Spring Security
- JWT bearer access tokens
- Spring Security OAuth2 Resource Server support for token validation
- Spring Security `JwtEncoder`/`JwtDecoder` for project-issued JWTs
- BCrypt password hashing
- role + organization ownership checks

### Realtime

- Spring WebSocket support
- Prefer STOMP topics if implementation stays simple
- REST polling is an acceptable fallback and must remain possible even if WebSocket is enabled

### Operations

- Spring Boot Actuator
- Docker Compose for PostgreSQL local infrastructure
- environment variables for secrets/production configuration

### Testing

- Spring Boot Test
- Spring Security Test
- Testcontainers PostgreSQL for persistence/business integration tests

---

## 5. Maven dependency baseline

Use Spring Boot dependency management for dependencies it manages. Do not pin arbitrary Spring module versions.

Required dependencies:

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webmvc</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-flyway</artifactId>
    </dependency>

    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-database-postgresql</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-websocket</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <version>3.0.3</version>
    </dependency>

    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-devtools</artifactId>
        <scope>runtime</scope>
        <optional>true</optional>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>

    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-test</artifactId>
        <scope>test</scope>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-testcontainers</artifactId>
        <scope>test</scope>
    </dependency>

    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers-postgresql</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

Do not add JJWT unless explicitly requested. Use Spring Security JWT support.

---

## 6. Repository/package architecture

Package by domain/feature. Suggested base package:

```text
com.hemogrid
|
|-- HemoGridApplication.java
|
|-- config/
|   |-- SecurityConfig.java
|   |-- OpenApiConfig.java
|   |-- WebSocketConfig.java
|   `-- AppProperties.java
|
|-- auth/
|   |-- api/
|   |-- application/
|   |-- domain/
|   `-- persistence/
|
|-- organization/
|   |-- api/
|   |-- application/
|   |-- domain/
|   `-- persistence/
|
|-- inventory/
|   |-- api/
|   |-- application/
|   |-- domain/
|   `-- persistence/
|
|-- request/
|   |-- api/
|   |-- application/
|   |-- domain/
|   `-- persistence/
|
|-- matching/
|   `-- application/
|
|-- realtime/
|   `-- application/
|
|-- admin/
|   `-- api/
|
`-- common/
    |-- api/
    |-- exception/
    |-- security/
    `-- util/
```

Do not create a giant global `controller/`, `service/`, `repository/`, `entity/` structure.

Within a feature, exact subpackages may be simplified if Codex judges that the repository is still small. The important boundary is feature ownership.

---

## 7. Domain model

### 7.1 Organization

Represents a hospital or blood bank.

Fields:

```text
id                  UUID PK
name                varchar not null
organization_type   varchar not null  // HOSPITAL | BLOOD_BANK
email               varchar nullable
phone               varchar nullable
address             varchar not null
city                varchar nullable
state               varchar nullable
latitude             double precision nullable
longitude            double precision nullable
active               boolean not null default true
created_at           timestamptz not null
updated_at           timestamptz not null
```

Rules:

- inventory belongs only to `BLOOD_BANK` organizations;
- blood requests originate from `HOSPITAL` organizations;
- inactive organizations must not participate in matching.

### 7.2 User

Fields:

```text
id                  UUID PK
organization_id     UUID FK -> organizations.id
full_name           varchar not null
email               varchar unique not null
password_hash       varchar not null
role                varchar not null
active               boolean not null default true
created_at           timestamptz not null
updated_at           timestamptz not null
```

Roles for MVP:

```text
PLATFORM_ADMIN
HOSPITAL_ADMIN
HOSPITAL_STAFF
BLOOD_BANK_ADMIN
BLOOD_BANK_STAFF
```

Every non-platform user belongs to one organization.

### 7.3 BloodInventory

Represents aggregate available units for one blood bank + blood group + component.

Fields:

```text
id                  UUID PK
organization_id     UUID FK -> organizations.id
blood_group         varchar not null
component           varchar not null
units_available     integer not null
units_reserved      integer not null default 0
version             bigint not null default 0
created_at           timestamptz not null
updated_at           timestamptz not null
```

Unique constraint:

```text
(organization_id, blood_group, component)
```

Core invariant:

```text
units_available >= 0
units_reserved >= 0
units_reserved <= units_available
```

Derived fulfilment quantity:

```text
units_free = units_available - units_reserved
```

Blood groups:

```text
A_POSITIVE
A_NEGATIVE
B_POSITIVE
B_NEGATIVE
AB_POSITIVE
AB_NEGATIVE
O_POSITIVE
O_NEGATIVE
```

Blood components:

```text
WHOLE_BLOOD
RED_CELLS
PLATELETS
PLASMA
```

Do not implement transfusion compatibility substitution logic in P0. A request for a group/component matches the same group/component. This avoids unsafe clinical assumptions in the hackathon MVP.

### 7.4 BloodRequest

Fields:

```text
id                       UUID PK
requester_organization_id UUID FK -> organizations.id
provider_organization_id  UUID nullable FK -> organizations.id
blood_group                varchar not null
component                  varchar not null
units_required             integer not null
urgency                    varchar not null
status                     varchar not null
clinical_reference         varchar nullable
notes                      text nullable
requested_at               timestamptz not null
accepted_at                timestamptz nullable
preparing_at               timestamptz nullable
dispatched_at              timestamptz nullable
delivered_at               timestamptz nullable
cancelled_at               timestamptz nullable
created_by_user_id          UUID FK -> users.id
updated_at                  timestamptz not null
```

Urgency:

```text
ROUTINE
URGENT
CRITICAL
```

Status:

```text
REQUESTED
ACCEPTED
PREPARING
IN_TRANSIT
DELIVERED
DECLINED
CANCELLED
EXPIRED
```

For the demo, `REQUESTED` means the request exists and may have a selected provider. `ACCEPTED` means the selected blood bank has successfully reserved the required units.

### 7.5 RequestCandidate

Persisting ranked matches is useful for audit/demo reproducibility.

Fields:

```text
id                       UUID PK
blood_request_id         UUID FK
provider_organization_id UUID FK
available_units_snapshot integer not null
distance_km              double precision nullable
rank_position            integer not null
match_score              double precision nullable
created_at                timestamptz not null
```

This table is optional only if time is critically short. If removed, matching results can be computed on demand, but the API response shape should remain stable.

---

## 8. Database and Flyway strategy

Flyway is the only schema authority.

Recommended migrations:

```text
src/main/resources/db/migration/
|-- V1__create_organizations.sql
|-- V2__create_users.sql
|-- V3__create_blood_inventory.sql
|-- V4__create_blood_requests.sql
|-- V5__create_request_candidates.sql
`-- V6__seed_demo_data.sql
```

If the project is still disposable during the first few hours, Codex may consolidate migrations before any shared database has used them. Once another developer or deployed environment has run migrations, never rewrite migration history; add a new migration.

### Required indexes

At minimum:

```text
users(email)
blood_inventory(organization_id, blood_group, component)
blood_requests(requester_organization_id, created_at desc)
blood_requests(provider_organization_id, created_at desc)
blood_requests(status)
request_candidates(blood_request_id, rank_position)
```

### Seed data

The demo should boot with useful data.

Seed at least:

- 1 platform admin
- 2 hospital organizations
- 4-6 blood-bank organizations around the chosen demo city
- 1 hospital user for the main demo
- 1 blood-bank user for the accepting bank
- inventory across all 8 blood groups and at least RED_CELLS
- deliberately create some banks with insufficient O-negative stock and one bank with enough stock

Example demo scenario:

```text
Hospital: Central Care Hospital
Need: O_NEGATIVE / RED_CELLS / 3 units / CRITICAL

Blood Bank A: 0 free units
Blood Bank B: 1 free unit
Blood Bank C: 5 free units, ~6 km away
Blood Bank D: 7 free units, ~12 km away

Expected top eligible match: Blood Bank C
```

Do not commit plain-text real credentials. Demo credentials can be configurable and documented in a local-only README or generated using BCrypt hashes in the seed migration.

---

## 9. Configuration strategy

Use `application.yaml` as the configuration source, with environment-variable overrides.

Recommended baseline:

```yaml
spring:
  application:
    name: hemogrid

  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/hemogrid}
    username: ${DB_USERNAME:hemogrid}
    password: ${DB_PASSWORD:hemogrid}
    driver-class-name: org.postgresql.Driver

  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        format_sql: true

  flyway:
    enabled: true
    locations: classpath:db/migration

server:
  port: ${SERVER_PORT:8080}

management:
  endpoints:
    web:
      exposure:
        include: health,info

springdoc:
  swagger-ui:
    path: /swagger-ui.html
  api-docs:
    path: /v3/api-docs

app:
  security:
    jwt-secret: ${JWT_SECRET}
    jwt-issuer: ${JWT_ISSUER:hemogrid-api}
    access-token-minutes: ${JWT_ACCESS_TOKEN_MINUTES:480}
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:3000,http://localhost:5173}
```

Never add a default JWT secret. Startup should fail clearly if `JWT_SECRET` is absent outside an explicitly local profile.

---

## 10. Docker Compose local infrastructure

The Spring Boot application may run directly from IntelliJ while PostgreSQL runs in Docker.

Recommended `compose.yaml`:

```yaml
services:
  postgres:
    image: postgres:17
    container_name: hemogrid-postgres
    environment:
      POSTGRES_DB: hemogrid
      POSTGRES_USER: hemogrid
      POSTGRES_PASSWORD: hemogrid
    ports:
      - "5432:5432"
    volumes:
      - hemogrid_postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U hemogrid -d hemogrid"]
      interval: 5s
      timeout: 5s
      retries: 10

volumes:
  hemogrid_postgres_data:
```

Do not add Redis/Kafka/RabbitMQ to Compose in P0.

---

## 11. Authentication and authorization

### 11.1 Login flow

Endpoints:

```text
POST /api/v1/auth/login
GET  /api/v1/auth/me
```

Login request:

```json
{
  "email": "hospital@demo.local",
  "password": "..."
}
```

Login response:

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": 28800,
  "user": {
    "id": "uuid",
    "fullName": "Demo Hospital User",
    "email": "hospital@demo.local",
    "role": "HOSPITAL_STAFF",
    "organization": {
      "id": "uuid",
      "name": "Central Care Hospital",
      "type": "HOSPITAL"
    }
  }
}
```

For the hackathon MVP, refresh tokens are optional. A reasonably long demo access token is acceptable if secrets are protected and the frontend handles expiration cleanly.

### 11.2 JWT claims

Include only necessary claims:

```text
sub = user UUID or stable user identifier
email
role
organizationId
organizationType
iss
iat
exp
```

Do not trust organization IDs passed from the frontend when the authenticated user's organization should be authoritative.

### 11.3 Authorization matrix

Hospital roles may:

- create blood requests for their own hospital;
- view their hospital's requests;
- view matching candidate summaries returned for their requests;
- cancel eligible requests from their hospital.

Blood-bank roles may:

- read/update their own inventory;
- view requests addressed/assigned to their organization;
- accept/decline those requests;
- progress accepted requests through allowed statuses.

Platform admin may:

- read organizations and operational data for the command-centre demo;
- optionally manage organizations/inventory if needed.

No user may operate on another organization's private data simply by changing an ID in a URL.

---

## 12. Core API contract

Base path:

```text
/api/v1
```

Use consistent JSON naming. Camel case is preferred.

### 12.1 Auth

```text
POST /api/v1/auth/login
GET  /api/v1/auth/me
```

### 12.2 Organizations

P0 read endpoints:

```text
GET /api/v1/organizations/me
GET /api/v1/organizations/{id}      // admin or safe public summary where appropriate
```

Avoid building broad organization CRUD unless needed for the demo. Seed data is faster.

### 12.3 Inventory

Blood-bank scoped:

```text
GET   /api/v1/inventory
PUT   /api/v1/inventory/{inventoryId}
PATCH /api/v1/inventory/{inventoryId}/units
```

Optional admin/demo:

```text
GET /api/v1/admin/inventory/network
```

Inventory update request example:

```json
{
  "unitsAvailable": 5
}
```

Response should include `unitsAvailable`, `unitsReserved`, and computed `unitsFree`.

### 12.4 Blood requests

Hospital:

```text
POST /api/v1/blood-requests
GET  /api/v1/blood-requests
GET  /api/v1/blood-requests/{requestId}
GET  /api/v1/blood-requests/{requestId}/candidates
POST /api/v1/blood-requests/{requestId}/select-provider
POST /api/v1/blood-requests/{requestId}/cancel
```

Blood bank:

```text
GET  /api/v1/provider/requests
GET  /api/v1/provider/requests/{requestId}
POST /api/v1/provider/requests/{requestId}/accept
POST /api/v1/provider/requests/{requestId}/decline
POST /api/v1/provider/requests/{requestId}/status
```

Alternative simplification: `POST /blood-requests` can return ranked candidates immediately, then `select-provider` sets the target bank.

### 12.5 Create request body

```json
{
  "bloodGroup": "O_NEGATIVE",
  "component": "RED_CELLS",
  "unitsRequired": 3,
  "urgency": "CRITICAL",
  "clinicalReference": "ER-2026-0819-001",
  "notes": "Emergency request"
}
```

Validation:

- `bloodGroup` required;
- `component` required;
- `unitsRequired` integer, minimum 1, reasonable maximum e.g. 20 for MVP validation;
- `urgency` required;
- notes length limited;
- requester organization comes from authenticated user.

### 12.6 Candidate response

```json
{
  "requestId": "uuid",
  "candidates": [
    {
      "organizationId": "uuid",
      "organizationName": "Maitama Blood Centre",
      "bloodGroup": "O_NEGATIVE",
      "component": "RED_CELLS",
      "unitsFree": 5,
      "distanceKm": 6.4,
      "canFullyFulfil": true,
      "rank": 1
    }
  ]
}
```

Do not return inventory that another organization is not authorized to see beyond what the request/matching UX needs.

### 12.7 Select provider

```json
{
  "providerOrganizationId": "uuid"
}
```

Rules:

- request must belong to caller's hospital;
- request must be in `REQUESTED` state;
- provider must be an active blood bank;
- provider should still have relevant inventory; final stock guarantee occurs during acceptance/reservation.

### 12.8 Accept request

No body required unless implementation wants a note.

Acceptance must be transactional.

Expected result:

```json
{
  "requestId": "uuid",
  "status": "ACCEPTED",
  "provider": {
    "id": "uuid",
    "name": "Maitama Blood Centre"
  },
  "inventory": {
    "bloodGroup": "O_NEGATIVE",
    "component": "RED_CELLS",
    "unitsReservedForRequest": 3
  },
  "acceptedAt": "2026-08-19T15:30:00Z"
}
```

### 12.9 Status transition body

```json
{
  "status": "PREPARING"
}
```

Allowed progression:

```text
ACCEPTED -> PREPARING -> IN_TRANSIT -> DELIVERED
```

Do not permit arbitrary backward transitions.

---

## 13. Request state machine

Allowed transitions:

```text
REQUESTED -> ACCEPTED
REQUESTED -> DECLINED
REQUESTED -> CANCELLED

ACCEPTED -> PREPARING
ACCEPTED -> CANCELLED      // only if product explicitly allows; must release reservation

PREPARING -> IN_TRANSIT
PREPARING -> CANCELLED     // optional; must release reservation

IN_TRANSIT -> DELIVERED

DELIVERED = terminal
DECLINED  = terminal
CANCELLED = terminal
EXPIRED   = terminal
```

For simplicity, a declined provider request may either end that request or allow the hospital to choose the next candidate. Prefer the latter only if the model can represent provider attempts cleanly. If time is short, a hospital may manually select another provider on the same `REQUESTED` request after a decline.

Centralize transition validation in one domain/service method. Do not scatter status rules across controllers.

---

## 14. Matching algorithm

### 14.1 P0 objective

Find active blood banks that can fulfil the exact requested blood group + component and rank the best ones.

P0 matching rules:

1. organization type must be `BLOOD_BANK`;
2. organization must be active;
3. exact blood group match;
4. exact component match;
5. compute `unitsFree = unitsAvailable - unitsReserved`;
6. full matches require `unitsFree >= unitsRequired`;
7. rank full matches by distance ascending where coordinates are available;
8. if coordinates are missing, place those candidates after distance-known full matches;
9. partial matches may be returned after full matches for information, but must be marked `canFullyFulfil=false`;
10. P0 must not automatically split one request across multiple blood banks.

### 14.2 Haversine distance

Use latitude/longitude stored on organizations. Implement Haversine distance in one tested utility/service.

Distance is a ranking aid, not a claim of road ETA.

If the frontend wants an ETA, label hackathon demo ETA as an estimate derived separately; do not pretend Haversine distance is driving time.

### 14.3 Deterministic ordering

When distances tie or are absent, use stable tie breakers:

1. full fulfilment first;
2. distance ascending;
3. more free units descending;
4. organization name ascending.

Tests must prove deterministic ordering.

---

## 15. Inventory reservation and concurrency - critical

This is one of the most important backend correctness areas.

### Problem

Two hospitals could simultaneously attempt to reserve the same free units.

### Required behavior

On provider acceptance:

1. begin transaction;
2. lock the relevant inventory row for the provider + blood group + component;
3. recompute free units from current database state;
4. if `unitsFree < unitsRequired`, reject with a conflict response;
5. increment `unitsReserved` by `unitsRequired`;
6. set request status to `ACCEPTED` and timestamps;
7. commit;
8. only after a successful commit, publish realtime notification.

Prefer a repository query using a pessimistic write lock for this critical row, or another equally safe Postgres/JPA mechanism.

### On cancellation before delivery

Release reserved units exactly once.

### On delivery

For the accepted reservation:

```text
units_available = units_available - units_required
units_reserved  = units_reserved  - units_required
```

Never allow either value below zero.

### Idempotency

Repeated `accept` or `delivered` calls must not reserve/consume the stock twice.

Return current state for safe repeated requests where sensible, or reject invalid repeated transitions consistently.

---

## 16. Realtime behavior

Realtime is for UX; REST remains the system of record.

Suggested WebSocket endpoint:

```text
/ws
```

Suggested topics:

```text
/topic/organizations/{organizationId}/requests
/topic/requests/{requestId}
```

Event envelope:

```json
{
  "eventType": "BLOOD_REQUEST_STATUS_CHANGED",
  "occurredAt": "2026-08-19T15:30:00Z",
  "requestId": "uuid",
  "organizationId": "uuid",
  "data": {
    "status": "ACCEPTED"
  }
}
```

Useful events:

```text
BLOOD_REQUEST_CREATED
BLOOD_REQUEST_ASSIGNED
BLOOD_REQUEST_ACCEPTED
BLOOD_REQUEST_DECLINED
BLOOD_REQUEST_STATUS_CHANGED
INVENTORY_UPDATED
```

Security must prevent arbitrary subscription to another organization's private events.

### Hackathon fallback

If authenticated WebSocket/STOMP configuration becomes a time sink, keep REST endpoints correct and let the frontend poll request status every 2-3 seconds. Do not delay the core demo for realtime infrastructure.

---

## 17. Swagger/OpenAPI contract

Swagger is a required collaboration surface for the frontend engineer, not cosmetic documentation.

Default development URLs:

```text
/swagger-ui.html
/v3/api-docs
```

OpenAPI configuration must define:

- API title: HemoGrid API
- API version: v1
- bearer JWT security scheme
- useful API description

For every frontend-facing endpoint:

- add concise `@Operation` summary/description;
- document expected response codes;
- annotate DTO fields where enum meaning is not obvious;
- ensure error schemas render;
- ensure Bearer auth is visible in Swagger UI;
- keep actual implementation and OpenAPI contract aligned.

Frontend engineer must be able to:

1. open Swagger;
2. login through `/auth/login`;
3. copy token;
4. authorize with Bearer token;
5. exercise the request/inventory flow without asking the backend engineer for payload shapes.

---

## 18. Common API response and error shape

Success responses may return domain DTOs directly. Errors must be consistent.

Recommended error:

```json
{
  "timestamp": "2026-08-19T15:31:00Z",
  "status": 409,
  "error": "CONFLICT",
  "code": "INSUFFICIENT_INVENTORY",
  "message": "The selected blood bank no longer has enough free units to accept this request.",
  "path": "/api/v1/provider/requests/uuid/accept",
  "fieldErrors": []
}
```

Validation example:

```json
{
  "timestamp": "...",
  "status": 400,
  "error": "BAD_REQUEST",
  "code": "VALIDATION_FAILED",
  "message": "Request validation failed.",
  "path": "/api/v1/blood-requests",
  "fieldErrors": [
    {
      "field": "unitsRequired",
      "message": "must be greater than or equal to 1"
    }
  ]
}
```

Implement with `@RestControllerAdvice`.

Suggested domain error codes:

```text
VALIDATION_FAILED
UNAUTHORIZED
FORBIDDEN
RESOURCE_NOT_FOUND
INVALID_REQUEST_STATUS
INVALID_STATUS_TRANSITION
INSUFFICIENT_INVENTORY
PROVIDER_NOT_SELECTED
ORGANIZATION_INACTIVE
ORGANIZATION_TYPE_MISMATCH
REQUEST_ALREADY_FINALIZED
```

---

## 19. HTTP status conventions

Use predictable status codes:

```text
200 OK             successful reads/updates/actions
201 Created        request/resource creation
204 No Content     optional for simple successful deletes/actions
400 Bad Request    malformed/validation failure
401 Unauthorized   missing/invalid authentication
403 Forbidden      authenticated but not allowed
404 Not Found      inaccessible/not-found resource
409 Conflict       stock race, invalid state transition, duplicate invariant
500 Internal Server Error unexpected failure
```

Do not return HTTP 200 with an internal `success=false` for actual errors.

---

## 20. DTO strategy

Use explicit request and response DTOs, preferably Java records when appropriate.

Examples:

```text
LoginRequest
LoginResponse
CurrentUserResponse
CreateBloodRequestRequest
BloodRequestResponse
BloodRequestSummaryResponse
CandidateResponse
SelectProviderRequest
UpdateRequestStatusRequest
InventoryResponse
UpdateInventoryRequest
ApiErrorResponse
FieldErrorResponse
```

Do not serialize entities directly.

Mapping can be manual. Do not add MapStruct in P0 unless mapping volume becomes clearly painful.

---

## 21. Logging and auditability

Use SLF4J structured, concise logs.

Log important state changes with identifiers:

- login failure without logging passwords;
- blood request created;
- provider selected;
- request accepted;
- reservation failure due to insufficient stock;
- request status changed;
- request cancelled;
- request delivered;
- unexpected exceptions.

Never log:

- passwords;
- raw JWTs;
- JWT signing secrets;
- sensitive headers.

For P0, a separate audit table is optional. The request timestamps and application logs are sufficient.

---

## 22. Security baseline

Required:

- BCrypt password encoder;
- stateless API sessions;
- CSRF disabled for stateless bearer-token REST endpoints;
- CORS explicitly configured from allowed origins;
- public allowlist limited to login, Swagger in non-production if desired, and health endpoint;
- all business endpoints authenticated;
- method/service-level ownership validation;
- no secrets in git;
- generic login failure message;
- validation of enum/request values;
- no user-controlled organization assignment during normal requests.

Recommended public paths in development:

```text
/api/v1/auth/login
/actuator/health
/v3/api-docs/**
/swagger-ui/**
/swagger-ui.html
```

Production may restrict Swagger if required, but for the hackathon frontend integration it should be available.

---

## 23. Testing strategy

Testing must focus on things most likely to break the demo or corrupt inventory.

### Unit tests

At minimum:

- Haversine/ranking logic;
- state transition validator;
- candidate sorting;
- JWT-related helpers if custom logic exists.

### Integration tests with PostgreSQL/Testcontainers

Prioritize:

1. Flyway migrations boot cleanly on empty PostgreSQL.
2. Create blood request persists correctly.
3. Matching returns a fully capable closer bank before a farther one.
4. Insufficient stock is not treated as full fulfilment.
5. Blood bank cannot accept another bank's request.
6. Hospital cannot view another hospital's private request.
7. Accepting reserves exact units.
8. Concurrent/second acceptance cannot over-reserve stock.
9. Cancellation releases reservation once.
10. Delivery decrements available stock and clears reservation once.
11. Invalid state transitions return conflict.

### Controller/security tests

Cover:

- login success/failure;
- unauthenticated access rejected;
- wrong role rejected;
- validation error shape;
- Swagger need not be exhaustively tested.

### Build gate

Before handoff/deployment:

```bash
./mvnw clean test
./mvnw clean package
```

---

## 24. Demo data and demo accounts

Create predictable demo accounts that can be changed by environment/profile.

Example naming only:

```text
Hospital user: hospital.demo@hemogrid.local
Blood bank user: bank.demo@hemogrid.local
Admin user: admin.demo@hemogrid.local
```

Never use these passwords in production. For the hackathon, document the local demo credentials outside the committed production config or use a clearly marked `demo` Spring profile.

The main demo should be deterministic. Stock levels should be set so the expected selected bank is known in advance.

---

## 25. Operational command-centre endpoints (P1)

Only after P0 is stable, add lightweight admin summary endpoints for the frontend command-centre screen.

Possible endpoint:

```text
GET /api/v1/admin/dashboard
```

Response may contain:

```json
{
  "activeBloodBanks": 6,
  "openRequests": 4,
  "criticalRequests": 2,
  "inventoryByBloodGroup": [
    {
      "bloodGroup": "O_NEGATIVE",
      "unitsAvailable": 8,
      "unitsReserved": 3,
      "unitsFree": 5,
      "status": "CRITICAL"
    }
  ]
}
```

Threshold labels can be rule-based for the hackathon. Do not market them as clinically validated forecasting.

---

## 26. Optional shortage signal (P2)

Only implement if the core is finished.

A safe hackathon version is a deterministic inventory risk indicator, not a fake sophisticated AI model.

Example:

```text
free units <= critical threshold -> CRITICAL
free units <= warning threshold  -> LOW
otherwise                         -> HEALTHY
```

If historical request data is available, a simple consumption-rate forecast can be added later. It must be clearly labeled as an estimate.

---

## 27. Backend Dockerfile for deployment

Prefer a multi-stage build.

Example shape:

```dockerfile
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw -q -DskipTests dependency:go-offline
COPY src ./src
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

The exact base-image patch may change. Use a maintained Java 21 runtime image.

Do not bake secrets into the image.

---

## 28. Deployment model

Target architecture:

```text
Frontend
   |
   | HTTPS REST / WebSocket
   v
Spring Boot API container
   |
   | JDBC/TLS where supported
   v
Managed PostgreSQL
```

Required production environment variables:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
JWT_SECRET
JWT_ISSUER
JWT_ACCESS_TOKEN_MINUTES
CORS_ALLOWED_ORIGINS
SERVER_PORT               // only if platform requires it
```

Deployment checklist:

1. PostgreSQL provisioned.
2. Database network access confirmed.
3. Environment variables set.
4. Application boots and Flyway completes.
5. `/actuator/health` returns UP.
6. Swagger loads if enabled.
7. Login works.
8. Seed/demo data exists in hackathon environment.
9. Frontend origin added to CORS.
10. End-to-end request flow tested against deployed API.
11. WebSocket tested; if unstable, frontend polling fallback enabled.

---

## 29. Frontend integration contract

The backend engineer must give the frontend engineer:

- deployed base URL;
- Swagger URL;
- demo credentials;
- enum values;
- request lifecycle diagram;
- WebSocket endpoint/topics if enabled;
- CORS confirmation;
- any feature flags/fallback notes.

The frontend must never need direct database access.

Recommended frontend flow:

```text
Login
  -> store access token
  -> GET /auth/me
  -> create request
  -> render candidates
  -> select provider
  -> subscribe/poll request
  -> show acceptance/status timeline
```

Blood-bank flow:

```text
Login
  -> GET inventory
  -> GET assigned/open provider requests
  -> accept request
  -> update status
  -> inventory UI refreshes
```

---

## 30. 72-hour backend execution plan

### Phase 0 - bootstrap (first 1-2 hours)

Codex tasks:

- create Spring Boot 4.1.x / Java 21 Maven project if not already initialized;
- add agreed dependencies;
- create package skeleton;
- add `application.yaml`;
- add `compose.yaml` with PostgreSQL;
- ensure app connects to local DB;
- add Flyway and prove first migration runs;
- add `/actuator/health`;
- configure Swagger and prove UI loads.

Exit criteria:

```text
docker compose up -d
./mvnw spring-boot:run
```

works, DB connects, Flyway succeeds, health is UP, Swagger is visible.

### Phase 1 - domain + security (hours 2-8)

Build:

- organizations;
- users;
- roles;
- JWT login;
- `/auth/me`;
- security filter chain;
- global error handling;
- seed demo users/orgs.

Exit criteria:

- hospital and bank accounts can log in;
- Bearer auth works in Swagger;
- role/organization information is present in authenticated context.

### Phase 2 - inventory (hours 8-13)

Build:

- inventory entity/migration/repository/service;
- blood-group/component enums;
- inventory read/update endpoints;
- ownership rules;
- tests.

Exit criteria:

- bank user can view/update only its inventory;
- hospital cannot mutate bank inventory;
- free/reserved values are correct.

### Phase 3 - request + matching (hours 13-22)

Build:

- request migration/entity/status;
- create request endpoint;
- matching service;
- Haversine utility;
- candidates endpoint/response;
- provider selection;
- request detail/history endpoints;
- tests.

Exit criteria:

- the seeded O-negative scenario reliably ranks the intended bank first.

### Phase 4 - acceptance + reservation (hours 22-30)

Build:

- provider request list/detail;
- accept/decline;
- transaction + row locking;
- inventory reservation;
- lifecycle service;
- cancel/release;
- delivery/consume;
- concurrency/idempotency tests.

Exit criteria:

- stock cannot over-reserve;
- request transitions are enforced;
- cancel/deliver adjust inventory exactly once.

### Phase 5 - realtime (hours 30-36)

Build:

- WebSocket/STOMP config;
- request events;
- organization-scoped events;
- frontend event contract;
- polling fallback remains functional.

Timebox this phase. If security/reliability is not stable quickly, use polling for the live demo.

### Phase 6 - hardening + deployment (hours 36-48)

Build/fix:

- Dockerfile;
- production environment configuration;
- CORS;
- health checks;
- deployment;
- deployed Flyway verification;
- deployed Swagger;
- deployed login/request flow;
- logs/error handling.

### Phase 7 - frontend support + demo hardening (hours 48-60)

- freeze API contracts where possible;
- resolve frontend integration bugs;
- improve OpenAPI descriptions;
- stabilize seed/demo data;
- add command-centre summary only if core is stable;
- test network failure/reload scenarios.

### Phase 8 - final 12 hours

Do not redesign architecture.

- run full tests;
- run full demo repeatedly;
- fix only blockers/high-risk bugs;
- verify production data state/reset procedure;
- prepare backup polling path;
- prepare backup API calls in Swagger/Postman only if frontend fails;
- freeze deployment.

---

## 31. Cut order if behind schedule

Cut features in this order:

1. shortage prediction;
2. admin command-centre analytics;
3. persisted candidate table;
4. WebSocket realtime - use polling;
5. nonessential organization management endpoints;
6. complex request decline/reassignment workflow.

Never cut:

- login/auth;
- inventory;
- create request;
- matching;
- provider selection;
- acceptance;
- reservation correctness;
- lifecycle status;
- Swagger;
- deployed end-to-end path.

---

## 32. P0 acceptance criteria

The backend is P0-complete only when all of these are true:

### Boot/infrastructure

- [ ] Java 21 project builds.
- [ ] PostgreSQL starts via Docker Compose locally.
- [ ] Flyway migrations apply from an empty DB.
- [ ] Hibernate validates schema without generating it.
- [ ] health endpoint works.

### Auth/security

- [ ] hospital user can log in.
- [ ] bank user can log in.
- [ ] JWT protects business endpoints.
- [ ] organization boundary cannot be bypassed with IDs.
- [ ] passwords are BCrypt hashes.

### Inventory

- [ ] bank can read its inventory.
- [ ] bank can update its available units.
- [ ] free units are derived correctly.
- [ ] invalid/negative units are rejected.

### Request/matching

- [ ] hospital creates a request.
- [ ] exact blood group/component matching works.
- [ ] full-capacity candidates rank before partial candidates.
- [ ] distance ranking is deterministic.
- [ ] hospital can select a provider.

### Fulfilment

- [ ] selected bank sees the request.
- [ ] other bank cannot accept it.
- [ ] acceptance atomically reserves stock.
- [ ] insufficient stock returns 409.
- [ ] status transitions are enforced.
- [ ] cancellation releases reservation.
- [ ] delivery consumes inventory and reservation exactly once.

### Frontend collaboration

- [ ] Swagger accurately documents all P0 endpoints.
- [ ] Bearer auth works in Swagger UI.
- [ ] frontend engineer has base URL and enum values.

### Deployment

- [ ] deployed API boots.
- [ ] deployed DB migrations succeed.
- [ ] deployed health is UP.
- [ ] CORS permits frontend.
- [ ] full live flow succeeds on deployed environment.

---

## 33. Code-quality constraints for Codex

Codex should follow these preferences unless existing code dictates otherwise:

- constructor injection only;
- avoid field injection;
- keep controllers thin;
- transactions at service/application layer;
- use `record` for immutable API DTOs when convenient;
- use `Instant` for timestamps;
- use `UUID` IDs;
- use `@Enumerated(EnumType.STRING)`;
- explicit unique/index database constraints in Flyway;
- no `ddl-auto=update`;
- no `Optional.get()` without guard;
- no catch-all exception swallowing;
- no magic string roles scattered throughout code;
- no returning `null` where an explicit optional/not-found behavior is clearer;
- no entity exposure over API;
- no business logic in mappers;
- no unnecessary generic repository/service abstractions;
- no Lombok `@Data` on JPA entities by default;
- be careful with JPA entity equality/hashcode;
- avoid eager-loading large collections by default;
- keep JSON recursion impossible through DTO mapping.

---

## 34. What Codex should ask before changing

Codex should pause and ask the user before:

- changing the agreed Java/Spring/Postgres stack;
- introducing a new infrastructure service;
- switching from monolith to microservices;
- adding a third-party paid API;
- changing the product's blood-matching semantics;
- implementing blood-group compatibility substitutions;
- adding patient clinical data beyond a simple non-identifying request reference/notes field;
- replacing Flyway with Hibernate schema generation;
- introducing a breaking frontend API contract after integration has begun.

For normal implementation details inside the spec, Codex should proceed without unnecessary clarification.

---

## 35. Suggested first Codex execution prompt

After placing `AGENTS.md` and this file in the repository, the first implementation instruction can be:

```text
Read AGENTS.md and docs/HEMOGRID_BACKEND_BUILD_SPEC.md completely.

We are starting the HemoGrid backend from the current repository state.
Implement Phase 0 only: bootstrap the Java 21 / Spring Boot 4.1.x Maven monolith, add the agreed dependencies, Docker Compose PostgreSQL infrastructure, application.yaml configuration, Flyway bootstrap migration, Actuator health endpoint, and springdoc Swagger UI.

Do not implement domain entities or authentication yet.
Inspect the repository before changing anything. Then implement Phase 0, run the application/tests you can run, fix failures, and report the files changed and the exact commands for me to verify locally.
```

Then move phase-by-phase instead of asking the agent to build the entire product in one uncontrolled prompt.

---

## 36. Final product principle

The backend exists to make one experience undeniable:

```text
Hospital needs blood
        -> HemoGrid finds an eligible blood bank
        -> hospital requests it
        -> blood bank accepts it
        -> inventory is safely reserved
        -> both sides see the request progress
        -> delivery finalizes inventory
```

Everything that makes this flow more reliable is valuable.
Everything that distracts from shipping this flow within 72 hours is secondary.
