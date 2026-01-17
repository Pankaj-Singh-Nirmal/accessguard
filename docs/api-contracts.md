# AccessGuard API Contracts

This document defines REST API contracts for:
- `accessguard-core` (pass management + access validation)
- `accessguard-audit` (immutable audit query)

It also defines Kafka event contracts between services.

## Conventions

### Base Path
- All REST endpoints are under: `/api/v1`

### Authentication and Authorization
- Auth is JWT (OAuth2 Resource Server).
- Tenant scope is mandatory:
  - Admin/Security requests: `tenantId` derived from JWT claim (e.g., `tid`)
  - Device requests: `tenantId` derived from device identity (and enforced in core)

Roles (high level):
- `TENANT_ADMIN`: pass management and administrative endpoints
- `ACCESS_DEVICE`: submit access attempts only
- `SECURITY`: read-only audit queries

### Identifier Naming Rules (Important)
To avoid any ambiguity between internal database IDs and business identifiers:

- **Codes are used at service boundaries (REST + Kafka):**
    - `doorCode` (e.g., `DOOR-A1`) — maps to `doors.door_code`
    - `zoneCode` (e.g., `ZONE-BLDG-A-F3`) — maps to `zones.zone_code`
    - `deviceCode` (e.g., `DEV-F3-READER-01`) — maps to `devices.device_code`
    - `passCode` (e.g., `AG-7F3K-P9D2-1XQ8`) — maps to `passes.pass_code`

- **UUIDs are internal and not exposed for doors/zones/devices:**
    - Core persists `door_id`, `zone_id`, `device_id` as UUID foreign keys.
    - Audit persists `door_code`, `zone_code`, `device_code` as strings for queryability.

- `passId` (UUID) is returned by pass management APIs and is used as a stable reference (`passRef`) in audit.

### Correlation
- Optional header: `X-Correlation-Id`
- If provided, services should:
    - include it in logs
    - propagate it to Kafka event envelope (where applicable)

### Error Model (non-device endpoints)
- Use **Problem Details for HTTP APIs** (RFC 9457) via Spring `ProblemDetail`.
- Include:
    - standard fields: `type`, `title`, `status`, `detail`, `instance`
    - extensions:
        - `errorCode` (string)
        - `violations` for validation errors: list of `{field, message}`

Device endpoint exception:
- Device access validation returns `200 OK` with `decision` + `reasonCode` for business outcomes
- `4xx` is reserved for malformed requests / auth failures only

---

## accessguard-core REST APIs

### 1) Pass Management (Role: TENANT_ADMIN)

#### Create Pass
- `POST /passes`

Request body (server generates `passCode` by default):
```json
{
  "visitorRef": "John Doe",
  "validFrom": "2026-01-11T10:00:00Z",
  "validTo": "2026-01-11T14:00:00Z",
  "scope": {
    "doorCodes": ["DOOR-A1", "DOOR-A2"],
    "zoneCodes": ["ZONE-BLDG-A-F3"]
  }
}
```

Response:

- 201 Created
```json
{
  "passId": "7c91e1b2-4d3a-42c3-9e91-abc123abc123",
  "passCode": "AG-7F3K-P9D2-1XQ8",
  "status": "ACTIVE",
  "visitorRef": "John Doe",
  "validFrom": "2026-01-11T10:00:00Z",
  "validTo": "2026-01-11T14:00:00Z",
  "scope": {
    "doorCodes": ["DOOR-A1", "DOOR-A2"],
    "zoneCodes": ["ZONE-BLDG-A-F3"]
  },
  "createdAt": "2026-01-11T09:55:00Z"
}
```

Errors:

- 400 validation (invalid window, invalid codes)

- 401/403 auth/forbidden

Notes:

- All identifiers are tenant-scoped.

- passCode is immutable after issuance (recommended).

- Actor/audit fields such as createdBy, updatedBy exist in persistence but are not exposed via this API contract.

#### Get Pass by ID


- GET /passes/{passId}

Responses:

- 200 OK

- 404 Not Found (tenant-scoped; no cross-tenant leakage)

- 401/403

#### Search Passes

- GET /passes?passCode=...&status=...&validFromFrom=...&validToTo=...&page=0&size=20&sort=createdAt,desc

Responses:

- 200 OK (paginated)

- 400 invalid filters

#### Update Pass (validity and/or scope)

- PATCH /passes/{passId}

Request:
```json
{
  "validFrom": "2026-01-11T10:30:00Z",
  "validTo": "2026-01-11T15:00:00Z",
  "scope": {
    "doorCodes": ["DOOR-A1"],
    "zoneCodes": ["ZONE-BLDG-A-F3"]
  }
}
```

Responses:

- 200 OK

- 404 Not Found

- 409 Conflict lifecycle rules (e.g., cannot update revoked)

#### Revoke Pass

- POST /passes/{passId}/revoke

Request:
```json
{
  "reason": "Contract ended early"
}
```

Responses:

- 200 OK or 204 No Content

- 404 Not Found

- 409 Conflict optional (policy); idempotent 200 is acceptable

### 2) Access Attempt Validation (Role: ACCESS_DEVICE)
#### Submit Access Attempt (Idempotent)

- POST /access-attempts

Headers:

- X-Correlation-Id (optional)

- Idempotency-Key (optional)

  - At least one of attemptId (body) or Idempotency-Key (header) must exist

  - Preferred: attemptId in body (stable)

Request:
```json
{
  "attemptId": "a3c8a6e1-1f4d-4f5b-9e0c-61d9c0a1b123",
  "doorCode": "DOOR-A1",
  "passCode": "AG-7F3K-P9D2-1XQ8",
  "occurredAt": "2026-01-11T10:05:12Z"
}
```

Response (always 200 OK for well-formed authenticated requests):
```json
{
  "attemptId": "a3c8a6e1-1f4d-4f5b-9e0c-61d9c0a1b123",
  "decision": "GRANTED",
  "reasonCode": "OK",
  "evaluatedAt": "2026-01-11T10:05:12Z",
  "validUntil": "2026-01-11T14:00:00Z"
}
```

Denied example:
```json
{
  "attemptId": "a3c8a6e1-1f4d-4f5b-9e0c-61d9c0a1b123",
  "decision": "DENIED",
  "reasonCode": "PASS_EXPIRED_OR_NOT_YET_VALID",
  "evaluatedAt": "2026-01-11T18:05:12Z"
}
```

Status codes:

- 200 OK for business outcomes (GRANTED/DENIED)

- 400 Bad Request malformed request (missing required fields; neither attemptId nor Idempotency-Key)

- 401/403 authentication/role issues

Reason codes (minimum set):

- OK

- DEVICE_NOT_ALLOWED

- DOOR_NOT_FOUND

- PASS_NOT_FOUND

- PASS_REVOKED

- PASS_EXPIRED_OR_NOT_YET_VALID

- OUT_OF_SCOPE

Idempotency rule:

- Duplicate submissions with the same (tenantId, attemptId) (or (tenantId, Idempotency-Key)) must return the same response body (decision + reasonCode + evaluatedAt policy).

---

## accessguard-audit REST APIs
### 1) Audit Query (Role: SECURITY)
#### Search Access Attempts

- GET /audit/access-attempts

Query parameters (all optional except pagination):

- from (ISO timestamp)

- to (ISO timestamp)

- decision (GRANTED|DENIED)

- doorCode (maps to audit projection door_code)

- zoneCode (maps to audit projection zone_code)

- deviceCode (maps to audit projection device_code)

- attemptId

- passRef (maps to audit projection pass_ref; typically core passId when known)

- page, size, sort

Response:

- 200 OK paginated list of immutable audit records

Errors:

- 400 invalid filters (e.g., from > to, invalid enum)

- 401/403 auth/forbidden

Notes:

- Tenant isolation is enforced via tenantId from JWT.

- Sorting default recommendation: evaluatedAt desc.

---

## Kafka Event Contracts (Core → Audit)

Kafka is the authoritative integration for the audit trail.

### Topic

- accessguard.events

### Event Envelope (standard)

- All events published by core must include:

```json
{
  "eventId": "c1b0c0a2-9d6f-4c1d-9b8a-7d3f0a2f7a11",
  "eventType": "accessguard.access_attempt_recorded",
  "schemaVersion": 1,
  "tenantId": "TENANT-123",
  "occurredAt": "2026-01-11T10:05:12Z",
  "correlationId": "corr-20260111-100512-001",
  "payload": { }
}
```

Field semantics:

- eventId: unique identifier for consumer idempotency (primary deduplication key)

- eventType: stable string identifier

- schemaVersion: integer for schema evolution; audit persists this in audit_events.schema_version

- tenantId: mandatory; consumers must enforce tenant-scoped persistence

- occurredAt: time of the access attempt occurrence (device-provided or normalized)

- correlationId: optional but recommended for tracing

### access_attempt_recorded Payload v1

```json
{
  "attemptId": "a3c8a6e1-1f4d-4f5b-9e0c-61d9c0a1b123",
  "deviceCode": "DEV-F3-READER-01",
  "doorCode": "DOOR-A1",
  "zoneCode": "ZONE-BLDG-A-F3",
  "passRef": "7c91e1b2-4d3a-42c3-9e91-abc123abc123",
  "decision": "GRANTED",
  "reasonCode": "OK",
  "occurredAt": "2026-01-11T10:05:12Z",
  "evaluatedAt": "2026-01-11T10:05:12Z"
}
```

Notes:

- deviceCode, doorCode, zoneCode are business identifiers for query-friendly audit persistence.

- passRef is a stable reference used by audit:

  - when the pass exists: passRef = passId (UUID from core)

  - when not found: passRef = null and reasonCode = PASS_NOT_FOUND

- Audit stores immutable records derived from this payload.

### Audit Consumer Idempotency Rules

Audit must deduplicate using:

- Primary: eventId

- Secondary: (tenantId, attemptId) where applicable

---

## Sync Integration (Feign) Policy

Feign is allowed for internal, non-authoritative calls only.

Strict rules:

- Access attempt decisioning must not depend on audit via Feign.

- Kafka is the authoritative path for audit persistence.

- Feign failures must not block core’s device API availability.

---

## Versioning and Compatibility
### REST APIs

- Versioned via /api/v1

- Backward-compatible changes allowed within a version:

  - adding optional fields

  - adding endpoints

- Breaking changes require /api/v2

### Kafka Events

- Backward-compatible changes preferred:

  - add optional fields

- Breaking changes require:

  - incrementing schemaVersion

  - consumer compatibility plan (support v1 and v2 concurrently for a period)