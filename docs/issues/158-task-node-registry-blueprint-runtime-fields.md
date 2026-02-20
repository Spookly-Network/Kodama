# #158 [Task] Node: registry runtime fields for blueprints

## Summary
Extend the node instance registry to store blueprint runtime fields and install completion status.

## Details
The registry must persist resolved runtime configuration so start/stop and heartbeat logic can use it consistently.

## Scope / Requirements
- Extend instance.json schema with:
  - containerImage
  - installScript
  - startCommand
  - slotsRequired
  - allocatedPorts (portsJson)
  - installCompleted (boolean)
- Update InstanceRegistryEntry and registry service read/write logic.
- Keep backward compatibility for existing registry entries (missing fields default).

## Acceptance Criteria
- Registry entries include new fields and can be read/written without errors.
- Existing entries without new fields still load with defaults.
