# HemoGrid frontend API handoff

This document describes the current Java controllers and outbound DTOs. Check the deployed OpenAPI document before frontend integration. The application is a synthetic demonstration, not a validated clinical decision system.

## Environments and smoke verification

- Local API: `http://localhost:8080`
- Deployed API: `https://hemo-grid-backend.onrender.com`
- Swagger: `/swagger-ui.html`; OpenAPI: `/v3/api-docs`
- Liveness: `/actuator/health`; readiness: `/actuator/health/readiness`

Run the non-mutating [smoke test](../scripts/smoke_deployed_backend.py) from a network-connected machine with `python3 scripts/smoke_deployed_backend.py`. Override with `HEMOGRID_API_BASE`. Set `HEMOGRID_SMOKE_EMAIL` and `HEMOGRID_SMOKE_PASSWORD` only for synthetic demo credentials to additionally verify login and authenticated reads. This script never changes requests or stock. Passing repository CI is **not** proof of a successful hosted deployment.

## Authentication and roles

`POST /api/v1/auth/login` with `{"email":"...","password":"..."}` returns `accessToken`, `tokenType`, `expiresIn` (seconds), and `user`. The user has `id`, `fullName`, `email`, `role` and `organization` (nullable), whose summary has `id`, `name` and `type`. Use `Authorization: Bearer <accessToken>` for protected endpoints. `GET /api/v1/auth/me` returns the current user, and `GET /api/v1/organizations/me` returns an organization-bound user's facility.

Synthetic demonstration accounts: hospital `hospital.demo@hemogrid.local` / `HospitalDemo123!`; Nairobi bank `bank.demo@hemogrid.local` / `BankDemo123!`; Embu bank `embu.bank@hemogrid.local` / `BankDemo123!`; coordinator `admin.demo@hemogrid.local` / `AdminDemo123!`. Other bank accounts are listed in the root README. Demo passwords must never be used with real-world patient or facility data.

## Enums and requests

- Blood groups: `A_POSITIVE`, `A_NEGATIVE`, `B_POSITIVE`, `B_NEGATIVE`, `AB_POSITIVE`, `AB_NEGATIVE`, `O_POSITIVE`, `O_NEGATIVE`.
- Components: `WHOLE_BLOOD`, `RED_CELLS`, `PLATELETS`, `PLASMA`. The validated hackathon evaluation concerns red cells.
- Urgency: `ROUTINE`, `URGENT`, `CRITICAL`.
- Status: `REQUESTED`, `ACCEPTED`, `PREPARING`, `IN_TRANSIT`, `DELIVERED`, `DECLINED`, `CANCELLED`, `EXPIRED`.

### Hospital officer endpoints

| Method | Path | Function |
| --- | --- | --- |
| GET | `/api/v1/blood-requests` | List own hospital's requests |
| POST | `/api/v1/blood-requests` | Create a request, HTTP 201 |
| GET | `/api/v1/blood-requests/{requestId}` | Read an owned request |
| GET | `/api/v1/blood-requests/{requestId}/candidates` | Get ranked candidate evidence |
| POST | `/api/v1/blood-requests/{requestId}/select-provider` | Select/change a provider while REQUESTED |
| POST | `/api/v1/blood-requests/{requestId}/cancel` | Cancel REQUESTED, ACCEPTED, or PREPARING request |

Create request example (synthetic): 

~~~json
{"bloodGroup":"O_NEGATIVE","component":"RED_CELLS","unitsRequired":3,"urgency":"CRITICAL","unknownPatientBloodGroup":false,"clinicalReference":"SYNTHETIC-DEMO-001","notes":"Demonstration only"}
~~~

`unitsRequired` must be 1–20. Null/unspecified patient group has a separate `unknownPatientBloodGroup` pathway. Use `{"providerOrganizationId":"<candidate-UUID>"}` to select. Do not treat displayed candidate stock as a guarantee; selection and acceptance recheck inventory.

`BloodRequestResponse` fields: `id`; `requester` and `provider` (nullable objects with `id`, `name`, `type`); `bloodGroup`; `allocatedBloodGroup`; `component`; `unitsRequired`; `urgency`; `status`; `unknownPatientBloodGroup`; `matchType`; `estimatedTransitHours`; `coldChainVerified`; `clinicalReference`; `notes`; `requestedAt`; `acceptedAt`; `preparingAt`; `dispatchedAt`; `deliveredAt`; `cancelledAt`; `updatedAt`.

Candidate response is `{"requestId":"<UUID>","candidates":[...]}`. Each candidate has `organizationId`, `organizationName`, `bloodGroup`, `offeredBloodGroup`, `component`, `matchType`, `unitsFree`, `distanceKm`, `estimatedTransitHours`, `coldChainVerified`, `fridgeTempCelsius`, `quarantinedUnitsExcluded`, `conservationRuleApplied`, `recommendationReasons` (string array), `canFullyFulfil` and `rank`. Distances/transport estimates can be null. Explain that these are synthetic estimates and recorded safety checks, not confirmed clinical suitability or measured ETAs.

### Blood-bank officer endpoints

| Method | Path | Function |
| --- | --- | --- |
| GET | `/api/v1/provider/requests` | Assigned requests only |
| GET | `/api/v1/provider/requests/{requestId}` | Assigned request |
| POST | `/api/v1/provider/requests/{requestId}/accept` | Confirm and reserve matching stock |
| POST | `/api/v1/provider/requests/{requestId}/decline` | Decline a pending request |
| POST | `/api/v1/provider/requests/{requestId}/status` | `{"status":"PREPARING"}` then IN_TRANSIT then DELIVERED |
| GET | `/api/v1/inventory` | Own bank's inventory |
| PATCH | `/api/v1/inventory/{inventoryId}/units` | `{"unitsAvailable":9}` |
| PUT | `/api/v1/inventory/{inventoryId}` | Same update payload |

Inventory fields: `id`, `bloodGroup`, `component`, `unitsAvailable`, `unitsReserved`, `unitsFree`, `quarantinedTempExcursionUnits`, `fridgeTempCelsius`, `coldChainStatus`, `expiringWithin7DaysUnits`, `dispatchableUnitsFree`, `coldChainVerified`, `updatedAt`. Display `dispatchableUnitsFree` as usable stock, not merely `unitsFree`. Delivery consumes the reserved units. Invalid transition, insufficient stock and cold-chain hold produce HTTP 409; refresh and reconcile the request rather than retrying automatically.

### Platform coordinator dispatch planning

Only a `PLATFORM_ADMIN` can call `GET /api/v1/dispatch-plans/requests` (up to 100 pending critical red-cell requests), or `POST /api/v1/dispatch-plans/preview` with:

~~~json
{"requestIds":["<request-UUID-1>","<request-UUID-2>"],"staffApproved":true}
~~~

The coordinator must actually obtain staff approval before asserting this Boolean; it is not a signed approval record. Choose 2–5 distinct requests. The preview is advisory and does **not** reserve stock. Hospital approval/selection and the assigned bank's acceptance remain separate actions.

The `DispatchPlanResponse` fields are `generatedAt`, `status`, `totalEstimatedTransitHours` (nullable), `visitedNodes`, `nearestEligibleBaseline`, `proposals` and `notes`. Status is `OPTIMAL`, `FEASIBLE`, `INFEASIBLE` or `LIMIT_REACHED`. Baseline plan has `status`, `assignments` (each `orderId`/`stockId`/`transitHours`), `totalTransitHours`, `visitedNodes`. Complete proposals include `requestId`, `requesterOrganizationId`, `providerOrganizationId`, `providerName`, `offeredBloodGroup`, `inventoryId`, `inventoryVersion`, `unitsRequired`, `dispatchableUnitsAtPlanning`, `unitsRemainingAfterWholePlan`, `estimatedTransitHours`, `recordedFridgeTempCelsius`, `inventoryUpdatedAt` and `rationale`. Never render an incomplete result as an approved allocation. See [dispatch planning details](DISPATCH_PLANNING.md).

## Errors and frontend behavior

Handled errors have `timestamp`, `status`, `error`, `code`, `message`, `path` and `fieldErrors` (each `field`/`message`). Validation uses `VALIDATION_FAILED`, HTTP 400. Handle 401 by signing in, 403 as insufficient permission, 404 as missing/inaccessible, 409 as conflict and 5xx as backend unavailable. `COLD_CHAIN_HOLD` is a specific 409 condition.

There is no WebSocket stream. Poll the hospital request detail and the blood-bank assignment list every 3–5 seconds while the page is active, with backoff on failures. Disable action buttons while requests are in flight, and refresh authoritative state after mutation. No automatic retries of mutations.

## Demo progression

Coordinator previews two staff-approved pending requests and explains shared stock versus the nearest-eligible baseline. A hospital officer selects an eligible supplier; that bank accepts, showing its reservation. The bank progresses PREPARING → IN_TRANSIT → DELIVERED; the hospital sees the same lifecycle. Demonstrate a stock or quarantine conflict using synthetic demo conditions only. Never claim clinical validation, live inventory ingestion, calibrated transport time or actual patient outcomes.

The Java DTOs/controllers and generated OpenAPI are authoritative when they differ from examples in this document.
