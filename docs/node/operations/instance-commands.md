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
- Added a container monitor that records exit codes and reasons when containers stop outside of stop/destroy commands.
- Destroy now stops (or force-kills) the container, removes it, deletes the instance workspace, and removes the local registry entry.
- Added a local registry listing endpoint for listing known instance records.
- Added optional retry settings for instance callbacks to the Brain.

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
- `GET /api/instances/registry` to list locally known instance registry entries.
- Registry listing requires the same Brain authentication as other instance command endpoints.
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
  - `/api/nodes/{nodeId}/instances/{instanceId}/destroyed` (after container removal and workspace cleanup)
- Stop uses the container id recorded in `instance.json` and calls Docker to stop the container gracefully.
- Destroy resolves the container id from `instance.json` when available; if missing, it falls back to the container name `kodama-instance-<instanceId>`.
- Destroy removes `instance.json` before deleting the instance workspace directory.
- Registry entries include the instance workspace path (relative to `node-agent.workspace-dir`) and last known container status.
- Registry entries capture the last exit code and exit reason when a container stops.
- Registry listings omit `variables` to avoid leaking runtime secrets.
- Instance callback retries are controlled by `node-agent.instance-callbacks.max-attempts` and
  `node-agent.instance-callbacks.retry-backoff-millis`.
- If the container is still running after the stop timeout, the node agent force-kills it.
- The stop timeout is configured via `node-agent.instance-runtime.stop-timeout-seconds` (defaults to Docker's own timeout when unset).
- The registry container status is updated to `stopped` after a successful stop.
- `variables` and `variablesJson` are mutually exclusive. When `variablesJson` is provided, the node agent parses it as a JSON map.
- Template cache lookups use `templateId` from the prepare payload as the cache key.

## Edge cases / risks
- Invalid payloads (missing instanceId, empty layers, invalid JSON) return HTTP 400 and trigger a `/failed` callback when possible.
- Cache download/merge failures result in HTTP 500 and a `/failed` callback attempt.
- Missing node auth token or invalid Brain base URL prevents callbacks; lifecycle commands still complete locally but the Brain will not receive updates.
- Start requires a container image from `variables` (`DOCKER_IMAGE`, `CONTAINER_IMAGE`, or `IMAGE`) or
  `node-agent.instance-runtime.image`; missing values fail the command.
- Start fails if the prepared workspace or `instance.json` registry record is missing.
- Stop fails if the registry is missing or does not contain a container id.
- If the container is missing at stop time, the node agent logs a warning, marks the registry as stopped, and preserves any
  existing exit metadata (otherwise it records an exit reason of `missing`).
- If the registry, container, or workspace is missing during destroy, the node agent treats it as already removed and continues cleanup.
- If the container disappears or stops between inspect and the Docker stop/kill calls, the node treats it as already stopped.
- If a container stops without a stop command (crash, manual stop), the monitor records the exit code/reason in the local registry.
- If a container is restarted manually, the monitor detects it on the next poll and updates the registry back to `running`.
- Stop errors attempt a `/failed` callback before returning the error.
- Destroy errors attempt a `/failed` callback before returning the error.
- Missing `portsJson` is allowed; port bindings fall back to `PORT`/`PORT_*` variables.
- Invalid or missing port mappings result in a failed start and a `/failed` callback attempt.
- If a success callback (`/prepared`, `/running`, `/stopped`, `/destroyed`) fails after the command completes, the node logs the error but does not send `/failed`.

## Links
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/instance/controller/InstanceCommandController.java`
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/instance/service/InstanceLifecycleService.java`
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/instance/service/InstanceStartService.java`
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/instance/service/InstancePrepareService.java`
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/instance/service/InstanceVariablesResolver.java`
- `contracts/nodeapi.yml`
- `docs/brain/node-command-dispatcher.md`
