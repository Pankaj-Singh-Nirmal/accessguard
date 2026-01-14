# AccessGuard

AccessGuard is a **multi-tenant access pass management and validation platform** designed to demonstrate backend engineering practices in Java and Spring Boot.

The system models real-world access control scenarios (buildings, floors, doors, passes, devices) and focuses on **correctness, auditability, and clear service boundaries** rather than UI or ops-heavy concerns.

---

## High-level Architecture

AccessGuard is implemented as two cooperating microservices:

- **accessguard-core**
  - Source of truth for access decisions
  - Manages passes, scopes, and validation rules
  - Exposes REST APIs for admins and access devices
  - Publishes access attempt events

- **accessguard-audit**
  - Consumes access attempt events asynchronously
  - Persists immutable audit records
  - Exposes read-only APIs for querying audit history

Communication:
- **Synchronous:** REST (Spring MVC, OpenFeign)
- **Asynchronous:** Kafka (event-driven audit trail)

---

## Core Business Flow (Simplified)

1. Tenant admin creates a pass with a validity window and scope (doors/zones).
2. An access device submits an access attempt using `(attemptId, doorId, passCode)`.
3. `accessguard-core` evaluates the request and returns:
   - `GRANTED` or `DENIED`
   - A deterministic reason code
4. The decision is published as an event.
5. `accessguard-audit` consumes the event and stores an immutable audit record.

---

## Technology Stack

- **Language:** Java 21
- **Framework:** Spring Boot 3.5.x (Spring MVC)
- **Persistence:** PostgreSQL, Flyway
- **Messaging:** Kafka
- **Security:** Spring Security (OAuth2 Resource Server, JWT)
- **Observability:** Actuator, Micrometer
- **Testing:** JUnit 5, Mockito, Testcontainers
- **Build:** Maven
- **CI:** GitHub Actions
- **Runtime:** Docker / Docker Compose

---

## Repository Structure

```text
accessguard/
  accessguard-core/
  accessguard-audit/
  accessguard-shared/
  docs/
  .github/
