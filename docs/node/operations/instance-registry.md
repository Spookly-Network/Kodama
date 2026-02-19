# Instance Registry (Node Agent)

## Purpose
Persist a local record of instance metadata after the node finishes preparing a workspace.

## What changed
- The prepare flow now writes an `instance.json` registry entry per instance.
- The start flow updates the registry with the Docker container id.
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
- Port reservations are derived from the `portsJson` host ports and treated as reserved while the registry entry exists.
- Legacy object-form `portsJson` scalar values (for example `{ "game": 25565 }`) are treated as container ports and do not reserve host ports.
- Legacy object-form host reservations are only derived from explicit `hostPort` fields (for example `{ "game": { "hostPort": 30000 } }`).
- Prepare serializes `hostPort` allocation and `instance.json` writes through a local reservation lock to avoid duplicate reservations under concurrent requests.
- The registry is overwritten on each successful prepare and updated again when the container id is recorded.
- Legacy registry files that predate runtime fields are still readable; missing runtime fields default to:
  - `startCommand: []`
  - `slotsRequired: 1`
  - `installCompleted: false`
  - nullable runtime strings remain `null`
- Legacy `allocatedPorts` is still accepted as an alias for `portsJson`.
- Container status updates are recorded when start marks the instance as `running` and stop marks it as `stopped`.
- The instance monitor polls tracked containers (including stopped ones) and reconciles status changes,
  so manual restarts are reflected in the registry.
- Destroy removes the registry entry as part of instance cleanup, which releases the port reservation.

## Edge cases / risks
- If the registry write fails, the prepare request fails and a `/failed` callback is attempted.
- Missing or invalid workspace paths are treated as preparation failures.
- Corrupt `portsJson` values in existing registries can block new allocations because reservations can no longer be read safely.
- When containers disappear outside of the node agent, the monitor records a stopped state with an exit reason of
  `missing` only when no exit metadata is already present in the registry.

## Links
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/instance/registry/InstanceRegistryService.java`
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/instance/service/InstancePrepareService.java`
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/instance/service/InstancePortAllocationService.java`
