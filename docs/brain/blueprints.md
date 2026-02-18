# Blueprints (Design Spec)

## Purpose
Define the canonical blueprint data model and Brain ↔ Node payload shapes for blueprint-driven instance creation.

## What changed
- Standardized blueprint core fields and defaults (slots required defaults to 1).
- Implemented Brain blueprint CRUD endpoints:
  - `GET /api/blueprints` (non-deleted entries only)
  - `POST /api/blueprints`
  - `GET /api/blueprints/{id}`
  - `PUT /api/blueprints/{id}`
  - `DELETE /api/blueprints/{id}` (soft delete via `deletedAt`)
- Implemented blueprint template assignment endpoints:
  - `GET /api/blueprints/{id}/template-assignments`
  - `POST /api/blueprints/{id}/template-assignments`
  - `DELETE /api/blueprints/{id}/template-assignments/{assignmentId}`
- Implemented blueprint port definition endpoints:
  - `GET /api/blueprints/{id}/ports`
  - `POST /api/blueprints/{id}/ports`
  - `DELETE /api/blueprints/{id}/ports/{portId}`
- Added request validation for blueprint CRUD:
  - required: `name`, `containerImage`, `startCommand`
  - optional: `installScript`, `variablesJson`
  - constraint: `slotsRequired >= 1` when provided
- Added validation for blueprint port definitions:
  - `name` is required and unique per blueprint
  - `protocol` must be `tcp` or `udp`
  - `containerPort` and `hostRange.min/max` must be between `1` and `65535`
  - `hostRange.step` must be `>= 1`
  - `hostRange.min <= hostRange.max`
- Defined override semantics: request fields replace blueprint values when provided.
- Established port definition shape (name, protocol, containerPort, hostRange).
- Added Brain persistence model for `blueprints`, `blueprint_template_assignments`,
  `blueprint_port_definitions`, and `blueprint_group_links` plus instance override columns.
- Added repository layer coverage for blueprint entities and attachment entities.
- Added prepare/start payload fields for resolved runtime values and port definitions.
- Added prepared callback payload with allocated ports (portsJson).
- Clarified soft delete behavior for blueprints (deletedAt, block new creates only).
- Ensured duplicate-name races at write time map to `409 Conflict` (DB unique constraint translation).

## How to use / impact
- Blueprints are normal entities with no versioning.
- Instance creation can reference a blueprint with optional overrides; overrides replace blueprint values (no merge).
- Ports are defined as ranges; nodes allocate host ports from ranges and return allocations in portsJson.
- Install script runs once before start command; the start command is an exec-form array.
- Slots are counted for instances in STARTING, RUNNING, and STOPPING; default slotsRequired is 1.

## Edge cases / risks
- Deleted blueprints must be rejected for new instance creation while existing instances keep their resolved values.
- Invalid port ranges or missing runtime fields (container image/start command) should be treated as failures.
- Keep legacy portsJson formats compatible while preferring the new port definition array.

## Links
- `contracts/openapi.yml`
- `contracts/nodeapi.yml`
- `docs/plan/blueprints/plan.md`
- `docs/issues/149-architecture-blueprint-data-model-and-contracts.md`
