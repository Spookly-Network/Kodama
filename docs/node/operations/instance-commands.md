# Instance Commands (Node Agent)

## Purpose
Describe the node agent endpoints that handle instance lifecycle commands from the Brain.

## What changed
- Added an instance prepare command handler that assembles cached template layers into a workspace.
- Added variable substitution and Brain callbacks as part of the prepare flow.
- Added a local instance registry record written after successful preparation.
- Added start/stop/destroy command handlers that send lifecycle callbacks to the Brain.

## How to use / impact
- `POST /api/instances/{instanceId}/prepare` with `NodePrepareInstanceRequest`.
- The node agent:
  - ensures each template layer is cached (downloading if needed),
  - merges layers into the instance `merged` workspace,
  - applies variable substitution,
  - writes `instance.json` metadata into the instance workspace,
  - calls back to the Brain with `/api/nodes/{nodeId}/instances/{instanceId}/prepared` (includes the node auth header when configured).
- `POST /api/instances/{instanceId}/start` with `NodeInstanceCommandRequest`.
- `POST /api/instances/{instanceId}/stop` with `NodeInstanceCommandRequest`.
- `POST /api/instances/{instanceId}/destroy` with `NodeInstanceCommandRequest`.
- `NodeInstanceCommandRequest` requires `instanceId` and accepts an optional `name` for logging.
- Start/stop/destroy currently acknowledge commands by calling back to the Brain:
  - `/api/nodes/{nodeId}/instances/{instanceId}/running`
  - `/api/nodes/{nodeId}/instances/{instanceId}/stopped`
  - `/api/nodes/{nodeId}/instances/{instanceId}/destroyed`
- `variables` and `variablesJson` are mutually exclusive. When `variablesJson` is provided, the node agent parses it as a JSON map.
- Template cache lookups use `templateId` from the prepare payload as the cache key.

## Edge cases / risks
- Invalid payloads (missing instanceId, empty layers, invalid JSON) return HTTP 400 and trigger a `/failed` callback when possible.
- Cache download/merge failures result in HTTP 500 and a `/failed` callback attempt.
- Missing node auth token or invalid Brain base URL prevents callbacks and fails the prepare request.
- Start/stop/destroy commands are acknowledged via callbacks only; instance runtime control is not implemented yet.

## Links
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/instance/controller/InstanceCommandController.java`
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/instance/service/InstanceLifecycleService.java`
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/instance/service/InstancePrepareService.java`
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/instance/service/InstanceVariablesResolver.java`
- `contracts/nodeapi.yml`
- `docs/brain/node-command-dispatcher.md`
