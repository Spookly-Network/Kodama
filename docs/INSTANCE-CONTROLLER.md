# InstanceController

`InstanceController` exposes the control-plane endpoints for creating and reading game server instances. It delegates all work to `InstanceService` and returns `InstanceDto` payloads.

- Base path: `/api/instances`
- Audience: web panel or automation clients that need to register instance metadata.
- Auth: follows global Brain HTTP security configuration.

## Endpoints

### GET /api/instances
- Returns every persisted instance and its template layers.
- Response: `200 OK` with `InstanceDto[]`. Instance order is not guaranteed; `templateLayers` are sorted by `orderIndex` ascending.
- Errors: default Spring errors only.

### GET /api/instances/{id}
- Path parameter: `id` (UUID).
- Response: `200 OK` with `InstanceDto`.
- Errors: `404 Not Found` if the instance does not exist.

### POST /api/instances
Creates a new instance record in state `REQUESTED` and logs an `InstanceEvent` (`REQUEST_RECEIVED`).

Request body (`Content-Type: application/json`):

| Field | Type | Notes |
| --- | --- | --- |
| `name` | string | Required; must be unique. Duplicate names return `409 Conflict`. |
| `displayName` | string | Required by persistence (must not be null or empty even though the DTO does not enforce it). |
| `requestedBy` | UUID | Optional user id recorded as `requestedByUserId`. |
| `nodeId` | UUID | Optional; must reference an existing node or the request returns `404 Not Found`. |
| `templateLayers` | array of `InstanceTemplateLayerRequest` | Required, non-empty; see below. |
| `variablesJson` | string | Optional raw JSON string persisted verbatim. |
| `portsJson` | string | Optional raw JSON string persisted verbatim. |

`InstanceTemplateLayerRequest`:
- `templateVersionId` (UUID, required). All referenced versions must exist; otherwise `404 Not Found`.
- `orderIndex` (integer, >= 0). Order indexes must be unique; duplicates return `400 Bad Request`.

Behavior:
- Validates that at least one template layer is provided.
- Loads all referenced template versions before persisting.
- Resolves the optional node id.
- Persists the instance with `state=REQUESTED`, `createdAt`/`updatedAt` set to the current UTC time, and writes template layers in ascending `orderIndex`.
- Records an `InstanceEvent` of type `REQUEST_RECEIVED`.
- Does not schedule or start the instance; this controller only registers the request.

Responses:
- `201 Created` with the stored `InstanceDto` (includes generated ids for the instance and each layer).
- `400 Bad Request` for validation errors (missing layers, duplicate `orderIndex`, blank `name`).
- `404 Not Found` when the node or any template version is missing.
- `409 Conflict` when an instance with the same `name` already exists.

## Data contracts

### InstanceDto
Fields returned by all endpoints:
- `id` (`UUID`)
- `name` (`String`)
- `displayName` (`String`)
- `state` (`InstanceState`: `REQUESTED`, `PREPARING`, `STARTING`, `RUNNING`, `STOPPING`, `DESTROYED`, `FAILED`)
- `nodeId` (`UUID`, nullable)
- `requestedBy` (`UUID`, nullable)
- `portsJson` (`String`, nullable)
- `variablesJson` (`String`, nullable)
- `createdAt`, `updatedAt`, `startedAt`, `stoppedAt` (`OffsetDateTime`, ISO-8601; `createdAt`/`updatedAt` minted in UTC)
- `failureReason` (`String`, nullable)
- `templateLayers` (`InstanceTemplateLayerDto[]`)

### InstanceTemplateLayerDto
- `id` (`UUID`)
- `templateVersionId` (`UUID`)
- `orderIndex` (`int`)

## Examples

Create instance request:

```json
POST /api/instances
{
  "name": "lobby-eu-1",
  "displayName": "Lobby EU #1",
  "requestedBy": "11111111-2222-3333-4444-555555555555",
  "nodeId": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
  "templateLayers": [
    { "templateVersionId": "10000000-0000-0000-0000-000000000001", "orderIndex": 0 },
    { "templateVersionId": "10000000-0000-0000-0000-000000000002", "orderIndex": 1 }
  ],
  "variablesJson": "{ \"ENV\": \"prod\", \"SEED\": \"12345\" }",
  "portsJson": "{ \"game\": 25565 }"
}
```

Success response:

```json
HTTP/1.1 201 Created
{
  "id": "99999999-8888-7777-6666-555555555555",
  "name": "lobby-eu-1",
  "displayName": "Lobby EU #1",
  "state": "REQUESTED",
  "nodeId": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
  "requestedBy": "11111111-2222-3333-4444-555555555555",
  "portsJson": "{ \"game\": 25565 }",
  "variablesJson": "{ \"ENV\": \"prod\", \"SEED\": \"12345\" }",
  "createdAt": "2024-06-10T18:42:31Z",
  "updatedAt": "2024-06-10T18:42:31Z",
  "startedAt": null,
  "stoppedAt": null,
  "failureReason": null,
  "templateLayers": [
    { "id": "20000000-0000-0000-0000-000000000001", "templateVersionId": "10000000-0000-0000-0000-000000000001", "orderIndex": 0 },
    { "id": "20000000-0000-0000-0000-000000000002", "templateVersionId": "10000000-0000-0000-0000-000000000002", "orderIndex": 1 }
  ]
}
```
