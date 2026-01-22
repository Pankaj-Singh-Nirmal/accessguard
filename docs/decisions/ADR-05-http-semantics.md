# ADR-0005: HTTP Semantics and Error Handling

## Status
Accepted

## Context
We need secure device-facing semantics (avoid information leakage) and a consistent error model for admin/audit APIs.

## Decision

### Device validation endpoint
For `POST /access-attempts`:
- Return `200 OK` for well-formed authenticated requests.
- Convey business outcome in body:
    - decision: GRANTED/DENIED
    - reasonCode: e.g., PASS_NOT_FOUND, DOOR_NOT_FOUND, OUT_OF_SCOPE
- Use `4xx` only for:
    - malformed payload (400)
    - authentication/authorization failures (401/403)

### Non-device endpoints
For admin and audit APIs:
- Use **Problem Details for HTTP APIs (RFC 9457)** via Spring `ProblemDetail`.
- Include `errorCode` and `violations` extensions for validation failures.
- If the JWT is valid but the `tenantId` claim is missing or blank, respond with **401 Unauthorized** (token is not acceptable for tenant-scoped APIs).

## Consequences
### Positive
- Security-friendly device API (reduced probing risk)
- Modern, standardized error responses for admin/audit
- Stable reasonCode taxonomy for operational troubleshooting

### Trade-offs
- Device clients must interpret decision bodies, not status codes, for business outcomes

## Alternatives Considered
- **Use 404/403 for domain outcomes on device API**: increases information leakage risk.
- **Return 403 for denied access attempts**: confuses authorization errors with business denial decisions.
- **Custom error JSON everywhere**: less interoperable than Problem Details.
