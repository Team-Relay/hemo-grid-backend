# Backend code review guide

1. Start with Flyway migrations and domain entities to understand persisted
   constraints and the request state machine.
2. Review repository lock queries before application services; the lock order
   explains the concurrency guarantees.
3. Follow `BloodRequestService` for the hospital workflow, then
   `ProviderRequestService` for reservation, fulfilment, cancellation, and
   delivery accounting.
4. Compare API records and controllers with the frontend
   `lib/api/backend-types.ts` and `docs/API_MAPPING.md`.
5. Review `SecurityConfig`, `JwtConfig`, and `GlobalExceptionHandler` as one
   public security/error boundary.
6. Read integration tests last to see each cross-module invariant exercised
   against PostgreSQL.

Every Java, YAML, container, build, and mutable test-SQL file has a concise
purpose comment. Method comments are reserved for security, transaction,
concurrency, or mapping behavior that is not obvious from the signature.
Already-applied Flyway migrations are the deliberate exception: even comment
edits change their checksums, so they remain byte-for-byte immutable. Generated
Maven wrapper files and binary artifacts are also not hand-commented.
