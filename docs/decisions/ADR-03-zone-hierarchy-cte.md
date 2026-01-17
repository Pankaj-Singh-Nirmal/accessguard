# ADR-0003: Zone Hierarchy Query Model (Recursive CTE)

## Status
Accepted

## Context
We need zone hierarchy (Building → Floor → Door) to support authorization checks. We want correctness and simplicity over premature optimization.

## Decision
Use a PostgreSQL recursive CTE for zone subtree resolution.

- Zones stored as adjacency list:
    - `zones(id, tenant_id, parent_zone_id, ...)`
- Doors reference a zone:
    - `doors(id, tenant_id, zone_id, ...)`
- Authorization checks:
    - If a pass scope includes a zone, it authorizes all descendant zones’ doors.

## Consequences
### Positive
- Simple schema and easy migrations
- No maintenance overhead of precomputed structures
- Clear SQL that can be tested and optimized if needed

### Trade-offs
- Recursive queries can become expensive at large scale
- Requires indexes and careful query design

## Alternatives Considered
- **Closure table**: faster reads, but additional write complexity and maintenance.
- **Materialized path**: simpler reads, but path updates are harder on re-parenting.
- **Application-side recursion**: increases roundtrips and complexity; less efficient than DB recursion.
