# #149 [Architecture] Blueprint data model and contracts

## Summary
Define the canonical blueprint data model, override semantics, and Brain-Node payload changes.

## Problem
Blueprints add new fields (ports, scripts, slots) that must align across Brain, Node, and contracts. Without a shared contract we risk inconsistent behavior and incomplete API coverage.

## Proposed Solution
- Blueprint core fields: name, permanent, slotsRequired, containerImage, installScript (optional), startCommand (exec form array), variablesJson (optional), deletedAt.
- Override semantics: request fields replace blueprint values when provided.
- Port definition shape: name, protocol, containerPort, hostRange {min,max,step}; no fixed host ports in blueprint.
- Node prepare payload includes resolved runtime fields and port definitions.
- Prepared callback includes allocated ports for Brain to persist as portsJson.
- Install script runs once before start command; /bin/sh is available.
- Slots are counted for STARTING, RUNNING, STOPPING; default slotsRequired is 1.
- Blueprint deletion is soft and blocks new instance creation only.

## Impact Analysis
- Brain: new entities, services, controllers, instance create flow, scheduling changes.
- Node: port allocator, registry schema, start logic, heartbeat usedSlots.
- Contracts: openapi.yml and nodeapi.yml updates.
- Docs: brain, node, instance workflow updates.

## Acceptance Criteria
- The decisions above are documented and approved for implementation.
- Payload shapes and defaults are explicit and consistent with `docs/plan/blueprints/plan.md`.
- Any remaining open questions are tracked as follow-up issues.
