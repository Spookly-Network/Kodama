# Instance Commands (Node Agent)

## Purpose
Describe the node agent endpoints that handle instance lifecycle commands from the Brain.

## What changed
- Added an instance prepare command handler that assembles cached template layers into a workspace.
- Added variable substitution and Brain callbacks as part of the prepare flow.

## How to use / impact
- `POST /api/instances/{instanceId}/prepare` with `NodePrepareInstanceRequest`.
- The node agent:
  - ensures each template layer is cached (downloading if needed),
  - merges layers into the instance `merged` workspace,
  - applies variable substitution,
  - calls back to the Brain with `/api/nodes/{nodeId}/instances/{instanceId}/prepared` (includes the node auth header when configured).
- `variables` and `variablesJson` are mutually exclusive. When `variablesJson` is provided, the node agent parses it as a JSON map.
- Template cache lookups use `templateId` from the prepare payload as the cache key.

## Edge cases / risks
- Invalid payloads (missing instanceId, empty layers, invalid JSON) return HTTP 400 and trigger a `/failed` callback when possible.
- Cache download/merge failures result in HTTP 500 and a `/failed` callback attempt.
- Missing node auth token or invalid Brain base URL prevents callbacks and fails the prepare request.

## Links
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/instance/controller/InstanceCommandController.java`
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/instance/service/InstancePrepareService.java`
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/instance/service/InstanceVariablesResolver.java`
- `docs/brain/node-command-dispatcher.md`
