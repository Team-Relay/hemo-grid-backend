# HemoGrid Frontend API Handoff

This is the frontend-facing contract for the current backend milestone. Keep this file open while building the VS Code frontend.

## Backend URLs

Local API:

```text
http://localhost:8080
```

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

Health check:

```text
GET http://localhost:8080/actuator/health
```

## Demo Users

Hospital:

```text
email: hospital.demo@hemogrid.local
password: HospitalDemo123!
```

Blood bank:

```text
email: bank.demo@hemogrid.local
password: BankDemo123!
```

Platform admin:

```text
email: admin.demo@hemogrid.local
password: AdminDemo123!
```

## Auth

Login:

```http
POST /api/v1/auth/login
Content-Type: application/json
```

Request:

```json
{
  "email": "hospital.demo@hemogrid.local",
  "password": "HospitalDemo123!"
}
```

Response:

```json
{
  "accessToken": "jwt-token",
  "tokenType": "Bearer",
  "expiresIn": 28800,
  "user": {
    "id": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
    "fullName": "Demo Hospital User",
    "email": "hospital.demo@hemogrid.local",
    "role": "HOSPITAL_STAFF",
    "organization": {
      "id": "11111111-1111-1111-1111-111111111111",
      "name": "Central Care Hospital",
      "type": "HOSPITAL"
    }
  }
}
```

Use this header for every protected endpoint:

```http
Authorization: Bearer <accessToken>
```

Current user:

```http
GET /api/v1/auth/me
Authorization: Bearer <accessToken>
```

Current user's organization:

```http
GET /api/v1/organizations/me
Authorization: Bearer <accessToken>
```

## Enums

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

Request urgency:

```text
ROUTINE
URGENT
CRITICAL
```

Request status:

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

Provider status update targets:

```text
PREPARING
IN_TRANSIT
DELIVERED
```

## Hospital Flow

Create a blood request:

```http
POST /api/v1/blood-requests
Authorization: Bearer <hospitalToken>
Content-Type: application/json
```

Request:

```json
{
  "bloodGroup": "O_NEGATIVE",
  "component": "RED_CELLS",
  "unitsRequired": 3,
  "urgency": "CRITICAL",
  "clinicalReference": "ER-2026-0821-001",
  "notes": "Emergency request"
}
```

Response status: `201 Created`

Response shape:

```json
{
  "id": "generated-request-id",
  "requester": {
    "id": "11111111-1111-1111-1111-111111111111",
    "name": "Central Care Hospital",
    "type": "HOSPITAL"
  },
  "provider": null,
  "bloodGroup": "O_NEGATIVE",
  "component": "RED_CELLS",
  "unitsRequired": 3,
  "urgency": "CRITICAL",
  "status": "REQUESTED",
  "clinicalReference": "ER-2026-0821-001",
  "notes": "Emergency request",
  "requestedAt": "2026-08-21T10:00:00Z",
  "acceptedAt": null,
  "preparingAt": null,
  "dispatchedAt": null,
  "deliveredAt": null,
  "cancelledAt": null,
  "updatedAt": "2026-08-21T10:00:00Z"
}
```

List hospital request history:

```http
GET /api/v1/blood-requests
Authorization: Bearer <hospitalToken>
```

Get one request:

```http
GET /api/v1/blood-requests/{requestId}
Authorization: Bearer <hospitalToken>
```

Get ranked candidates:

```http
GET /api/v1/blood-requests/{requestId}/candidates
Authorization: Bearer <hospitalToken>
```

Response shape:

```json
{
  "requestId": "generated-request-id",
  "candidates": [
    {
      "organizationId": "22222222-2222-2222-2222-222222222222",
      "organizationName": "Maitama Blood Centre",
      "bloodGroup": "O_NEGATIVE",
      "component": "RED_CELLS",
      "unitsFree": 5,
      "distanceKm": 2.83,
      "canFullyFulfil": true,
      "rank": 1
    }
  ]
}
```

Select a provider:

```http
POST /api/v1/blood-requests/{requestId}/select-provider
Authorization: Bearer <hospitalToken>
Content-Type: application/json
```

Request:

```json
{
  "providerOrganizationId": "22222222-2222-2222-2222-222222222222"
}
```

Cancel a request:

```http
POST /api/v1/blood-requests/{requestId}/cancel
Authorization: Bearer <hospitalToken>
```

Cancellation is allowed while the request is `REQUESTED`, `ACCEPTED`, or `PREPARING`. If inventory was reserved, cancellation releases it.

## Blood Bank Flow

List requests assigned to the authenticated blood bank:

```http
GET /api/v1/provider/requests
Authorization: Bearer <bankToken>
```

Get one assigned request:

```http
GET /api/v1/provider/requests/{requestId}
Authorization: Bearer <bankToken>
```

Accept and reserve inventory:

```http
POST /api/v1/provider/requests/{requestId}/accept
Authorization: Bearer <bankToken>
```

Decline:

```http
POST /api/v1/provider/requests/{requestId}/decline
Authorization: Bearer <bankToken>
```

Progress status:

```http
POST /api/v1/provider/requests/{requestId}/status
Authorization: Bearer <bankToken>
Content-Type: application/json
```

Request:

```json
{
  "status": "PREPARING"
}
```

Valid progression:

```text
ACCEPTED -> PREPARING -> IN_TRANSIT -> DELIVERED
```

Delivery consumes reserved inventory.

## Inventory

List inventory for the authenticated blood bank:

```http
GET /api/v1/inventory
Authorization: Bearer <bankToken>
```

Response item shape:

```json
{
  "id": "30000000-0000-0000-0000-000000000008",
  "bloodGroup": "O_NEGATIVE",
  "component": "RED_CELLS",
  "unitsAvailable": 5,
  "unitsReserved": 0,
  "unitsFree": 5
}
```

Update available units:

```http
PATCH /api/v1/inventory/{inventoryId}/units
Authorization: Bearer <bankToken>
Content-Type: application/json
```

Request:

```json
{
  "unitsAvailable": 9
}
```

`PUT /api/v1/inventory/{inventoryId}` accepts the same body.

## Error Shape

All handled errors use this shape:

```json
{
  "timestamp": "2026-08-21T10:00:00Z",
  "status": 409,
  "error": "CONFLICT",
  "message": "Request cannot be cancelled in its current status."
}
```

Validation errors currently return a generic message:

```json
{
  "timestamp": "2026-08-21T10:00:00Z",
  "status": 400,
  "error": "BAD_REQUEST",
  "message": "Request validation failed."
}
```

## Frontend Polling Guidance

Realtime WebSocket is not implemented yet. For the first frontend build, use polling:

Hospital request detail page:

```text
GET /api/v1/blood-requests/{requestId}
```

Blood bank dashboard:

```text
GET /api/v1/provider/requests
```

Polling every 3 to 5 seconds is enough for the hackathon demo.

## Demo Happy Path

1. Login as the hospital user.
2. Create an `O_NEGATIVE / RED_CELLS / 3 units / CRITICAL` request.
3. Read candidates from `/api/v1/blood-requests/{requestId}/candidates`.
4. Select `Maitama Blood Centre` or another full-match candidate.
5. Login as the blood-bank user.
6. Load assigned provider requests.
7. Accept the selected request.
8. Move status to `PREPARING`.
9. Move status to `IN_TRANSIT`.
10. Move status to `DELIVERED`.
11. Confirm the hospital request status and blood-bank inventory changed.

