# #165 [Documentation] Blueprint docs and contract updates

## Summary
Update documentation and contracts to reflect blueprint-driven instance creation.

## Details
Blueprints change the API surface and node command payloads. Contracts and module docs must reflect the new behavior.

## Scope / Requirements
- Update /contracts/openapi.yml:
  - blueprint CRUD and attachments
  - CreateInstanceRequest changes (blueprintId and overrides)
  - InstanceDto fields (blueprintId, overrides, portsJson)
  - prepared callback body with portsJson
- Update /contracts/nodeapi.yml:
  - prepare payload includes runtime fields and port definitions
  - prepared callback payload shape
- Update docs:
  - docs/INSTANCE-CONTROLLER.md
  - docs/brain/node-command-dispatcher.md
  - docs/node/operations/instance-commands.md
  - docs/node/operations/instance-registry.md
  - docs/brain/scheduling-service.md
  - docs/NODE-FLOW.md
  - add a blueprint doc under docs/brain or docs/instance

## Acceptance Criteria
- Contracts and docs match implemented behavior.
- Examples include blueprintId, port definitions, and portsJson array format.
