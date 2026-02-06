# Control Plane Package Layout

The Control Plane (Brain) uses a layered package structure to keep HTTP concerns, business logic, and persistence clearly separated. New code should follow these boundaries to avoid circular dependencies and to make the architecture obvious to newcomers.

## Package Map

All Control Plane classes live under `net.spookly.kodama.brain`.

| Package | Responsibility |
| --- | --- |
| `config` | Spring configuration such as CORS, HTTP clients, and security. |
| `security` | Authentication, authorization primitives, and request filters. |
| `controller` | HTTP controllers that expose REST endpoints. Controllers translate HTTP requests into service calls and return DTOs. |
| `service` | Application services that orchestrate business logic and transactions. Services hide repositories from controllers. |
| `domain` | JPA entities and domain enums. Domain types are persistence-aware but free of HTTP or controller knowledge. |
| `repository` | Spring Data repositories for CRUD access to domain entities. |
| `dto` | Request/response DTOs used by controllers. DTOs keep HTTP contracts separate from domain entities. |
| `events` | Domain and integration event definitions. |
| `scheduling` | Scheduling policies and algorithms. |
| `util` | Shared utilities that do not fit another package but should remain framework-agnostic. |

## Layering Rules

- Controllers depend on services and DTOs only; they should **never call repositories directly**.
- Services coordinate business logic and depend on repositories, domain entities, and other services.
- Domain entities must not depend on controllers, DTOs, or services.
- DTOs are HTTP-facing contracts and must not leak persistence annotations or repository logic.
- Repositories only work with domain entities and return domain types (or projections) to services.
- Utility classes should be stateless and free of framework-specific behavior unless placed in `config`.

Keeping these rules in mind prevents tight coupling and makes it possible to evolve each layer independently.

## Example Flow

The node registration flow demonstrates the intended layering:

1. **Controller**: `NodeController` exposes `/api/nodes` endpoints and accepts `NodeRegistrationRequest` payloads.
2. **Service**: `NodeService` handles registration/heartbeat logic and persists changes through `NodeRepository`.
3. **Repository**: `NodeRepository` extends Spring Data JPA to load and store the `Node` domain entity.
4. **Domain**: `Node` and `NodeStatus` capture persisted state without any HTTP or DTO knowledge.
5. **DTO**: `NodeDto` shapes the API response, keeping HTTP contracts isolated from the JPA entity.

This illustrates how controllers remain thin, services encapsulate business rules, and repositories focus solely on persistence.