# Backend architecture

HemoGrid is a Java 21 Spring Boot modular monolith. Each feature is split into
`api`, `application`, `domain`, and `persistence` packages so HTTP contracts,
workflow orchestration, business state, and database access remain distinct.

## Module ownership

- `auth` authenticates users and maps JWT claims to application roles.
- `organization` owns the hospital/blood-bank tenant boundary.
- `inventory` owns bank stock and row-level reservation locks.
- `matching` rebuilds eligible candidates from current free inventory.
- `request` owns the blood-request state machine and both role workflows.
- `common.exception` owns the stable client-facing error envelope.
- `config` owns JSON, JWT, CORS, HTTP security, and OpenAPI configuration.

Controllers accept validated records and delegate to application services.
Services define transaction boundaries and enforce tenant access. Entities own
local invariants and lifecycle timestamps. Repositories expose only the queries
and locks required by those workflows.

## Request and inventory invariant

Provider acceptance acquires a pessimistic lock on the selected request before
checking status and reserving inventory. The matching inventory row is also
locked. Repeated acceptance is idempotent, while simultaneous calls cannot
reserve units twice. Cancellation releases reserved units and delivery consumes
them from available stock.

Flyway is the only schema-evolution mechanism; Hibernate runs with
`ddl-auto=validate` and must never create production tables implicitly.
