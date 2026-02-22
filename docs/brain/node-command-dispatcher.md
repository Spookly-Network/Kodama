# Node Command Dispatcher (Brain -> Node)

This document captures the HTTP contract used by the Brain command dispatcher to send instance commands to node agents.

## Base URL

`Node.baseUrl` is used as the root for node command endpoints.
When `node.tls.enabled=true`, `Node.baseUrl` must use `https://` or dispatch fails before the HTTP request is sent.

## Brain -> Node Commands

### Prepare

`POST /api/instances/{instanceId}/prepare`

Body: `NodePrepareInstanceRequest`

```json
{
  "instanceId": "04a46594-c578-462c-b2ec-fbbec84cd148",
  "name": "hytale-eu-1",
  "displayName": "Hytale EU #1",
  "containerImage": "ghcr.io/spookly-network/hytale:latest",
  "installScript": "./install.sh",
  "startCommand": ["./run-server.sh", "--port", "${PORT}"],
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
  "portsJson": null,
  "variables": {
    "SERVER_NAME": "Hytale EU #1"
  },
  "variablesJson": null,
  "layers": [
    {
      "templateVersionId": "f46e4888-f9f7-4e10-a7e8-393f3166ac9f",
      "templateId": "1474e8eb-7391-4231-af03-48e451f6fcd8",
      "version": "v1.0.2",
      "checksum": "sha256:8ca2806adff16442984f4f7c35cfaf5f5a10967f65e6b4f3e52f31f39a2f4f16",
      "s3Key": "templates/hytale/base/v1.0.2.tar.gz",
      "metadataJson": null,
      "orderIndex": 0
    }
  ]
}
```

Rules:
- `containerImage`, `installScript`, `startCommand`, `slotsRequired`, and `portDefinitions` are resolved before dispatch from blueprint + request overrides.
- `variables` and `variablesJson` are mutually exclusive.
- `portsJson` in prepare is legacy input and is only used when node allocation does not produce runtime ports.

### Start

`POST /api/instances/{instanceId}/start`

Body: `NodeInstanceCommandRequest`

```json
{
  "instanceId": "04a46594-c578-462c-b2ec-fbbec84cd148",
  "name": "hytale-eu-1"
}
```

### Stop

`POST /api/instances/{instanceId}/stop`

Body: `NodeInstanceCommandRequest`

### Destroy

`POST /api/instances/{instanceId}/destroy`

Body: `NodeInstanceCommandRequest`

## Node -> Brain Callback Shape (Prepared)

The prepared callback endpoint handled by Brain:
- `POST /api/nodes/{nodeId}/instances/{instanceId}/prepared`
- Request body is optional.
- When present, body is:

```json
{
  "portsJson": "[{\"name\":\"game\",\"protocol\":\"udp\",\"containerPort\":7777,\"hostPort\":14000}]"
}
```

`portsJson` is a serialized JSON array string and is persisted on the instance by Brain.

## Additional Node Commands

### Purge cache

`POST /api/cache/purge`

Body is optional:

```json
{
  "templateId": "starter"
}
```

### Dev-mode

`POST /api/node/dev-mode`

```json
{
  "devMode": true
}
```

## Configuration

The Brain uses:
- `node.command-timeout-seconds` (`NODE_COMMAND_TIMEOUT_SECONDS`)
- `node.command-max-attempts` (`NODE_COMMAND_MAX_ATTEMPTS`)
- `node.command-retry-backoff-millis` (`NODE_COMMAND_RETRY_BACKOFF_MILLIS`)
- `node.tls.enabled` (`NODE_TLS_ENABLED`, default `false`)
- `node.tls.trust-store-path` (`NODE_TLS_TRUST_STORE_PATH`, required when TLS enabled)
- `node.tls.trust-store-password` (`NODE_TLS_TRUST_STORE_PASSWORD`, required when TLS enabled)
- `node.tls.trust-store-type` (`NODE_TLS_TRUST_STORE_TYPE`, default `PKCS12`)
- `node.tls.key-store-path` (`NODE_TLS_KEY_STORE_PATH`, optional)
- `node.tls.key-store-password` (`NODE_TLS_KEY_STORE_PASSWORD`, required when key-store-path is set)
- `node.tls.key-store-type` (`NODE_TLS_KEY_STORE_TYPE`, default `PKCS12`)

Failure behavior when `node.tls.enabled=true`:
- Missing TLS files/passwords fail Brain startup.
- Invalid TLS file paths or unreadable key/trust store contents fail Brain startup.
- Any `http://` node base URL is rejected with a validation error.
