# Blueprints Feature Plan

## Purpose
Introduce a reusable blueprint entity that defines how instances are created. Instances reference a blueprint and can override selected fields at creation time.

## Scope
- Brain: new blueprint entity + API, instance creation with blueprint resolution, persistence for overrides, updated prepare/start payloads.
- Node: port allocation from ranges, install script execution before start, start command handling, registry updates, new prepared callback payload.
- Contracts/docs: update OpenAPI and Node API contracts, plus module docs for Brain and Node.

## Core Rules
- Blueprints are normal entities (no versioning).
- Instances store only a reference (`blueprintId`) plus explicit overrides.
- Request overrides replace blueprint values (no merge).
- Blueprint templates are independent from group templates.
- Ports are allocated by the node and reserved until destroy.

## Data Model
Blueprint tables (Brain):
- `blueprints`
  - id (UUID)
  - name (unique)
  - permanent (boolean)
  - slotsRequired (int)
  - containerImage (string)
  - installScript (text)
  - startCommandJson (json array of string)
  - variablesJson (json object)
  - deletedAt (timestamp, nullable)
  - createdAt, updatedAt
- `blueprint_template_assignments`
  - blueprintId, templateId, templateVersionId (nullable), priority
- `blueprint_port_definitions`
  - blueprintId
  - name
  - protocol (tcp/udp)
  - containerPort
  - hostRangeMin, hostRangeMax, hostRangeStep
- `blueprint_group_links`
  - blueprintId, groupId

Instance changes (Brain):
- Add `blueprintId` (nullable for legacy flow).
- Add blueprint-backed instance columns (nullable):
  - permanent
  - slotsRequired
  - containerImage
  - installScript
  - startCommandJson
  - portDefinitionsJson (list of port definitions)
- Reuse existing `variablesJson` on instances for blueprint overrides.

Node registry changes:
- Store resolved values (container image, start command, install script).
- Store allocated ports in `portsJson`.
- Track install execution status (e.g., `installCompleted`).

## API Surface
Brain (new):
- `GET /api/blueprints`
- `POST /api/blueprints`
- `GET /api/blueprints/{id}`
- `PUT /api/blueprints/{id}`
- `DELETE /api/blueprints/{id}` (soft delete, set `deletedAt`)
- `GET /api/blueprints/{id}/template-assignments`
- `POST /api/blueprints/{id}/template-assignments`
- `DELETE /api/blueprints/{id}/template-assignments/{assignmentId}`
- `GET /api/blueprints/{id}/ports`
- `POST /api/blueprints/{id}/ports`
- `DELETE /api/blueprints/{id}/ports/{portId}`
- `GET /api/blueprints/{id}/groups`
- `PUT /api/blueprints/{id}/groups/{groupId}`
- `DELETE /api/blueprints/{id}/groups/{groupId}`

Brain (existing, extend):
- `POST /api/instances` accepts optional `blueprintId` and override fields.
  - If `blueprintId` is present, `templateLayers` becomes optional.
  - Overrides replace blueprint values when provided.
  - Blueprint deletion blocks new instance creation.

Node callbacks (Brain):
- `/api/nodes/{nodeId}/instances/{instanceId}/prepared` accepts payload with `portsJson`
  (allocated host+container ports), persisted on instance.

Node command (Brain -> Node):
- `POST /api/instances/{instanceId}/prepare` adds `ports` (port definitions list).

## Request/Response Shapes (Key Fields)
Blueprint port definition (list element):
```json
{
  "name": "game",
  "protocol": "udp",
  "containerPort": 7777,
  "hostRange": { "min": 1000, "max": 2000, "step": 10 }
}
```

Prepared callback payload:
```json
{
  "portsJson": "[{\"name\":\"game\",\"protocol\":\"udp\",\"containerPort\":7777,\"hostPort\":1400}]"
}
```

## Instance Creation Flow (Blueprint)
1. Request `POST /api/instances` with `blueprintId` and optional overrides.
2. Brain loads blueprint (reject if deleted).
3. Brain resolves effective values:
   - Use override if present, else blueprint value.
   - For lists/maps, overrides replace the blueprint list/map.
4. Brain persists instance with `blueprintId` + override fields.
5. Brain creates instance group memberships based on blueprint group links (unless overridden).
6. Brain resolves template assignments:
   - Use override assignments if provided.
   - Else use blueprint template assignments.
7. Brain dispatches `prepare` to node with resolved layers, variables, and port definitions.

## Node Prepare Flow
1. Validate payload (layers, port definitions).
2. Allocate host ports from node-local ranges; reserve until destroy.
3. Store allocated ports in registry (`portsJson`) and update variables:
   - `PORT` when single port
   - `PORT_<NAME>` when multiple ports
4. Continue normal prepare (cache, merge, variables).
5. Callback `/prepared` with `portsJson` so Brain persists it.

## Node Start Flow (Install + Start)
1. Load registry (install script, start command, image).
2. If install not completed:
   - Run install once using `/bin/sh` before start command.
   - Mark `installCompleted` in registry (or sentinel file in workspace).
3. Start container with resolved image and `startCommand` (Docker exec form).
4. Continue existing callbacks and registry updates.

## Scheduling and Slots
- `slotsRequired` comes from blueprint unless overridden.
- `usedSlots` counts instances in states: STARTING, RUNNING, STOPPING.
- Scheduling rejects nodes without enough available slots.

## Deletion Semantics
- Blueprint delete is soft (`deletedAt` set).
- New instance creation with a deleted blueprint is rejected.
- Existing instances continue to reference the blueprint for resolved values.

## Validation and Errors
- Missing blueprint or deleted blueprint: `404`/`409` on create.
- Invalid port ranges or no available ports: prepare fails, `/failed` callback.
- Missing container image or start command: start fails, `/failed` callback.
- Invalid startCommandJson or variablesJson: `400` on create or prepare.

## Backward Compatibility
- Instances without `blueprintId` follow current behavior.
- Node port resolver accepts legacy `portsJson` (map of name -> containerPort) but prefers new array form.

## Contract/Docs Updates
- `contracts/openapi.yml`: blueprints APIs, instance create overrides, prepared callback payload.
- `contracts/nodeapi.yml`: prepare request ports + prepared callback body.
- Docs:
  - `docs/INSTANCE-CONTROLLER.md`
  - `docs/brain/node-command-dispatcher.md`
  - `docs/node/operations/instance-commands.md`
  - `docs/node/operations/instance-registry.md`
  - `docs/brain/scheduling-service.md`
  - `docs/NODE-FLOW.md`
  - New blueprint docs (Brain or Instance module).

## Testing Plan
- Brain: create instance via blueprint, override fields, deleted blueprint rejection.
- Node: port allocation and reservation, prepared callback includes ports.
- Node: install script runs once and precedes start command.
- Scheduling: slotsRequired affects selection and usedSlots.
