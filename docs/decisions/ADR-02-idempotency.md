# ADR-0002: Idempotency Strategy (attemptId and eventId)

## Status
Accepted

## Context
Devices may retry requests due to timeouts/network issues. Kafka consumption assumes at-least-once delivery. We need deterministic behavior under retries and safe event reprocessing.

## Decision

### Core idempotency
- Device sends a stable `attemptId` per physical attempt.
- Core enforces uniqueness on `(tenantId, attemptId)`.
- Duplicate submissions return the same response body:
    - decision
    - reasonCode
    - evaluatedAt (policy-defined stability)

### Audit idempotency
- Events include a globally unique `eventId`.
- Audit deduplicates primarily by `eventId`.
- Secondary deduplication may use `(tenantId, attemptId)`.

## Consequences
### Positive
- Safe retries for devices
- Safe reprocessing for audit consumer (no duplicate audit records)
- Predictable behavior under failure and retry conditions

### Trade-offs
- Requires DB constraints and careful persistence ordering
- Requires event envelope standardization

## Alternatives Considered
- **Rely on client-side retries only (no server idempotency)**: leads to duplicated attempts and inconsistent audit.
- **Idempotency-Key header only**: workable, but `attemptId` in payload is clearer for devices and events.
- **Exactly-once processing**: significantly more complex and not required for correctness here.
