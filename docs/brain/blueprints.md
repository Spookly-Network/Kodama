# Blueprints (Design Spec)

## Purpose
Define the canonical blueprint data model and Brain ↔ Node payload shapes for blueprint-driven instance creation.

## What changed
- Standardized blueprint core fields and defaults (slots required defaults to 1).
- Defined override semantics: request fields replace blueprint values when provided.
- Established port definition shape (name, protocol, containerPort, hostRange).
- Added prepare/start payload fields for resolved runtime values and port definitions.
- Added prepared callback payload with allocated ports (portsJson).
- Clarified soft delete behavior for blueprints (deletedAt, block new creates only).

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
