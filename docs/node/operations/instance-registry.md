# Instance Registry (Node Agent)

## Purpose
Persist a local record of instance metadata after the node finishes preparing a workspace.

## What changed
- The prepare flow now writes an `instance.json` registry entry per instance.
- The start flow updates the registry with the Docker container id and status transitions (`starting` -> `running`).
- The start flow now rolls back container runtime fields to a cleared `stopped` state when start cleanup removes a failed container.
- The start flow marks `installCompleted=true` after a successful `installScript` run.
- The stop flow updates the registry with the latest container status.
- The node agent monitors container state changes and records exit codes and reasons when containers stop.
- The destroy flow deletes the `instance.json` registry entry before removing the workspace.
- Registry entries now persist allocated port reservations in `portsJson` (array entries with `name`, `protocol`, `containerPort`, `hostPort`).

## How to use / impact
- Location: `${NODE_AGENT_WORKSPACE_DIR:-./data}/instances/<instanceId>/instance.json`.
- Contents include:
  - instance id, name, display name
  - resolved runtime fields: `containerImage`, `installScript`, `startCommand`, `slotsRequired`
  - ports JSON allocation payload (`portsJson`)
  - install completion status (`installCompleted`)
  - resolved variables map used for substitution
  - template layer list from the prepare request
  - prepared timestamp
  - container id (set after the instance is started)
  - container status and status timestamp (updated on start/stop/monitor)
  - container exit code and exit reason (recorded when containers stop)
- Port reservations are derived from `portsJson` host ports and tracked per protocol (`tcp`/`udp`) while the registry entry exists.
- `portsJson` array entries are preferred for reservation, and legacy object entries are also accepted when each entry includes `hostPort` (and optional `protocol`).
- Variable values are not used as reservation sources.
- Entries missing `portsJson`, array entries without `hostPort`, or legacy object values without `hostPort` do not participate in host-port reservation.
- Prepare serializes `hostPort` allocation and `instance.json` writes through a local reservation lock to avoid duplicate reservations under concurrent requests.
- The registry is overwritten on each successful prepare and updated again when the container id is recorded.
- Install completion is persisted in `installCompleted` and is used to skip repeated install script runs.
- Registry files that predate runtime fields are still readable; missing runtime fields default to:
  - `startCommand: []`
  - `slotsRequired: 1`
  - `installCompleted: false`
  - nullable runtime strings remain `null`
- Container status updates are recorded as lifecycle transitions:
  - start marks `starting` before container startup, then `running` after startup succeeds
  - start cleanup after failure clears `containerId`, records `stopped`, and sets `containerExitReason=start-failed`
  - stop marks `stopping` before Docker stop, then `stopped` after stop completes
- Heartbeat `usedSlots` is computed from local registry entries with status `starting`, `running`, or `stopping`,
  summing `slotsRequired` (defaulting to `1` when missing) and clamping to configured node capacity.
- The instance monitor polls tracked containers (including stopped ones) and reconciles status changes,
  so manual restarts are reflected in the registry.
- Destroy removes the registry entry as part of instance cleanup, which releases the port reservation.

Example `instance.json` fragment:

```json
{
  "instanceId": "04a46594-c578-462c-b2ec-fbbec84cd148",
  "name": "hytale-eu-1",
  "containerImage": "ghcr.io/spookly-network/hytale:latest",
  "slotsRequired": 2,
  "portsJson": "[{\"name\":\"game\",\"protocol\":\"udp\",\"containerPort\":7777,\"hostPort\":14000}]",
  "containerStatus": "running"
}
```

## Edge cases / risks
- If the registry write fails, the prepare request fails and a `/failed` callback is attempted.
- If writing `installCompleted=true` fails after an install script run, start fails before Docker container creation.
- If start fails after writing `starting`, cleanup attempts to remove the container and clear container runtime fields in the registry.
- Missing or invalid workspace paths are treated as preparation failures.
- Corrupt JSON, invalid `hostPort` values, or invalid `protocol` values can block new allocations because reservations can no longer be read safely.
- When containers disappear outside of the node agent, the monitor records a stopped state with an exit reason of
  `missing` only when no exit metadata is already present in the registry.

## Links
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/instance/registry/InstanceRegistryService.java`
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/instance/service/InstancePrepareService.java`
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/instance/service/InstancePortAllocationService.java`
