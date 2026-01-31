# Instance Commands (Node Agent)

## Purpose
Describe the node agent endpoints that handle instance lifecycle commands from the Brain.

## What changed
- Added an instance prepare command handler that assembles cached template layers into a workspace.
- Added variable substitution and Brain callbacks as part of the prepare flow.
- Added a local instance registry record written after successful preparation.
- Added start/stop/destroy command handlers that send lifecycle callbacks to the Brain.
- Start now creates and starts a Docker container from the prepared workspace and records its container id locally.
- Stop now resolves the container id from the local registry, stops the container, and records the stopped state.

## How to use / impact
- `POST /api/instances/{instanceId}/prepare` with `NodePrepareInstanceRequest`.
- All instance command endpoints require Brain authentication (shared token header or client certificate, depending on node-agent auth configuration).
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
- Start now:
  - reads `instance.json` from the workspace,
  - creates a Docker container with the merged workspace mounted,
  - maps ports from the variable map (and `portsJson` when present; uses `PORT_<NAME>` variables or `PORT` when only one port exists),
  - injects env vars (including `INSTANCE_ID` and `NODE_NAME`),
  - records the container id to the registry,
  - then calls back with `/api/nodes/{nodeId}/instances/{instanceId}/running`.
- The container image defaults to `node-agent.instance-runtime.image` when not provided as `DOCKER_IMAGE`/`CONTAINER_IMAGE`/`IMAGE`.
- The merged workspace is mounted at `node-agent.instance-runtime.workspace-mount-path` and the working directory defaults to the same path.
- Stop/destroy acknowledge commands by calling back to the Brain:
  - `/api/nodes/{nodeId}/instances/{instanceId}/stopped` (after stopping the container locally)
  - `/api/nodes/{nodeId}/instances/{instanceId}/destroyed`
- Stop uses the container id recorded in `instance.json` and calls Docker to stop the container gracefully.
- If the container is still running after the stop timeout, the node agent force-kills it.
- The stop timeout is configured via `node-agent.instance-runtime.stop-timeout-seconds` (defaults to Docker's own timeout when unset).
- The registry container status is updated to `stopped` after a successful stop.
- `variables` and `variablesJson` are mutually exclusive. When `variablesJson` is provided, the node agent parses it as a JSON map.
- Template cache lookups use `templateId` from the prepare payload as the cache key.

## Edge cases / risks
- Invalid payloads (missing instanceId, empty layers, invalid JSON) return HTTP 400 and trigger a `/failed` callback when possible.
- Cache download/merge failures result in HTTP 500 and a `/failed` callback attempt.
- Missing node auth token or invalid Brain base URL prevents callbacks and fails the prepare request.
- Start requires a container image from `variables` (`DOCKER_IMAGE`, `CONTAINER_IMAGE`, or `IMAGE`) or
  `node-agent.instance-runtime.image`; missing values fail the command.
- Start fails if the prepared workspace or `instance.json` registry record is missing.
- Stop fails if the registry is missing or does not contain a container id.
- If the container is missing at stop time, the node agent logs a warning, marks the registry as stopped, and still sends the stop callback.
- If the container disappears or stops between inspect and the Docker stop/kill calls, the node treats it as already stopped.
- Stop errors attempt a `/failed` callback before returning the error.
- Missing `portsJson` is allowed; port bindings fall back to `PORT`/`PORT_*` variables.
- Invalid or missing port mappings result in a failed start and a `/failed` callback attempt.
- If the `/running` callback fails after the container starts, the node logs the error but does not send `/failed`.

## Links
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/instance/controller/InstanceCommandController.java`
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/instance/service/InstanceLifecycleService.java`
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/instance/service/InstanceStartService.java`
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/instance/service/InstancePrepareService.java`
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/instance/service/InstanceVariablesResolver.java`
- `contracts/nodeapi.yml`
- `docs/brain/node-command-dispatcher.md`
