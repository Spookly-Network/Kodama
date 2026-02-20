# InstanceController

`InstanceController` exposes the Brain endpoints for creating and reading instances.

- Base path: `/api/instances`
- Controller: `backend/brain/src/main/java/net/spookly/kodama/brain/controller/InstanceController.java`
- Service owner: `backend/brain/src/main/java/net/spookly/kodama/brain/service/InstanceService.java`

## Endpoints

### GET /api/instances
- Returns all instances with resolved template layers.
- Response: `200 OK` with `InstanceDto[]`.

### GET /api/instances/{id}
- Returns one instance by UUID.
- Response: `200 OK` with `InstanceDto`.
- Errors: `404 Not Found` when id does not exist.

### POST /api/instances
Creates a new instance in state `REQUESTED` and stores resolved runtime configuration.

Request fields:

| Field | Type | Notes |
| --- | --- | --- |
| `name` | string | Required and unique. |
| `displayName` | string | Required persisted display label. |
| `blueprintId` | UUID | Optional. When present, blueprint defaults are resolved first. |
| `requestedBy` | UUID | Optional requestor id. |
| `nodeId` | UUID | Optional fixed node assignment. |
| `region` | string | Optional scheduling filter. |
| `tags` | string | Optional comma-separated scheduling tags. |
| `devModeAllowed` | boolean | Optional scheduling filter for node dev mode. |
| `permanent` | boolean | Optional runtime override. |
| `slotsRequired` | int | Optional runtime override (`>=1`, defaults to `1`). |
| `containerImage` | string | Optional runtime override. |
| `installScript` | string | Optional runtime override. |
| `startCommand` | array of string | Optional runtime override (non-empty when provided). |
| `portDefinitions` | array | Optional override list (`name`, `protocol`, `containerPort`, `hostRange`). |
| `groupIds` | UUID[] | Optional override list of groups. |
| `templateLayers` | `TemplateAssignmentRequest[]` | Required when `blueprintId` is absent; overrides blueprint assignments when present. |
| `variables` | object | Optional map. Mutually exclusive with `variablesJson`. |
| `variablesJson` | string | Optional raw JSON string. Mutually exclusive with `variables`. |
| `portsJson` | string | Optional legacy ports payload string forwarded to node prepare. |

Override semantics:
- When `blueprintId` is provided, request fields replace blueprint values when provided.
- No merge is performed for list fields (`templateLayers`, `groupIds`, `portDefinitions`): request value wins entirely.

Responses:
- `201 Created` with `InstanceDto`.
- `400 Bad Request` for validation failures.
- `404 Not Found` for unknown node/template/blueprint references.
- `409 Conflict` for duplicate name or capacity conflicts.

## InstanceDto
Returned fields:
- `id`, `name`, `displayName`, `state`
- `nodeId`, `blueprintId`, `requestedBy`
- `region`, `tags`, `devModeAllowed`, `slotsRequired`
- `portsJson`, `variablesJson`
- `createdAt`, `updatedAt`, `startedAt`, `stoppedAt`
- `failureReason`
- `templateLayers`

`InstanceDto` exposes `blueprintId` and the effective persisted values. It does not expose a separate nested `overrides` object.

## Example Request (Blueprint + Overrides)

```json
POST /api/instances
{
  "name": "hytale-eu-1",
  "displayName": "Hytale EU #1",
  "blueprintId": "6c08fc07-cbbd-4585-b79c-3ff771d7f533",
  "region": "eu-west-1",
  "tags": "primary,ssd",
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
  "templateLayers": [
    {
      "templateId": "b2cbeb19-5adf-4283-b648-3df0e7f58096",
      "templateVersionId": "527dc44e-c188-4da8-a870-4d3f77f8fc9f",
      "priority": 0
    }
  ],
  "variables": {
    "SERVER_NAME": "Hytale EU #1"
  }
}
```

## Example Response (`portsJson` Array String)

```json
HTTP/1.1 201 Created
{
  "id": "e5338bc2-665d-40fd-940b-368ad86ab672",
  "name": "hytale-eu-1",
  "displayName": "Hytale EU #1",
  "state": "REQUESTED",
  "nodeId": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
  "blueprintId": "6c08fc07-cbbd-4585-b79c-3ff771d7f533",
  "requestedBy": null,
  "region": "eu-west-1",
  "tags": "primary,ssd",
  "devModeAllowed": null,
  "slotsRequired": 2,
  "portsJson": "[{\"name\":\"game\",\"protocol\":\"udp\",\"containerPort\":7777,\"hostPort\":14000}]",
  "variablesJson": "{\"SERVER_NAME\":\"Hytale EU #1\"}",
  "createdAt": "2026-02-20T12:00:00Z",
  "updatedAt": "2026-02-20T12:00:00Z",
  "startedAt": null,
  "stoppedAt": null,
  "failureReason": null,
  "templateLayers": []
}
```
