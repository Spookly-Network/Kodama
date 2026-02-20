# Node Agent Flow

This document describes the runtime flow between Brain and node agent for instance lifecycle commands.

## 1. Registration

On startup:
1. Node sends registration to Brain (`/api/nodes/register`).
2. Brain responds with `nodeId` and heartbeat interval.

## 2. Heartbeats

Node sends heartbeat to Brain (`/api/nodes/{nodeId}/heartbeat`) with:
- `status`
- `usedSlots`

`usedSlots` is computed from local registry entries in states `starting`, `running`, and `stopping` by summing `slotsRequired`.

## 3. Prepare Flow

Brain dispatches:
- `POST /api/instances/{instanceId}/prepare`
- payload includes resolved runtime fields (`containerImage`, `installScript`, `startCommand`, `slotsRequired`)
- payload includes port definitions (`portDefinitions`) and template layers (`layers`)

Node steps:
1. Validate payload (`instanceId`, layers, runtime fields where needed).
2. Ensure templates are cached (download if missing/changed).
3. Allocate host ports from each `portDefinitions[*].hostRange`.
4. Merge layers into workspace and apply variable substitution.
5. Persist `instance.json` registry with resolved runtime fields and `portsJson`.
6. Send prepared callback:
   - `POST /api/nodes/{nodeId}/instances/{instanceId}/prepared`
   - optional body: `{"portsJson":"[{\"name\":\"game\",\"protocol\":\"udp\",\"containerPort\":7777,\"hostPort\":14000}]"}`
7. Trigger local start flow.

## 4. Start Flow

Node start steps:
1. Load prepared registry entry and workspace.
2. Validate `containerImage` + `startCommand`.
3. Run `installScript` once when present and not yet completed.
4. Create Docker container with merged workspace mount.
5. Bind ports from `portsJson` array entries (`containerPort` + `hostPort`).
6. Mark local status `starting` then `running`.
7. Send `POST /api/nodes/{nodeId}/instances/{instanceId}/running`.

## 5. Stop and Destroy

Stop:
1. Mark local status `stopping`.
2. Stop Docker container (graceful timeout, then kill if needed).
3. Mark local status `stopped`.
4. Send `POST /api/nodes/{nodeId}/instances/{instanceId}/stopped`.

Destroy:
1. Stop/kill container if present.
2. Remove container and workspace.
3. Remove local registry entry (releases reserved ports).
4. Send `POST /api/nodes/{nodeId}/instances/{instanceId}/destroyed`.

## 6. Failure Handling

- Prepare/start/stop/destroy failures attempt `POST /api/nodes/{nodeId}/instances/{instanceId}/failed`.
- Success callbacks that fail to deliver are logged and retried per callback settings; command outcome is not rolled back.
