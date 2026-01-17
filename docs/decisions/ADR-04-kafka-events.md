# ADR-0004: Kafka as Authoritative Audit Integration

## Status
Accepted

## Context
We need an immutable audit trail without making audit availability a dependency for device validation latency and reliability.

## Decision
Kafka is the authoritative integration for the audit trail:

- Core publishes `accessguard.access_attempt_recorded` events after persisting the decision.
- Audit consumes events and persists immutable audit records.

Synchronous integration (Feign) is allowed only for non-authoritative, non-critical calls (e.g., status-style checks). Core must not depend on audit synchronously to return a device decision.

## Consequences
### Positive
- Device validation stays low-latency and resilient
- Clear eventual consistency boundary
- Audit is decoupled from the validation critical path

### Trade-offs
- Requires consumer idempotency and retry/DLQ concept
- Requires event schema discipline and versioning awareness

## Alternatives Considered
- **Synchronous write to audit**: immediate consistency but critical-path coupling.
- **Dual-write (core DB + audit DB)**: complex failure handling and compensation.
- **Outbox pattern**: strong reliability, but more moving parts; can be added later if needed.
