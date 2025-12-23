# Node Command Dispatcher (Brain -> Node)

This document captures the HTTP contract used by the Brain to send instance commands to a node agent.

## Base URL

`Node.baseUrl` from node registration is treated as the root of the node API.

## Endpoints

### Prepare

`POST /api/instances/{instanceId}/prepare`

Body: `NodePrepareInstanceRequest`

```json
{
  "instanceId": "uuid",
  "name": "string",
  "displayName": "string",
  "portsJson": "string|null",
  "variables": {
    "KEY": "VALUE"
  },
  "variablesJson": "string|null",
  "layers": [
    {
      "templateVersionId": "uuid",
      "version": "string",
      "checksum": "string",
      "s3Key": "string",
      "metadataJson": "string|null",
      "orderIndex": 0
    }
  ]
}
```

`variables` and `variablesJson` are mutually exclusive. Brain will send `variables` when provided, otherwise it forwards `variablesJson` from the instance record.

### Start

`POST /api/instances/{instanceId}/start`

Body: `NodeInstanceCommandRequest`

```json
{
  "instanceId": "uuid",
  "name": "string"
}
```

### Stop

`POST /api/instances/{instanceId}/stop`

Body: `NodeInstanceCommandRequest`

### Destroy

`POST /api/instances/{instanceId}/destroy`

Body: `NodeInstanceCommandRequest`

## Configuration

The Brain uses the following configuration properties:

- `node.command-timeout-seconds` (`NODE_COMMAND_TIMEOUT_SECONDS`) for connect/read timeout.
- `node.command-max-attempts` (`NODE_COMMAND_MAX_ATTEMPTS`) for retry attempts.
- `node.command-retry-backoff-millis` (`NODE_COMMAND_RETRY_BACKOFF_MILLIS`) for retry backoff.
