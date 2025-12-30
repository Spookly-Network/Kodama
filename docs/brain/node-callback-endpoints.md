# Node callback endpoints

Nodes call these endpoints to report instance lifecycle transitions to the control plane.

Base path: `/api/nodes/{nodeId}/instances/{instanceId}`

## Endpoints

- `POST /prepared`
  - Updates instance state to `STARTING`.
  - Logs `PREPARE_COMPLETED` event.
- `POST /running`
  - Updates instance state to `RUNNING`.
  - Logs `START_COMPLETED` event.
- `POST /stopped`
  - Updates instance state to `STOPPED`.
  - Logs `STOP_COMPLETED` event.
- `POST /destroyed`
  - Updates instance state to `DESTROYED`.
  - Logs `DESTROY_COMPLETED` event.
- `POST /failed`
  - Updates instance state to `FAILED`.
  - Logs `FAILURE_REPORTED` event.

## Validation

- `nodeId` must exist.
- `instanceId` must exist and be assigned to the provided `nodeId`.

## Responses

- `200 OK` for valid callbacks.
- `404 Not Found` when node or instance does not exist.
- `409 Conflict` when the instance is not assigned to the node.
