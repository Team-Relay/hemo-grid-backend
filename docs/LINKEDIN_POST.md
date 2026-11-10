# LinkedIn post draft

## Ready-to-publish version

I have been building **HemoGrid**, a full-stack platform for coordinating blood
requests between hospitals and blood-bank providers.

I did not want the backend to be just another CRUD API. The interesting part of
this problem is protecting the workflow when real business rules collide:

- A hospital should only see its own requests.
- A blood bank should only see requests assigned to it.
- Provider matching should consider the exact blood group and component,
  available capacity, and distance.
- Two concurrent actions must not reserve the same stock.
- Inventory must be reserved on acceptance, released on eligible cancellation,
  and consumed only after delivery.
- Every request must follow valid transitions from `REQUESTED` through
  fulfilment.

To model this, I built the backend as a **Java 21 / Spring Boot modular
monolith**, with domain-oriented modules for authentication, organizations,
inventory, matching, and blood requests.

Some engineering decisions I am especially happy with:

- ✅ Stateless JWT authentication with role and organization boundaries
- ✅ PostgreSQL transactions and pessimistic row locks for safe reservations
- ✅ A guarded request state machine instead of arbitrary status updates
- ✅ Flyway migrations with Hibernate schema validation
- ✅ A stable API error contract consumed by the Next.js frontend
- ✅ Springdoc OpenAPI documentation and Actuator health probes
- ✅ 20 PostgreSQL-backed integration tests, including concurrent acceptance

On the frontend, I am mapping the hospital and blood-bank journeys in Next.js
with intentional loading, empty, success, and error states—not just happy-path
screens.

The project is still in progress. The core hospital-to-blood-bank flow is now
working, while onboarding, deeper administrative workflows, notifications, and
deployment are the next milestones. The planned deployment stack is Vercel,
Render, and Neon PostgreSQL.

Building HemoGrid is helping me become more deliberate about domain modelling,
API contracts, concurrency, integration testing, and the boundary between a
backend and the UI that consumes it.

I am sharing the work publicly and would genuinely value feedback from backend
and full-stack engineers—especially on the module boundaries, inventory
reservation model, and API design.

Backend: https://github.com/dennisikechukwu/hemo-grid-backend

Frontend: https://github.com/dennisikechukwu/hemo-grid-frontend

#Java #SpringBoot #PostgreSQL #Nextjs #FullStackDevelopment #BuildInPublic

## Before publishing

- Attach two or three clear screenshots: the hospital dashboard, provider
  matching result, and blood-bank request/inventory view.
- If possible, include a short screen recording of the complete request flow;
  it will communicate more than an architecture screenshot alone.
- Add the live URL only after deployment and end-to-end verification.
- Keep the wording “in progress” until onboarding, production security review,
  observability, and deployment are complete.
- Reply to technical comments with the trade-offs you considered; that is often
  more valuable than presenting every decision as final.
