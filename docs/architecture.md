# AccessGuard Architecture

This document describes the high-level architecture of AccessGuard with focus on **service boundaries**, **data ownership**, **communication patterns**, **multi-tenancy**, and **idempotency**. It is the authoritative HLD reference for implementation.

---
## Scope

- Two microservices:
    - `accessguard-core` (source of truth for access decisions and pass management)
    - `accessguard-audit` (immutable audit log and query)
- Both services are tenant-aware and enforce **tenant isolation** at all layers.

---
## Architecture Documentation

### Diagrams index (HLD + LLD)

 - HLD

   - `docs/diagrams/hld/01-end-to-end-activity.puml` - end-to-end activity flow

 - LLD

   - `docs/diagrams/lld/01-validate-access-attempt-sequence.puml` - core validation + event publish + audit consume (async)

   - `docs/diagrams/lld/02-audit-query-sequence.puml` - security query path + filtering/pagination

   - `docs/diagrams/lld/03-core-domain-state-machine.puml` - pass lifecycle state machine

### Contracts and decisions links

- `docs/api-contracts.md` - REST + Kafka contracts (source of truth for interfaces)

- `docs/decisions/README.md` - Architecture Decision Record (ADR) index

---
## Service Boundaries and Responsibilities

### accessguard-core (Source of Truth)

Responsibilities:
- Pass lifecycle management:
    - Create / update / revoke passes
    - Generate and manage `passCode` (server-generated)
- Access decisioning:
    - Evaluate device access attempts deterministically
    - Produce `GRANTED` / `DENIED` and a stable `reasonCode`
- Publish authoritative events:
    - Emit `access_attempt_recorded` after persisting the attempt + decision

Non-responsibilities:
- Long-term audit query and retention policies (owned by audit service)
- Reporting/analytics beyond operational needs

### accessguard-audit (Immutable Log + Query)

Responsibilities:
- Consume access attempt events from Kafka
- Persist immutable audit records (append-only semantics)
- Provide read-only audit query APIs (filtering, pagination)

Non-responsibilities:
- Making access decisions
- Mutating pass state (no pass CRUD)

---
## Data Ownership

### Core-owned data (PostgreSQL: core DB)

Owned tables (illustrative, exact names per migrations):
- `passes` (pass aggregate root)
- `pass_scopes` (normalized scope associations)
    - `pass_scope_doors`
    - `pass_scope_zones`
- `devices` (device identity and tenant association)
- `doors` (door registry, tenant-scoped)
- `zones` (zone hierarchy, tenant-scoped)
- `access_attempts` (operational record of attempts and decisions; used for idempotency and traceability)

Ownership rules:
- `accessguard-core` is the **only writer** to core tables.
- Other services must not read core DB directly.

### Audit-owned data (PostgreSQL: audit DB)

Owned tables (illustrative):
- `audit_events` (event ingestion log, idempotency guard)
- `audit_access_attempts` (immutable audit projection / read model)

Ownership rules:
- `accessguard-audit` is the **only writer** to audit tables.
- Core does not read audit DB directly.

---
## Communication Model: Sync vs Async

### Asynchronous (Authoritative path): Kafka

Kafka is the authoritative mechanism for propagating access attempts to the audit service.

- Core publishes event after persisting the access attempt.
- Audit consumes and persists an immutable record.

Authoritative guarantees:
- The audit trail is derived from Kafka events, not from synchronous calls.
- Audit persistence is eventually consistent relative to the access attempt response.

### Synchronous (Non-critical path): OpenFeign

Feign is reserved for non-authoritative, non-critical-path internal calls. Examples:
- Ingestion status checks (optional operational endpoints)
- Liveness-style coordination if needed (not required for correctness)

Strict rule:
- The access attempt response must **not** depend on synchronous audit calls.
- If Feign is unavailable, access decisioning in core must continue unaffected.
- Internal calls are authenticated using service JWTs with role INTERNAL (mapped to ROLE_INTERNAL in Spring).

---
## Tenancy Model

### Tenant propagation

- Every request is tenant-scoped.
- Tenant context is established from authentication:
    - `tenantId` is derived from JWT claim `tenantId` for admins/security users
    - For devices, tenant context is derived from device identity, and then enforced in all lookups

Tenant enforcement rules:
- All reads/writes in core are filtered by `tenantId`.
- All reads/writes in audit are filtered by `tenantId`.
- Cross-tenant access must be impossible even if identifiers collide (e.g., same `doorId` in different tenants).

### Roles (high level)

- `TENANT_ADMIN`: pass management and administrative operations
- `ACCESS_DEVICE`: submit access attempts only
- `SECURITY`: query audit logs (read-only)

---
## Idempotency

### Core idempotency: `attemptId`

Purpose:
- Prevent duplicate processing when devices retry (network issues, timeouts).
- Ensure deterministic behavior under retries.

Rule:
- For a given tenant, `(tenantId, attemptId)` must map to a single immutable result:
    - same `decision`
    - same `reasonCode`
    - same timestamps as returned previously (or a stable evaluatedAt policy)

Implementation note (design-level):
- Enforced by a unique constraint in `access_attempts` and/or an idempotency table.

### Audit idempotency: `eventId` and/or `attemptId`

Purpose:
- Ensure the consumer can safely retry and handle at-least-once delivery.

Rule:
- The audit consumer must deduplicate events using:
    - `eventId` (preferred), or
    - `(tenantId, attemptId)` where applicable

Implementation note (design-level):
- Enforced via unique constraint on `audit_events.event_id` and/or `audit_access_attempts.tenant_id + attempt_id`.

---
## Operational Consistency Guarantees

- Device access decision is **strongly consistent** within core (core DB transaction).
- Audit log is **eventually consistent** (Kafka → audit consumer → audit DB).
- Observability uses correlation IDs to trace a single attempt across:
    - device request
    - core decision + event publish
    - audit consumer persistence
    - audit query