# ADR-0001: Service Boundaries (core vs audit)

## Status
Accepted

## Context
AccessGuard requires:
- deterministic access decisioning for devices
- an immutable audit trail with query capability
- tenant isolation across all flows
  We want clear ownership boundaries and a realistic service model without unnecessary operational complexity.

## Decision
We split the system into two services:

### accessguard-core
- Source of truth for:
    - pass lifecycle (create/update/revoke)
    - access attempt decisioning (GRANTED/DENIED + reasonCode)
    - publishing authoritative access attempt events
- Owns the core operational model and tables.

### accessguard-audit
- Consumes authoritative access attempt events
- Persists immutable audit records
- Provides read-only audit query APIs
- Owns audit projection tables.

## Consequences
### Positive
- Clear ownership boundaries and data encapsulation
- Event-driven audit trail with eventual consistency
- Core remains low-latency for device validation

### Trade-offs
- Requires messaging infrastructure and consumer idempotency
- Audit queries are eventually consistent relative to device responses

## Alternatives Considered
- **Single service**: simpler deployment, but weaker boundary clarity and less realistic separation of concerns.
- **Audit reads core DB directly**: reduces duplication, but violates data ownership and creates tight coupling.
- **Synchronous write to audit**: stronger immediate consistency, but makes audit availability part of the critical path.
