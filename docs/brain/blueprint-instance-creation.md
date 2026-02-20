# Blueprint-Driven Instance Creation

## Purpose

Document the concrete flow used when `POST /api/instances` references a blueprint and optional overrides.

## Request Resolution

1. Brain loads the blueprint when `blueprintId` is present.
2. Brain resolves effective fields:
   - runtime: `permanent`, `slotsRequired`, `containerImage`, `installScript`, `startCommand`
   - template assignments
   - group links
   - variables
   - port definitions
3. Request fields replace blueprint values when provided (list values replace the full list).
4. Brain persists the instance with `blueprintId`, resolved runtime fields, and `portDefinitionsJson`.

## Create Instance Example

```json
POST /api/instances
{
  "name": "hytale-eu-1",
  "displayName": "Hytale EU #1",
  "blueprintId": "6c08fc07-cbbd-4585-b79c-3ff771d7f533",
  "slotsRequired": 2,
  "portDefinitions": [
    {
      "name": "game",
      "protocol": "udp",
      "containerPort": 7777,
      "hostRange": {
        "min": 14000,
        "max": 14100,
        "step": 1
      }
    }
  ],
  "variables": {
    "SERVER_NAME": "Hytale EU #1"
  }
}
```

## Prepare Dispatch to Node

Brain sends:
- `POST /api/instances/{instanceId}/prepare`
- payload includes resolved runtime fields and `portDefinitions`

Key payload fields:
- `containerImage`
- `installScript`
- `startCommand`
- `slotsRequired`
- `portDefinitions`
- `variables` or `variablesJson`
- `layers`

## Prepared Callback with Allocated Ports

Node reports allocated ports back to Brain:

```json
POST /api/nodes/{nodeId}/instances/{instanceId}/prepared
{
  "portsJson": "[{\"name\":\"game\",\"protocol\":\"udp\",\"containerPort\":7777,\"hostPort\":14000}]"
}
```

Brain stores this serialized array on the instance `portsJson` field.

## Contract References

- `contracts/openapi.yml`
- `contracts/nodeapi.yml`
- `docs/INSTANCE-CONTROLLER.md`
- `docs/brain/node-command-dispatcher.md`
